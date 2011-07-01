package com.citrix.xenserver.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.PrintStream;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.RepaintManager;

public class VNCControls extends JPanel
{
  private static final Logger logger = Logger.getLogger(VNCControls.class.getName());
  static final long serialVersionUID = 0L;
  public JPanel consolePanel = new JPanel(true);
  public JPanel buttonPanel = new JPanel(true);
  private final JPanel undockedPanel = new JPanel(true);
  public VNCFullscreen undockedConsole;
  private final JButton fullscreenButton = new JButton();
  private final JButton dockButton = new JButton();
  private final JButton findconsoleButton = new JButton();
  private final JButton redockButton = new JButton();
  private final JButton ctrlaltdelButton = new JButton();
  public Main main;
  public JPanel backPanel;
  public JPanel controls;
  private Color _backColor;

  public VNCControls(Main paramMain, JPanel paramJPanel, Color paramColor, boolean paramBoolean)
  {
    this.main = paramMain;
    this.backPanel = paramJPanel;
    this.controls = this;
    this._backColor = paramColor;
    setupConsole();
    setupButtons(paramBoolean);
    initialize();
    setColors();
  }

  private void setColors() {
    this.backPanel.setBackground(this._backColor);
    this.controls.setBackground(this._backColor);
    this.consolePanel.setBackground(this._backColor);
    this.buttonPanel.setBackground(this._backColor);
    this.undockedPanel.setBackground(this._backColor);
    this.fullscreenButton.setBackground(this._backColor);
    this.dockButton.setBackground(this._backColor);
    this.findconsoleButton.setBackground(this._backColor);
    this.redockButton.setBackground(this._backColor);
    this.ctrlaltdelButton.setBackground(this._backColor);
  }

  private void initialize() {
    RepaintManager.currentManager(this.main.canvas_).setDoubleBufferingEnabled(true);
    this.main.canvas_.setDoubleBuffered(true);

    BorderLayout localBorderLayout = new BorderLayout();
    setLayout(localBorderLayout);

    add(this.buttonPanel, "North");
    add(this.consolePanel, "Center");

    setColors();
    this.consolePanel.addComponentListener(new ComponentListener()
    {
      public void componentHidden(ComponentEvent paramComponentEvent)
      {
      }

      public void componentMoved(ComponentEvent paramComponentEvent)
      {
      }

      public void componentResized(ComponentEvent paramComponentEvent)
      {
        if (VNCControls.this.main != null) {
          VNCControls.this.main.canvas_.setMaxHeight(paramComponentEvent.getComponent().getHeight());
          VNCControls.this.main.canvas_.setMaxWidth(paramComponentEvent.getComponent().getWidth());
        }
        if (VNCControls.this.consolePanel != null) {
          VNCControls.this.consolePanel.invalidate();
          VNCControls.this.consolePanel.validate();
        }
      }

      public void componentShown(ComponentEvent paramComponentEvent) {
      } } );
  }

  private void setupButtons(boolean paramBoolean) {
    this.fullscreenButton.setText("Fullscreen (Ctrl+Alt)");
    this.dockButton.setText("Undock");
    this.findconsoleButton.setText("Find Console");
    this.redockButton.setText("Redock Console");
    this.ctrlaltdelButton.setText("Send Ctrl-Alt-Del");

    this.fullscreenButton.setVisible(true);
    this.dockButton.setVisible(true);
    this.redockButton.setVisible(true);
    this.findconsoleButton.setVisible(true);
    this.ctrlaltdelButton.setVisible(paramBoolean);

    FlowLayout localFlowLayout = new FlowLayout();
    localFlowLayout.setHgap(0);
    localFlowLayout.setAlignment(0);
    this.buttonPanel.setLayout(localFlowLayout);
    this.buttonPanel.add(this.ctrlaltdelButton);

    localFlowLayout = new FlowLayout();
    localFlowLayout.setAlignment(0);
    this.undockedPanel.setLayout(localFlowLayout);
    this.undockedPanel.add(this.findconsoleButton);
    this.undockedPanel.add(this.redockButton);
    this.undockedPanel.setVisible(false);

    this.dockButton.addActionListener(dockListener());
    this.fullscreenButton.addActionListener(fullscreenListener());

    this.findconsoleButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent paramActionEvent) {
        VNCControls.this.undockedConsole.focus();
      }
    });
    this.redockButton.addActionListener(redockListener());

    this.ctrlaltdelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent paramActionEvent) {
        VNCControls.this.main.canvas_.sendCtrlAltDel();
        VNCControls.this.main.canvas_.requestFocusInWindow();
      } } );
  }

  private ActionListener redockListener() {
    return new ActionListener()
    {
      public void actionPerformed(ActionEvent paramActionEvent) {
        VNCControls.this.undockedConsole.dispose();
      }
    };
  }

  public void setupConsole() {
    BorderLayout localBorderLayout = new BorderLayout();
    this.consolePanel.setLayout(localBorderLayout);
    this.consolePanel.add(this.main.canvas_, "North");
  }

  private ActionListener fullscreenListener() {
    return new ActionListener()
    {
      public void actionPerformed(ActionEvent paramActionEvent) {
        VNCControls.this.controls.remove(VNCControls.this.consolePanel);
        VNCFullscreen localVNCFullscreen = new VNCFullscreen(VNCControls.this.consolePanel, VNCControls.this.main.canvas_, true, VNCControls.this._backColor);

        localVNCFullscreen.addWindowListener(new WindowListener() {
          public void windowActivated(WindowEvent paramWindowEvent) {
          }

          public void windowClosed(WindowEvent paramWindowEvent) {
            VNCControls.this.controls.add(VNCControls.this.consolePanel);
            VNCControls.this.main.canvas_.setMaxHeight(VNCControls.this.consolePanel.getHeight());
            VNCControls.this.main.canvas_.setMaxWidth(VNCControls.this.consolePanel.getWidth());
            VNCControls.this.controls.invalidate();
            VNCControls.this.controls.validate();
            VNCControls.this.main.canvas_.invalidate();
            VNCControls.this.main.canvas_.validate();
            VNCControls.this.main.canvas_.requestFocusInWindow();
          }

          public void windowClosing(WindowEvent paramWindowEvent) {
          }

          public void windowDeactivated(WindowEvent paramWindowEvent) {
            ((VNCFullscreen)paramWindowEvent.getComponent()).dispose();
          }

          public void windowDeiconified(WindowEvent paramWindowEvent)
          {
          }

          public void windowIconified(WindowEvent paramWindowEvent)
          {
          }

          public void windowOpened(WindowEvent paramWindowEvent)
          {
          }
        });
        VNCControls.this.backPanel.invalidate();
        VNCControls.this.backPanel.repaint();
      } } ;
  }

  private ActionListener dockListener() {
    return new ActionListener()
    {
      public void actionPerformed(ActionEvent paramActionEvent)
      {
        VNCControls.this.backPanel.remove(VNCControls.this.controls);
        VNCControls.this.undockedPanel.setVisible(true);
        VNCControls.this.undockedConsole = null;
        VNCControls.this.undockedConsole = new VNCFullscreen(VNCControls.this.controls, VNCControls.this.main.canvas_, false, VNCControls.this._backColor);

        VNCControls.this.undockedConsole.setTitle("Console");
        VNCControls.this.dockButton.setText("Redock");
        try {
          VNCControls.this.dockButton.setIcon(new ImageIcon(getClass().getResource("attach_24.png")));
        }
        catch (Exception localException) {
          System.out.println(localException.getMessage());
        }
        VNCControls.this.undockedConsole.addWindowListener(new WindowListener() {
          public void windowActivated(WindowEvent paramWindowEvent) {
          }

          public void windowClosed(WindowEvent paramWindowEvent) {
            VNCControls.this.undockedPanel.setVisible(false);
            VNCControls.this.backPanel.add(VNCControls.this.controls);
            VNCControls.this.dockButton.setText("Undock");
            try {
              VNCControls.this.dockButton.setIcon(new ImageIcon(getClass().getResource("detach_24.png")));
            }
            catch (Exception localException) {
              System.out.println(localException.getMessage());
            }
            VNCControls.this.backPanel.invalidate();
            VNCControls.this.backPanel.validate();
            if (VNCControls.this.main != null) {
              VNCControls.this.main.canvas_.setMaxHeight(VNCControls.this.consolePanel.getHeight());
              VNCControls.this.main.canvas_.setMaxWidth(VNCControls.this.consolePanel.getWidth());
              VNCControls.this.main.canvas_.invalidate();
              VNCControls.this.main.canvas_.repaint();
              VNCControls.this.main.canvas_.requestFocusInWindow();
            }
            VNCControls.this.dockButton.removeActionListener(VNCControls.this.dockButton.getActionListeners()[0]);

            VNCControls.this.dockButton.addActionListener(VNCControls.this.dockListener());
          }

          public void windowClosing(WindowEvent paramWindowEvent)
          {
          }

          public void windowDeactivated(WindowEvent paramWindowEvent)
          {
          }

          public void windowDeiconified(WindowEvent paramWindowEvent)
          {
          }

          public void windowIconified(WindowEvent paramWindowEvent)
          {
          }

          public void windowOpened(WindowEvent paramWindowEvent)
          {
          }
        });
        VNCControls.this.backPanel.invalidate();
        VNCControls.this.backPanel.repaint();
        VNCControls.this.dockButton.removeActionListener(VNCControls.this.dockButton.getActionListeners()[0]);

        VNCControls.this.dockButton.addActionListener(VNCControls.this.redockListener());
      }
    };
  }
}