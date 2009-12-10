package org.eastmanhouse.ejp;

import com.canto.cumulus.GUID;
import com.canto.cumulus.Item;
import com.canto.cumulus.MultiItemCollection;
import com.canto.cumulus.ui.Application;
import com.canto.cumulus.ui.CollectionWindow;
import com.canto.cumulus.ui.MultiItemPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
/**
 *
 * @author ryand
 */
public class TrussWorker extends Thread {

  private Application application;

  private JProgressBar progressBar;
  private JFrame window;

  private int numberDone;
  private int numberItems;

  private GUID accessionNumberUndelimited = new GUID("{8e7b8aee-fc2c-b149-aa35-eeac955a1693}");
  private GUID accessionNumberDelimited = new GUID("{e448ad71-fb24-a34e-8291-b2574a79f89b}");

  private GUID title = new GUID("{af4b2e43-5f6a-11d2-8f20-0000c0e166dc}");
  private GUID maker = new GUID("{d252ff13-356c-4940-b599-58704fc92787}");
  private GUID date = new GUID("{0df2b732-9ba7-2f44-b23a-cb07a4b6a0e4}");
  private GUID dimensions = new GUID("{6d093902-671f-7d44-b0c4-0af27f6a4a6d}");
  private GUID medium  = new GUID("{8b094ab9-f251-ad41-b09b-76cc5a0a5a7f}");


  public TrussWorker(Application application) {


    super();

    this.application = application;

    try {
    System.setErr(new java.io.PrintStream(new java.io.FileOutputStream("/tmp/mystderr")));
    } catch (Exception e){
       System.err.println("Toasted.");
    }

    System.err.println("Started.");
    

  }

  @Override
  public void run(){
    System.err.println("Running");
    initWindow();
  }

  private MultiItemCollection getSelection(){
        CollectionWindow currentCollectionWindow = application.getCurrentCollectionWindow();
        MultiItemPane mainPanel = currentCollectionWindow.getMainRecordPane();
        MultiItemCollection currentCollection = mainPanel.getMultiItemCollection();

        return currentCollection;

  }


  public void initWindow(){
      // Set sizes, location, titles, close operation, colors
      window = new JFrame("Importing Data...");
      window.setTitle("Truss Building...");
      window.setSize(384, 120);
      window.setBackground(Color.lightGray);
      window.setLocation(0, 75);
      window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      numberDone = 0;

      JLabel label=new JLabel("Importing Data from TMS...");

      progressBar = new JProgressBar(0, numberItems);
      progressBar.setStringPainted(true);
      progressBar.setPreferredSize(new Dimension(360,20));

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));

      panel.add(label);
      panel.add(progressBar);

      window.getContentPane().add(panel);
      window.setVisible(true);
      window.toFront();
      
      importData(application);
   }
  
  private void importData(Application application) {

          System.err.println("ImportData running...");
         // here we go.
         ArrayList<String> accessionNumbers = new ArrayList<String>();


         MultiItemCollection currentCollection = getSelection();
         numberItems = currentCollection.getItemCount();

        for (Item item : currentCollection){

          String accNumUndelim = item.getStringValue(accessionNumberUndelimited);

          System.err.println("Processing " + accNumUndelim);

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
            System.err.println("Can't save items: " + e.getMessage());
          }
          updateProgress();
        }

      }

  private void updateProgress(){
    int currentNumber = progressBar.getValue();
    int newNumber = currentNumber + 1;

    progressBar.setValue(newNumber);

    System.err.println(">" + newNumber);

    if (progressBar.getValue() == progressBar.getMaximum()){
      shutWindow();
    }
    
  }

  private void shutWindow(){
       progressBar.setValue(progressBar.getMinimum());
       progressBar.setString("");
       window.setVisible(false);
  }

  // takes accession number, returns HashMap...
  public static HashMap getData(String accessionNumber) {
          System.err.println("getData running...");

    
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

        System.err.println(e.getMessage());

      }

    return map;

  }



}
