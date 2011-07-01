package com.citrix.xenserver.console;

import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class VNCFullscreen extends JFrame
  implements KeyListener
{
  private static final Logger logger = Logger.getLogger(VNCFullscreen.class.getName());
  static final long serialVersionUID = 0L;
  JPanel _panel;
  VNCCanvas _canvas;
  JPanel _buttons;

  public VNCFullscreen(JPanel console, VNCCanvas canvas, boolean fullscreen, Color c)
  {
    setBackground(c);
    this._panel = console;
    this._canvas = canvas;
    if (fullscreen) {
      setUndecorated(true);
      this._canvas.isFullscreen = true;
      this._canvas.screen = this;
      GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().setFullScreenWindow(this);
      setDefaultCloseOperation(2);
      addKeyListener(this);
    } else {
      setSize(800, 600);
    }
    setDefaultCloseOperation(2);
    setVisible(true);
    add(this._panel);
    this._canvas.requestFocusInWindow();
  }

  public void dispose() {
    setVisible(false);
    if (this._panel != null) {
      remove(this._panel);
    }
    if (this._buttons != null) {
      remove(this._buttons);
    }
    super.dispose();
  }

  public void focus() {
    if (getState() == 1) {
      setState(0);
    }
    requestFocus();
    this._canvas.requestFocusInWindow();
  }

  public void NoMessinDispose() {
    super.dispose();
  }

  public void keyPressed(KeyEvent e) {
    if ((e.isControlDown()) && (e.getKeyCode() == 18))
      dispose();
    else
      this._canvas.keyPressed(e);
  }

  public void keyReleased(KeyEvent e)
  {
    this._canvas.keyReleased(e);
  }

  public void keyTyped(KeyEvent e)
  {
    this._canvas.keyTyped(e);
  }
}