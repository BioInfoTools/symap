package symap;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import blockview.BlockViewFrame;
import circview.CircFrame;
import dotplot.ControlPanel;
import dotplot.Data;
import dotplot.Plot;
import symap.frame.HelpBar;
import symap.frame.SyMAPFrame;
import symap.pool.DatabaseUser;
import symap.projectmanager.common.SumFrame;
import symap.projectmanager.common.SyMAPFrameCommon;
import symapCE.SyMAPExp;
import symapQuery.SyMAPQueryFrame;
import symap.SyMAP;
import symap.SyMAPConstants;
import util.DatabaseReader;
import util.LinkLabel;
import util.Utilities;


@SuppressWarnings("serial") // Prevent compiler warning for missing serialVersionUID
public class SyMAPBrowserApplet extends JApplet implements MouseMotionListener
{
	public static final String DATABASE_URL  = "database";
	public static final String USERNAME      = "username";
	public static final String PASSWORD      = "password";
	public static final String PROJECTS      = "projects";
	public static final String GROUPS      = "groups";
	public static final String USEGRP      = "use_groups";
	public static final String TITLEPROP      = "title";
	
	DatabaseReader db;
	
	TreeMap<String,JCheckBox> name2chk = new TreeMap<String,JCheckBox>();
	TreeMap<String,Integer> name2idx = new TreeMap<String,Integer>();
	TreeMap<String,String> name2grp = new TreeMap<String,String>();
	TreeSet<String> fpcs = new TreeSet<String>();
	
	JButton expBtn, circBtn, dotBtn, sumBtn, blkBtn, qryBtn;
	JPanel helpPanel;
	JTextArea txtHelp;
	HashMap<JButton,String> btnText; // because tooltips come up outside the browser window
	
	static String TITLE="SyMAP Project Browser";
	static String PROJS="<html><b><font size=3>&nbsp;Projects</font></b></html>";
	static String SEQS="<html><b><font size=3>&nbsp;Sequence Projects</font></b></html>";
	static String FPCS="<html><b><font size=3>&nbsp;FPC Projects</font></b></html>";
	
	static String HELP_START="Select one or more projects to begin.";
	static String HELP_EXP="Show the Chromosome Explorer from the selected projects. This also allows you " +
			 " view dotplots and circle plots for subsets of chromosomes, as well as close-up " +
			" alignments.";
	static String HELP_CIRC="Circle plot including all chromosomes of all selected projects.";
	static String HELP_DOT="Dot plot including all chromosomes of all selected projects. " +
			" You can zoom in to selected regions to see a close-up alignment.";
	static String HELP_BLK="Show a map of the synteny blocks for two species. Click to zoom in to " +
			" a close-up view of a particular block.";
	static String HELP_SUM="Show a summary of the alignment between two selected projects.";
	static String HELP_QRY="Show orthologous gene groups, determined by synteny, between two or more species." +
			" You can also perform general queries for annotation keywords, find " +
			" regions of synteny which lack current annotation, or find orphan genes.";
	static String CITATION="Citation:  C. Soderlund, M. Bomhoff, and W. Nelson (2011) " +
			 "SyMAP v3.4: a turnkey synteny system with application to plant genomes " +
			 " Nucleic Acids Research 39(10):e68 ";
	
	boolean ready=false;
	public void init() {
		super.init();

		try 
		{
			SyMAP.printVersion();
				
			// mdb added 4/8/08
			if (!SyMAP.checkJavaSupported(this))
				return;

			// First get the projects they want to show, and retain the order they gave
			String projects = getParameter(PROJECTS);
			Vector<String> projList = new Vector<String>();
			if (projects != null)
			{
				System.out.println("projects:" + projects);
				for (String proj : projects.split(","))
				{
					projList.add(proj.trim().toLowerCase());
				}
			}
			
			// See if they want to show the group headings
			boolean useGrp = (getParameter(USEGRP) != null);
			
			String title = getParameter(TITLEPROP);
			if (title == null) title = TITLE;
			
 			db = getDatabaseReader();
			if (db == null)
			{
				System.err.println("Unable to connect to database using parameters given");
				return;
			}
			ResultSet rs;
			Statement s = db.getConnection().createStatement();
			btnText = new HashMap<JButton,String>();
			
			JPanel toptop = new JPanel();
			toptop.setLayout(new BoxLayout(toptop,BoxLayout.Y_AXIS));
			toptop.setBackground(Color.white);
			toptop.setBorder(null);
			
			JPanel titlePanel = new JPanel();
			titlePanel.setLayout(new BoxLayout(titlePanel,BoxLayout.X_AXIS));
			titlePanel.setBackground(Color.white);
			titlePanel.setBorder(null);
			JLabel lblTitle = new JLabel(title);
			Font curFont = lblTitle.getFont();
			lblTitle.setFont(curFont.deriveFont( Font.BOLD, curFont.getSize() + 4 ) );
			lblTitle.setAlignmentX(CENTER_ALIGNMENT);
			lblTitle.setMaximumSize(lblTitle.getPreferredSize());
			titlePanel.add(Box.createHorizontalGlue());
			titlePanel.add(lblTitle);
			titlePanel.add(Box.createHorizontalGlue());
			
			JPanel top = new JPanel();
			top.setLayout(new BoxLayout(top,BoxLayout.X_AXIS));
			top.setBackground(Color.white);
			top.setBorder(null);
						
			JPanel left = new JPanel();
			left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));
			left.setBackground(Color.white);
			left.setAlignmentX(LEFT_ALIGNMENT);
			left.setAlignmentY(TOP_ALIGNMENT);
			left.setBorder(null);
			
			JPanel right = new JPanel();
			right.setLayout(new BoxLayout(right,BoxLayout.Y_AXIS));
			right.setBackground(Color.white);
			right.setAlignmentY(TOP_ALIGNMENT);
			right.setAlignmentX(LEFT_ALIGNMENT);
			right.setBorder(null);
			right.add(Box.createVerticalStrut(20));

			txtHelp = new JTextArea(HELP_START, 8,20);
			txtHelp.setLineWrap( true );
			txtHelp.setWrapStyleWord( true );
			txtHelp.setEditable(false);
			Border border = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			txtHelp.setBorder(BorderFactory.createCompoundBorder(border, 
		            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

			helpPanel = new JPanel();
			helpPanel.setLayout(new BoxLayout(helpPanel,BoxLayout.Y_AXIS));
			helpPanel.setBackground(Color.white);
			helpPanel.setBorder(null);
			helpPanel.add(Box.createVerticalStrut(20));
			helpPanel.add(txtHelp);
			helpPanel.setAlignmentX(LEFT_ALIGNMENT);
			helpPanel.setAlignmentY(TOP_ALIGNMENT);
			helpPanel.setMaximumSize(helpPanel.getPreferredSize());
			helpPanel.add(Box.createVerticalStrut(30));

			LinkLabel helpLink = new LinkLabel("Click for online help");
	        helpLink.setFont(new Font(helpLink.getFont().getName(), Font.PLAIN, helpLink.getFont().getSize()));
	        helpLink.addMouseListener(new MouseAdapter() 
	        {
				public void mouseClicked(MouseEvent e) 
				{
					try
					{
						Utilities.setCursorBusy(SyMAPBrowserApplet.this, true);
						SyMAPBrowserApplet.this.getAppletContext().showDocument(new URL(SyMAP.USER_GUIDE_URL), "_blank" );
						Utilities.setCursorBusy(SyMAPBrowserApplet.this, false);
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
				}
			});	
	        helpLink.setMaximumSize(helpLink.getPreferredSize());
	        helpLink.setAlignmentX(CENTER_ALIGNMENT);
	        JPanel linkPanel = new JPanel();
	        linkPanel.setLayout(new BoxLayout(linkPanel,BoxLayout.X_AXIS));
	        linkPanel.add(Box.createHorizontalGlue());
	        linkPanel.add(helpLink);
	        linkPanel.add(Box.createHorizontalGlue());
	        
	        helpPanel.add(helpLink);
	        helpPanel.add(Box.createVerticalGlue());

			JTextArea txtCite = new JTextArea(CITATION, 2, 60);			
			txtCite.setLineWrap(true);
			txtCite.setWrapStyleWord(true);
			txtCite.setMaximumSize(txtCite.getPreferredSize());
			txtCite.setAlignmentY(TOP_ALIGNMENT);
			txtCite.setAlignmentX(CENTER_ALIGNMENT);
			txtCite.setEditable(false);
			txtCite.setFont(txtCite.getFont().deriveFont(12.0F));
			
			JPanel citePanel = new JPanel();
			citePanel.setLayout(new BoxLayout(citePanel,BoxLayout.X_AXIS));
			citePanel.setBackground(Color.white);
			citePanel.setBorder(null);
			citePanel.add(Box.createHorizontalGlue());
			citePanel.add(txtCite);
			citePanel.add(Box.createHorizontalGlue());
			
			
			JPanel list = new JPanel();
			list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));
			list.setBackground(Color.white);
			list.setBorder(null);
			
			rs = s.executeQuery("select projects.name, type, value, idx from projects, proj_props" +
					" where proj_props.name='display_name' and proj_props.proj_idx=idx ");
			while (rs.next())
			{
				String name = rs.getString(1).toLowerCase();
				String type = rs.getString(2);
				String dispname = rs.getString(3);
				int idx = rs.getInt(4);
				
				if (projList.size() > 0)
				{
					if (!projList.contains(name))
					{
						continue;
					}
				}
				
				name2idx.put(name, idx);
				JCheckBox chk = new JCheckBox(dispname);
				chk.setBackground(Color.white);
				chk.setSelected(false);
				name2chk.put(name, chk);
				chk.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						enableBtns();
					}
				});

				
				if (type.equals("fpc"))
				{
					fpcs.add(name);
				}
				
			}
			if (fpcs.size() > 0)
			{
				list.add(new JLabel(SEQS));
			}
			else
			{
				list.add(new JLabel(PROJS));				
			}
			list.add(Box.createVerticalStrut(10));
			
			if (projList.size() == 0)
			{
				projList.addAll(name2chk.keySet());
			}
			
			for (String name : projList) // use projList to keep the order specified in tag (if any)
			{
				if (!fpcs.contains(name))
				{
					list.add(name2chk.get(name));
					list.add(Box.createVerticalStrut(5));
				}
			}
			if (fpcs.size() > 0) // if this isn't true the next loop does nothing either
			{
				list.add(Box.createVerticalStrut(5));
				list.add(new JLabel(FPCS));
				list.add(Box.createVerticalStrut(10));
			}
			for (String name : projList)
			{
				if (fpcs.contains(name))
				{
					list.add(name2chk.get(name));
					list.add(Box.createVerticalStrut(5));
				}
			}	
			list.add(Box.createVerticalGlue());
			JScrollPane listS = new JScrollPane(list);
			listS.setBorder(null);
			left.add(listS);
			left.add(Box.createVerticalGlue());
			
			//int listw = Math.min(400,list.getPreferredSize().width+20);
			//left.setPreferredSize(new Dimension(listw,list.getPreferredSize().height + 50));
			//left.setMaximumSize(new Dimension(listw,list.getPreferredSize().height + 50));
			
			expBtn = new JButton("Chromosome Explorer");
			//expBtn.setToolTipText("Launch the Chromosome Explorer (choose one or more species)");
			expBtn.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					new Thread(new Runnable() {
						public void run() {
							expBtn.setBackground(Color.BLUE);
							runExplorer();
							expBtn.setBackground(Color.white);
						}
					}).start();

				}
			});
			expBtn.addMouseMotionListener(this);
			btnText.put(expBtn,HELP_EXP);
			right.add(expBtn);
			right.add(Box.createVerticalStrut(7));
			
			circBtn = new JButton("Circle View");
			circBtn.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					new Thread(new Runnable() {
						public void run() {
							circBtn.setBackground(Color.blue);
							runCircle();
							circBtn.setBackground(Color.white);
						}
					}).start();
				}
			});
			circBtn.addMouseMotionListener(this);
			btnText.put(circBtn,HELP_CIRC);
			right.add(circBtn);
			right.add(Box.createVerticalStrut(7));

			blkBtn = new JButton("Block View");
			blkBtn.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					blkBtn.setBackground(Color.blue);
					runBlocks();
					blkBtn.setBackground(Color.white);
				}
			});
			blkBtn.addMouseMotionListener(this);
			btnText.put(blkBtn,HELP_BLK);
			right.add(blkBtn);
			right.add(Box.createVerticalStrut(7));

			dotBtn = new JButton("Dot Plot View");
			dotBtn.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					new Thread(new Runnable() {
						public void run() {
							dotBtn.setBackground(Color.blue);
							runDots();
							dotBtn.setBackground(Color.white);
						}
					}).start();
				}
			});
			dotBtn.addMouseMotionListener(this);
			btnText.put(dotBtn,HELP_DOT);
			right.add(dotBtn);
			right.add(Box.createVerticalStrut(7));

			qryBtn = new JButton("Ortholog Queries");
			qryBtn.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					new Thread(new Runnable() {
						public void run() {
							qryBtn.setBackground(Color.blue);
							runQuery();
							qryBtn.setBackground(Color.white);
						}
					}).start();
				}
			});
			qryBtn.addMouseMotionListener(this);
			btnText.put(qryBtn,HELP_QRY);
			right.add(qryBtn);
			right.add(Box.createVerticalStrut(7));

			sumBtn = new JButton("Summary");
			sumBtn.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					new Thread(new Runnable() {
						public void run() {
							sumBtn.setBackground(Color.blue);
							runSum();
							sumBtn.setBackground(Color.white);
						}
					}).start();
				}
			});
			right.add(sumBtn);
			btnText.put(sumBtn,HELP_SUM);
			sumBtn.addMouseMotionListener(this);
			right.add(Box.createVerticalStrut(7));
			
			enableBtns();

			right.add(Box.createVerticalGlue());
			//right.setMaximumSize(right.getPreferredSize());
			
			top.add(Box.createHorizontalStrut(10));
			top.add(left);
			top.add(Box.createHorizontalStrut(20));
			top.add(right);
			top.add(Box.createHorizontalStrut(20));
			top.add(helpPanel);
			top.add(Box.createHorizontalGlue());
			top.setAlignmentX(LEFT_ALIGNMENT);
			top.setAlignmentY(TOP_ALIGNMENT);
			
			//titlePanel.setAlignmentX(LEFT_ALIGNMENT);
			toptop.add(titlePanel);
			toptop.add(Box.createVerticalStrut(20));
			
			
			toptop.add(top);
			toptop.add(Box.createVerticalStrut(5));
			
			citePanel.setAlignmentX(LEFT_ALIGNMENT);
			citePanel.setAlignmentY(TOP_ALIGNMENT);
			toptop.add(citePanel);
			toptop.add(Box.createVerticalGlue());
			
			toptop.addMouseMotionListener(this);

			Container cp = getContentPane();
			cp.setBackground(Color.white);
			cp.add(toptop, BorderLayout.PAGE_START);
			
			ready = true;
		
			System.out.println("Initialization done, applet is ready.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean isReady() {
		return ready;
	}

	/**
	 * Method <code>start</code> calls <code>showFrame()</code> if the
	 * applet is to be shown (i.e. show parameter was not set to false).
	 *
	 * @see #showFrame()
	 */
	public void start() {
		//System.out.println("Entering SyMAPApplet.start()");
	}

	public void stop() {
		 //System.out.println("Entering SyMAPApplet.stop()");
	}

	public void destroy() {
		db.close();
		//System.out.println("Entering SyMAPApplet.destroy()");
	}
	public void runSum()
	{
		Vector<Integer> idxs = new Vector<Integer>();
		for (String proj : name2chk.keySet())
		{
			if (name2chk.get(proj).isSelected())
			{
				idxs.add(name2idx.get(proj));
			}
		}
		if (idxs.size() > 2)
		{
			System.err.println("More than two projects selected!");
			return;
		}
		if (idxs.size() == 1)
		{
			idxs.add(idxs.get(0));
		}
		try
		{
			SumFrame frame = new SumFrame(db,idxs.get(0),idxs.get(1));
			frame.setPreferredSize(new Dimension(1100,450));
			frame.setMinimumSize(new Dimension(1100,450));
			frame.toFront();
			frame.setVisible(true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}		
	}
	public void runQuery()
	{
        SyMAPQueryFrame symapQ = new SyMAPQueryFrame(this,db, false);
        
		Utilities.setResClass(this.getClass());
		Utilities.setHelpParentFrame(symapQ);

		for (String proj : name2chk.keySet())
		{
			if (fpcs.contains(proj)) continue;
			if (name2chk.get(proj).isSelected())
			{
				String type = (fpcs.contains(proj) ? "fpc" : "pseudo");
				if (!symapQ.hasProject(proj, type)) 
				{
					if ( !symapQ.addProject(proj, type) ) 
					{
						System.err.println("Failed to add project (" + proj + ") to query page");
						return;
					}
				}
			}
		}
		
		symapQ.build();
		symapQ.toFront();
		symapQ.setVisible(true);
	}
	public void runDots()
	{
		HelpBar helpBar = new HelpBar(-1, 17, true, false, false); 
		
		// Start data download
		Data data = new Data(this);
		Plot plot = new Plot(data, helpBar);
		ControlPanel controls = new ControlPanel(this, data, plot, helpBar);

		Vector<String> projList = new Vector<String>();
		for (String proj : name2chk.keySet())
		{
			if (name2chk.get(proj).isSelected())
			{
				projList.add(proj);
			}
		}

		data.initialize( projList.toArray(new String[0]) );
		controls.setProjects( data.getProjects() ); 

		
		JFrame topFrame = new JFrame();
		//Dimension dpSz = plot.getScrollPane().getPreferredSize();
		//Dimension hpSz = helpBar.getPreferredSize();
		//Dimension size = new Dimension(Math.max(dpSz.width, hpSz.width), dpSz.height + hpSz.height);
		topFrame.setPreferredSize(new Dimension(900,800));
		topFrame.setMinimumSize(new Dimension(900,800));
		topFrame.add(plot.getScrollPane(), BorderLayout.CENTER);
		topFrame.add(helpBar, 			 BorderLayout.SOUTH); 
		topFrame.toFront();
		topFrame.setVisible(true);
	}
	public void runCircle()
	{
		HelpBar helpBar = new HelpBar(-1, 17, true, false, false); 
		TreeSet<Integer> pidxs = new TreeSet<Integer>();
		for (String proj : name2chk.keySet())
		{
			if (name2chk.get(proj).isSelected())
			{
				pidxs.add(name2idx.get(proj));
			}
		}
		int[] pidxList = new int[pidxs.size()];
		int i = 0;
		for (int idx : pidxs)
		{
			pidxList[i] = idx;
			i++;
		}
		CircFrame frame = new CircFrame(null, db, pidxList, null,helpBar); 
		frame.setVisible(true);
		frame.toFront();

	}
	public void runExplorer()
	{
		int numProjects = 0;
		for (String proj : name2chk.keySet())
		{
			if (!name2chk.get(proj).isSelected()) continue;
			numProjects++;
		}
		if (numProjects > SyMAP.MAX_PROJECTS)
		{
			System.err.println("Too many projects selected (max=" + SyMAP.MAX_PROJECTS + ")");
			Utilities.showErrorMessage("Too many projects selected (max=" + SyMAP.MAX_PROJECTS + ")");
			return;
		}
		try
		{
			SyMAPExp symapExp = new SyMAPExp(this,db);
			for (String proj : name2chk.keySet())
			{
				if (!name2chk.get(proj).isSelected()) continue;
				String type = (fpcs.contains(proj) ? "fpc" : "pseudo");
				if (!symapExp.hasProject(proj, type)) 
				{
					if ( !symapExp.addProject(proj, type) ) 
					{
						return;
					}
				}
			}
			SyMAPFrameCommon frame = symapExp.getFrame();
			symapExp.build();
			frame.build();
			frame.setVisible(true);
			frame.toFront();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	void runBlocks()
	{
		Vector<Integer> idxs = new Vector<Integer>();
		for (String proj : name2chk.keySet())
		{
			if (name2chk.get(proj).isSelected())
			{
				idxs.add(name2idx.get(proj));
			}
		}
		if (idxs.size() > 2)
		{
			System.err.println("More than two projects selected!");
			return;
		}
		if (idxs.size() == 1)
		{
			idxs.add(idxs.get(0));
		}
		try
		{
			BlockViewFrame bvframe = new BlockViewFrame(db,idxs.get(0),idxs.get(1));
			bvframe.toFront();
			bvframe.setVisible(true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}

	}
	void enableBtns()
	{
		int nTot = 0;
		int nSeq = 0;
		int nFPC = 0;
		for (String name : name2chk.keySet())
		{
			if (name2chk.get(name).isSelected())
			{
				nTot++;
				if (fpcs.contains(name)) nFPC++;
				else nSeq++;
			}
		}
		expBtn.setEnabled(false);
		circBtn.setEnabled(false);
		dotBtn.setEnabled(false);
		sumBtn.setEnabled(false);
		blkBtn.setEnabled(false);
		qryBtn.setEnabled(false);
		if (nTot > 0)
		{
			expBtn.setEnabled(true);
			dotBtn.setEnabled(true);
			circBtn.setEnabled(true);
		}
		if (nSeq > 1)
		{
			qryBtn.setEnabled(true);
			
		}
		if (nTot == 2)
		{
			sumBtn.setEnabled(true);
			if (nSeq > 0)
			{
				blkBtn.setEnabled(true);
			}
		}
		
	}
		private DatabaseReader getDatabaseReader() {
			return DatabaseUser.getDatabaseReader(SyMAPConstants.DB_CONNECTION_SYMAP_APPLET,
				getParameter(DATABASE_URL),
				getParameter(USERNAME),
				getParameter(PASSWORD),
				Utilities.getHost(this));
	}

	public void mouseMoved(MouseEvent e)   
	{ 
		Object src = e.getSource();
		if (src instanceof JButton)
		{
			txtHelp.setText(btnText.get(((JButton)src)));
		}
		else
		{
			txtHelp.setText(HELP_START);			
		}
	}
	public void mouseDragged(MouseEvent e)   
	{ 
	}
	
}
