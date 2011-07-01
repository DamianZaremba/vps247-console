package com.citrix.xenserver.console;

import java.awt.Color;
import java.awt.Image;

public abstract interface VNCClient
{
  public abstract void clientDrawImage(Image paramImage, int paramInt1, int paramInt2, int paramInt3, int paramInt4);

  public abstract void clientSetCursor(Image paramImage, int paramInt1, int paramInt2);

  public abstract void clientCopyRectangle(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6);

  public abstract void clientFillRectangle(int paramInt1, int paramInt2, int paramInt3, int paramInt4, Color paramColor);

  public abstract void clientFrameBufferUpdate();

  public abstract void clientBell();

  public abstract void clientCutText(String paramString);

  public abstract void clientDesktopSize(int paramInt1, int paramInt2);
}