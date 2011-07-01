package com.citrix.xenserver.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.PrintStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Initialize extends JApplet
  implements ConnectionListener, ConsoleListener
{
  private static final Logger logger = Logger.getLogger(Initialize.class.getName());
  static final long serialVersionUID = 0L;
  public static URL path;
  private Main main;
  private VNCControls controls;
  private JPanel background = new JPanel(new BorderLayout(), true);
  private JPanel errorPanel = new JPanel(true);
  private Thread t;
  public JTextArea console = new JTextArea();

  private int retries = 5;
  private boolean connecting = false;

  private boolean logOnConsole = false;
  private boolean hideCADButton = false;

  public Dimension getPreferredSize()
  {
    return this.main.canvas_.getPreferredSize();
  }

  private String[] getArgs()
  {
    String[] arrayOfString;
    if ("true".equals(getParameter("USEURL"))) {
      arrayOfString = new String[3];
      arrayOfString[0] = getParameter("URL");
      arrayOfString[1] = getParameter("SESSION");
      arrayOfString[2] = getParameter("USEURL");
    } else {
      arrayOfString = new String[4];
      arrayOfString[0] = getParameter("IPADDRESS");
      arrayOfString[1] = getParameter("PASSWORD");
      arrayOfString[2] = getParameter("USEURL");
      arrayOfString[3] = getParameter("PORT");
    }
    return arrayOfString;
  }

  public void init() {
    try {
      this.logOnConsole = Boolean.valueOf(getParameter("CONSOLELOGGING")).booleanValue();
      this.hideCADButton = Boolean.valueOf(getParameter("HIDECAD")).booleanValue();
      path = getDocumentBase();
      writeline("");
      writeline("Loading UI...");
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      writeline("Initializing...");
      String[] arrayOfString1 = getArgs();
      Color localColor;
      if (getParameter("BACKCOLOR") != null) {
        localObject = getParameter("BACKCOLOR");
        String[] arrayOfString2 = ((String)localObject).split(":");
        if ((arrayOfString2.length == 3) && (arrayOfString2[0].length() == 2) && (arrayOfString2[1].length() == 2) && (arrayOfString2[2].length() == 2))
        {
          localColor = new Color(Integer.parseInt(arrayOfString2[0], 16), Integer.parseInt(arrayOfString2[1], 16), Integer.parseInt(arrayOfString2[2], 16));
        }
        else
        {
          localColor = Color.WHITE;
        }
      } else {
        localColor = Color.WHITE;
      }
      setBackground(localColor);
      writeline("Starting main...");
      this.main = new Main(arrayOfString1, this, this);
      writeline("Creating controls...");
      this.controls = new VNCControls(this.main, this.background, localColor, !this.hideCADButton);
      writeline("Adding controls...");
      this.background.add(this.controls);
      add(this.background);

      this.errorPanel.setBackground(localColor);
      this.errorPanel.setLayout(new BorderLayout());
      this.console.setBackground(Color.white);
      this.console.setEditable(false);
      Object localObject = new JScrollPane(this.console);
      ((JScrollPane)localObject).setVerticalScrollBarPolicy(22);

      ((JScrollPane)localObject).setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

      this.errorPanel.add((Component)localObject, "Center");
    } catch (Exception localException) {
      writeline(localException.getMessage());
    }
  }

  public void start()
  {
    writeline("Starting...");
    this.main.connect();
  }

  public void stop() {
    writeline("Stopping...");
    if ((this.main != null) && (this.main.stream_ != null) && (this.main.stream_.isConnected()))
      this.main.stream_.disconnect();
  }

  public void destroy()
  {
    writeline("Destroying...");
  }

  public void ConnectionClosed() {
    writeline("Connection closed");
    if (this.retries > 0)
    {
      this.controls.consolePanel.remove(this.main.canvas_);
      this.controls.consolePanel.setLayout(new BorderLayout());
      this.controls.consolePanel.add(this.errorPanel);
      this.controls.invalidate();
      this.controls.validate();
      this.controls.consolePanel.invalidate();
      this.controls.consolePanel.validate();

      this.t = new Thread(new Runnable()
      {
        public void run() {
          Initialize.this.writeline("Reconnecting in 5 seconds...");
          try {
            Thread.sleep(5000L);
          } catch (Exception localException) {
            Initialize.this.writeline(localException.getMessage());
          }
          Initialize.this.writeline("Retry ".concat(Integer.toString(6 - Initialize.this.retries)).concat(" out of 5"));

          Initialize.this.main.connect();
          Initialize.access$010(Initialize.this);
        }
      });
      this.t.start();
    }
  }

  public void ConnectionLost(String paramString) {
    if (this.main != null) {
      if (paramString != null)
        writeline("Connection lost: ".concat(paramString));
      else {
        writeline("Connection lost");
      }
      if (!this.connecting) {
        this.connecting = true;
        ConnectionClosed();
      }
    }
  }

  public void ConnectionMade() {
    this.controls.consolePanel.remove(this.errorPanel);
    this.controls.setupConsole();
    this.controls.consolePanel.getParent().repaint();
    this.controls.consolePanel.invalidate();
    this.controls.consolePanel.validate();
    this.main.canvas_.requestFocusInWindow();
    this.connecting = false;
    this.retries = 5;
  }

  public void ConnectionFailed(String paramString) {
    if (this.main != null) {
      writeline(paramString);

      this.connecting = true;
      ConnectionClosed();
    }
  }

  public void writeline(String paramString)
  {
    SwingUtilities.invokeLater(new Runnable(paramString) {
      public void run() {
        if ((Initialize.this.logOnConsole) && (this.val$line != null)) {
          Initialize.this.console.append(this.val$line);
          Initialize.this.console.append("\n");
        }
        System.out.println(this.val$line);
      }
    });
  }
}