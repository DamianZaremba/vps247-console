package com.citrix.xenserver.console;

import java.io.PrintStream;
import java.security.KeyStore;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SSLHelper
{
  private static final Logger logger = Logger.getLogger(SSLHelper.class.getName());

  public static final String[] cipherSuites = { "TLS_DHE_RSA_WITH_AES_128_CBC_SHA" };
  private static SSLContext sSSLContext = null;
  private static SSLHelper instance = new SSLHelper();

  public static SSLHelper getInstance()
  {
    return instance;
  }

  public SSLContext getSSLContext() {
    return sSSLContext;
  }

  private void init() {
    try {
      SSLContext pSSLContext = SSLContext.getInstance("SSL", "SunJSSE");

      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509", 
        "SunJSSE");
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(
        "SunX509", "SunJSSE");

      KeyStore ks = KeyStore.getInstance("JCEKS", "SunJCE");

      char[] password = "b3646d1424de7a06".toCharArray();
      ks.load(getClass().getResourceAsStream("client.ks"), password);

      kmf.init(ks, password);
      tmf.init(ks);

      pSSLContext
        .init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
      sSSLContext = pSSLContext;
    } catch (Exception e) {
      System.out.println("ERROR: failed to initialize SSLContext: " + 
        e.getMessage());
      e.printStackTrace();
    }
  }

  private SSLHelper()
  {
    init();
  }
}