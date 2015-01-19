package dotplot;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Component;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import java.applet.Applet;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JComboBox;
import javax.swing.DefaultBoundedRangeModel;

import java.util.Observer;
import java.util.Observable;

import symap.SyMAP;
import symap.frame.HelpBar;
import symap.frame.HelpListener;
import util.Utilities;

@SuppressWarnings("serial") // Prevent compiler warning for missing serialVersionUID
public class ControlPanel extends JPanel implements Observer,
													HelpListener // mdb added 7/6/09
{
    public static final int MIN_DOT_SIZE = 1;
    public static final int MAX_DOT_SIZE = 5;//10 // mdb changed 7/8/09
	
    private Data data;
    private Plot plot;
    private JButton homeButton;
    private JButton minusButton;
    //private JButton minusminusButton;	// mdb removed 3/2/09
    private JButton plusButton;
    //private JButton plusplusButton; 	// mdb removed 3/2/09
    private JButton filterButton;
    private JButton helpButton;
    //private JButton resetButton; 		// mdb removed 5/20/09 - combined with home button
    //private JButton fitButton;		// mdb removed 7/8/09 #164
    private JButton showImageButton;
    //private JButton scaleButton;		// mdb removed 7/8/09 #164
    private JSlider hitSizeSlider;
    private JCheckBox scaleCheckbox; 	// mdb added 7/8/09 #164
    private JLabel referenceLabel;      // mdb added 2/1/10 #210
    private JComboBox referenceSelector;// mdb added 2/1/10 #210
    
    public ControlPanel(final Applet applet, Data d, Plot p, HelpBar hb) {
		this.data = d;
		this.plot = p;
	
		//setBackground(Color.WHITE);
	
		hitSizeSlider = new JSlider(new DefaultBoundedRangeModel(MIN_DOT_SIZE,1,MIN_DOT_SIZE,MAX_DOT_SIZE));
		hitSizeSlider.setBackground(getBackground()); // mdb added 7/8/09
		//hitSizeSlider.setPaintTicks(true);
		hitSizeSlider.setSnapToTicks(true);
		hitSizeSlider.setMajorTickSpacing(1);
		hitSizeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
			    data.setDotSize(hitSizeSlider.getValue());
			}
		});
		hitSizeSlider.setName("Dot Size: Change dot size."); 	// mdb added 1/5/10
		hb.addHelpListener(hitSizeSlider,this); 				// mdb added 1/5/10
	
		//PropertiesReader props = new PropertiesReader(getClass().getResource(DotPlot.DOTPLOT_PROPS)); // mdb removed 1/29/09
	
		helpButton = (JButton) Utilities.createButton(this,"/images/help.gif",
				"Help: Online documentation." + Utilities.getBrowserPopupMessage(applet),
				hb,null,false);

// mdb removed 4/30/09 #162		
//		if (data.isHelpAvailable())
//		    data.enableHelpOnButton(helpButton,"dpcontrolpanel");
//		else
//		    helpButton.setEnabled(false);
		
		// mdb added 5/1/09 #162
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String url = SyMAP.USER_GUIDE_URL + "#dotplot_display";
				if ( !Utilities.tryOpenURL(applet, url) )
					System.err.println("Error opening URL: " + url);
			}
		});
	
		homeButton       = (JButton)  Utilities.createButton(this,"/images/home.gif","Home: Go back to full view.",hb,buttonListener,false);
		homeButton.setEnabled(data.isZoomed());
	
		filterButton     = (JButton)  Utilities.createButton(this,"Filters","Filters: Change filter settings.",hb,buttonListener,false);
		//resetButton      = (JButton)  createButton("/images/reset.gif","Change all settings back to defaults",true,false); // mdb removed 5/20/09 - combined with home button
		//minusminusButton = (JButton)  createButton("/images/minusminus.gif","Decrease the zoom greatly",true,false); // mdb removed 3/2/09
		minusButton      = (JButton)  Utilities.createButton(this,"/images/minus.gif","Shrink: Decrease the scale.",hb,buttonListener,false);
		plusButton       = (JButton)  Utilities.createButton(this,"/images/plus.gif","Grow: Increase the scale.",hb,buttonListener,false);
		//plusplusButton   = (JButton)  createButton("/images/plusplus.gif","Increase the zoom greatly",true,false); // mdb removed 3/2/09
		//fitButton        = (JButton)  createButton("Fit to Window","Change zoom settings back to default",hb,true,false); // mdb removed 7/8/09 #164
		showImageButton  = (JButton)  Utilities.createButton(this,"/images/print.gif",
				"Save: Save as image." + Utilities.getBrowserPopupMessage(applet),
				hb,buttonListener,false);
		//scaleButton      = (JButton)  createButton("/images/scale.gif","Scale y-axis",hb,true,false); // mdb removed 7/8/09 #164
		scaleCheckbox = (JCheckBox) Utilities.createButton(this,"Scale","Scale: Draw to BP scale.",hb,buttonListener,true); // mdb added 7/8/09 #164
		scaleCheckbox.setBackground(getBackground()); // mdb added 7/8/09 #164
		
		// mdb added 2/1/10 #210
		referenceSelector = new JComboBox();
		referenceSelector.addActionListener(buttonListener);
		referenceSelector.setName("Reference: Change reference (x-axis) project.");
		hb.addHelpListener(referenceSelector,this);
		
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		setLayout(gbl);
		gbc.fill = 2;
		gbc.gridheight = 1;
		gbc.ipadx = 5;
		gbc.ipady = 8;
		addToGrid(gbl,gbc,homeButton,1,2);
		//addToGrid(gbl,gbc,resetButton,1,2); 		// mdb removed 5/20/09 - combined with home button
		//addToGrid(gbl,gbc,minusminusButton,1,1); 	// mdb removed 3/2/09
		addToGrid(gbl,gbc,minusButton,1,1);
		addToGrid(gbl,gbc,plusButton,1,1);
		//addToGrid(gbl,gbc,plusplusButton,1,2); 	// mdb removed 3/2/09
		addToGrid(gbl,gbc,scaleCheckbox,1,2); 		// mdb added 7/8/09 #164
		//addToGrid(gbl,gbc,scaleButton,1,1); 		// mdb removed 7/8/09 #164
		//addToGrid(gbl,gbc,fitButton,1,2);			// mdb removed 7/8/09 #164
		addToGrid(gbl,gbc,referenceLabel = new JLabel("Reference:"),1,0);// mdb added 2/1/10 #210
		addToGrid(gbl,gbc,referenceSelector,1,1); 		// mdb added 2/1/10 #210
		addToGrid(gbl,gbc,filterButton,1,2);
		addToGrid(gbl,gbc,new JLabel("Size:"),1,0);
		addToGrid(gbl,gbc,hitSizeSlider,1,5);
		addToGrid(gbl,gbc,showImageButton,1,1);
		addToGrid(gbl,gbc,helpButton,GridBagConstraints.REMAINDER,0);
	
		data.addObserver(this);
    }
    
    private ActionListener buttonListener = new ActionListener() {
	    public void actionPerformed(ActionEvent evt) {
			Object src = evt.getSource();
			if (src == filterButton) Filter.showFilter(data);
			else {
			    if (src == homeButton 
			    		&& data.getNumVisibleGroups() > 2) // mdb added condition 1/14/10 #203 - fix zoom with only two groups
			    {
			    	data.setHome(); 
			    	data.resetAll();
			    }
			    //else if (src == resetButton)      data.resetAll(); // mdb removed 5/20/09 - combined with home button
			    //else if (src == minusminusButton) data.factorZoom(0.8); // mdb removed 3/2/09
			    else if (src == minusButton)      data.factorZoom(0.95);
			    else if (src == plusButton)       data.factorZoom(1/0.95);
			    //else if (src == plusplusButton)   data.factorZoom(1/0.8); // mdb removed 3/2/09
			    else if (src == showImageButton)  data.getSyMAP().getImageViewer().showImage(plot);
			    //else if (src == fitButton)        { data.setZoom(Data.DEFAULT_ZOOM); data.setScale(false); } // mdb removed 7/8/09 #164
			    //else if (src == scaleButton)      data.setScale(true);	// mdb removed 7/8/09 #164
			    else if (src == scaleCheckbox) { // mdb added 7/8/09 #164
			    	if (scaleCheckbox.isSelected())
			    		data.setScale(true);
			    	else {
			    		data.setZoom(Data.DEFAULT_ZOOM); 
			    		data.setScale(false); 
			    	}
			    }
			    else if (src == referenceSelector) {
			    	data.setReference((Project)referenceSelector.getSelectedItem());
			    }
			}
	    }
	};

    public void update(Observable obs, Object obj) { // for Observer interface
    	homeButton.setEnabled(true/*data.isZoomed()*/); // mdb changed 12/1/09
    }
	
    private void addToGrid(GridBagLayout gbl, GridBagConstraints gbc, Component comp, int i, int sep) {
		gbc.gridwidth = i;
		gbl.setConstraints(comp,gbc);
		add(comp);
		if (sep > 0) addToGrid(gbl,gbc,new JLabel(),1,0);
		if (sep > 1) {
		    addToGrid(gbl,gbc,new JSeparator(JSeparator.VERTICAL),1,0);
		    //addToGrid(gbl,gbc,new JLabel(),1,0); // mdb removed 5/20/09
		}
    }
    
    // mdb added 7/6/09
	public String getHelpText(MouseEvent event) { 
		Component comp = (Component)event.getSource();
		return comp.getName();
	}
	
	// mdb added 2/1/10 #210
	public void setProjects(Project[] projects) {
		if (projects == null) {
			referenceLabel.setVisible(false);
			referenceSelector.setVisible(false);
			return;
		}
		
		for (Project p : projects)
			referenceSelector.addItem(p);
		
		// Disable if self-alignment
		if (projects.length == 2 && projects[0].getID() == projects[1].getID())
			referenceSelector.setEnabled(false);
	}
}
