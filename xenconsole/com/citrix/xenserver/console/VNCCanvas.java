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
  private static final Logger logger;
  static final long serialVersionUID = 0L;
  private static final KeyMap keyMap;
  public boolean isFullscreen = false;
  public VNCFullscreen screen;
  private static final int BW = 0;
  private final Toolkit toolkit;
  private VNCStream stream_;
  private BufferedImage image_ = null;

  private Graphics2D imageGraphics_ = null;

  private final Rectangle damageStreamside = new Rectangle();
  private Cursor _cursor;
  private final Rectangle damageEventside = new Rectangle();

  private final ImageIcon thumbnailIcon = new ImageIcon();

  private String serverText_ = "";

  private double scaleFactor = 1.0D;
  private ConsoleListener _console;
  private int _windowHeight;
  private int _windowWidth;
  private int _streamHeight;
  private int _streamWidth;
  public JFrame Frame;
  public JScrollPane ScrollPane;

  public VNCCanvas()
  {
    this.toolkit = Toolkit.getDefaultToolkit();

    setDoubleBuffered(true);
    setOpaque(false);

    BufferedImage localBufferedImage = new BufferedImage(200, 150, 1);

    Graphics localGraphics = localBufferedImage.getGraphics();
    localGraphics.fillRect(0, 0, 200, 150);
    this.thumbnailIcon.setImage(localBufferedImage);

    setFocusTraversalKeysEnabled(false);
    addFocusListener(this);
    addMouseListener(this);
    addMouseMotionListener(this);
    addMouseWheelListener(this);
    addKeyListener(this);
  }

  public Dimension getPreferredSize()
  {
    if (this.image_ != null)
    {
      return new Dimension((int)(this.scaleFactor * 10000.0D), (int)(this.scaleFactor * 10000.0D));
    }
    return super.getPreferredSize();
  }

  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  public void setStream(VNCStream paramVNCStream) {
    this.stream_ = paramVNCStream;
  }

  private void setScaleFactor(double paramDouble)
  {
    this.scaleFactor = paramDouble;
  }

  private int Translate(int paramInt) {
    return paramInt * 1;
  }

  private int TranslateRev(int paramInt) {
    return (int)(paramInt / (1.0D - 0.0D / this._streamHeight));
  }

  public void setUpdateThumbnail(boolean paramBoolean) {
    assert (SwingUtilities.isEventDispatchThread());

    if (paramBoolean)
      repairThumbnail();
  }

  public void setConsoleListener(ConsoleListener paramConsoleListener)
  {
    this._console = paramConsoleListener;
  }

  public void paintComponent(Graphics paramGraphics)
  {
    if ((this.image_ != null) && (this.ui == null)) {
      Graphics2D localGraphics2D = (Graphics2D)paramGraphics;
      localGraphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      localGraphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

      localGraphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

      localGraphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

      localGraphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);

      localGraphics2D.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);

      if (!this.isFullscreen) {
        localGraphics2D.drawImage(this.image_, 0, 0, Translate(this._streamWidth) - 0, Translate(this._streamHeight) - 0, null);
      }
      else
      {
        localGraphics2D.drawImage(this.image_, 0, 0, Translate(this._streamWidth), Translate(this._streamHeight), null);
      }
    }
  }

  public void paintBorder(Graphics paramGraphics)
  {
    setSize(Translate(this._streamWidth) + 3, Translate(this._streamHeight) + 3);
    super.paintBorder(paramGraphics);
  }

  public void paintComponents(Graphics paramGraphics)
  {
  }

  public int getConsoleLeft()
  {
    return (this._windowWidth - Translate(this._streamWidth)) / 2 + 3;
  }

  public int getConsoleTop() {
    return (this._windowHeight - Translate(this._streamHeight)) / 2 + 3;
  }

  public void setMaxHeight(int paramInt) {
    this._windowHeight = (paramInt + 6);
    if ((this._streamHeight > 1) && (this._streamWidth > 1)) {
      double d1 = this._windowHeight / this._streamHeight;
      double d2 = this._windowWidth / this._streamWidth;
      if (d1 < d2)
        setScaleFactor(d1);
      else
        setScaleFactor(d2);
    }
    else {
      setScaleFactor(1.0D);
    }
  }

  public void setMaxWidth(int paramInt) {
    this._windowWidth = (paramInt + 6);
    if ((this._streamWidth > 1) && (this._streamHeight > 1)) {
      double d1 = this._windowHeight / this._streamHeight;
      double d2 = this._windowWidth / this._streamWidth;
      if (d1 < d2)
        setScaleFactor(d1);
      else
        setScaleFactor(d2);
    }
    else {
      setScaleFactor(1.0D);
    }
  }

  public void setVisible(boolean paramBoolean)
  {
    super.setVisible(paramBoolean);
    if (paramBoolean)
      this.stream_.unpause();
    else
      this.stream_.pause();
  }

  private void pointerEvent(MouseEvent paramMouseEvent)
  {
    if (hasFocus()) {
      int i = 0;
      if ((paramMouseEvent.getModifiersEx() & 0x400) != 0) {
        i |= 1;
      }
      if ((paramMouseEvent.getModifiersEx() & 0x800) != 0) {
        i |= 2;
      }
      if ((paramMouseEvent.getModifiersEx() & 0x1000) != 0) {
        i |= 4;
      }
      this.stream_.pointerEvent(i, TranslateRev(paramMouseEvent.getX()), TranslateRev(paramMouseEvent.getY()));
    }
  }

  public void mouseReleased(MouseEvent paramMouseEvent)
  {
    pointerEvent(paramMouseEvent);
    if (hasFocus())
      if (this._cursor != null) {
        setCursor(this._cursor);
      } else {
        int[] arrayOfInt = new int[256];
        Image localImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, arrayOfInt, 0, 16));

        setCursor(Toolkit.getDefaultToolkit().createCustomCursor(localImage, new Point(0, 0), "invisibleCursor"));
      }
  }

  public void mousePressed(MouseEvent paramMouseEvent)
  {
    if (this._cursor != null) {
      setCursor(this._cursor);
    } else {
      int[] arrayOfInt = new int[256];
      Image localImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, arrayOfInt, 0, 16));

      setCursor(Toolkit.getDefaultToolkit().createCustomCursor(localImage, new Point(0, 0), "invisibleCursor"));
    }

    if (!hasFocus()) {
      requestFocusInWindow();
      setCursor(Cursor.getDefaultCursor());
    }
    pointerEvent(paramMouseEvent);
  }

  public void mouseExited(MouseEvent paramMouseEvent) {
    setCursor(Cursor.getDefaultCursor());
  }

  public void mouseEntered(MouseEvent paramMouseEvent)
  {
    if (hasFocus()) {
      if (this._cursor != null) {
        setCursor(this._cursor);
      } else {
        int[] arrayOfInt = new int[256];
        Image localImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, arrayOfInt, 0, 16));

        setCursor(Toolkit.getDefaultToolkit().createCustomCursor(localImage, new Point(0, 0), "invisibleCursor"));
      }
    }
    else {
      setCursor(Cursor.getDefaultCursor());
    }
    pointerEvent(paramMouseEvent);
  }

  public void mouseClicked(MouseEvent paramMouseEvent)
  {
    if (!hasFocus())
      requestFocusInWindow();
  }

  public void mouseMoved(MouseEvent paramMouseEvent)
  {
    pointerEvent(paramMouseEvent);
  }

  public void mouseDragged(MouseEvent paramMouseEvent) {
    pointerEvent(paramMouseEvent);
  }

  public void mouseWheelMoved(MouseWheelEvent paramMouseWheelEvent)
  {
    if (hasFocus()) {
      int i = paramMouseWheelEvent.getX();
      int j = paramMouseWheelEvent.getY();
      int k = paramMouseWheelEvent.getWheelRotation();

      this.stream_.pointerWheelEvent(TranslateRev(i), TranslateRev(j), k);
      paramMouseWheelEvent.consume();
    }
  }

  public void keyTyped(KeyEvent paramKeyEvent) {
    paramKeyEvent.consume();
  }

  public void sendCtrlAltDel() {
    this.stream_.sendCtrlAltDelete();
  }

  private void key(KeyEvent paramKeyEvent, boolean paramBoolean) {
    if (hasFocus()) {
      if ((paramKeyEvent.getKeyCode() == 18) && (paramKeyEvent.isControlDown()) && (this.isFullscreen))
      {
        this.screen.dispose();
        this.isFullscreen = false;
      }
      else if ((paramKeyEvent.getKeyCode() != 18) || (!paramKeyEvent.isControlDown()))
      {
        int i = keyMap.getKeysym(paramKeyEvent);
        if (i != -1) {
          this.stream_.keyEvent(paramBoolean, i);
        }
      }
      paramKeyEvent.consume();
    }
  }

  public void keyReleased(KeyEvent paramKeyEvent) {
    key(paramKeyEvent, false);
  }

  public void clientBell()
  {
  }

  public void keyPressed(KeyEvent paramKeyEvent)
  {
    key(paramKeyEvent, true);
  }

  public void clientDesktopSize(int paramInt1, int paramInt2)
  {
    this._console.writeline("Desktop size is now " + paramInt1 + "; " + paramInt2);
    this._streamHeight = paramInt2;
    this._streamWidth = paramInt1;

    if ((this._windowHeight != 0) && (this._windowWidth != 0)) {
      setMaxHeight(this._windowHeight);
      setMaxWidth(this._windowWidth);
    }

    BufferedImage localBufferedImage = new BufferedImage(paramInt1, paramInt2, 1);

    this.imageGraphics_ = ((Graphics2D)localBufferedImage.getGraphics());

    SwingUtilities.invokeLater(new Runnable(localBufferedImage) {
      public void run() {
        VNCCanvas.access$002(VNCCanvas.this, this.val$image2);
        Container localContainer = VNCCanvas.this.getParent();
        if (localContainer != null) {
          localContainer.invalidate();
          localContainer.validate();
        }
      }
    });
  }

  private void damage(int paramInt1, int paramInt2, int paramInt3, int paramInt4)
  {
    paramInt1 += 0;
    paramInt2 += 0;

    if (this.damageStreamside.isEmpty()) {
      this.damageStreamside.x = Translate(paramInt1);
      this.damageStreamside.y = Translate(paramInt2);
      this.damageStreamside.width = Translate(paramInt3);
      this.damageStreamside.height = Translate(paramInt4);
    } else {
      this.damageStreamside.add(new Rectangle(Translate(paramInt1), Translate(paramInt2), Translate(paramInt3), Translate(paramInt4)));
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
    BufferedImage localBufferedImage = this.image_;
    if (localBufferedImage != null);
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
    } catch (InterruptedException localInterruptedException) {
      this._console.writeline(localInterruptedException.getMessage());
    } catch (InvocationTargetException localInvocationTargetException) {
      this._console.writeline(localInvocationTargetException.getMessage());
    }
  }

  public void clientDrawImage(Image paramImage, int paramInt1, int paramInt2, int paramInt3, int paramInt4)
  {
    this.imageGraphics_.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

    this.imageGraphics_.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

    this.imageGraphics_.drawImage(paramImage, paramInt1, paramInt2, null);
    damage(paramInt1, paramInt2, paramInt3, paramInt4);
  }

  public void clientSetCursor(Image paramImage, int paramInt1, int paramInt2)
  {
    int i = paramImage.getWidth(null);
    int j = paramImage.getHeight(null);
    Dimension localDimension = this.toolkit.getBestCursorSize(i, j);

    if ((i <= localDimension.width) && (j <= localDimension.height) && ((i < localDimension.width) || (j < localDimension.height)))
    {
      localObject = new BufferedImage(localDimension.width, localDimension.height, 2);

      Graphics2D localGraphics2D = (Graphics2D)((BufferedImage)localObject).getGraphics();
      localGraphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

      localGraphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

      localGraphics2D.setColor(new Color(0, 0, 0, 0));
      localGraphics2D.fillRect(0, 0, localDimension.width, localDimension.height);
      localGraphics2D.drawImage(paramImage, 0, 0, null);
      paramImage = (Image)localObject;
    }

    Object localObject = this.toolkit.createCustomCursor(paramImage, new Point(paramInt1, paramInt2), "");

    this._cursor = ((Cursor)localObject);

    SwingUtilities.invokeLater(new Runnable((Cursor)localObject) {
      public void run() {
        VNCCanvas.this.setCursor(this.val$cursor);
      }
    });
  }

  public void clientCopyRectangle(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6)
  {
    this.imageGraphics_.copyArea(paramInt1, paramInt2, paramInt3, paramInt4, paramInt5 - paramInt1, paramInt6 - paramInt2);
    damage(paramInt5, paramInt6, paramInt3, paramInt4);
  }

  public void clientFillRectangle(int paramInt1, int paramInt2, int paramInt3, int paramInt4, Color paramColor)
  {
    this.imageGraphics_.setColor(paramColor);
    this.imageGraphics_.fillRect(paramInt1, paramInt2, paramInt3, paramInt4);
    damage(paramInt1, paramInt2, paramInt3, paramInt4);
  }

  public void clientCutText(String paramString)
  {
    SwingUtilities.invokeLater(new Runnable(paramString) {
      public void run() {
        VNCCanvas.access$402(VNCCanvas.this, this.val$text);
      } } );
  }

  public void getClipboard() {
    StringSelection localStringSelection = new StringSelection(this.serverText_);
    Clipboard localClipboard = this.toolkit.getSystemClipboard();
    try {
      localClipboard.setContents(localStringSelection, this);
    } catch (IllegalStateException localIllegalStateException) {
      this._console.writeline(localIllegalStateException.getMessage());
    }
  }

  public void setClipboard() {
    Clipboard localClipboard = this.toolkit.getSystemClipboard();
    Transferable localTransferable = localClipboard.getContents(null);
    if ((localTransferable != null) && (localTransferable.isDataFlavorSupported(DataFlavor.stringFlavor)))
      try
      {
        String str = (String)localTransferable.getTransferData(DataFlavor.stringFlavor);

        this.stream_.clientCutText(str);
      } catch (Throwable localThrowable) {
        this._console.writeline(localThrowable.getMessage());
      }
  }

  public void lostOwnership(Clipboard paramClipboard, Transferable paramTransferable)
  {
  }

  public void focusGained(FocusEvent paramFocusEvent)
  {
    if (this._cursor != null) {
      setCursor(this._cursor);
    } else {
      int[] arrayOfInt = new int[256];
      Image localImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, arrayOfInt, 0, 16));

      setCursor(Toolkit.getDefaultToolkit().createCustomCursor(localImage, new Point(0, 0), "invisibleCursor"));
    }

    if (!this.isFullscreen) {
      setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLUE, 1), BorderFactory.createLineBorder(getParent().getBackground(), 2)));
    }
    else
    {
      setBorder(BorderFactory.createEmptyBorder());
    }
    setFocusable(true);

    sendRelease(18);
    sendRelease(17);
    sendRelease(16);
  }

  private void sendRelease(int paramInt) {
    this.stream_.keyEvent(false, keyMap.getMappedKey(paramInt));
  }

  public void focusLost(FocusEvent paramFocusEvent)
  {
    if (getParent() != null) {
      if (!this.isFullscreen) {
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getParent().getBackground(), 1), BorderFactory.createLineBorder(getParent().getBackground(), 2)));
      }
      else
      {
        setBorder(BorderFactory.createEmptyBorder());
      }
      setCursor(Cursor.getDefaultCursor());
    }
  }

  public ImageIcon getThumbnailIcon() {
    return this.thumbnailIcon;
  }

  static
  {
    logger = Logger.getLogger(VNCCanvas.class.getName());

    keyMap = KeyMap.getInstance();
  }
}