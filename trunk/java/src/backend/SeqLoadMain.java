package backend;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Vector;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.ResultSet;

import backend.Project;
import symap.pool.DatabaseUser;
import util.Cancelled;
import util.ErrorCount;
import util.Logger;
import util.Utilities;

// Load pseudomolecule sequences to SyMAP database.

// Also fix bad chars if any

public class SeqLoadMain 
{
	static int projIdx = 0;
	private static final int CHUNK_SIZE = 1000000; // don't change this or client breaks!!

	
	public static boolean run(UpdatePool pool, Logger log, String projName) throws Exception 
	{
		try
		{
			long startTime = System.currentTimeMillis();
			
			log.msg("Loading sequences for " + projName);
			
			pool.updateSchemaTo40();
			
			String projDir = "data/pseudo/" + projName;
			
			// Load project params file and set defaults for unspecified fields
			SyProps props = new SyProps(log, new File(projDir + "/params"));
			if (props.getProperty("display_name") == null)
				props.setProperty("display_name", projName);
			if (props.getProperty("name") == null)
				props.setProperty("name", projName);
			if (props.getProperty("category") == null)
				props.setProperty("category", "Uncategorized");
//			if (props.getProperty("group") != null && !props.getProperty("group").equals(""))
//				props.setProperty("category", props.getProperty("group"));				
			if (props.getProperty("description") == null)
				props.setProperty("description", "");
	
			// Remove existing project and create new one
			projIdx = pool.getProjIdx(projName, ProjType.pseudo);
			if (projIdx > 0) // delete existing project
				pool.deleteProject(projIdx);
			
			if (pool.projectExists(projName)) {
				log.msg("An FPC project with the same exists, please remove it first\n" +
						"or select a different name for this project.");
				return false;
			}
			
			pool.createProject(projName, ProjType.pseudo);
			projIdx = pool.getProjIdx(projName, ProjType.pseudo);
			props.uploadProjProps(pool, projIdx, 
					new String[] { "name","display_name", "description", 
									"category", "grp_prefix", "grp_sort", "grp_type","order_against",
									"replace_bad_char","min_display_size_bp","mask_all_but_genes","min_size","sequence_files",
									"anno_files"});
	
			Vector<File> seqFiles = new Vector<File>();
			
			// note we still need this dir even if sequences are not in it
			String seqDir = projDir + "/sequence/pseudo";
			Utils.checkCreateDir(new File(projDir + "/sequence"));
			Utils.checkCreateDir(new File(seqDir));
			
			if (!props.containsKey("sequence_files")) props.setProperty("sequence_files", "");
			if (props.getProperty("sequence_files").equals(""))
			{
				// Get list of FASTA sequence files
				
				log.msg("sequence_files not specified - looking in " + seqDir);
				File sdf = new File(seqDir);
				if (sdf.exists() && sdf.isDirectory())
				{
					for (File f2 : sdf.listFiles())
					{
						seqFiles.add(f2);
					}
				}
				else {
					log.msg("Can't find sequence directory " + seqDir);
					ErrorCount.inc();
					return false;
				}
			}
			else
			{
				String[] fileList = props.getProperty("sequence_files").split(",");
				int i = 0;
				for (String filstr : fileList)
				{
					if (filstr == null) continue;
					if (filstr.trim().equals("")) continue;
					File f = new File(filstr);
					if (!f.exists())
					{
						log.msg("Can't find sequence file " + filstr);
					}
					else if (f.isDirectory())
					{
						for (File f2 : f.listFiles())
						{
							seqFiles.add(f2);
							i++;
						}
					}
					else
					{
						seqFiles.add(f);
						i++;
					}
				}
			}
			log.msg("checking sequence files");
			
			// Scan and upload the sequences
	
			String prefix = props.getProperty("grp_prefix");
			boolean autoReplace = (props.getProperty("replace_bad_char").equals("1") ? true : false);
			Vector<String> grpList = new Vector<String>();
			int nSeqs = 0;
			long totalSize = 0;
			int minSize = Integer.parseInt(props.getProperty("min_size"));
			log.msg("Sequences at least " + minSize + " bp will be loaded");
			log.msg("To change this, set the 'min_size' project parameter.");
			for (File f : seqFiles) 
			{
				int nBadCharLines = 0;
				if (f.isFile()) 
				{
					log.msg(f.getName());
					BufferedReader fh = new BufferedReader( new FileReader(f));
					int numHeaders = 0;
					StringBuffer curSeq = new StringBuffer();
					String grpName = null;
					String grpFullName = null;
					while (fh.ready())
					{
						String line = fh.readLine();
						if (Cancelled.isCancelled()) break;
						if (line.startsWith(">"))
						{
							if (grpName != null)
							{
								if (curSeq.length() >= minSize)
								{
									grpList.add(grpName);
									System.out.print(grpName + ":" + curSeq.length() + "               \r");
									uploadSequence(grpName,grpFullName,curSeq.toString(),f.getName(),pool,nSeqs+1);	
									nSeqs++;
									totalSize += curSeq.length();
								}
							}
							grpName = null;
							curSeq.setLength(0);
							numHeaders++;
							if (!prefix.equals("") && !line.startsWith(">" + prefix))
							{
								log.msg("Invalid sequence name " + line);
								log.msg("Name should start with the prefix " + prefix);
								ErrorCount.inc();
								return false;
							}
							grpName = parseGrpName(line,prefix);
							grpFullName = parseGrpFullName(line);
							if (grpName==null || grpFullName==null 
									|| grpName.equals("") || grpFullName.equals(""))
							{
								throw(new Exception("Unable to parse group name from:" + line));
							}

							if (grpName == null) 
							{
								log.msg("Invalid sequence name " + line);
								log.msg("Names can contain letters, numbers, and underscores");
								ErrorCount.inc();
								return false;
							}	
							/*if (grpName.equals("0"))
							{
								log.msg("Invalid group name '0' from line: " + line);
								log.msg("Please change this fasta header and reload the sequences.");
								ErrorCount.inc();
								return false;
							}*/
						}	
						else
						{
							line = line.replaceAll("\\s+",""); 

							if (line.matches(".*[^agctnAGCTN].*"))
							{
								nBadCharLines++;
								//ErrorCount.inc();
								line = line.replaceAll("[^agctnAGCTN]", "N");									
							}

							curSeq.append(line);
						}
					}
				
					if (grpName != null)
					{
						if (curSeq.length() >= minSize)
						{
							grpList.add(grpName);
							uploadSequence(grpName,grpFullName,curSeq.toString(),f.getName(),pool,nSeqs+1);	
							nSeqs++;
							totalSize += curSeq.length();
						}
					}
					if (numHeaders > 1)
					{
						log.msg(f.getName() + ":" + numHeaders + " sequences loaded");
					}
				}
				if (nBadCharLines > 0)
				{
					log.msg(nBadCharLines + " lines contained characters other than AGCT; these will be replaced by N");
				}
				Utils.setProjProp(projIdx,"badCharLines","" + nBadCharLines,pool);
			}
			log.msg(nSeqs + " sequences loaded, total size " + totalSize);
			if (nSeqs >= 2500)
			{
				log.msg("More than 2500 sequences loaded!");
				log.msg("It is recommended to reload with a higher min_size setting, before proceeding");
				ErrorCount.inc();
			}
			if (nSeqs == 0) 
			{
				log.msg("No sequences were loaded!!");
				Utilities.showWarningMessage("No sequences were loaded! Check for problems with the sequence files and re-load.");
				return false;
			}
			updateSortOrder(grpList,pool,props,log);
					
			log.msg("Done:  " + Utilities.getDurationString(System.currentTimeMillis()-startTime) + "\n");
		}
		catch (OutOfMemoryError e)
		{
			log.msg("\n\nOut of memory! To fix, \nA)Make sure you are using a 64-bit computer\nB)Launch SyMAP using the -m option to specify higher memory.\n\n");
			System.out.println("\n\nOut of memory! To fix, \nA)Make sure you are using a 64-bit computer\nB)Launch SyMAP using the -m option to specify higher memory.\n\n");
			System.exit(0);
			return false;
		}
		return true;
	}
	// The prefix must be present. 
	// WMN 7/12: if no prefix, just use the given name.
	// Note, expects the starting ">" as well
	static String parseGrpName(String in, String prefix)
	{
		in = in + " "; // hack, otherwise we need two cases in the regex
		String regx = ">\\s*(" + prefix + ")(\\w+)\\s?.*";
		Pattern pat = Pattern.compile(regx,Pattern.CASE_INSENSITIVE);
		Matcher m = pat.matcher(in);
		if (m.matches())
			return m.group(2);
		return parseGrpFullName(in);
	}
	static String parseGrpFullName(String in)
	{
		String regx = ">\\s*(\\w+)\\s?.*";
		Pattern pat = Pattern.compile(regx,Pattern.CASE_INSENSITIVE);
		Matcher m = pat.matcher(in);
		if (m.matches())
			return m.group(1);
		System.err.println("Unable to parse group name from:" + in);
		return null;
	}
	public static void updateSortOrder(Vector<String> grpList,UpdatePool pool,SyProps props,Logger log) throws Exception
	{
		// First, just set it to the idx order
		int minIdx;
		ResultSet rs = pool.executeQuery("select min(idx) as minidx from groups where proj_idx=" + projIdx);
		rs.first();
		minIdx = rs.getInt("minidx");
		pool.executeUpdate("update groups set sort_order = idx+1-" + minIdx + " where proj_idx=" + projIdx);
		//pool.executeUpdate("update groups set sort_order = 1 where proj_idx=" + projIdx);
		
		//if (!props.getProperty("grp_sort").equals("file"))
		//{
			// Order the groups based on the param file settings
			GroupSorter gs = null;
			if (!props.getProperty("grp_order").equals("")) 
			{
				String[] grpOrder = props.getProperty("grp_order").split(",");
				gs = new GroupSorter(grpOrder);
			}
			else if (props.getProperty("grp_sort").equals("numeric")) 
			{
				for (String grp : grpList)
				{
					try
					{
						Integer.parseInt(grp);
					}
					catch(Exception e)
					{
						log.msg("Group name " + grp + " is not a number; groups will not be sorted.");
						ErrorCount.inc();
						return;
					}
				}
				
				gs = new GroupSorter(GrpSortType.Numeric);
			}
			else if (props.getProperty("grp_sort").startsWith("alpha")) 
			{
				gs = new GroupSorter(GrpSortType.Alpha);
			}
	
			if (gs != null)
			{
				String undef = "";
				for (String grp : grpList)
				{
					if (!gs.orderCheck(grp))
					{
						undef += grp + ", ";
					}
				}
				if (undef.length() > 0) 
				{
					log.msg("Group order not defined for: " + undef.substring(0, undef.length()-2));
					ErrorCount.inc();
					return;
				}
				Collections.sort(grpList,gs);
			}
			for (int i = 1; i <= grpList.size(); i++)
			{
				String grp = grpList.get(i-1);
				pool.executeUpdate("update groups set sort_order=" + i + " where proj_idx=" + projIdx + 
						" and name='" + grp + "'");
			}
		//}

	}
	public static void uploadSequence(String grp, String fullname, String seq, String file,UpdatePool pool,int order) throws Exception
	{
		// First, create the group
		pool.executeUpdate("INSERT INTO groups VALUES('0','" + projIdx + "','" + 
				grp + "','" + fullname + "'," + order + ",'0')" );
		String sql = "select max(idx) as maxidx from groups where proj_idx=" + projIdx;
		ResultSet rs = pool.executeQuery(sql);
		rs.first();
		int grpIdx = rs.getInt("maxidx");
		
		// Now, create the pseudos entry		
		pool.executeUpdate("insert into pseudos (grp_idx,file,length) values(" + grpIdx + ",'" + file + "'," + seq.length() + ")");
		
		// Finally, upload the sequence in chunks
		for (int chunk = 0; chunk*CHUNK_SIZE < seq.length(); chunk++)
		{
			int start = chunk*CHUNK_SIZE;
			int len = Math.min(CHUNK_SIZE, seq.length() - start );
			//System.out.println("chunk " + chunk + " start " + start + " len " + len);
			String cseq = seq.substring(start,start + len );
			String st = "INSERT INTO pseudo_seq2 VALUES('" + grpIdx + "','" + chunk + "','" + cseq + "')";
			pool.executeUpdate(st);
		}
	}


	public static void main(String[] args) 
	{	
		if (args.length < 1) {
			System.out.println("Usage: ..."); 
			return;
		}
		
		try {
			FileInputStream propFH = new FileInputStream(Utils.getParamsName());
			Properties mDBProps = new Properties();
			mDBProps.load(propFH);
			
			String dbstr = DatabaseUser.getDatabaseURL(mDBProps.getProperty("db_server"), mDBProps.getProperty("db_name"));
			UpdatePool pool = new UpdatePool(dbstr, 
					mDBProps.getProperty("db_adminuser"), 
					mDBProps.getProperty("db_adminpasswd"));
			run( pool, new Log("symap.log"), args[0] );
			DatabaseUser.shutdown();
		}
		catch (Exception e) {
			e.printStackTrace();
			DatabaseUser.shutdown();
			System.exit(-1);
		}
	}
}
