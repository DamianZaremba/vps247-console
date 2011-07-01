package com.citrix.xenserver.console;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class VNCCanvas extends JComponent
  implements VNCClient, ClipboardOwner, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, FocusListener
{
  private static final Logger logger = Logger.getLogger(VNCCanvas.class.getName());
  static final long serialVersionUID = 0L;
  private static final KeyMap keyMap = KeyMap.getInstance();

  public boolean scale = false;
  public boolean isFullscreen = false;
  public boolean viewOnly = false;
  public VNCFullscreen screen;
  private static final int BW = 3;
  private final Toolkit toolkit;
  private VNCStream stream_;
  public BufferedImage image_ = null;

  private Graphics2D imageGraphics_ = null;

  private final Rectangle damageStreamside = new Rectangle();
  private Cursor _cursor;
  private final Rectangle damageEventside = new Rectangle();

  private BufferedImage thumbnail = null;

  public final ImageIcon thumbnailIcon = new ImageIcon();

  public Graphics thumbnailGraphics = null;

  private boolean updateThumbnail = true;
  private double thumbnailRatio;
  private String serverText_ = "";
  private Cursor dotCursor;
  private double scaleFactor = 1.0D;
  private double actualScaleFactor = 1.0D;
  private ConsoleListener _console;
  private int _windowHeight;
  private int _windowWidth;
  public int _streamHeight;
  public int _streamWidth;
  private int _desktopWidth;
  private int _desktopHeight;
  public JFrame Frame;
  public JScrollPane ScrollPane;

  public VNCCanvas()
  {
    this.toolkit = Toolkit.getDefaultToolkit();

    Dimension cursorSize = this.toolkit.getBestCursorSize(5, 5);

    int[] pixels = new int[cursorSize.width * cursorSize.height];
    pixels[0] = 0;
    pixels[1] = -1;
    pixels[2] = -1;
    pixels[3] = -1;
    pixels[4] = 0;

    pixels[cursorSize.width] = -1;
    pixels[(cursorSize.width + 1)] = -16777216;
    pixels[(cursorSize.width + 2)] = -16777216;
    pixels[(cursorSize.width + 3)] = -16777216;
    pixels[(cursorSize.width + 4)] = -1;

    pixels[(cursorSize.width * 2)] = -1;
    pixels[(cursorSize.width * 2 + 1)] = -16777216;
    pixels[(cursorSize.width * 2 + 2)] = -16777216;
    pixels[(cursorSize.width * 2 + 3)] = -16777216;
    pixels[(cursorSize.width * 2 + 4)] = -1;

    pixels[(cursorSize.width * 3)] = -1;
    pixels[(cursorSize.width * 3 + 1)] = -16777216;
    pixels[(cursorSize.width * 3 + 2)] = -16777216;
    pixels[(cursorSize.width * 3 + 3)] = -16777216;
    pixels[(cursorSize.width * 3 + 4)] = -1;

    pixels[(cursorSize.width * 4)] = 0;
    pixels[(cursorSize.width * 4 + 1)] = -1;
    pixels[(cursorSize.width * 4 + 2)] = -1;
    pixels[(cursorSize.width * 4 + 3)] = -1;
    pixels[(cursorSize.width * 4 + 4)] = 0;

    Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(cursorSize.width, cursorSize.height, pixels, 0, cursorSize.width));
    this.dotCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "dotCursor");

    this._cursor = this.dotCursor;

    setCursor(this._cursor);

    setOpaque(false);

    setFocusTraversalKeysEnabled(false);
    addFocusListener(this);
    addMouseListener(this);
    addMouseMotionListener(this);
    addMouseWheelListener(this);
    addKeyListener(this);
  }

  public Dimension getPreferredSize() {
    if (this.image_ != null) {
      return new Dimension(Translate(this._streamWidth), 
        Translate(this._streamHeight));
    }
    return super.getPreferredSize();
  }

  public void setScaling(boolean scale)
  {
    this.scale = scale;
    if (!scale) {
      this._streamWidth = (this._desktopWidth + 6);
      this._streamHeight = (this._desktopHeight + 6);
      setScaleFactor(1.0D);
    } else {
      this._streamWidth = this._desktopWidth;
      this._streamHeight = this._desktopHeight;
      setScaleFactor(this.actualScaleFactor);
      setMaxWidth(this._windowWidth);
      setMaxHeight(this._windowHeight);
    }

    SwingUtilities.invokeLater(new Runnable()
    {
      public void run() {
        Container parent = VNCCanvas.this.getParent();
        if (parent != null) {
          parent.invalidate();
          parent.validate();
        }
      } } );
  }

  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  public void setStream(VNCStream stream) {
    this.stream_ = stream;
  }

  private void setScaleFactor(double sf)
  {
    if (!this.scale) {
      this.actualScaleFactor = sf;
      this.scaleFactor = 1.0D;
    } else {
      this.scaleFactor = sf;
    }
  }

  private int Translate(int i) {
    return (int)(i * this.scaleFactor);
  }

  private int TranslateRev(int i) {
    return (int)(i / (this.scaleFactor - 3.0D / this._streamHeight));
  }

  public void setUpdateThumbnail(boolean updateThumbnail)
  {
  }

  public void setConsoleListener(ConsoleListener console)
  {
    this._console = console;
  }

  public void paintComponent(Graphics graphics)
  {
    if ((this.image_ != null) && (this.ui == null)) {
      Graphics2D g2 = (Graphics2D)graphics;
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, 
        RenderingHints.VALUE_RENDER_SPEED);
      g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, 
        RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
        RenderingHints.VALUE_ANTIALIAS_OFF);
      g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, 
        RenderingHints.VALUE_COLOR_RENDER_SPEED);
      g2.setRenderingHint(RenderingHints.KEY_DITHERING, 
        RenderingHints.VALUE_DITHER_DISABLE);
      if (!this.isFullscreen)
        g2.drawImage(this.image_, 3, 3, Translate(this._streamWidth) - 6, 
          Translate(this._streamHeight) - 6, null);
      else
        g2.drawImage(this.image_, 0, 0, Translate(this._streamWidth), 
          Translate(this._streamHeight), null);
    }
  }

  public void paintBorder(Graphics graphics)
  {
    setSize(Translate(this._streamWidth), Translate(this._streamHeight));
    super.paintBorder(graphics);
  }

  public void paintComponents(Graphics graphics)
  {
  }

  public int getConsoleLeft()
  {
    return (this._windowWidth - Translate(this._streamWidth)) / 2;
  }

  public int getConsoleTop() {
    return (this._windowHeight - Translate(this._streamHeight)) / 2;
  }

  public void setMaxHeight(int h) {
    this._windowHeight = h;
    if ((this._streamHeight > 1) && (this._streamWidth > 1)) {
      double sf1 = this._windowHeight / this._streamHeight;
      double sf2 = this._windowWidth / this._streamWidth;
      if (sf1 < sf2)
        setScaleFactor(sf1);
      else
        setScaleFactor(sf2);
    }
    else {
      setScaleFactor(1.0D);
    }
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run() {
        Container parent = VNCCanvas.this.getParent();
        if (parent != null) {
          parent.invalidate();
          parent.validate();
        }
      }
    });
  }

  public void setMaxWidth(int w) {
    this._windowWidth = w;
    if ((this._streamWidth > 1) && (this._streamHeight > 1)) {
      double sf1 = this._windowHeight / this._streamHeight;
      double sf2 = this._windowWidth / this._streamWidth;
      if (sf1 < sf2)
        setScaleFactor(sf1);
      else
        setScaleFactor(sf2);
    }
    else {
      setScaleFactor(1.0D);
    }
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run() {
        Container parent = VNCCanvas.this.getParent();
        if (parent != null) {
          parent.invalidate();
          parent.validate();
        }
      }
    });
  }

  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible)
      this.stream_.unpause();
    else
      this.stream_.pause();
  }

  private void pointerEvent(MouseEvent event)
  {
    if ((hasFocus()) && (!this.viewOnly)) {
      int buttonMask = 0;
      if ((event.getModifiersEx() & 0x400) != 0) {
        buttonMask |= 1;
      }
      if ((event.getModifiersEx() & 0x800) != 0) {
        buttonMask |= 2;
      }
      if ((event.getModifiersEx() & 0x1000) != 0) {
        buttonMask |= 4;
      }
      this.stream_.pointerEvent(buttonMask, TranslateRev(event.getX()), 
        TranslateRev(event.getY()));
    }
  }

  public void mouseReleased(MouseEvent event)
  {
    if (!this.viewOnly) {
      pointerEvent(event);
      if (hasFocus())
        if (this._cursor != null) {
          setCursor(this._cursor);
        } else {
          int[] pixels = new int[256];
          Image image = Toolkit.getDefaultToolkit().createImage(
            new MemoryImageSource(16, 16, pixels, 0, 
            16));
          setCursor(Toolkit.getDefaultToolkit().createCustomCursor(image, 
            new Point(0, 0), "invisibleCursor"));
        }
    }
  }

  public void mousePressed(MouseEvent event)
  {
    if (!this.viewOnly) {
      if (this._cursor != null) {
        setCursor(this._cursor);
      } else {
        int[] pixels = new int[256];
        Image image = Toolkit.getDefaultToolkit()
          .createImage(
          new MemoryImageSource(16, 16, 
          pixels, 0, 16));
        setCursor(Toolkit.getDefaultToolkit().createCustomCursor(image, 
          new Point(0, 0), "invisibleCursor"));
      }

      if (!hasFocus()) {
        requestFocusInWindow();
        setCursor(Cursor.getDefaultCursor());
      }
      pointerEvent(event);
    }
  }

  public void mouseExited(MouseEvent event) {
    setCursor(Cursor.getDefaultCursor());
  }

  public void mouseEntered(MouseEvent event)
  {
    if (hasFocus()) {
      if (this._cursor != null) {
        setCursor(this._cursor);
      } else {
        int[] pixels = new int[256];
        Image image = Toolkit.getDefaultToolkit().createImage(
          new MemoryImageSource(16, 16, pixels, 0, 
          16));
        setCursor(Toolkit.getDefaultToolkit().createCustomCursor(image, 
          new Point(0, 0), "invisibleCursor"));
      }
    }
    else setCursor(Cursor.getDefaultCursor());

    if (!this.viewOnly)
      pointerEvent(event);
  }

  public void mouseClicked(MouseEvent event)
  {
    if (!hasFocus())
      requestFocusInWindow();
  }

  public void mouseMoved(MouseEvent event)
  {
    if (!this.viewOnly)
      pointerEvent(event);
  }

  public void mouseDragged(MouseEvent event)
  {
    if (!this.viewOnly)
      pointerEvent(event);
  }

  public void mouseWheelMoved(MouseWheelEvent event)
  {
    if ((hasFocus()) && (!this.viewOnly)) {
      int x = event.getX();
      int y = event.getY();
      int r = event.getWheelRotation();

      this.stream_.pointerWheelEvent(TranslateRev(x), TranslateRev(y), r);
      event.consume();
    }
  }

  public void keyTyped(KeyEvent event) {
    if (!this.viewOnly)
      event.consume();
  }

  public void sendCtrlAltDel()
  {
    this.stream_.sendCtrlAltDelete();
  }

  private void key(KeyEvent event, boolean pressed) {
    if ((hasFocus()) && (!this.viewOnly)) {
      if ((event.getKeyCode() == 18) && (event.isControlDown()) && 
        (this.isFullscreen))
      {
        this.screen.dispose();
        this.isFullscreen = false;
      }
      else if ((event.getKeyCode() != 18) || 
        (!event.isControlDown()))
      {
        int keysym = keyMap.getKeysym(event);
        if (keysym != -1) {
          this.stream_.keyEvent(pressed, keysym);
        }
      }
      event.consume();
    }
  }

  public void keyReleased(KeyEvent event) {
    if (!this.viewOnly)
      key(event, false);
  }

  public void clientBell()
  {
  }

  public void keyPressed(KeyEvent event)
  {
    if (!this.viewOnly)
      key(event, true);
  }

  public void clientDesktopSize(int width, int height)
  {
    this._console.writeline("Desktop size is now " + width + "; " + height);
    this._streamHeight = height;
    this._streamWidth = width;
    this._desktopWidth = width;
    this._desktopHeight = height;

    if (!this.scale) {
      this._streamWidth += 6;
      this._streamHeight += 6;
    }

    if ((this._windowHeight != 0) && (this._windowWidth != 0)) {
      setMaxHeight(this._windowHeight);
      setMaxWidth(this._windowWidth);
    }

    BufferedImage image2 = new BufferedImage(width, height, 
      1);
    this.imageGraphics_ = ((Graphics2D)image2.getGraphics());

    SwingUtilities.invokeLater(new Runnable(image2) {
      public void run() {
        VNCCanvas.this.image_ = this.val$image2;
        Container parent = VNCCanvas.this.getParent();
        if (parent != null) {
          parent.invalidate();
          parent.validate();
        }
      }
    });
  }

  private void damage(int x, int y, int width, int height)
  {
    x += 3;
    y += 3;

    if (this.damageStreamside.isEmpty()) {
      this.damageStreamside.x = Translate(x);
      this.damageStreamside.y = Translate(y);
      this.damageStreamside.width = Translate(width);
      this.damageStreamside.height = Translate(height);
    } else {
      this.damageStreamside.add(
        new Rectangle(Translate(x), Translate(y), 
        Translate(width), Translate(height)));
    }
  }

  private void repair()
  {
    if (!this.damageEventside.isEmpty())
    {
      paintImmediately(this.damageEventside);
      this.damageEventside.width = 0;
    }
  }

  private void repairThumbnail()
  {
    Image i = this.image_;
    System.out.println("Updating thumbnail");
    if (i != null) {
      int w = Translate(this._streamWidth);
      int h = Translate(this._streamHeight);
      this.thumbnailGraphics.drawImage(
        i, 
        0, 0, (int)(w * this.thumbnailRatio), (int)(h * this.thumbnailRatio), 
        0, 0, w, h, null);
    }
  }

  public void clientFrameBufferUpdate()
  {
    try
    {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          VNCCanvas.this.damageEventside.add(VNCCanvas.this.damageStreamside);
          VNCCanvas.this.damageStreamside.width = 0;
          VNCCanvas.this.repair();
        } } );
    }
    catch (InterruptedException exn) {
      this._console.writeline(exn.getMessage());
    } catch (InvocationTargetException exn) {
      this._console.writeline(exn.getMessage());
    }
  }

  public void clientDrawImage(Image image, int x, int y, int width, int height)
  {
    this.imageGraphics_.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
      RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    this.imageGraphics_.setRenderingHint(RenderingHints.KEY_RENDERING, 
      RenderingHints.VALUE_RENDER_SPEED);
    this.imageGraphics_.drawImage(image, x, y, null);
    damage(x, y, width, height);
  }

  public void clientSetCursor(Image image, int x, int y)
  {
    int imageWidth = image.getWidth(null);
    int imageHeight = image.getHeight(null);
    Dimension cursorSize = this.toolkit.getBestCursorSize(imageWidth, 
      imageHeight);

    if ((imageWidth <= cursorSize.width) && (imageHeight <= cursorSize.height) && (
      (imageWidth < cursorSize.width) || (imageHeight < cursorSize.height))) {
      BufferedImage bi = new BufferedImage(cursorSize.width, 
        cursorSize.height, 2);
      Graphics2D g = (Graphics2D)bi.getGraphics();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, 
        RenderingHints.VALUE_RENDER_SPEED);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.setColor(new Color(0, 0, 0, 0));
      g.fillRect(0, 0, cursorSize.width, cursorSize.height);
      g.drawImage(image, 0, 0, null);
      image = bi;
    }
    Cursor cursor;
    Cursor cursor;
    if ((imageWidth == 0) && (imageHeight == 0)) {
      System.out.println("********** NO CURSOR ***********");
      cursor = new Cursor(1);
    } else {
      System.out.println("W = " + imageWidth + ", H = " + imageHeight);
      cursor = this.toolkit.createCustomCursor(image, 
        new Point(x, y), "");
    }

    this._cursor = cursor;

    SwingUtilities.invokeLater(new Runnable(cursor) {
      public void run() {
        VNCCanvas.this.setCursor(this.val$cursor);
      }
    });
  }

  public void clientCopyRectangle(int x, int y, int width, int height, int dx, int dy)
  {
    this.imageGraphics_.copyArea(x, y, width, height, dx - x, dy - y);
    damage(dx, dy, width, height);
  }

  public void clientFillRectangle(int x, int y, int width, int height, Color color)
  {
    this.imageGraphics_.setColor(color);
    this.imageGraphics_.fillRect(x, y, width, height);
    damage(x, y, width, height);
  }

  public void clientCutText(String text)
  {
    SwingUtilities.invokeLater(new Runnable(text) {
      public void run() {
        VNCCanvas.this.serverText_ = this.val$text;
      } } );
  }

  public void getClipboard() {
    StringSelection stringSelection = new StringSelection(this.serverText_);
    Clipboard clipboard = this.toolkit.getSystemClipboard();
    try {
      clipboard.setContents(stringSelection, this);
    } catch (IllegalStateException e) {
      this._console.writeline(e.getMessage());
    }
  }

  public void setClipboard() {
    Clipboard clipboard = this.toolkit.getSystemClipboard();
    Transferable contents = clipboard.getContents(null);
    if ((contents != null) && 
      (contents.isDataFlavorSupported(DataFlavor.stringFlavor)))
      try {
        String text = (String)contents
          .getTransferData(DataFlavor.stringFlavor);
        this.stream_.clientCutText(text);
      } catch (Throwable t) {
        this._console.writeline(t.getMessage());
      }
  }

  public void lostOwnership(Clipboard clipboard, Transferable contents)
  {
  }

  public void focusGained(FocusEvent e)
  {
    if (this._cursor != null) {
      setCursor(this._cursor);
    } else {
      int[] pixels = new int[256];
      Image image = Toolkit.getDefaultToolkit()
        .createImage(
        new MemoryImageSource(16, 16, 
        pixels, 0, 16));
      setCursor(Toolkit.getDefaultToolkit().createCustomCursor(image, 
        new Point(0, 0), "invisibleCursor"));
    }
    if (!this.isFullscreen)
      setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.BLUE, 1), 
        BorderFactory.createLineBorder(getParent().getBackground(), 2)));
    else {
      setBorder(BorderFactory.createEmptyBorder());
    }
    setFocusable(true);

    sendRelease(18);
    sendRelease(17);
    sendRelease(16);
  }

  private void sendRelease(int keycode) {
    this.stream_.keyEvent(false, keyMap.getMappedKey(keycode));
  }

  public void focusLost(FocusEvent e)
  {
    if (getParent() != null) {
      if (!this.isFullscreen)
        setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(getParent().getBackground(), 1), 
          BorderFactory.createLineBorder(getParent()
          .getBackground(), 2)));
      else {
        setBorder(BorderFactory.createEmptyBorder());
      }
      setCursor(Cursor.getDefaultCursor());
    }
  }

  public ImageIcon getThumbnailIcon() {
    return this.thumbnailIcon;
  }
}