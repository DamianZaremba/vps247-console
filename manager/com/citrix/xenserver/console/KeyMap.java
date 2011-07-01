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
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(
        new InputStreamReader(getClass()
        .getResourceAsStream("KeyMap.properties")));
      String line;
      while ((line = reader.readLine()) != null) {
        String line = line.trim();
        if ((line.length() == 0) || (line.startsWith("#"))) {
          continue;
        }
        int index = line.indexOf("=0x");
        if (index == -1) {
          throw new IOException("malformed line: " + line);
        }
        String keycode_str = line.substring(0, index);
        int keycode = KeyEvent.class.getField(keycode_str).getInt(null);
        int keysym = Integer.parseInt(line.substring(index + 3), 16);
        if (this.map_.containsKey(Integer.valueOf(keycode))) {
          throw new IOException("Duplicate entry " + line);
        }
        this.map_.put(Integer.valueOf(keycode), Integer.valueOf(keysym));
      }
    } catch (Throwable t) {
      logger.log(Level.WARNING, t.getMessage(), t);
    }
  }

  int unicodeToKeysym(int c) {
    if (c < 32)
    {
      return c + 96;
    }if ((c <= 126) || ((160 <= c) && (c <= 255))) {
      return c;
    }
    return -1;
  }

  public int getKeysym(KeyEvent event) {
    int result = getMappedKey(event.getKeyCode());
    if (result == -1) {
      char c = event.getKeyChar();
      return c == 65535 ? -1 : unicodeToKeysym(c);
    }
    return result;
  }

  public int getMappedKey(int keycode)
  {
    Integer ks = (Integer)this.map_.get(Integer.valueOf(keycode));
    return ks == null ? -1 : ks.intValue();
  }
}