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

  public static void main(String[] paramArrayOfString)
    throws Throwable
  {
    new Main(paramArrayOfString, new Initialize(), new Initialize());
  }

  public Main(String[] paramArrayOfString, ConnectionListener paramConnectionListener, ConsoleListener paramConsoleListener)
  {
    if ("true".equals(paramArrayOfString[2])) {
      this.usessl = true;
    } else {
      this.usessl = false;
      this.port = Integer.parseInt(paramArrayOfString[3]);
    }
    this.path = paramArrayOfString[0];
    this.auth = paramArrayOfString[1];
    this.stream_ = new VNCStream(this.canvas_, paramConnectionListener, paramConsoleListener);
    this.canvas_.setStream(this.stream_);
    this.canvas_.setConsoleListener(paramConsoleListener);
    this._listener = paramConnectionListener;
    this._console = paramConsoleListener;
  }

  public void connect()
  {
    try
    {
      Object localObject1;
      Object localObject2;
      if (this.usessl) {
        this.stream_.disconnect();
        localObject1 = new URL(this.path);
        String str = this.auth;
        localObject2 = new RawHTTP("CONNECT", ((URL)localObject1).getHost(), 443, ((URL)localObject1).getPath().concat("?").concat(((URL)localObject1).getQuery()), str, "https".equals(((URL)localObject1).getProtocol()), this._listener, this._console);

        ((RawHTTP)localObject2).connect();
        this.stream_.connect((RawHTTP)localObject2, new char[0]);
      } else {
        this.stream_.disconnect();
        localObject1 = this.auth;
        int i = ((String)localObject1).length();
        localObject2 = new char[i];
        ((String)localObject1).getChars(0, i, localObject2, 0);
        this.stream_.connectSocket(new Socket(this.path, this.port), localObject2);
      }
    } catch (Throwable localThrowable) {
      if (this._listener != null)
        SwingUtilities.invokeLater(new Runnable(localThrowable) {
          public void run() {
            Main.this._listener.ConnectionFailed(this.val$t.getMessage());
          }
        });
    }
  }
}