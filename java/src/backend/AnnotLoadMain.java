package backend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Comparator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import symap.pool.DatabaseUser;
import util.Cancelled;
import util.ErrorCount;
import util.Logger;
import util.Utilities;

public class AnnotLoadMain 
{
	private Logger log;
	private UpdatePool pool;
	private SyProps props;
	private Project project;
	private int projIdx;
	private String projDir;
	private TreeMap<String,Integer> typeCounts;
	private TreeSet<String> typesToLoad;
	private TreeSet<String> geneNames;
	private static Properties mDBProps = null;
	
	public AnnotLoadMain(UpdatePool pool, Logger log, Properties props) {
		this.log = log;
		this.pool = pool;
		if (props != null) mDBProps = props;
}
	
	public static void main(String[] args) 
	{
		try {
			if (args.length < 1) {
				System.out.println("Usage:  annotation <project>\n");
				System.exit(-1);
			}
			
			FileInputStream propFH = new FileInputStream(Utils.getParamsName());
			mDBProps = new Properties();
			mDBProps.load(propFH);
	
			String dbstr = DatabaseUser.getDatabaseURL(mDBProps.getProperty("db_server"), mDBProps.getProperty("db_name"));
			UpdatePool pool = new UpdatePool(dbstr,
					mDBProps.getProperty("db_adminuser"),
					mDBProps.getProperty("db_adminpasswd"));
	
			AnnotLoadMain annot = new AnnotLoadMain(pool, new Log("symap.log"), null);
			annot.run(args[0]);
			DatabaseUser.shutdown();
		}
		catch (Exception e) {
			e.printStackTrace();
			DatabaseUser.shutdown();
			System.exit(-1);
		}
	}
	
	public boolean run(String projName) throws Exception {
		long startTime = System.currentTimeMillis();
		
		log.msg("Loading annotation for " + projName);

		pool.updateSchemaTo40();

		// Initialize local data
		typeCounts = new TreeMap<String,Integer>();
		typesToLoad = new TreeSet<String>();
		geneNames = new TreeSet<String>();
		projDir = "data/pseudo/" + projName;
		props = new SyProps(log, new File(projDir + "/params"), mDBProps);
		project = new Project(pool, log, props, projName, ProjType.pseudo, QueryType.Either);
		projIdx = project.getIdx();
		
		// Parse user-specified types
		String annot_types = props.getProperty("annot_types");
		Integer keywordMin = props.getInt("annot_kw_mincount");
		String annotKW = props.getString("annot_keywords");
		
		// mdb added 8/5/09 - if no types specified then limit to types we render
		if (annot_types == null || annot_types.length() == 0) 
			annot_types = "gene,exon,frame,gap,centromere";
		
		if (annot_types != null && !annot_types.equals(""))
		{
			String [] ats = annot_types.split("\\s*,\\s*");
			for (String at : ats)
			{
				at = at.trim();
				if (at.length() > 0)
					typesToLoad.add(at);
			}
		}

		String annotDir = projDir + "/annotation";
		Vector<File> annotFiles = new Vector<File>();

		if (!props.containsKey("anno_files")) props.setProperty("anno_files", "");

		if (props.getProperty("anno_files").equals(""))
		{
			// Check for annotation directory
			log.msg("anno_files not specified - looking in " + annotDir);
			File ad = new File(annotDir);
			if (!ad.isDirectory()) {
				log.msg("No annotation files provided");
				return true; // this is not considered an error
			}
			for (File f2 : ad.listFiles())
			{
				annotFiles.add(f2);
			}

		}
		else
		{
			String[] fileList = props.getProperty("anno_files").split(",");
			int i = 0;
			for (String filstr : fileList)
			{
				if (filstr == null) continue;
				if (filstr.trim().equals("")) continue;
				File f = new File(filstr);
				if (!f.exists())
				{
					log.msg("Can't find annotation file " + filstr);
				}
				else if (f.isDirectory())
				{
					for (File f2 : f.listFiles())
					{
						annotFiles.add(f2);
						i++;
					}
				}
				else
				{
					annotFiles.add(f);
					i++;
				}
			}
		}
		
		
		
		// Load annotation files
		deleteCurrentAnnotations();
		pool.executeUpdate("delete from pairs where proj1_idx=" + projIdx + " or proj2_idx=" + projIdx);
		//pool.executeUpdate("update pairs set aligndate=null,aligned=0 where proj1_idx=" + projIdx + " or proj2_idx=" + projIdx);
		
		int nFiles = 0;
		final TreeMap<String,Integer> keywords = new TreeMap<String,Integer>();
		
		for (File af : annotFiles)
		{
			if (!af.isFile()) continue;
			nFiles++;
			loadFile(af,keywords);
			if (Cancelled.isCancelled()) break;
		}
		log.msg(nFiles + " file(s) loaded");
				
		// Print annotation counts
		log.msg("Types (* indicates not loaded):");
		for (String type : typeCounts.keySet())
			log.msg("   " + type + " " + typeCounts.get(type) + (!typesToLoad.contains(type) ? "*" : ""));
		
		Vector<String> sortedKeys = new Vector<String>();
		sortedKeys.addAll(keywords.keySet());
		Collections.sort(sortedKeys, new Comparator<String>()
        {
	        public int compare(String o1, String o2)
	        {
	                return keywords.get(o2).compareTo(keywords.get(o1));
	        }
        });
		if (annotKW.equals(""))
		{
			log.msg("Keywords identified (with count > " + keywordMin + ")");
	        pool.executeUpdate("delete from annot_key where proj_idx=" + projIdx);
	        for (String key : sortedKeys)
	        {
	        	int count = keywords.get(key);
	        	if (count < keywordMin) break;
	        	log.msg(key + ":" + count);
	        	pool.executeUpdate("insert into annot_key (proj_idx,keyname,count) values (" + 
	        			projIdx + ",'" + key + "'," + count + ")");
	        }
		}
		else
		{
			pool.executeUpdate("delete from annot_key where proj_idx=" + projIdx);
			log.msg("Loading user-specified annotation keywords");
			String[] kws = annotKW.trim().split(",");
			for (String kw : kws)
			{
				kw = kw.trim();
				int count = (keywords.containsKey(kw) ? keywords.get(kw) : 0);
				if (count == 0)
				{
					log.msg("WARNING:" + kw + " was not found in annotation files");
					ErrorCount.inc();
				}
	        	pool.executeUpdate("insert into annot_key (proj_idx,keyname,count) values (" + 
	        			projIdx + ",'" + kw + "'," + count + ")");
				
			}
			
		}

		pool.executeUpdate("update projects set hasannot=1,annotdate=NOW() where idx=" + projIdx);
		
		// Release data from heap
		geneNames = null; 
		typesToLoad = null;
		typeCounts = null;
		System.gc(); // Java treats this as a suggestion
		
		log.msg("Done:  " + Utilities.getDurationString(System.currentTimeMillis()-startTime) + "\n");
		
		return true;
	}
	class ValueComparator implements Comparator
	{

		  TreeMap<String,Integer> base;
		  public ValueComparator(TreeMap<String,Integer> base) 
		  {
		      this.base = base;
		  }
		  public int compare(Object a, Object b) {

			    if((Integer)base.get(a) < (Integer)base.get(b)) {
			      return 1;
			    } else if((Integer)base.get(a) == (Integer)base.get(b)) {
			      return 0;
			    } else {
			      return -1;
			    }
			  }

	}	
	
	private void loadFile(File f,TreeMap<String,Integer> keywords) throws Exception
	{
		int totalLoaded = 0;
		boolean forceLoadAll = props.getBoolean("all_annot"); // mdb added 10/5/09
		
		log.msg("Loading " + f.getName());
		Vector<String> vals = new Vector<String>();
		vals.add(0,"");
		vals.add(1,"");
		vals.add(2,"");
		vals.add(3,"");
		vals.add(4,"");
		vals.add(5,"");
		vals.add(6,"");
	
		BufferedReader fh = new BufferedReader( new FileReader(f));
		String line;
		int lineNum = 0;
		int numParseErrors = 0;
		int numIDErrors = 0;
		int numGrpErrors = 0;
		final int MAX_ERROR_MESSAGES = 5;
		
		PreparedStatement ps = pool.prepareStatement("insert into pseudo_annot (grp_idx,type,name,start,end,strand,text) values (?,?,?,?,?,?,'')"); // 
		while ((line = fh.readLine()) != null)
		{
			if (Cancelled.isCancelled()) break;
			lineNum++;
			if (line.startsWith("#")) continue; // skip comment
			
			String[] fs = line.split("\t");
			if (fs.length < 9) {
				ErrorCount.inc();
				if (numParseErrors++ < MAX_ERROR_MESSAGES) {
					log.msg("Parse error: expecting at least 9 tab-delimited fields in gff file at line " + lineNum);
					if (numParseErrors >= MAX_ERROR_MESSAGES)
						log.msg("(Suppressing further messages of this type)");
				}
				continue; // skip this annotation
			}
			String chr 		= fs[0];
			String type1 	= fs[1];
			String type 	= pool.sqlSanitize(fs[2]);
			int start		= Integer.parseInt(fs[3]);
			int end 		= Integer.parseInt(fs[4]);
			String strand	= fs[6];
			String attr		= fs[8];
			
			if (type.equals("CDS")) type = "exon";
			if (type.contains("RNA")) type = "gene";
			
			// mdb: need better verification for all fields here
			if (strand == null || (!strand.equals("-") && !strand.equals("+")))
				strand = "+";
			if (type == null || type.length() == 0)
				type = "unknown";
			else if (type.length() > 20)
				type = type.substring(0, 20);
			
			// RepeatMasker doesn't follow the gff standard
			if (type1.equals("RepeatMasker") && type.equals("similarity"))
            	type = "RepeatMasker";
			
			// mdb added 8/14/09 - reduce memory usage
			type = type.intern();
			strand = strand.intern();
			
			if (!typeCounts.containsKey(type))
				typeCounts.put(type, 1);
			else
				typeCounts.put(type, 1 + typeCounts.get(type));
			
			// Discard unsupported types as specified in project params file.
			if (typesToLoad.size() > 0 &&  !typesToLoad.contains(type)) 
				continue; // skip this annotation
			
			int grpIdx = project.grpIdxFromQuery(chr);
			if (grpIdx < 0) {
				ErrorCount.inc();
				if (numGrpErrors++ < MAX_ERROR_MESSAGES) {
					log.msg("Could not determine chromosome/group for " + chr + " on line " + lineNum);
					if (numGrpErrors >= MAX_ERROR_MESSAGES)
						log.msg("(Suppressing further messages of this type)");
				}
				continue; // skip this annotation
			}
			
			// Swap coordinates so that start is always less than end.
			if (start > end) {
				int tmp = start;
				start = end;
				end = tmp;
			}
			
			// Don't load exon descriptions as they are the same as the 
			// corresponding gene descriptions.
			if (type == "exon") 
				attr = "";
			
			// 	IF CHANGED, CHANGE CODE IN QUERY PAGE PARSING ALSO
			String[] fields = attr.split(";");
			for (String field : fields)
			{
				String[] words = field.trim().split("[\\s,=]");
				String key = words[0].trim();
				if (key.equals("")) 
				{
					continue;
				}
				if (key.endsWith("="))
				{
					key = key.substring(0,key.length() - 1);
				}
				if (!keywords.containsKey(key))
				{
					keywords.put(key, 0);
				}
				keywords.put(key, 1 + keywords.get(key));
			}
			// Load annotation into database
			
			ps.setInt(1,grpIdx);
			ps.setString(2,type);
			ps.setString(3,attr);
			ps.setInt(4,start);
			ps.setInt(5,end);
			ps.setString(6,strand);
			
/*			vals.set(0, String.valueOf(grpIdx));
			vals.set(2, pool.sqlSanitize(attr)); // mdb changed 7/15/09
			vals.set(1, type);
			vals.set(3, String.valueOf(start));
			vals.set(4, String.valueOf(end));
			vals.set(5, strand);
			vals.set(6, ""); // mdb changed 7/15/09
			pool.bulkInsertAdd("pseudo_annot", vals);*/
			ps.addBatch();
			totalLoaded++;
			if (totalLoaded % 10000 == 0) ps.executeBatch();
			if ((totalLoaded % 10000) == 0)
				log.msg(totalLoaded + " annotations");
		}
		if (totalLoaded % 10000 != 0) ps.executeBatch();
		//pool.finishBulkInserts();
		log.msg(totalLoaded + " annotations loaded from " + f.getName() 
				+ ", " + (numParseErrors+numGrpErrors+numIDErrors) + " discarded");
		fh.close();
	}
	
	private void deleteCurrentAnnotations() throws SQLException
	{
		String st = "DELETE FROM pseudo_annot USING pseudo_annot,groups " +
					" WHERE groups.proj_idx='" + projIdx + 
					"' AND pseudo_annot.grp_idx=groups.idx";
		pool.executeUpdate(st);
	}
	

	// mdb added 5/12/09
	private static void handleSeqChunk(
			String seq, int start, int end, // chunk data and coordinates within pseudomolecule
			Vector<FASTASequence> genes,
			BufferedWriter pfh) 
	throws Exception
	{
		char[] output = new char[seq.length()];
		Arrays.fill(output, 'N');
		
		Vector<FASTASequence> genesToRemove = new Vector<FASTASequence>();
		
		for (FASTASequence g : genes) {
			if (g.start <= end) { // this gene is in this chunk
				int geneStart = Math.max(0, g.start - start);
				int geneEnd = Math.min(seq.length(), g.end - start);
				
				for (int i = geneStart; i < geneEnd; i++) // copy in gene
					output[i] = seq.charAt(i);
				
				if (g.end <= end)
					genesToRemove.add(g);
				// else there is still more to do on this gene in a later chunk
			}
		}
		
		// Write gene-masked file
		Utils.fastaPrint(pfh, "", output);	

		// Remove processed genes
		genes.removeAll(genesToRemove);
	}
	


}
