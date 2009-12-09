package org.eastmanhouse.ejp;

import com.canto.cumulus.CumulusException;
import com.canto.cumulus.GUID;
import com.canto.cumulus.Item;
import com.canto.cumulus.MultiItemCollection;
import com.canto.cumulus.ejp.AbstractEJP;
import com.canto.cumulus.ejp.EJPContext;
import com.canto.cumulus.ui.event.ActionHandler;
import com.canto.cumulus.ui.Application;
import com.canto.cumulus.ui.Menu;
import com.canto.cumulus.ui.MenuBar;
import com.canto.cumulus.ui.MenuItem;
import com.canto.cumulus.ui.CollectionWindow;
import com.canto.cumulus.ui.MultiItemPane;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
/**
 *
 * @author ryand
 */
public class Truss extends AbstractEJP implements ActionHandler{
  private Logger logger;
  private static FileHandler fh;

  private org.eastmanhouse.ejp.TrussWindow trussWindow;

  private Application application;
  private GUID accessionNumberUndelimited = new GUID("{8e7b8aee-fc2c-b149-aa35-eeac955a1693}");
  private GUID accessionNumberDelimited = new GUID("{e448ad71-fb24-a34e-8291-b2574a79f89b}");

  private GUID title = new GUID("{af4b2e43-5f6a-11d2-8f20-0000c0e166dc}");
  private GUID maker = new GUID("{d252ff13-356c-4940-b599-58704fc92787}");
  private GUID date = new GUID("{0df2b732-9ba7-2f44-b23a-cb07a4b6a0e4}");
  private GUID dimensions = new GUID("{6d093902-671f-7d44-b0c4-0af27f6a4a6d}");
  private GUID medium  = new GUID("{8b094ab9-f251-ad41-b09b-76cc5a0a5a7f}");

  public Truss() {

    super();

    logger = Logger.getLogger("org.eastmanhouse.ejp");
    try {
    fh = new FileHandler("/tmp/log/org.eastmanhouse.ejp.txt");
    fh.setFormatter(new SimpleFormatter());
    
    } catch (Exception e) {
      javax.swing.JOptionPane.showMessageDialog(null, "Can't set logger: " + e.getMessage());
    }

    logger.addHandler(fh);
    logger.setLevel(Level.ALL);

    logger.fine("Launched!");
  }


  public void initialize(EJPContext ejpContext) {
    super.initialize(ejpContext);

    this.application = Application.getInstance();
    String menuLabel = "Truss";
    MenuBar menuBar = application.getMenuBar();

    Menu menu = null;

    try{

        menu = menuBar.getMenu(menuLabel);

    } catch (CumulusException e){

        menu = new Menu(menuLabel);
        menuBar.addMenu(menu);

    }

    MenuItem menuItem = new MenuItem("TMS Import");
    menuItem.setActionHandler(this);
    menuItem.setShortcut('t', com.canto.cumulus.ui.MenuItem.MODIFIER_CTRL);
    menuItem.setActionCommand("import");
    menu.addMenuItem(menuItem);
}


  public String getName(){
    return "Truss";
  }


  public String getID(){
    return "org.eastmanhouse.ejp.Truss";
  }


   public void handleAction(ActionEvent event) {
      Object item = event.getSource(); // item will be either ToolbarButton or MenuItem
      _handleAction(((MenuItem) item).getActionCommand());

   }

   /**
    * Called by handleAction method.
    * The actionCommand String is passed in, letting us know which action to take.
    * @param command - the actionCommand assigned to the component.
    */
   private void _handleAction(String command) {

      if(command.equals("import")) {
         // here we go.
        CollectionWindow currentCollectionWindow = application.getCurrentCollectionWindow();
        MultiItemPane mainPanel = currentCollectionWindow.getMainRecordPane();
        MultiItemCollection currentCollection = mainPanel.getMultiItemCollection();

        int collectionCount = currentCollection.getItemCount();
        ArrayList<String> accessionNumbers = new ArrayList<String>();

        try {
        trussWindow = new TrussWindow(collectionCount);
        } catch (Exception e){

         javax.swing.JOptionPane.showMessageDialog(null, getStackTrace(e));
        }

        trussWindow.setVisible(true);
        trussWindow.toFront();

        for (Item item : currentCollection){

          String accNumUndelim = item.getStringValue(accessionNumberUndelimited);

          //colonize accession number
          String accNumDelim = accNumUndelim.replaceAll("([0-9]{4})([0-9]{4})([0-9]{4})","$1:$2:$3");

          accessionNumbers.add(accNumDelim);
          HashMap map = getData(accNumDelim);

          //javax.swing.JOptionPane.showMessageDialog(null, map.toString());

          String accessionNumberDelimitedStringValue = (String) map.get("accessionNumber");
          String makerStringValue = (String) map.get("artist");
          String titleStringValue = (String) map.get("title");
          String dateStringValue = (String) map.get("date");
          String dimensionsStringValue = (String) map.get("dimensions");
          String mediumStringValue = (String) map.get("medium");

          try {
          item.setStringValue(maker, makerStringValue );
          item.setStringValue(title, titleStringValue );
          item.setStringValue(date, dateStringValue );
          item.setStringValue(dimensions, dimensionsStringValue );
          item.setStringValue(medium, mediumStringValue );
          item.setStringValue(accessionNumberDelimited, accessionNumberDelimitedStringValue);

          item.save();
          } catch (Exception e){
            javax.swing.JOptionPane.showMessageDialog(null, "Can't save items: " + e.getMessage());
          }
          trussWindow.update(1);
        }

      }
   }

      // Counts the number of records selected in the Cumulus client
   private void countSelectedRecords() {
      CollectionWindow col = application.getCurrentCollectionWindow();
   }

     // takes accession number, returns HashMap...
     public static HashMap getData(String accessionNumber) {
    
      HashMap<Object, Object> map = new HashMap<Object,Object>();

      map.put("accessionNumber","[Record Not Found]");
      map.put("title"," ");
      map.put("artist"," ");
      map.put("date"," ");
      map.put("medium"," ");
      map.put("dimensions"," ");

      String URLString = "http://192.168.10.160:8080/getData.php?id=" + accessionNumber;

      URL theURL;
      Document doc;
      DocumentBuilder db;
      DocumentBuilderFactory dbf;

      try {

        theURL = new URL(URLString);

        dbf = DocumentBuilderFactory.newInstance();
        db = dbf.newDocumentBuilder();
        InputStream urlData = theURL.openStream();
        
        doc = db.parse( urlData );

        doc.getDocumentElement().normalize();

        NodeList titleElement = doc.getElementsByTagName("title");
        String title = titleElement.item(0).getTextContent();

        if (title.length() != 0){
          map.put("title",title);
        }

        NodeList accessionNumberElement = doc.getElementsByTagName("accessionNumber");
        String accessionNumberDelimited = accessionNumberElement.item(0).getTextContent();

        if (accessionNumberDelimited.length() != 0){
          map.put("accessionNumber",accessionNumberDelimited);
        }

        NodeList artistElement = doc.getElementsByTagName("artist");
        String artist = artistElement.item(0).getTextContent();


        if (artist.length() != 0){
          map.put("artist",artist);
        }

        NodeList dateElement = doc.getElementsByTagName("date");
        String date = dateElement.item(0).getTextContent();

        if (date.length() != 0){
          map.put("date",date);
        }

        NodeList mediumElement = doc.getElementsByTagName("medium");
        String medium = mediumElement.item(0).getTextContent();

        if (medium.length() != 0){
          map.put("medium",medium);
        }

        NodeList dimensionsElement = doc.getElementsByTagName("dimensions");
        String dimensions = dimensionsElement.item(0).getTextContent();

        if (dimensions.length() != 0){
          map.put("dimensions",dimensions);
        }
        
      } catch (Exception e) {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();

      }

    return map;

  }

   private void logStackTrace(Exception e){
     StackTraceElement elements[] = e.getStackTrace();
     for (int i=0, n=elements.length; i<n; i++) {
        logger.log(Level.WARNING, elements[i].getMethodName());
     }
   }

   private static String getStackTrace(Throwable e){
    final Writer result = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(result);
    e.printStackTrace(printWriter);
    return result.toString();
}
}
