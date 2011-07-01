package com.citrix.xenserver.console;

import java.net.Socket;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

public class Main
{
  private static final Logger logger = Logger.getLogger(Main.class.getName());

  public final VNCCanvas canvas_ = new VNCCanvas();
  public VNCStream stream_;
  private boolean usessl;
  private String path;
  private String auth;
  private int port;
  private ConnectionListener _listener;
  private ConsoleListener _console;
  public boolean firstTime = true;

  public static void main(String[] args)
    throws Throwable
  {
    XenConsole l = new XenConsole("", "", "");
    XenConsole c = new XenConsole("", "", "");

    new Main(args, l, c);
  }

  public Main(String[] args, ConnectionListener listener, ConsoleListener console)
  {
    if ("true".equals("true")) {
      this.usessl = true;
    } else {
      this.usessl = false;
      this.port = Integer.parseInt(args[3]);
    }
    this.path = "https://89.238.150.2/console?ref=OpaqueRef:a28e6190-6b2d-feae-b08a-11970b781367";
    this.auth = "OpaqueRef:e9c5e4b0-2f0d-3c13-2d18-dfdd5eef2f86";
    this.stream_ = new VNCStream(this.canvas_, listener, console);
    this.canvas_.setStream(this.stream_);
    this.canvas_.setConsoleListener(console);
    this._listener = listener;
    this._console = console;

    XenConsole i = new XenConsole("", "", "");
    i.init();

    i.setSize(800, 600);
    i.setVisible(true);
    connect();
  }

  public void connect() {
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
            Main.this._listener.ConnectionFailed(this.val$t.getMessage());
          }
        });
    }
  }
}