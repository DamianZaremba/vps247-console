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
      SSLContext localSSLContext = SSLContext.getInstance("SSL", "SunJSSE");

      KeyManagerFactory localKeyManagerFactory = KeyManagerFactory.getInstance("SunX509", "SunJSSE");

      TrustManagerFactory localTrustManagerFactory = TrustManagerFactory.getInstance("SunX509", "SunJSSE");

      KeyStore localKeyStore = KeyStore.getInstance("JCEKS", "SunJCE");

      char[] arrayOfChar = "b3646d1424de7a06".toCharArray();
      localKeyStore.load(getClass().getResourceAsStream("client.ks"), arrayOfChar);

      localKeyManagerFactory.init(localKeyStore, arrayOfChar);
      localTrustManagerFactory.init(localKeyStore);

      localSSLContext.init(localKeyManagerFactory.getKeyManagers(), localTrustManagerFactory.getTrustManagers(), null);

      sSSLContext = localSSLContext;
    } catch (Exception localException) {
      System.out.println("ERROR: failed to initialize SSLContext: " + localException.getMessage());

      localException.printStackTrace();
    }
  }

  private SSLHelper()
  {
    init();
  }
}