package com.citrix.xenserver.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class RawHTTP
{
  private static final Logger logger = Logger.getLogger(RawHTTP.class.getName());

  private static final Pattern END_PATTERN = Pattern.compile("^\r\n$");
  private static final Pattern HEADER_PATTERN = Pattern.compile("^([A-Z_a-z0-9-]+):\\s*(.*)\r\n$");

  private static final Pattern HTTP_PATTERN = Pattern.compile("^HTTP/\\d+\\.\\d+ (\\d*) (.*)\r\n$");
  private final String command;
  private final String host;
  private final int port;
  private final String path;
  private final String session;
  private final boolean useSSL;
  private final Map<String, String> responseHeaders = new HashMap();
  private InputStream ic;
  private OutputStream oc;
  private Socket s;
  private ConsoleListener _console;
  private static final TrustManager[] trustAllCerts = { new X509TrustManager()
  {
    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }

    public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
    {
    }

    public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
    {
    }
  }
   };

  public InputStream getInputStream()
  {
    return this.ic;
  }

  public OutputStream getOutputStream() {
    return this.oc;
  }

  public Socket getSocket() {
    return this.s;
  }

  public RawHTTP(String paramString1, String paramString2, int paramInt, String paramString3, String paramString4, boolean paramBoolean, ConnectionListener paramConnectionListener, ConsoleListener paramConsoleListener)
  {
    this.command = paramString1;
    this.host = paramString2;
    this.port = paramInt;
    this.path = paramString3;
    this.session = paramString4;
    this.useSSL = paramBoolean;
    this._console = paramConsoleListener;
  }

  private Socket _getSocket()
    throws IOException
  {
    if (this.useSSL) {
      SSLContext localSSLContext = SSLHelper.getInstance().getSSLContext();

      SSLSocket localSSLSocket = null;
      try {
        localSSLContext.init(null, trustAllCerts, new SecureRandom());
        SSLSocketFactory localSSLSocketFactory = localSSLContext.getSocketFactory();
        localSSLSocket = (SSLSocket)localSSLSocketFactory.createSocket(this.host, this.port);
      }
      catch (IOException localIOException) {
        this._console.writeline("IOException: " + localIOException.getMessage());
        throw localIOException;
      } catch (KeyManagementException localKeyManagementException) {
        this._console.writeline("KeyManagementException: " + localKeyManagementException.getMessage());
      }
      return localSSLSocket;
    }
    return new Socket(this.host, this.port);
  }

  public Socket connect() throws IOException
  {
    String[] arrayOfString = makeHeaders();
    this.s = _getSocket();
    try {
      this.oc = this.s.getOutputStream();
      String str2;
      for (str2 : arrayOfString) {
        this.oc.write(str2.getBytes());
        this.oc.write("\r\n".getBytes());
      }
      this.oc.flush();
      this.ic = this.s.getInputStream();
      while (true) {
        ??? = readline(this.ic);

        Matcher localMatcher = END_PATTERN.matcher((CharSequence)???);
        if (localMatcher.matches()) {
          return this.s;
        }

        localMatcher = HEADER_PATTERN.matcher((CharSequence)???);
        if (localMatcher.matches()) {
          this.responseHeaders.put(localMatcher.group(1), localMatcher.group(2));
          continue;
        }

        localMatcher = HTTP_PATTERN.matcher((CharSequence)???);
        if (localMatcher.matches()) {
          String str1 = localMatcher.group(1);
          str2 = localMatcher.group(2);
          if (!"200".equals(str1))
            throw new IOException("HTTP status " + str1 + " " + str2);
        }
        else
        {
          throw new IOException("Unknown HTTP line " + (String)???);
        }
      }
    } catch (IOException localIOException) {
      this.s.close();
      throw localIOException;
    } catch (RuntimeException localRuntimeException) {
      this.s.close();
    }throw localRuntimeException;
  }

  public Map<String, String> getResponseHeaders()
  {
    return this.responseHeaders;
  }

  private String[] makeHeaders() {
    String[] arrayOfString = { String.format("%s %s HTTP/1.0", new Object[] { this.command, this.path }), String.format("Host: %s", new Object[] { this.host }), String.format("Cookie: session_id=%s", new Object[] { this.session }), "" };

    return arrayOfString;
  }

  private static String readline(InputStream paramInputStream) throws IOException {
    String str = "";
    try {
      while (true) {
        int i = paramInputStream.read();

        if (i == -1) {
          return str;
        }
        str = str + (char)i;
        if (i == 10)
          return str;
      }
    } catch (IOException localIOException) {
      paramInputStream.close();
    }throw localIOException;
  }
}