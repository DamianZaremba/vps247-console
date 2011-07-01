package com.vps247.admin;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.Scrollable;

class VMSummaryPanel extends JPanel
  implements Scrollable
{
  private static final long serialVersionUID = 1L;

  public VMSummaryPanel()
  {
    super(new GridLayout(0, 1));
    setBackground(Color.WHITE);
  }

  public Dimension getPreferredSize() {
    return new Dimension(getParent().getWidth(), getComponentCount() * 176);
  }

  public Dimension getPreferredScrollableViewportSize()
  {
    return new Dimension(getParent().getWidth(), getComponentCount() * 176);
  }

  public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2)
  {
    return 176;
  }

  public boolean getScrollableTracksViewportHeight()
  {
    return false;
  }

  public boolean getScrollableTracksViewportWidth()
  {
    return false;
  }

  public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2)
  {
    return 20;
  }
}