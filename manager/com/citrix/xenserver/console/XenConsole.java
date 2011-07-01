package com.citrix.xenserver.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class XenConsole extends JPanel
  implements ConnectionListener, ConsoleListener
{
  private static final Logger logger = Logger.getLogger(XenConsole.class.getName());
  static final long serialVersionUID = 0L;
  public final VNCCanvas canvas_ = new VNCCanvas();
  public VNCStream stream_;
  public boolean noConnect = false;
  private boolean usessl;
  public String path;
  public String auth;
  private int port;
  private ConnectionListener _listener;
  private ConsoleListener _console;
  private String title;
  public VNCControls controls;
  private JPanel background = new JPanel(new BorderLayout(), true);
  private JPanel errorPanel = new JPanel(true);
  private Thread t;
  public JTextArea console = new JTextArea();

  private int retries = 120;
  public boolean connecting = false;

  private boolean logOnConsole = false;
  private boolean hideCADButton = false;
  public JPanel containingPanel;

  public XenConsole(String url, String session, String title)
  {
    this.path = url;
    this.auth = session;

    this.containingPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(2, 2, 2, 2);
    gridBagConstraints.anchor = 10;

    this.containingPanel.add(this.canvas_);

    this.stream_ = new VNCStream(this.canvas_, this, this);
    this.canvas_.setStream(this.stream_);
    this.canvas_.setConsoleListener(this);
    this._listener = this;
    this._console = this;
    this.usessl = true;
    this.title = title;

    init();
  }

  public String getTitle()
  {
    return this.title;
  }

  private String[] getArgs()
  {
    String[] args;
    if ("true".equals("true")) {
      String[] args = new String[3];
      args[0] = "https://89.238.150.2/console?ref=OpaqueRef:a28e6190-6b2d-feae-b08a-11970b781367";
      args[1] = "OpaqueRef:902fbede-ada2-640b-3d3b-304991382f08";
      args[2] = "true";
    } else {
      args = new String[0];
    }

    return args;
  }

  public void init()
  {
    try {
      this.logOnConsole = true;
      this.hideCADButton = false;

      writeline("");
      writeline("Loading UI...");
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      writeline("Initializing...");
      String[] args = getArgs();

      break label138;
      String s = "white";
      String[] vals = s.split(":");
      Color c;
      label138: Color c;
      if ((vals.length == 3) && (vals[0].length() == 2) && 
        (vals[1].length() == 2) && (vals[2].length() == 2)) {
        c = new Color(Integer.parseInt(vals[0], 16), 
          Integer.parseInt(vals[1], 16), Integer.parseInt(vals[2], 
          16));
      } else {
        Color c = Color.WHITE; break label142;

        c = Color.WHITE;
      }
      label142: setBackground(c);
      writeline("Starting main...");

      writeline("Creating controls...");
      setLayout(new BorderLayout());
      this.controls = new VNCControls(this, this, c, !this.hideCADButton);
      writeline("Adding controls...");
      add(this.controls);

      this.errorPanel.setBackground(c);
      this.errorPanel.setLayout(new BorderLayout());
      this.console.setBackground(Color.white);
      this.console.setEditable(false);
      JScrollPane areaScrollPane = new JScrollPane(this.console);
      areaScrollPane
        .setVerticalScrollBarPolicy(22);
      areaScrollPane.setBorder(BorderFactory.createLineBorder(
        Color.BLACK, 1));
      this.errorPanel.add(areaScrollPane, "Center");
    }
    catch (Exception e)
    {
      writeline(e.getMessage());
      e.printStackTrace();
    }
  }

  public void start()
  {
    writeline("Starting...");
    connect();
  }

  public void stop() {
    writeline("Stopping...");
    if ((this.stream_ != null) && (this.stream_.isConnected()))
      this.stream_.disconnect();
  }

  public void destroy()
  {
    writeline("Destroying...");
  }

  public void ConnectionClosed() {
    writeline("Connection closed");
    this.stream_.disconnect();
    this.stream_._connected = false;
    this.connecting = false;
  }

  public void ConnectionLost(String reason)
  {
    if (reason != null)
      writeline("Connection lost: ".concat(reason));
    else {
      writeline("Connection lost");
    }

    ConnectionClosed();
  }

  public void ConnectionMade()
  {
    if (this.controls == null) {
      System.out.println("IS NULL");
    }

    this.controls.consolePanel.getParent().repaint();
    this.controls.consolePanel.invalidate();
    this.controls.consolePanel.validate();
    this.canvas_.requestFocusInWindow();
    this.connecting = false;
    this.retries = 120;
  }

  public void ConnectionFailed(String reason) {
    writeline("Connection failed: ".concat(reason));

    this.connecting = true;
    ConnectionClosed();
  }

  public void writeline(String line)
  {
    SwingUtilities.invokeLater(new Runnable(line) {
      public void run() {
        if ((XenConsole.this.logOnConsole) && (this.val$line != null)) {
          XenConsole.this.console.append(this.val$line);
          XenConsole.this.console.append("\n");
        }
        System.out.println(this.val$line);
      } } );
  }

  public void connect() {
    if (!this.noConnect)
      try {
        if (this.usessl) {
          this.stream_.disconnect();
          URL uri = new URL(this.path);
          String uuid = this.auth;
          RawHTTP http = new RawHTTP("CONNECT", uri.getHost(), 443, uri
            .getPath().concat("?").concat(uri.getQuery()), uuid, 
            "https".equals(uri.getProtocol()), this._listener, this._console);
          http.connect();
          this.stream_.connect(http, new char[0]);
        } else {
          this.stream_.disconnect();
          String password = this.auth;
          int n = password.length();
          char[] c = new char[n];
          password.getChars(0, n, c, 0);
          this.stream_.connectSocket(new Socket(this.path, this.port), c);
        }
      } catch (Throwable t) {
        if (this._listener != null)
          SwingUtilities.invokeLater(new Runnable(t) {
            public void run() {
              XenConsole.this._listener.ConnectionFailed(this.val$t.getMessage());
            }
          });
      }
  }
}