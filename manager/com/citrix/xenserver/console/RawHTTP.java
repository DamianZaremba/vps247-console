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
  private static final TrustManager[] trustAllCerts = { 
    new X509TrustManager() {
    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType)
    {
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType)
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

  public RawHTTP(String command, String host, int port, String path, String session, boolean useSSL, ConnectionListener listner, ConsoleListener console)
  {
    this.command = command;
    this.host = host;
    this.port = port;
    this.path = path;
    this.session = session;
    this.useSSL = useSSL;
    this._console = console;
  }

  private Socket _getSocket()
    throws IOException
  {
    if (this.useSSL) {
      SSLContext context = SSLHelper.getInstance()
        .getSSLContext();
      SSLSocket ssl = null;
      try {
        context.init(null, trustAllCerts, new SecureRandom());
        SocketFactory factory = context.getSocketFactory();
        ssl = (SSLSocket)factory.createSocket(this.host, this.port);
      }
      catch (IOException e) {
        this._console.writeline("IOException: " + e.getMessage());
        throw e;
      } catch (KeyManagementException e) {
        this._console.writeline("KeyManagementException: " + e.getMessage());
      }
      return ssl;
    }
    return new Socket(this.host, this.port);
  }

  public Socket connect() throws IOException
  {
    String[] headers = makeHeaders();
    this.s = _getSocket();
    try {
      this.oc = this.s.getOutputStream();
      for (String header : headers) {
        this.oc.write(header.getBytes());
        this.oc.write("\r\n".getBytes()); } this.oc.flush();
      this.ic = this.s.getInputStream();
      String line;
      while (true) { line = readline(this.ic);

        Matcher m = END_PATTERN.matcher(line);
        if (m.matches()) {
          return this.s;
        }

        m = HEADER_PATTERN.matcher(line);
        if (m.matches()) {
          this.responseHeaders.put(m.group(1), m.group(2));
          continue;
        }

        m = HTTP_PATTERN.matcher(line);
        if (!m.matches()) break;
        String status_code = m.group(1);
        String reason_phrase = m.group(2);
        if (!"200".equals(status_code)) {
          throw new IOException("HTTP status " + status_code + 
            " " + reason_phrase);
        }
      }
      throw new IOException("Unknown HTTP line " + line);
    }
    catch (IOException exn)
    {
      this.s.close();
      throw exn;
    } catch (RuntimeException exn) {
      this.s.close();
    }throw exn;
  }

  public Map<String, String> getResponseHeaders()
  {
    return this.responseHeaders;
  }

  private String[] makeHeaders() {
    String[] headers = { String.format("%s %s HTTP/1.0", new Object[] { this.command, this.path }), 
      String.format("Host: %s", new Object[] { this.host }), 
      String.format("Cookie: session_id=%s", new Object[] { this.session }), "" };
    return headers;
  }

  private static String readline(InputStream ic) throws IOException {
    String result = "";
    try { int c;
      do { c = ic.read();

        if (c == -1) {
          return result;
        }
        result = result + (char)c; }
      while (c != 10);
      return result;
    } catch (IOException e)
    {
      ic.close();
    }throw e;
  }
}