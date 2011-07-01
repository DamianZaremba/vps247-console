package com.citrix.xenserver.console;

public abstract interface ConnectionListener
{
  public abstract void ConnectionMade();

  public abstract void ConnectionLost(String paramString);

  public abstract void ConnectionClosed();

  public abstract void ConnectionFailed(String paramString);
}