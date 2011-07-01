package com.citrix.xenserver.console;

import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyMap
{
  private static final Logger logger = Logger.getLogger(KeyMap.class.getName());

  static KeyMap instance_ = new KeyMap();

  Map<Integer, Integer> map_ = new HashMap();

  public static KeyMap getInstance()
  {
    return instance_;
  }

  public KeyMap()
  {
    BufferedReader localBufferedReader = null;
    try {
      localBufferedReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("KeyMap.properties")));
      String str1;
      while ((str1 = localBufferedReader.readLine()) != null) {
        str1 = str1.trim();
        if ((str1.length() == 0) || (str1.startsWith("#"))) {
          continue;
        }
        int i = str1.indexOf("=0x");
        if (i == -1) {
          throw new IOException("malformed line: " + str1);
        }
        String str2 = str1.substring(0, i);
        int j = KeyEvent.class.getField(str2).getInt(null);
        int k = Integer.parseInt(str1.substring(i + 3), 16);
        if (this.map_.containsKey(Integer.valueOf(j))) {
          throw new IOException("Duplicate entry " + str1);
        }
        this.map_.put(Integer.valueOf(j), Integer.valueOf(k));
      }
    } catch (Throwable localThrowable) {
      logger.log(Level.WARNING, localThrowable.getMessage(), localThrowable);
    }
  }

  int unicodeToKeysym(int paramInt) {
    if (paramInt < 32)
    {
      return paramInt + 96;
    }if ((paramInt <= 126) || ((160 <= paramInt) && (paramInt <= 255))) {
      return paramInt;
    }
    return -1;
  }

  public int getKeysym(KeyEvent paramKeyEvent) {
    int i = getMappedKey(paramKeyEvent.getKeyCode());
    if (i == -1) {
      int j = paramKeyEvent.getKeyChar();
      return j == 65535 ? -1 : unicodeToKeysym(j);
    }
    return i;
  }

  public int getMappedKey(int paramInt)
  {
    Integer localInteger = (Integer)this.map_.get(Integer.valueOf(paramInt));
    return localInteger == null ? -1 : localInteger.intValue();
  }
}