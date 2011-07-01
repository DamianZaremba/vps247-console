package com.citrix.xenserver.console;

import java.awt.Color;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DirectColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.swing.SwingUtilities;

public class VNCStream
{
  private static final Logger logger;
  public static final int NO_IP = 0;
  public static final int INCORRECT_PASSWORD = 1;
  public static final int CONNECTION_FAILED = 2;
  public static final int CONNECTION_SUCCEEDED = 4;
  private static final int THUMBNAIL_SLEEP_TIME = 500;
  private static final ColorSpace colorSpace_;
  private static final int RAW_ENCODING = 0;
  private static final int COPY_RECTANGLE_ENCODING = 1;
  private static final int RRE_ENCODING = 2;
  private static final int CORRE_ENCODING = 4;
  private static final int HEXTILE_ENCODING = 5;
  private static final int CURSOR_PSEUDO_ENCODING = -239;
  private static final int DESKTOP_SIZE_PSEUDO_ENCODING = -223;
  private static final int SET_PIXEL_FORMAT = 0;
  private static final int SET_ENCODINGS = 2;
  private static final int FRAMEBUFFER_UPDATE_REQUEST = 3;
  private static final int KEY_EVENT = 4;
  private static final int POINTER_EVENT = 5;
  private static final int CLIENT_CUT_TEXT = 6;
  private static final int RAW_SUBENCODING = 1;
  private static final int BACKGROUND_SPECIFIED_SUBENCODING = 2;
  private static final int FOREGROUND_SPECIFIED_SUBENCODING = 4;
  private static final int ANY_SUBRECTS_SUBENCODING = 8;
  private static final int SUBRECTS_COLORED_SUBENCODING = 16;
  private static final int FRAME_BUFFER_UPDATE = 0;
  private static final int BELL = 2;
  private static final int SERVER_CUT_TEXT = 3;
  private static final int MAX_STRING_LENGTH = 65536;
  public boolean _connected = false;
  private static final int[] encodings_32;
  private static final int[] encodings_8;
  private static final int KEY_CTRL_L = 65507;
  private static final int KEY_ALT_L = 65513;
  private static final int KEY_DELETE = 65535;
  private final VNCClient client_;
  public Helper helper = null;
  private ConnectionListener _listener;
  private ConsoleListener _console;

  static
  {
    logger = Logger.getLogger(VNCStream.class.getName());

    colorSpace_ = 
      ColorSpace.getInstance(1000);

    encodings_32 = new int[] { 5, 
      4, 2, 1, 
      0, -239, -223 };

    encodings_8 = new int[] { 5, 
      4, 2, 1, 
      0, -223 };
    try
    {
      byte[] keyBytes = new byte[8];
      byte[] challenge = new byte[16];
      Cipher cipher = Cipher.getInstance("DES");
      cipher.init(1, 
        SecretKeyFactory.getInstance("DES")
        .generateSecret(new DESKeySpec(keyBytes)));
      cipher.doFinal(challenge);
    } catch (Exception e) {
      if (!$assertionsDisabled) throw new AssertionError(e);
    }
  }

  public VNCStream(VNCClient client, ConnectionListener listener, ConsoleListener console)
  {
    this.client_ = client;
    this._listener = listener;
    this._console = console;
  }

  public void connect(RawHTTP rawHttp, char[] password)
    throws IOException
  {
    this._console.writeline("Connecting...");
    synchronized (this)
    {
      Helper h;
      this.helper = 
        (h = new Helper(this.client_, rawHttp, password, this._listener, 
        this._console));
    }
    Helper h;
    h.start();
    this._connected = true;
    this._console.writeline("Connected");
  }

  public void connectSocket(Socket socket, char[] password)
    throws IOException
  {
    this._console.writeline("Connecting...");
    synchronized (this)
    {
      Helper h;
      this.helper = 
        (h = new Helper(this.client_, socket, password, this._listener, 
        this._console));
    }
    Helper h;
    h.start();
    this._connected = true;
    this._console.writeline("Connected");
  }

  public void disconnect()
  {
    synchronized (this) {
      Helper h = this.helper;
      this.helper = null;
    }
    Helper h;
    if (h != null) {
      h.terminate();
    }
    this._connected = false;
  }

  public synchronized void pause() {
    if (this.helper != null)
      this.helper.pause();
  }

  public boolean isConnected()
  {
    return this._connected;
  }

  public synchronized void unpause() {
    if (this.helper != null)
      this.helper.unpause();
  }

  public synchronized void setUpdateThumbnail(boolean updateThumbnail)
  {
    if (this.helper != null)
      this.helper.setUpdateThumbnail(updateThumbnail);
  }

  public synchronized void keyEvent(boolean down, int key)
  {
    if (this.helper != null)
      this.helper.keyEvent(down, key);
  }

  public synchronized void pointerEvent(int buttonMask, int x, int y)
  {
    if (this.helper != null)
      this.helper.pointerEvent(buttonMask, x, y);
  }

  public synchronized void sendCtrlAltDelete()
  {
    if (this.helper != null)
      this.helper.sendCtrlAltDelete();
  }

  public synchronized void pointerWheelEvent(int x, int y, int r)
  {
    if (this.helper != null)
      this.helper.pointerWheelEvent(x, y, r);
  }

  public synchronized void clientCutText(String text)
  {
    if (this.helper != null)
      this.helper.clientCutText(text);
  }

  private static final class Helper extends Thread
  {
    private final VNCClient client_;
    private final char[] password;
    private final DataInputStream in_;
    private final DataOutputStream out_;
    private final Socket socket;
    private volatile boolean running = true;
    private volatile Thread runThread = null;

    private boolean paused = false;
    private boolean updateThumbnail = false;
    private int width_;
    private int height_;
    private VNCPixelFormat pixelFormat_;
    private int[] bandOffsets32_;
    private ColorModel colorModel32_;
    private int[] bitMasks8_;
    private ColorModel colorModel8_;
    private byte[] data = new byte[1530];
    private ConnectionListener _listener;
    private ConsoleListener _console;

    private void writePadding(int n)
      throws IOException
    {
      for (int i = 0; i < n; i++)
        this.out_.write(0);
    }

    private void writeFlag(boolean v) throws IOException
    {
      this.out_.writeByte(v ? 1 : 0);
    }

    private void writeInt8(int v) throws IOException {
      this.out_.writeByte(v);
    }

    private void writeInt16(int v) throws IOException {
      this.out_.writeShort(v);
    }

    private void writeInt32(int v) throws IOException {
      this.out_.writeInt(v);
    }

    private void writeString(String s) throws IOException {
      writeInt32(s.length());
      this.out_.write(s.getBytes("US-ASCII"));
    }

    private void readPadding(int n) throws IOException {
      this.in_.skip(n);
    }

    private void readFully(byte[] b, int off, int len) throws IOException {
      int n = 0;
      while (n < len) {
        int count = this.in_.read(b, off + n, len - n);
        if (count < 0) {
          throw new EOFException();
        }
        n += count;
      }
    }

    private boolean readFlag() throws IOException {
      return readCard8() != 0;
    }

    private int readCard8() throws IOException {
      int v = this.in_.read();
      if (v < 0) {
        throw new EOFException();
      }
      return v;
    }

    private int readCard16() throws IOException {
      int b1 = readCard8();
      int b0 = readCard8();
      return (short)(b1 << 8 | b0);
    }

    private int readCard32() throws IOException {
      int b3 = readCard8();
      int b2 = readCard8();
      int b1 = readCard8();
      int b0 = readCard8();
      return b3 << 24 | b2 << 16 | b1 << 8 | b0;
    }

    private String readString() throws IOException {
      int length = readCard32();
      if ((length < 0) || (length >= 65536)) {
        throw new VNCStream.VNCException("Invalid string length: " + length);
      }
      byte[] buffer = new byte[length];
      readFully(buffer, 0, length);
      return new String(buffer, "US-ASCII");
    }

    private VNCStream.ProtocolVersion getProtocolVersion() throws IOException {
      byte[] buffer = new byte[12];
      readFully(buffer, 0, 12);
      String s = new String(buffer, "US-ASCII");
      Pattern pattern = Pattern.compile("RFB ([0-9]{3})\\.([0-9]{3})\n");
      Matcher matcher = pattern.matcher(s);
      if (!matcher.matches()) {
        throw new VNCStream.VNCException("expected protocol version: " + s);
      }

      return new VNCStream.ProtocolVersion(Integer.parseInt(matcher.group(1)), 
        Integer.parseInt(matcher.group(2)));
    }

    private void sendProtocolVersion() throws IOException {
      synchronized (this.out_) {
        this.out_.write("RFB 003.003\n".getBytes("US-ASCII"));
        this.out_.flush();
      }
    }

    private VNCPixelFormat readPixelFormat() throws IOException {
      VNCPixelFormat pixelFormat = new VNCPixelFormat();
      pixelFormat.bitsPerPixel_ = readCard8();
      pixelFormat.depth_ = readCard8();
      pixelFormat.bigEndian_ = readFlag();
      pixelFormat.trueColor_ = readFlag();
      pixelFormat.redMax_ = readCard16();
      pixelFormat.greenMax_ = readCard16();
      pixelFormat.blueMax_ = readCard16();
      pixelFormat.redShift_ = readCard8();
      pixelFormat.greenShift_ = readCard8();
      pixelFormat.blueShift_ = readCard8();
      readPadding(3);
      this._console.writeline("readPixelFormat " + pixelFormat.bitsPerPixel_ + 
        " " + pixelFormat.depth_);
      return pixelFormat;
    }

    private void writePixelFormat(VNCPixelFormat pixelFormat) throws IOException
    {
      this._console.writeline("writePixelFormat " + pixelFormat.bitsPerPixel_ + 
        " " + pixelFormat.depth_);

      writeInt8(pixelFormat.bitsPerPixel_);
      writeInt8(pixelFormat.depth_);
      writeFlag(pixelFormat.bigEndian_);
      writeFlag(pixelFormat.trueColor_);
      writeInt16(pixelFormat.redMax_);
      writeInt16(pixelFormat.greenMax_);
      writeInt16(pixelFormat.blueMax_);
      writeInt8(pixelFormat.redShift_);
      writeInt8(pixelFormat.greenShift_);
      writeInt8(pixelFormat.blueShift_);
      writePadding(3);
    }

    private void writePixelFormat() throws IOException
    {
      writeInt8(0);
      writePadding(3);
      writePixelFormat(this.pixelFormat_);
    }

    private void force32bpp() throws IOException {
      this._console.writeline("force32bpp()");

      this.pixelFormat_.bitsPerPixel_ = 32;
      this.pixelFormat_.depth_ = 24;
      this.pixelFormat_.trueColor_ = true;
      this.pixelFormat_.redMax_ = 255;
      this.pixelFormat_.greenMax_ = 255;
      this.pixelFormat_.blueMax_ = 255;
      this.pixelFormat_.redShift_ = 16;
      this.pixelFormat_.greenShift_ = 8;
      this.pixelFormat_.blueShift_ = 0;

      setupPixelFormat();

      synchronized (this.out_) {
        writePixelFormat();
      }
    }

    private void setupPixelFormat() throws IOException {
      this._console.writeline("setupPixelFormat(" + this.pixelFormat_.bitsPerPixel_ + 
        ")");

      if (this.pixelFormat_.bitsPerPixel_ == 32) {
        this.bandOffsets32_ = 
          new int[] { 
          this.pixelFormat_.redShift_ >> 3, 
          this.pixelFormat_.greenShift_ >> 3, 
          this.pixelFormat_.blueShift_ >> 3, this.pixelFormat_.bigEndian_ ? new int[] { 
          3 - (this.pixelFormat_.redShift_ >> 3), 
          3 - (this.pixelFormat_.greenShift_ >> 3), 
          3 - (this.pixelFormat_.blueShift_ >> 3) } : 
          3 };
        this.colorModel32_ = 
          new ComponentColorModel(VNCStream.colorSpace_, true, 
          true, 
          1, 0);
      } else if (this.pixelFormat_.bitsPerPixel_ == 8) {
        this.bitMasks8_ = new int[] { 
          this.pixelFormat_.redMax_ << this.pixelFormat_.redShift_, 
          this.pixelFormat_.greenMax_ << this.pixelFormat_.greenShift_, 
          this.pixelFormat_.blueMax_ << this.pixelFormat_.blueShift_ };
        this.colorModel8_ = 
          new DirectColorModel(8, this.bitMasks8_[0], 
          this.bitMasks8_[1], this.bitMasks8_[2]);
      } else {
        throw new IOException("unexpected bits per pixel: " + 
          this.pixelFormat_.bitsPerPixel_);
      }
    }

    private void writeSetEncodings() throws IOException {
      this._console.writeline("writeSetEncodings");
      int[] encodings;
      int[] encodings;
      if (this.pixelFormat_.bitsPerPixel_ == 8) {
        this._console.writeline("Disabling local cursors for VNC.");
        encodings = VNCStream.encodings_8;
      } else {
        encodings = VNCStream.encodings_32;
      }

      writeSetEncodings(encodings);
    }

    private void writeSetEncodings(int[] encodings) throws IOException {
      writeInt8(2);
      writePadding(1);
      writeInt16(encodings.length);
      for (int i = 0; i < encodings.length; i++)
        writeInt32(encodings[i]);
    }

    private void writeFramebufferUpdateRequest(int x, int y, int width, int height, boolean incremental)
      throws IOException
    {
      writeInt8(3);
      writeFlag(incremental);
      writeInt16(x);
      writeInt16(y);
      writeInt16(width);
      writeInt16(height);
    }

    private static byte reverse(byte v) {
      byte r = 0;
      if ((v & 0x1) != 0)
        r = (byte)(r | 0x80);
      if ((v & 0x2) != 0)
        r = (byte)(r | 0x40);
      if ((v & 0x4) != 0)
        r = (byte)(r | 0x20);
      if ((v & 0x8) != 0)
        r = (byte)(r | 0x10);
      if ((v & 0x10) != 0)
        r = (byte)(r | 0x8);
      if ((v & 0x20) != 0)
        r = (byte)(r | 0x4);
      if ((v & 0x40) != 0)
        r = (byte)(r | 0x2);
      if ((v & 0x80) != 0)
        r = (byte)(r | 0x1);
      return r;
    }

    private void handshake() throws IOException {
      VNCStream.ProtocolVersion protocolVersion = getProtocolVersion();
      if (protocolVersion.major_ < 3)
        throw new VNCStream.VNCException("don't know protocol version " + 
          protocolVersion.major_);
    }

    private void authenticationExchange()
      throws IOException
    {
      int scheme = readCard32();
      if (scheme == 0) {
        String reason = readString();
        throw new VNCStream.VNCException("connection failed: " + reason);
      }if (scheme != 1)
      {
        if (scheme == 2) {
          try {
            byte[] keyBytes = new byte[8];
            for (int i = 0; (i < 8) && (i < this.password.length); i++) {
              keyBytes[i] = reverse((byte)this.password[i]);
            }
            Arrays.fill(this.password, '\000');
            DESKeySpec keySpec = new DESKeySpec(keyBytes);
            SecretKeyFactory keyFactory = 
              SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(keySpec);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(1, secretKey);
            Arrays.fill(keyBytes, 0);
            byte[] challenge = new byte[16];
            readFully(challenge, 0, 16);
            byte[] response = cipher.doFinal(challenge);
            synchronized (this.out_) {
              this.out_.write(response, 0, 16);
              this.out_.flush();
            }
          } catch (Exception e) {
            throw new VNCStream.VNCException("authentication: DES error");
          }
          int status = readCard32();
          if (status != 0)
          {
            if (status == 1)
              throw new VNCStream.VNCException("authentication failed");
            if (status == 2)
              throw new VNCStream.VNCException("too many authentication failures");
          }
        } else {
          throw new VNCStream.VNCException("unexpected authentication scheme: " + 
            scheme);
        }
      }
    }

    private void clientInitialization() throws IOException {
      this._console.writeline("clientInitialisation");
      synchronized (this.out_) {
        writeFlag(true);
        this.out_.flush();
      }
    }

    private void serverInitialization() throws IOException {
      this._console.writeline("serverInitialisation");
      int width = readCard16();
      int height = readCard16();
      this.pixelFormat_ = readPixelFormat();
      readString();

      force32bpp();

      desktopSize(width, height);
      synchronized (this.out_) {
        writeSetEncodings();
      }
    }

    private void writeKey(boolean down, int key)
      throws IOException
    {
      writeInt8(4);
      writeFlag(down);
      writePadding(2);
      writeInt32(key);
    }

    void keyEvent(boolean down, int key) {
      synchronized (this.out_) {
        try {
          writeKey(down, key);
          this.out_.flush();
        }
        catch (IOException localIOException) {
        }
      }
    }

    void pointerEvent(int buttonMask, int x, int y) {
      if (x < 0)
        x = 0;
      else if (x >= this.width_) {
        x = this.width_ - 1;
      }

      if (y < 0)
        y = 0;
      else if (y >= this.height_) {
        y = this.height_ - 1;
      }

      synchronized (this.out_) {
        try {
          pointerEvent_(buttonMask, x, y);
          this.out_.flush();
        } catch (IOException e) {
          this._console.writeline(e.getMessage());
        }
      }
    }

    public void sendCtrlAltDelete() {
      synchronized (this.out_) {
        try {
          writeKey(true, 65507);
          writeKey(true, 65513);
          writeKey(true, 65535);
          writeKey(false, 65507);
          writeKey(false, 65513);
          writeKey(false, 65535);
          this.out_.flush();
        } catch (IOException e) {
          this._console.writeline(e.getMessage());
        }
      }
    }

    void pointerWheelEvent(int x, int y, int r) {
      synchronized (this.out_)
      {
        try
        {
          int m;
          int m;
          if (r < 0) {
            r = -r;
            m = 8;
          } else {
            m = 16;
          }
          for (int i = 0; i < r; i++) {
            pointerEvent_(m, x, y);
            pointerEvent_(0, x, y);
          }

          this.out_.flush();
        } catch (IOException e) {
          this._console.writeline(e.getMessage());
        }
      }
    }

    private void pointerEvent_(int buttonMask, int x, int y) throws IOException
    {
      writeInt8(5);
      writeInt8(buttonMask);
      writeInt16(x);
      writeInt16(y);
    }

    void clientCutText(String text) {
      this._console.writeline("cutEvent");

      synchronized (this.out_) {
        try {
          writeInt8(6);
          writePadding(3);
          writeString(text);
          this.out_.flush();
        } catch (IOException e) {
          this._console.writeline(e.getMessage());
        }
      }
    }

    private Image createImage(int width, int height, byte[] data, int length, byte[] mask_data)
    {
      if (this.pixelFormat_.bitsPerPixel_ == 32) {
        SampleModel sampleModel = new PixelInterleavedSampleModel(
          0, width, height, 4, width * 4, 
          this.bandOffsets32_);

        if (mask_data == null) {
          int index = 3;
          for (int i = 0; i < width * height; i++) {
            data[index] = -1;
            index += 4;
          }
        } else {
          int stride = width + 7 >> 3;
          int index = 3;
          int mask_index = 0;
          for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
              data[index] = ((mask_data[(mask_index + x / 8)] & 1 << 7 - (x & 0x7)) == 0 ? 0 : 
                -1);
              index += 4;
            }
            mask_index += stride;
          }
        }

        DataBuffer dataBuffer = new DataBufferByte(data, length);
        WritableRaster raster = Raster.createWritableRaster(
          sampleModel, dataBuffer, null);
        return new BufferedImage(this.colorModel32_, raster, true, null);
      }if (this.pixelFormat_.bitsPerPixel_ == 8) {
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
          0, width, height, width, this.bitMasks8_);
        DataBuffer dataBuffer = new DataBufferByte(data, length);
        WritableRaster raster = Raster.createWritableRaster(
          sampleModel, dataBuffer, null);
        return new BufferedImage(this.colorModel8_, raster, true, null);
      }
      throw new RuntimeException("unexpected bits per pixel");
    }

    private void readRawEncoding(int x, int y, int width, int height)
      throws IOException
    {
      this.client_.clientDrawImage(readRawEncoding_(width, height, false), x, 
        y, width, height);
    }

    private Image readRawEncoding_(int width, int height, boolean mask)
      throws IOException
    {
      if ((width < 0) || (height < 0)) {
        throw new VNCStream.VNCException("Invalid size: " + width + " x " + 
          height);
      }
      int pixelSize = this.pixelFormat_.bitsPerPixel_ + 7 >> 3;
      int length = width * height * pixelSize;
      byte[] data = new byte[length];
      readFully(data, 0, length);

      byte[] mask_data = (byte[])null;
      if (mask) {
        int scanline = width + 7 >> 3;
        int mask_length = scanline * height;

        mask_data = new byte[mask_length];
        readFully(mask_data, 0, mask_length);
      }

      return createImage(width, height, data, length, mask_data);
    }

    private void readCopyRectangleEncoding(int dx, int dy, int width, int height) throws IOException
    {
      int x = readCard16();
      int y = readCard16();
      this.client_.clientCopyRectangle(x, y, width, height, dx, dy);
    }

    private Color readColor() throws IOException {
      if (this.pixelFormat_.bitsPerPixel_ == 32) {
        readFully(this.data, 0, 4);
        return new Color(this.data[this.bandOffsets32_[0]] & 0xFF, 
          this.data[this.bandOffsets32_[1]] & 0xFF, 
          this.data[this.bandOffsets32_[2]] & 0xFF);
      }if (this.pixelFormat_.bitsPerPixel_ == 8) {
        int p = readCard8();
        int r = this.colorModel8_.getRed(p);
        int g = this.colorModel8_.getGreen(p);
        int b = this.colorModel8_.getBlue(p);
        return new Color(r, g, b);
      }
      throw new RuntimeException("unexpected bits per pixel");
    }

    private void readRREEncoding(int x, int y, int width, int height)
      throws IOException
    {
      int n = readCard32();
      Color background = readColor();
      this.client_.clientFillRectangle(x, y, width, height, background);
      for (int i = 0; i < n; i++) {
        Color foreground = readColor();
        int rx = readCard16();
        int ry = readCard16();
        int rw = readCard16();
        int rh = readCard16();
        this.client_.clientFillRectangle(x + rx, y + ry, rw, rh, foreground);
      }
    }

    private void readCoRREEncoding(int x, int y, int width, int height) throws IOException
    {
      int n = readCard32();
      Color background = readColor();
      this.client_.clientFillRectangle(x, y, width, height, background);
      for (int i = 0; i < n; i++) {
        Color foreground = readColor();
        int rx = readCard8();
        int ry = readCard8();
        int rw = readCard8();
        int rh = readCard8();
        this.client_.clientFillRectangle(x + rx, x + ry, rw, rh, foreground);
      }
    }

    private void readFillRectangles(int rx, int ry, int n) throws IOException
    {
      int pixelSize = this.pixelFormat_.bitsPerPixel_ + 7 >> 3;
      int length = n * (pixelSize + 2);
      readFully(this.data, 0, length);
      int index = 0;
      for (int i = 0; i < n; i++)
      {
        if (this.pixelFormat_.bitsPerPixel_ == 32) {
          Color foreground = new Color(
            this.data[(index + this.bandOffsets32_[0])] & 0xFF, this.data[
            (index + 
            this.bandOffsets32_[1])] & 0xFF, this.data[
            (index + 
            this.bandOffsets32_[2])] & 0xFF);
          index += 4;
        }
        else
        {
          Color foreground;
          if (this.pixelFormat_.bitsPerPixel_ == 8) {
            int p = this.data[(index++)];
            int r = this.colorModel8_.getRed(p);
            int g = this.colorModel8_.getGreen(p);
            int b = this.colorModel8_.getBlue(p);
            foreground = new Color(r, g, b);
          } else {
            throw new RuntimeException("unexpected bits per pixel");
          }
        }
        Color foreground;
        int sxy = this.data[(index++)] & 0xFF;
        int sx = sxy >> 4;
        int sy = sxy & 0xF;
        int swh = this.data[(index++)] & 0xFF;
        int sw = (swh >> 4) + 1;
        int sh = (swh & 0xF) + 1;
        this.client_.clientFillRectangle(rx + sx, ry + sy, sw, sh, 
          foreground);
      }
    }

    private void readRectangles(int rx, int ry, int n, Color foreground) throws IOException
    {
      for (int i = 0; i < n; i++) {
        int sxy = readCard8();
        int sx = sxy >> 4;
        int sy = sxy & 0xF;
        int swh = readCard8();
        int sw = (swh >> 4) + 1;
        int sh = (swh & 0xF) + 1;
        this.client_.clientFillRectangle(rx + sx, ry + sy, sw, sh, 
          foreground);
      }
    }

    private void readHextileEncoding(int x, int y, int width, int height) throws IOException
    {
      Color background = Color.BLACK;
      Color foreground = Color.WHITE;
      int xCount = width + 15 >> 4;
      int yCount = height + 15 >> 4;
      for (int yi = 0; yi < yCount; yi++) {
        int ry = y + (yi << 4);
        int rh = yi == yCount - 1 ? height & 0xF : 16;
        if (rh == 0) {
          rh = 16;
        }
        for (int xi = 0; xi < xCount; xi++) {
          int rx = x + (xi << 4);
          int rw = xi == xCount - 1 ? width & 0xF : 16;
          if (rw == 0) {
            rw = 16;
          }
          int mask = readCard8();
          if ((mask & 0x1) != 0) {
            readRawEncoding(rx, ry, rw, rh);
          } else {
            if ((mask & 0x2) != 0) {
              background = readColor();
            }
            this.client_.clientFillRectangle(rx, ry, rw, rh, background);
            if ((mask & 0x4) != 0) {
              foreground = readColor();
            }
            if ((mask & 0x8) != 0) {
              int n = readCard8();
              if ((mask & 0x10) != 0)
                readFillRectangles(rx, ry, n);
              else
                readRectangles(rx, ry, n, foreground);
            }
          }
        }
      }
    }

    private void readCursorPseudoEncoding(int x, int y, int width, int height) throws IOException
    {
      try
      {
        this.client_.clientSetCursor(readRawEncoding_(width, height, true), 
          x, y);
      }
      catch (Throwable exn)
      {
        this._console.writeline(exn.getMessage());
        this._console
          .writeline("Disabling local cursors for VNC due to exception.");
        synchronized (this.out_) {
          writeSetEncodings(VNCStream.encodings_8);
        }
      }
    }

    private void readFrameBufferUpdate() throws IOException {
      readPadding(1);
      int n = readCard16();

      for (int i = 0; i < n; i++) {
        int x = readCard16();
        int y = readCard16();
        int width = readCard16();
        int height = readCard16();
        int encoding = readCard32();

        switch (encoding) {
        case 0:
          readRawEncoding(x, y, width, height);
          break;
        case 2:
          readRREEncoding(x, y, width, height);
          break;
        case 4:
          readCoRREEncoding(x, y, width, height);
          break;
        case 1:
          readCopyRectangleEncoding(x, y, width, height);
          break;
        case 5:
          readHextileEncoding(x, y, width, height);
          break;
        case -239:
          readCursorPseudoEncoding(x, y, width, height);
          break;
        case -223:
          desktopSize(width, height);
          break;
        default:
          throw new VNCStream.VNCException("unimplemented encoding: " + 
            encoding);
        }
      }
      this.client_.clientFrameBufferUpdate();
    }

    private void desktopSize(int width, int height) throws IOException {
      this.width_ = width;
      this.height_ = height;
      this.client_.clientDesktopSize(width, height);
    }

    private void readServerCutText() throws IOException {
      readPadding(3);
      String text = readString();
      this.client_.clientCutText(text);
    }

    private void readServerMessage()
      throws IOException
    {
      int type = readCard8();
      switch (type)
      {
      case 0:
        readFrameBufferUpdate();
        break;
      case 2:
        this.client_.clientBell();
        break;
      case 3:
        this._console.writeline("Cut text");
        readServerCutText();
        break;
      case 1:
      default:
        throw new VNCStream.VNCException("unknown server message: " + type);
      }
    }

    Helper(VNCClient client, RawHTTP rawHttp, char[] password, ConnectionListener listener, ConsoleListener console)
      throws IOException
    {
      this.client_ = client;
      this.password = password;
      this.socket = rawHttp.getSocket();
      this._listener = listener;
      this._console = console;
      this.socket.setReceiveBufferSize(65536);
      this.socket.setTcpNoDelay(true);

      this.in_ = 
        new DataInputStream(new BufferedInputStream(rawHttp
        .getInputStream()));

      this.out_ = 
        new DataOutputStream(new BufferedOutputStream(rawHttp
        .getOutputStream()));
    }

    Helper(VNCClient client, Socket socket, char[] password, ConnectionListener listener, ConsoleListener console)
      throws IOException
    {
      this.client_ = client;
      this.password = password;
      this.socket = socket;
      this._listener = listener;
      this._console = console;
      socket.setReceiveBufferSize(65536);
      socket.setTcpNoDelay(true);

      this.in_ = 
        new DataInputStream(new BufferedInputStream(socket
        .getInputStream()));

      this.out_ = 
        new DataOutputStream(new BufferedOutputStream(socket
        .getOutputStream()));
    }

    void connect() throws IOException {
      handshake();
      sendProtocolVersion();
      authenticationExchange();
    }

    public void run() {
      this.runThread = Thread.currentThread();
      try
      {
        handshake();
        sendProtocolVersion();
        authenticationExchange();
        clientInitialization();
        serverInitialization();

        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            VNCStream.Helper.this._listener.ConnectionMade();
          }
        });
        boolean incremental = false;
        while (this.running) {
          synchronized (this) {
            while (this.paused) {
              if (this.updateThumbnail) {
                wait(500L);
                break;
              }
              wait();
            }

          }

          synchronized (this.out_) {
            writeFramebufferUpdateRequest(0, 0, this.width_, this.height_, 
              incremental);
            this.out_.flush();
          }
          incremental = true;
          readServerMessage();
        }
      } catch (IOException e) {
        String message = e != null ? e.getMessage() : "Unknown";
        SwingUtilities.invokeLater(new Runnable(message) {
          public void run() {
            VNCStream.Helper.this._listener.ConnectionLost(this.val$message);
          } } );
      }
      catch (InterruptedException exn) {
        this._console.writeline(exn.getMessage());
      } finally {
        close();
        this.runThread = null;
      }
    }

    private void close()
    {
      this._console.writeline("VNCStream.Helper.close");
      try {
        try {
          try {
            this.in_.close();
          } finally {
            this.out_.close();
          }
        } finally {
          this.socket.close();
        }
      }
      catch (IOException localIOException1)
      {
      }
      catch (RuntimeException localRuntimeException1)
      {
      }
    }

    protected void finalize()
    {
    }

    synchronized void pause()
    {
      this.paused = true;
    }

    synchronized void unpause()
    {
      this.paused = false;
      notify();
    }

    synchronized void setUpdateThumbnail(boolean updateThumbnail)
    {
      this.updateThumbnail = updateThumbnail;
      notify();
    }

    void terminate()
    {
      this.running = false;
      Thread t = this.runThread;
      if (t != null)
        t.interrupt();
    }
  }

  private static class ProtocolVersion
  {
    int major_;
    int minor_;

    ProtocolVersion(int major_, int minor_)
    {
      this.major_ = major_;
      this.minor_ = minor_;
    }
  }

  public static class VNCException extends IOException
  {
    static final long serialVersionUID = 0L;

    VNCException(String msg)
    {
      super();
    }
  }
}