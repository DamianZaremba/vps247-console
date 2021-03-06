package org.json;

public class HTTPTokener extends JSONTokener
{
  public HTTPTokener(String s)
  {
    super(s);
  }

  public String nextToken() throws JSONException {
    StringBuffer sb = new StringBuffer();
    char c;
    do c = next();
    while (
      Character.isWhitespace(c));
    if ((c == '"') || (c == '\'')) {
      char q = c;
      while (true) {
        c = next();
        if (c < ' ') {
          throw syntaxError("Unterminated string.");
        }
        if (c == q) {
          return sb.toString();
        }
        sb.append(c);
      }
    }
    while (true) {
      if ((c == 0) || (Character.isWhitespace(c))) {
        return sb.toString();
      }
      sb.append(c);
      c = next();
    }
  }
}