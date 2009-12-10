package org.eastmanhouse.ejp;

import com.canto.cumulus.CumulusException;
import com.canto.cumulus.ejp.AbstractEJP;
import com.canto.cumulus.ejp.EJPContext;
import com.canto.cumulus.ui.event.ActionHandler;
import com.canto.cumulus.ui.Application;
import com.canto.cumulus.ui.Menu;
import com.canto.cumulus.ui.MenuBar;
import com.canto.cumulus.ui.MenuItem;
import java.awt.event.ActionEvent;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
/**
 *
 * @author ryand
 */

public class Truss extends AbstractEJP implements ActionHandler{
  private Logger logger;
  private static FileHandler fh;

  private Application application;

  public Truss() {

    super();
    
    try {
    System.setErr(new java.io.PrintStream(new java.io.FileOutputStream("/tmp/mystderr")));
    } catch (Exception e){
       System.err.println("Toasted.");
    }

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
        beginTask();
      }
   }

   private void beginTask(){
    TrussWorker tw = new TrussWorker(application);
    new Thread(tw).start();
   }

}