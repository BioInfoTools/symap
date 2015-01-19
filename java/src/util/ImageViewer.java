package util;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
import javax.swing.JFileChooser;
import javax.imageio.ImageIO;
//import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.freehep.graphicsbase.util.export.ExportDialog;
import org.freehep.graphicsbase.util.export.ExportDialog;
import org.freehep.graphicsio.emf.EMFExportFileType;
import org.freehep.graphicsio.java.JAVAExportFileType;
import org.freehep.graphicsio.pdf.PDFExportFileType;
import org.freehep.graphicsio.ps.PSExportFileType;
import org.freehep.graphicsio.ps.EPSExportFileType;
import org.freehep.graphicsio.svg.SVGExportFileType;
import org.freehep.graphicsio.swf.SWFExportFileType;

public class ImageViewer {

    public ImageViewer() { }

    /**
     * Method <code>showImage</code> prompts the user to save the file to disk
     * and then opens the image in a web browser window. The url of the new image is returned.
     *
     * @param comp a <code>Component</code> value of the component to be made into an image
     * @return an <code>URL</code> value of the URL of the new image or null if there was any problems or the image was not saved.
     */


    public static void showImage(Component comp) {
		try
		{
	        ExportDialog export = new ExportDialog();
            //export.addExportFileType(new EMFExportFileType());
            //export.addExportFileType(new JAVAExportFileType());
            export.addExportFileType(new PDFExportFileType());
            export.addExportFileType(new EPSExportFileType());
            export.addExportFileType(new PSExportFileType());
            export.addExportFileType(new SVGExportFileType());
            //export.addExportFileType(new SWFExportFileType());
                           

	        export.showExportDialog( comp, "Export view as ...", comp, "export" );
		    System.err.println("Image save complete");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
    }
 
    public URL showImageOld(Component comp) {
		URL ret = null;
		BufferedImage bimg = createImage(comp);
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new ImageFilter());
		if (fc.showSaveDialog(comp) == JFileChooser.APPROVE_OPTION) {
		    File f = fc.getSelectedFile();
		    if (f != null) {
				try {
				    ImageIO.write(bimg,ImageFilter.getFormatName(f),f);
				    if (Utilities.openURL(f.toURL().toString(),true))
					ret = f.toURL();
				}
				catch (Exception e) {
				    e.printStackTrace();
				}
		    }
		}
		return ret;
    }
    /**
     * Method <code>getImage</code> returns the image object
     *
     * @param url an <code>URL</code> value of the url of the image
     * @return an <code>Image</code> value
     */
    public Image getImage(URL url) {
    	return Toolkit.getDefaultToolkit().getImage(url);
    }

    /**
     * Method <code>getIcon</code> returns an icon.
     *
     * This method is equivalent to <code>new ImageIcon(getImage(url))</code>
     * 
     * @param url an <code>URL</code> value
     * @return an <code>Icon</code> value
     */
    public Icon getIcon(URL url) {
		Image image = getImage(url);
		if (image != null) return new ImageIcon(image);
		return null;
    }
    
    // mdb added 1/29/08
	public static ImageIcon getImageIcon(String strImagePath) {
	    java.net.URL imgURL = ImageViewer.class.getResource(strImagePath);
	    if (imgURL != null)
	    	return new ImageIcon(imgURL);
	    else {
	    	//System.err.println("Couldn't find icon: "+strImagePath);
	    	return null;
	    }
	}

    public static Image getImage(Applet applet, URL url) {
		if (applet != null) {
		    AppletContext context = applet.getAppletContext();
		    Image image = null;
		    if (url != null && context != null) {
		    	image = context.getImage(url);
		    }
		    return image;
		}
		return Toolkit.getDefaultToolkit().getImage(url);
    }

    public static Icon getIcon(Applet applet, URL url) {
		Image image = getImage(applet,url);
		if (image != null) return new ImageIcon(image);
	    return null;
    }

    /**
     * Method <code>createImage</code> creates an image from the component <code>comp</code>.
     *
     * @param comp a <code>Component</code> value
     * @return a <code>BufferedImage</code> value
     */
    public static BufferedImage createImage(Component comp) {
		Dimension size = comp.getSize();
		BufferedImage bimg = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		//BufferedImage bimg = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = bimg.createGraphics();
	        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setBackground(comp.getBackground());
		g2.setPaint(comp.getBackground());
		g2.fill(new Rectangle(0,0,size.width,size.height));
		comp.paint(g2);
		return bimg;
    }

    private static class ImageFilter extends FileFilter {
		private static final String[] EXTENSIONS = {"jpeg","jpg"}; //,"gif","tiff","tif","png"};
		private static final String[] EXT_FORMAT = {"jpeg","jpeg"}; //,"gif","tiff","tiff","png"};
	
		public boolean accept(File f) {
		    return f.isDirectory() || Utilities.contains(EXTENSIONS,Utilities.getFileExtension(f));
		}
	
		public String getDescription() {
		    return "JPEG Image";
		}
	
		public static String getFormatName(File file) {
		    return getFormatName(Utilities.getFileExtension(file));
		}
	
		public static String getFormatName(String extension) {
		    for (int i = 0; i < EXTENSIONS.length; i++)
			if (EXTENSIONS[i].equals(extension)) return EXT_FORMAT[i];
		    return "jpeg"; // default
		}
    }
}
