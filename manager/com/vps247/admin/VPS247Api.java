package com.vps247.admin;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

class VPS247Api
{
  private static String api_key = "Hard coding auth details in stuff you hand out is **BAD**";

  public static String postPath(String path) {
    String data = "";
    System.out.println(path);
    try {
      URL u = new URL("http://admin.vps247.com" + path);
      HttpURLConnection c = (HttpURLConnection)u.openConnection();

      c.setRequestProperty("x-vps247-api-key", Manager.manager.arguments[0]);
      c.setRequestMethod("POST");
      DataInputStream dis = new DataInputStream(c.getInputStream());
      String line;
      while ((line = dis.readLine()) != null)
      {
        String line;
        data = data + line + "\n";
      }
      System.out.println(data);
    } catch (Exception e) {
      System.out.println("ERROR");
    }
    return data;
  }

  public static String getPath(String path) {
    System.out.println(Manager.manager.arguments[0]);
    String data = "";
    try {
      URL u = new URL("http://admin.vps247.com" + path);
      HttpURLConnection c = (HttpURLConnection)u.openConnection();
      c.setRequestProperty("x-vps247-api-key", Manager.manager.arguments[0]);
      DataInputStream dis = new DataInputStream(c.getInputStream());
      String line;
      while ((line = dis.readLine()) != null)
      {
        String line;
        data = data + line + "\n";
      }
    } catch (Exception e) {
      System.out.println("ERROR");
    }
    return data;
  }
}
