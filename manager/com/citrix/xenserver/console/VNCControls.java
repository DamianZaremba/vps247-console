package com.citrix.xenserver.console;

import com.vps247.admin.Manager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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

  private final JCheckBox scaleCheckBox = new JCheckBox("Scale");
  private final JCheckBox viewOnlyCheckBox = new JCheckBox("View only");
  public XenConsole main;
  public JPanel backPanel;
  public JPanel controls;
  private Color _backColor;

  public VNCControls(XenConsole main_, JPanel applet_, Color c, boolean showCADButton)
  {
    this.main = main_;
    this.backPanel = applet_;
    this.controls = this;
    this._backColor = c;
    setupConsole();
    setupButtons(showCADButton);
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
    this.scaleCheckBox.setBackground(this._backColor);
    this.viewOnlyCheckBox.setBackground(this._backColor);
  }

  private void initialize()
  {
    BorderLayout layout = new BorderLayout();
    setLayout(layout);

    add(this.buttonPanel, "North");
    add(this.consolePanel, "Center");

    setColors();
    this.consolePanel.addComponentListener(new ComponentListener()
    {
      public void componentHidden(ComponentEvent e)
      {
      }

      public void componentMoved(ComponentEvent e)
      {
      }

      public void componentResized(ComponentEvent e)
      {
        System.out.println("RESIZED");

        if (VNCControls.this.main != null) {
          System.out.println("SET SIZE");
          VNCControls.this.main.canvas_.setMaxHeight(e.getComponent().getHeight());
          VNCControls.this.main.canvas_.setMaxWidth(e.getComponent().getWidth());
        }
        if (VNCControls.this.consolePanel != null) {
          System.out.println("VALIDATE");
          VNCControls.this.consolePanel.invalidate();
          VNCControls.this.consolePanel.validate();
          VNCControls.this.main.canvas_.invalidate();
        }
      }

      public void componentShown(ComponentEvent e) {
      } } );
  }

  private void setupButtons(boolean showCADButton) {
    this.fullscreenButton.setText("Fullscreen (Ctrl+Alt)");
    this.dockButton.setText("Undock");
    this.findconsoleButton.setText("Find Console");
    this.redockButton.setText("Redock Console");
    this.ctrlaltdelButton.setText("Send Ctrl-Alt-Del");

    this.fullscreenButton.setVisible(true);
    this.dockButton.setVisible(true);
    this.redockButton.setVisible(true);
    this.findconsoleButton.setVisible(true);
    this.scaleCheckBox.setVisible(true);
    this.viewOnlyCheckBox.setVisible(true);
    this.ctrlaltdelButton.setVisible(showCADButton);

    FlowLayout layout = new FlowLayout();
    layout.setHgap(0);
    layout.setAlignment(0);
    this.buttonPanel.setLayout(layout);
    this.buttonPanel.add(this.ctrlaltdelButton);
    this.buttonPanel.add(this.dockButton);
    this.buttonPanel.add(this.scaleCheckBox);
    this.buttonPanel.add(this.viewOnlyCheckBox);

    layout = new FlowLayout();
    layout.setAlignment(0);
    this.undockedPanel.setLayout(layout);
    this.undockedPanel.add(this.findconsoleButton);
    this.undockedPanel.add(this.redockButton);
    this.undockedPanel.setVisible(false);

    this.dockButton.addActionListener(dockListener());
    this.fullscreenButton.addActionListener(fullscreenListener());

    this.findconsoleButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e) {
        VNCControls.this.undockedConsole.focus();
      }
    });
    this.scaleCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (VNCControls.this.scaleCheckBox.isSelected())
        {
          JScrollPane sp = (JScrollPane)VNCControls.this.main.containingPanel.getParent().getParent();

          sp.remove(VNCControls.this.main.containingPanel);
          VNCControls.this.consolePanel.remove(sp);
          VNCControls.this.consolePanel.add(VNCControls.this.main.containingPanel, "Center");
          VNCControls.this.consolePanel.revalidate();
        }
        else {
          VNCControls.this.consolePanel.remove(VNCControls.this.main.containingPanel);

          JScrollPane sp = new JScrollPane(VNCControls.this.main.containingPanel);
          sp.setBorder(null);
          VNCControls.this.consolePanel.add(sp);
          VNCControls.this.consolePanel.validate();
        }

        VNCControls.this.main.canvas_.setScaling(VNCControls.this.scaleCheckBox.isSelected());
      }
    });
    this.scaleCheckBox.setSelected(false);

    this.viewOnlyCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        VNCControls.this.main.canvas_.viewOnly = VNCControls.this.viewOnlyCheckBox.isSelected();
      }
    });
    this.redockButton.addActionListener(redockListener());

    this.ctrlaltdelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e) {
        VNCControls.this.main.canvas_.sendCtrlAltDel();
        VNCControls.this.main.canvas_.requestFocusInWindow();
      } } );
  }

  private ActionListener redockListener() {
    return new ActionListener()
    {
      public void actionPerformed(ActionEvent e) {
        VNCControls.this.undockedConsole.dispose();
      }
    };
  }

  public void setupConsole() {
    System.out.println("SETUP CONSOLExxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    BorderLayout layout = new BorderLayout();
    this.consolePanel.setLayout(layout);

    this.consolePanel.removeAll();
    JScrollPane sp = new JScrollPane(this.main.containingPanel);
    sp.setBorder(null);
    this.consolePanel.removeAll();
    this.consolePanel.add(sp, "Center");

    this.consolePanel.revalidate();
  }

  private ActionListener fullscreenListener()
  {
    return new ActionListener()
    {
      public void actionPerformed(ActionEvent e) {
        VNCControls.this.controls.remove(VNCControls.this.consolePanel);
        VNCFullscreen fc = new VNCFullscreen(VNCControls.this.consolePanel, 
          VNCControls.this.main.canvas_, true, VNCControls.this._backColor);
        fc.addWindowListener(new WindowListener() {
          public void windowActivated(WindowEvent e) {
          }

          public void windowClosed(WindowEvent e) {
            VNCControls.this.controls.add(VNCControls.this.consolePanel);
            VNCControls.this.main.canvas_.setMaxHeight(VNCControls.this.consolePanel.getHeight());
            VNCControls.this.main.canvas_.setMaxWidth(VNCControls.this.consolePanel.getWidth());
            VNCControls.this.controls.invalidate();
            VNCControls.this.controls.validate();
            VNCControls.this.main.canvas_.invalidate();
            VNCControls.this.main.canvas_.validate();
            VNCControls.this.main.canvas_.requestFocusInWindow();
          }

          public void windowClosing(WindowEvent e) {
          }

          public void windowDeactivated(WindowEvent e) {
            ((VNCFullscreen)e.getComponent()).dispose();
          }

          public void windowDeiconified(WindowEvent e)
          {
          }

          public void windowIconified(WindowEvent e)
          {
          }

          public void windowOpened(WindowEvent e)
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
      public void actionPerformed(ActionEvent e)
      {
        VNCControls.this.backPanel.remove(VNCControls.this.controls);
        VNCControls.this.undockedPanel.setVisible(true);
        VNCControls.this.undockedConsole = null;
        VNCControls.this.undockedConsole = 
          new VNCFullscreen(VNCControls.this.controls, VNCControls.this.main.canvas_, 
          false, VNCControls.this._backColor);
        VNCControls.this.undockedConsole.setTitle(VNCControls.this.main.getTitle());
        VNCControls.this.undockedConsole.setIconImage(new ImageIcon(Manager.class.getResource("brick.png")).getImage());
        VNCControls.this.dockButton.setText("Redock");

        VNCControls.this.undockedConsole.addWindowListener(new WindowListener() {
          public void windowActivated(WindowEvent e) {
          }

          public void windowClosed(WindowEvent e) {
            VNCControls.this.undockedPanel.setVisible(false);
            VNCControls.this.backPanel.add(VNCControls.this.controls);
            VNCControls.this.dockButton.setText("Undock");

            VNCControls.this.backPanel.invalidate();
            VNCControls.this.backPanel.validate();
            if (VNCControls.this.main != null) {
              VNCControls.this.main.canvas_.setMaxHeight(VNCControls.this.consolePanel.getHeight());
              VNCControls.this.main.canvas_.setMaxWidth(VNCControls.this.consolePanel.getWidth());
              VNCControls.this.main.canvas_.invalidate();
              VNCControls.this.main.canvas_.repaint();
              VNCControls.this.main.canvas_.requestFocusInWindow();
            }
            VNCControls.this.dockButton.removeActionListener(VNCControls.this.dockButton
              .getActionListeners()[0]);
            VNCControls.this.dockButton.addActionListener(VNCControls.this.dockListener());
          }

          public void windowClosing(WindowEvent e)
          {
          }

          public void windowDeactivated(WindowEvent e)
          {
          }

          public void windowDeiconified(WindowEvent e)
          {
          }

          public void windowIconified(WindowEvent e)
          {
          }

          public void windowOpened(WindowEvent e)
          {
          }
        });
        VNCControls.this.backPanel.invalidate();
        VNCControls.this.backPanel.repaint();
        VNCControls.this.dockButton
          .removeActionListener(VNCControls.this.dockButton.getActionListeners()[0]);
        VNCControls.this.dockButton.addActionListener(VNCControls.this.redockListener());
      }
    };
  }
}