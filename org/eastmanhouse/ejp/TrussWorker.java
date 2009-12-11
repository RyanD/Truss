package org.eastmanhouse.ejp;

import com.canto.cumulus.GUID;
import com.canto.cumulus.Item;
import com.canto.cumulus.MultiItemCollection;
import com.canto.cumulus.ui.Application;
import com.canto.cumulus.ui.CollectionWindow;
import com.canto.cumulus.ui.MultiItemPane;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author ryand
 */
public class TrussWorker {

  private static final TrussWorker INSTANCE = new TrussWorker();

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
  private GUID medium = new GUID("{8b094ab9-f251-ad41-b09b-76cc5a0a5a7f}");
  protected boolean isRunning = false;
  /**
   * Constructor.
   */
  private TrussWorker() {

    super();

    this.application = com.canto.cumulus.ui.Application.getInstance();

    try {
      System.setErr(new java.io.PrintStream(new java.io.FileOutputStream("/tmp/mystderr")));
    } catch (Exception e) {
      System.err.println("Toasted.");
    }

    System.err.println("Starting...");
    
    initWindow();

  }
  /**
   * Inits the window.
   */

   public static TrussWorker getInstance() {
      return INSTANCE;
   }

  protected void initWindow() {
    // Set sizes, location, titles, close operation, colors
    window = new JFrame("Importing Data...");
    window.setSize(384, 120);
    window.setBackground(Color.lightGray);
    window.setLocation(0, 75);
    window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

    JLabel label = new JLabel("Importing Data from TMS...");

    progressBar = new JProgressBar(0, numberItems);
    progressBar.setStringPainted(true);
    progressBar.setPreferredSize(new Dimension(360, 20));
    progressBar.setMaximum(100);
    progressBar.setValue(0);

    JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));

    panel.add(label);
    panel.add(progressBar);

    window.getContentPane().add(panel);
  }

  /**
   * Begins importing
   */

  protected void finishImport(){
    isRunning = false;
    window.setVisible(false);
    progressBar.setValue(0);
    window.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    System.err.println("Done.");
  }

  protected void beginImport() {

    if (!isRunning){
      isRunning = true;

      window.setVisible(true);
      window.toFront();
      window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

      SwingWorker worker = new SwingWorker() {

        @Override
        protected Object clone() throws CloneNotSupportedException {
          return super.clone();
        }

        @Override
        protected void done() {
          finishImport();
        }

        @Override
        protected Object doInBackground() throws Exception {
          importData(application);
          return null;
        }

        /**
         * Function that actually controls the import process
         */
        protected void importData(Application application) {

          System.err.println("ImportData running...");
          // here we go.
          ArrayList<String> accessionNumbers = new ArrayList<String>();


          MultiItemCollection currentCollection = getSelection();
          numberItems = currentCollection.getItemCount();
          numberDone  = 0;
          
          for (Item item : currentCollection) {

            String accNumUndelim = item.getStringValue(accessionNumberUndelimited);

            System.err.println("Processing " + accNumUndelim);

            //colonize accession number
            String accNumDelim = accNumUndelim.replaceAll("([0-9]{4})([0-9]{4})([0-9]{4})", "$1:$2:$3");

            accessionNumbers.add(accNumDelim);
            HashMap map = getData(accNumDelim);

            try {
              item.setStringValue(maker, (String) map.get("artist"));
              item.setStringValue(title, (String) map.get("title"));
              item.setStringValue(date, (String) map.get("date"));
              item.setStringValue(dimensions, (String) map.get("dimensions"));
              item.setStringValue(medium, (String) map.get("medium"));
              item.setStringValue(accessionNumberDelimited, (String) map.get("accessionNumber"));

              item.save();
            } catch (Exception e) {
              System.err.println("Can't save items: " + e.getMessage());
            }

            numberDone++;
            updateProgress(numberDone);
          }
        }

        protected void updateProgress(int numberDone) {
          float numberDoneFloat = numberDone * 1.0f;
          float numberItemsFloat = numberItems * 1.0f;
          float progress = ((numberDoneFloat / numberItemsFloat) * 100);

          int progressInt = (Math.round(progress));

          setProgress(progressInt);
        }
      };

      PropertyChangeListener updateListener = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
          String strPropertyName = evt.getPropertyName();

          if ("progress".equals(strPropertyName)) {
            progressBar.setValue(Integer.parseInt(evt.getNewValue().toString()));
          }
        }
      };

      worker.addPropertyChangeListener(updateListener);
      worker.execute();

      } else {
        System.err.println("Attempted paralllel invokation");
      }
  }



  /**
   * Method to return the current selection from Cumulus
   * @returs the current collection.
   */
  protected MultiItemCollection getSelection() {
    CollectionWindow currentCollectionWindow = application.getCurrentCollectionWindow();
    MultiItemPane mainPanel = currentCollectionWindow.getMainRecordPane();
    MultiItemCollection currentCollection = mainPanel.getMultiItemCollection();

    return currentCollection;

  }

  /**
   * Method that takes an accession number and returns data from the TMS web service.
   *
   * @param accessionNumber accession number to send to the webservice
   * @return Hashmap of data from an item returned by the web service
   */
  protected static HashMap getData(String accessionNumber) {


    HashMap<Object, Object> map = new HashMap<Object, Object>();

    map.put("accessionNumber", "[Record Not Found]");
    map.put("title", " ");
    map.put("artist", " ");
    map.put("date", " ");
    map.put("medium", " ");
    map.put("dimensions", " ");

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

      doc = db.parse(urlData);

      doc.getDocumentElement().normalize();

      NodeList titleElement = doc.getElementsByTagName("title");
      String title = titleElement.item(0).getTextContent();

      if (title.length() != 0) {
        map.put("title", title);
      }

      NodeList accessionNumberElement = doc.getElementsByTagName("accessionNumber");
      String accessionNumberDelimited = accessionNumberElement.item(0).getTextContent();

      if (accessionNumberDelimited.length() != 0) {
        map.put("accessionNumber", accessionNumberDelimited);
      }

      NodeList artistElement = doc.getElementsByTagName("artist");
      String artist = artistElement.item(0).getTextContent();


      if (artist.length() != 0) {
        map.put("artist", artist);
      }

      NodeList dateElement = doc.getElementsByTagName("date");
      String date = dateElement.item(0).getTextContent();

      if (date.length() != 0) {
        map.put("date", date);
      }

      NodeList mediumElement = doc.getElementsByTagName("medium");
      String medium = mediumElement.item(0).getTextContent();

      if (medium.length() != 0) {
        map.put("medium", medium);
      }

      NodeList dimensionsElement = doc.getElementsByTagName("dimensions");
      String dimensions = dimensionsElement.item(0).getTextContent();

      if (dimensions.length() != 0) {
        map.put("dimensions", dimensions);
      }

    } catch (Exception e) {

      System.err.println(e.getMessage());

    }

    return map;

  }

}
