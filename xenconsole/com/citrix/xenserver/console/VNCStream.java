package com.citrix.xenserver.console;

import java.awt.Color;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
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
  private boolean _connected = false;
  private static final int[] encodings_32;
  private static final int[] encodings_8;
  private static final int KEY_CTRL_L = 65507;
  private static final int KEY_ALT_L = 65513;
  private static final int KEY_DELETE = 65535;
  private final VNCClient client_;
  public Helper helper = null;
  private ConnectionListener _listener;
  private ConsoleListener _console;

  public VNCStream(VNCClient paramVNCClient, ConnectionListener paramConnectionListener, ConsoleListener paramConsoleListener)
  {
    this.client_ = paramVNCClient;
    this._listener = paramConnectionListener;
    this._console = paramConsoleListener;
  }

  public void connect(RawHTTP paramRawHTTP, char[] paramArrayOfChar)
    throws IOException
  {
    this._console.writeline("Connecting...");
    Helper localHelper;
    synchronized (this) {
      this.helper = (localHelper = new Helper(this.client_, paramRawHTTP, paramArrayOfChar, this._listener, this._console));
    }

    localHelper.start();
    this._connected = true;
    this._console.writeline("Connected");
  }

  public void connectSocket(Socket paramSocket, char[] paramArrayOfChar)
    throws IOException
  {
    this._console.writeline("Connecting...");
    Helper localHelper;
    synchronized (this) {
      this.helper = (localHelper = new Helper(this.client_, paramSocket, paramArrayOfChar, this._listener, this._console));
    }

    localHelper.start();
    this._connected = true;
    this._console.writeline("Connected");
  }

  public void disconnect()
  {
    Helper localHelper;
    synchronized (this) {
      localHelper = this.helper;
      this.helper = null;
    }

    if (localHelper != null) {
      localHelper.terminate();
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

  public synchronized void setUpdateThumbnail(boolean paramBoolean)
  {
    if (this.helper != null)
      this.helper.setUpdateThumbnail(paramBoolean);
  }

  public synchronized void keyEvent(boolean paramBoolean, int paramInt)
  {
    if (this.helper != null)
      this.helper.keyEvent(paramBoolean, paramInt);
  }

  public synchronized void pointerEvent(int paramInt1, int paramInt2, int paramInt3)
  {
    if (this.helper != null)
      this.helper.pointerEvent(paramInt1, paramInt2, paramInt3);
  }

  public synchronized void sendCtrlAltDelete()
  {
    if (this.helper != null)
      this.helper.sendCtrlAltDelete();
  }

  public synchronized void pointerWheelEvent(int paramInt1, int paramInt2, int paramInt3)
  {
    if (this.helper != null)
      this.helper.pointerWheelEvent(paramInt1, paramInt2, paramInt3);
  }

  public synchronized void clientCutText(String paramString)
  {
    if (this.helper != null)
      this.helper.clientCutText(paramString);
  }

  static
  {
    logger = Logger.getLogger(VNCStream.class.getName());

    colorSpace_ = ColorSpace.getInstance(1000);

    encodings_32 = new int[] { 5, 4, 2, 1, 0, -239, -223 };

    encodings_8 = new int[] { 5, 4, 2, 1, 0, -223 };
    try
    {
      byte[] arrayOfByte1 = new byte[8];
      byte[] arrayOfByte2 = new byte[16];
      Cipher localCipher = Cipher.getInstance("DES");
      localCipher.init(1, SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(arrayOfByte1)));

      localCipher.doFinal(arrayOfByte2);
    } catch (Exception localException) {
      if (!$assertionsDisabled) throw new AssertionError(localException);
    }
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

    private void writePadding(int paramInt)
      throws IOException
    {
      for (int i = 0; i < paramInt; i++)
        this.out_.write(0);
    }

    private void writeFlag(boolean paramBoolean) throws IOException
    {
      this.out_.writeByte(paramBoolean ? 1 : 0);
    }

    private void writeInt8(int paramInt) throws IOException {
      this.out_.writeByte(paramInt);
    }

    private void writeInt16(int paramInt) throws IOException {
      this.out_.writeShort(paramInt);
    }

    private void writeInt32(int paramInt) throws IOException {
      this.out_.writeInt(paramInt);
    }

    private void writeString(String paramString) throws IOException {
      writeInt32(paramString.length());
      this.out_.write(paramString.getBytes("US-ASCII"));
    }

    private void readPadding(int paramInt) throws IOException {
      this.in_.skip(paramInt);
    }

    private void readFully(byte[] paramArrayOfByte, int paramInt1, int paramInt2) throws IOException {
      int i = 0;
      while (i < paramInt2) {
        int j = this.in_.read(paramArrayOfByte, paramInt1 + i, paramInt2 - i);
        if (j < 0) {
          throw new EOFException();
        }
        i += j;
      }
    }

    private boolean readFlag() throws IOException {
      return readCard8() != 0;
    }

    private int readCard8() throws IOException {
      int i = this.in_.read();
      if (i < 0) {
        throw new EOFException();
      }
      return i;
    }

    private int readCard16() throws IOException {
      int i = readCard8();
      int j = readCard8();
      return (short)(i << 8 | j);
    }

    private int readCard32() throws IOException {
      int i = readCard8();
      int j = readCard8();
      int k = readCard8();
      int m = readCard8();
      return i << 24 | j << 16 | k << 8 | m;
    }

    private String readString() throws IOException {
      int i = readCard32();
      if ((i < 0) || (i >= 65536)) {
        throw new VNCStream.VNCException("Invalid string length: " + i);
      }
      byte[] arrayOfByte = new byte[i];
      readFully(arrayOfByte, 0, i);
      return new String(arrayOfByte, "US-ASCII");
    }

    private VNCStream.ProtocolVersion getProtocolVersion() throws IOException {
      byte[] arrayOfByte = new byte[12];
      readFully(arrayOfByte, 0, 12);
      String str = new String(arrayOfByte, "US-ASCII");
      Pattern localPattern = Pattern.compile("RFB ([0-9]{3})\\.([0-9]{3})\n");
      Matcher localMatcher = localPattern.matcher(str);
      if (!localMatcher.matches()) {
        throw new VNCStream.VNCException("expected protocol version: " + str);
      }

      return new VNCStream.ProtocolVersion(Integer.parseInt(localMatcher.group(1)), Integer.parseInt(localMatcher.group(2)));
    }

    private void sendProtocolVersion() throws IOException
    {
      synchronized (this.out_) {
        this.out_.write("RFB 003.003\n".getBytes("US-ASCII"));
        this.out_.flush();
      }
    }

    private VNCPixelFormat readPixelFormat() throws IOException {
      VNCPixelFormat localVNCPixelFormat = new VNCPixelFormat();
      localVNCPixelFormat.bitsPerPixel_ = readCard8();
      localVNCPixelFormat.depth_ = readCard8();
      localVNCPixelFormat.bigEndian_ = readFlag();
      localVNCPixelFormat.trueColor_ = readFlag();
      localVNCPixelFormat.redMax_ = readCard16();
      localVNCPixelFormat.greenMax_ = readCard16();
      localVNCPixelFormat.blueMax_ = readCard16();
      localVNCPixelFormat.redShift_ = readCard8();
      localVNCPixelFormat.greenShift_ = readCard8();
      localVNCPixelFormat.blueShift_ = readCard8();
      readPadding(3);
      this._console.writeline("readPixelFormat " + localVNCPixelFormat.bitsPerPixel_ + " " + localVNCPixelFormat.depth_);

      return localVNCPixelFormat;
    }

    private void writePixelFormat(VNCPixelFormat paramVNCPixelFormat) throws IOException
    {
      this._console.writeline("writePixelFormat " + paramVNCPixelFormat.bitsPerPixel_ + " " + paramVNCPixelFormat.depth_);

      writeInt8(paramVNCPixelFormat.bitsPerPixel_);
      writeInt8(paramVNCPixelFormat.depth_);
      writeFlag(paramVNCPixelFormat.bigEndian_);
      writeFlag(paramVNCPixelFormat.trueColor_);
      writeInt16(paramVNCPixelFormat.redMax_);
      writeInt16(paramVNCPixelFormat.greenMax_);
      writeInt16(paramVNCPixelFormat.blueMax_);
      writeInt8(paramVNCPixelFormat.redShift_);
      writeInt8(paramVNCPixelFormat.greenShift_);
      writeInt8(paramVNCPixelFormat.blueShift_);
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
      this._console.writeline("setupPixelFormat(" + this.pixelFormat_.bitsPerPixel_ + ")");

      if (this.pixelFormat_.bitsPerPixel_ == 32) {
        this.bandOffsets32_ = new int[] { this.pixelFormat_.redShift_ >> 3, this.pixelFormat_.greenShift_ >> 3, this.pixelFormat_.blueShift_ >> 3, this.pixelFormat_.bigEndian_ ? new int[] { 3 - (this.pixelFormat_.redShift_ >> 3), 3 - (this.pixelFormat_.greenShift_ >> 3), 3 - (this.pixelFormat_.blueShift_ >> 3), 0 } : 3 };

        this.colorModel32_ = new ComponentColorModel(VNCStream.colorSpace_, true, true, 1, 0);
      }
      else if (this.pixelFormat_.bitsPerPixel_ == 8) {
        this.bitMasks8_ = new int[] { this.pixelFormat_.redMax_ << this.pixelFormat_.redShift_, this.pixelFormat_.greenMax_ << this.pixelFormat_.greenShift_, this.pixelFormat_.blueMax_ << this.pixelFormat_.blueShift_ };

        this.colorModel8_ = new DirectColorModel(8, this.bitMasks8_[0], this.bitMasks8_[1], this.bitMasks8_[2]);
      }
      else {
        throw new IOException("unexpected bits per pixel: " + this.pixelFormat_.bitsPerPixel_);
      }
    }

    private void writeSetEncodings() throws IOException
    {
      this._console.writeline("writeSetEncodings");
      int[] arrayOfInt;
      if (this.pixelFormat_.bitsPerPixel_ == 8) {
        this._console.writeline("Disabling local cursors for VNC.");
        arrayOfInt = VNCStream.encodings_8;
      } else {
        arrayOfInt = VNCStream.encodings_32;
      }

      writeSetEncodings(arrayOfInt);
    }

    private void writeSetEncodings(int[] paramArrayOfInt) throws IOException {
      writeInt8(2);
      writePadding(1);
      writeInt16(paramArrayOfInt.length);
      for (int i = 0; i < paramArrayOfInt.length; i++)
        writeInt32(paramArrayOfInt[i]);
    }

    private void writeFramebufferUpdateRequest(int paramInt1, int paramInt2, int paramInt3, int paramInt4, boolean paramBoolean)
      throws IOException
    {
      writeInt8(3);
      writeFlag(paramBoolean);
      writeInt16(paramInt1);
      writeInt16(paramInt2);
      writeInt16(paramInt3);
      writeInt16(paramInt4);
    }

    private static byte reverse(byte paramByte) {
      int i = 0;
      if ((paramByte & 0x1) != 0)
        i = (byte)(i | 0x80);
      if ((paramByte & 0x2) != 0)
        i = (byte)(i | 0x40);
      if ((paramByte & 0x4) != 0)
        i = (byte)(i | 0x20);
      if ((paramByte & 0x8) != 0)
        i = (byte)(i | 0x10);
      if ((paramByte & 0x10) != 0)
        i = (byte)(i | 0x8);
      if ((paramByte & 0x20) != 0)
        i = (byte)(i | 0x4);
      if ((paramByte & 0x40) != 0)
        i = (byte)(i | 0x2);
      if ((paramByte & 0x80) != 0)
        i = (byte)(i | 0x1);
      return i;
    }

    private void handshake() throws IOException {
      VNCStream.ProtocolVersion localProtocolVersion = getProtocolVersion();
      if (localProtocolVersion.major_ < 3)
        throw new VNCStream.VNCException("don't know protocol version " + localProtocolVersion.major_);
    }

    private void authenticationExchange()
      throws IOException
    {
      int i = readCard32();
      Object localObject1;
      if (i == 0) {
        localObject1 = readString();
        throw new VNCStream.VNCException("connection failed: " + (String)localObject1);
      }if (i != 1)
      {
        if (i == 2) {
          try {
            localObject1 = new byte[8];
            for (int k = 0; (k < 8) && (k < this.password.length); k++) {
              localObject1[k] = reverse((byte)this.password[k]);
            }
            Arrays.fill(this.password, '\000');
            DESKeySpec localDESKeySpec = new DESKeySpec(localObject1);
            SecretKeyFactory localSecretKeyFactory = SecretKeyFactory.getInstance("DES");

            SecretKey localSecretKey = localSecretKeyFactory.generateSecret(localDESKeySpec);
            Cipher localCipher = Cipher.getInstance("DES");
            localCipher.init(1, localSecretKey);
            Arrays.fill(localObject1, 0);
            byte[] arrayOfByte1 = new byte[16];
            readFully(arrayOfByte1, 0, 16);
            byte[] arrayOfByte2 = localCipher.doFinal(arrayOfByte1);
            synchronized (this.out_) {
              this.out_.write(arrayOfByte2, 0, 16);
              this.out_.flush();
            }
          } catch (Exception localException) {
            throw new VNCStream.VNCException("authentication: DES error");
          }
          int j = readCard32();
          if (j != 0)
          {
            if (j == 1)
              throw new VNCStream.VNCException("authentication failed");
            if (j == 2)
              throw new VNCStream.VNCException("too many authentication failures");
          }
        } else {
          throw new VNCStream.VNCException("unexpected authentication scheme: " + i);
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
      int i = readCard16();
      int j = readCard16();
      this.pixelFormat_ = readPixelFormat();
      readString();

      force32bpp();

      desktopSize(i, j);
      synchronized (this.out_) {
        writeSetEncodings();
      }
    }

    private void writeKey(boolean paramBoolean, int paramInt)
      throws IOException
    {
      writeInt8(4);
      writeFlag(paramBoolean);
      writePadding(2);
      writeInt32(paramInt);
    }

    void keyEvent(boolean paramBoolean, int paramInt) {
      synchronized (this.out_) {
        try {
          writeKey(paramBoolean, paramInt);
          this.out_.flush();
        }
        catch (IOException localIOException) {
        }
      }
    }

    void pointerEvent(int paramInt1, int paramInt2, int paramInt3) {
      if (paramInt2 < 0)
        paramInt2 = 0;
      else if (paramInt2 >= this.width_) {
        paramInt2 = this.width_ - 1;
      }

      if (paramInt3 < 0)
        paramInt3 = 0;
      else if (paramInt3 >= this.height_) {
        paramInt3 = this.height_ - 1;
      }

      synchronized (this.out_) {
        try {
          pointerEvent_(paramInt1, paramInt2, paramInt3);
          this.out_.flush();
        } catch (IOException localIOException) {
          this._console.writeline(localIOException.getMessage());
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
        } catch (IOException localIOException) {
          this._console.writeline(localIOException.getMessage());
        }
      }
    }

    void pointerWheelEvent(int paramInt1, int paramInt2, int paramInt3) {
      synchronized (this.out_)
      {
        try
        {
          int i;
          if (paramInt3 < 0) {
            paramInt3 = -paramInt3;
            i = 8;
          } else {
            i = 16;
          }
          for (int j = 0; j < paramInt3; j++) {
            pointerEvent_(i, paramInt1, paramInt2);
            pointerEvent_(0, paramInt1, paramInt2);
          }

          this.out_.flush();
        } catch (IOException localIOException) {
          this._console.writeline(localIOException.getMessage());
        }
      }
    }

    private void pointerEvent_(int paramInt1, int paramInt2, int paramInt3) throws IOException
    {
      writeInt8(5);
      writeInt8(paramInt1);
      writeInt16(paramInt2);
      writeInt16(paramInt3);
    }

    void clientCutText(String paramString) {
      this._console.writeline("cutEvent");

      synchronized (this.out_) {
        try {
          writeInt8(6);
          writePadding(3);
          writeString(paramString);
          this.out_.flush();
        } catch (IOException localIOException) {
          this._console.writeline(localIOException.getMessage());
        }
      }
    }

    private Image createImage(int paramInt1, int paramInt2, byte[] paramArrayOfByte1, int paramInt3, byte[] paramArrayOfByte2)
    {
      Object localObject;
      DataBufferByte localDataBufferByte;
      WritableRaster localWritableRaster;
      if (this.pixelFormat_.bitsPerPixel_ == 32) {
        localObject = new PixelInterleavedSampleModel(0, paramInt1, paramInt2, 4, paramInt1 * 4, this.bandOffsets32_);
        int i;
        int j;
        if (paramArrayOfByte2 == null) {
          i = 3;
          for (j = 0; j < paramInt1 * paramInt2; j++) {
            paramArrayOfByte1[i] = -1;
            i += 4;
          }
        } else {
          i = paramInt1 + 7 >> 3;
          j = 3;
          int k = 0;
          for (int m = 0; m < paramInt2; m++) {
            for (int n = 0; n < paramInt1; n++) {
              paramArrayOfByte1[j] = ((paramArrayOfByte2[(k + n / 8)] & 1 << 7 - (n & 0x7)) == 0 ? 0 : -1);

              j += 4;
            }
            k += i;
          }
        }

        localDataBufferByte = new DataBufferByte(paramArrayOfByte1, paramInt3);
        localWritableRaster = Raster.createWritableRaster((SampleModel)localObject, localDataBufferByte, null);

        return new BufferedImage(this.colorModel32_, localWritableRaster, true, null);
      }if (this.pixelFormat_.bitsPerPixel_ == 8) {
        localObject = new SinglePixelPackedSampleModel(0, paramInt1, paramInt2, paramInt1, this.bitMasks8_);

        localDataBufferByte = new DataBufferByte(paramArrayOfByte1, paramInt3);
        localWritableRaster = Raster.createWritableRaster((SampleModel)localObject, localDataBufferByte, null);

        return new BufferedImage(this.colorModel8_, localWritableRaster, true, null);
      }
      throw new RuntimeException("unexpected bits per pixel");
    }

    private void readRawEncoding(int paramInt1, int paramInt2, int paramInt3, int paramInt4)
      throws IOException
    {
      this.client_.clientDrawImage(readRawEncoding_(paramInt3, paramInt4, false), paramInt1, paramInt2, paramInt3, paramInt4);
    }

    private Image readRawEncoding_(int paramInt1, int paramInt2, boolean paramBoolean)
      throws IOException
    {
      if ((paramInt1 < 0) || (paramInt2 < 0)) {
        throw new VNCStream.VNCException("Invalid size: " + paramInt1 + " x " + paramInt2);
      }

      int i = this.pixelFormat_.bitsPerPixel_ + 7 >> 3;
      int j = paramInt1 * paramInt2 * i;
      byte[] arrayOfByte1 = new byte[j];
      readFully(arrayOfByte1, 0, j);

      byte[] arrayOfByte2 = null;
      if (paramBoolean) {
        int k = paramInt1 + 7 >> 3;
        int m = k * paramInt2;

        arrayOfByte2 = new byte[m];
        readFully(arrayOfByte2, 0, m);
      }

      return createImage(paramInt1, paramInt2, arrayOfByte1, j, arrayOfByte2);
    }

    private void readCopyRectangleEncoding(int paramInt1, int paramInt2, int paramInt3, int paramInt4) throws IOException
    {
      int i = readCard16();
      int j = readCard16();
      this.client_.clientCopyRectangle(i, j, paramInt3, paramInt4, paramInt1, paramInt2);
    }

    private Color readColor() throws IOException {
      if (this.pixelFormat_.bitsPerPixel_ == 32) {
        readFully(this.data, 0, 4);
        return new Color(this.data[this.bandOffsets32_[0]] & 0xFF, this.data[this.bandOffsets32_[1]] & 0xFF, this.data[this.bandOffsets32_[2]] & 0xFF);
      }

      if (this.pixelFormat_.bitsPerPixel_ == 8) {
        int i = readCard8();
        int j = this.colorModel8_.getRed(i);
        int k = this.colorModel8_.getGreen(i);
        int m = this.colorModel8_.getBlue(i);
        return new Color(j, k, m);
      }
      throw new RuntimeException("unexpected bits per pixel");
    }

    private void readRREEncoding(int paramInt1, int paramInt2, int paramInt3, int paramInt4)
      throws IOException
    {
      int i = readCard32();
      Color localColor1 = readColor();
      this.client_.clientFillRectangle(paramInt1, paramInt2, paramInt3, paramInt4, localColor1);
      for (int j = 0; j < i; j++) {
        Color localColor2 = readColor();
        int k = readCard16();
        int m = readCard16();
        int n = readCard16();
        int i1 = readCard16();
        this.client_.clientFillRectangle(paramInt1 + k, paramInt2 + m, n, i1, localColor2);
      }
    }

    private void readCoRREEncoding(int paramInt1, int paramInt2, int paramInt3, int paramInt4) throws IOException
    {
      int i = readCard32();
      Color localColor1 = readColor();
      this.client_.clientFillRectangle(paramInt1, paramInt2, paramInt3, paramInt4, localColor1);
      for (int j = 0; j < i; j++) {
        Color localColor2 = readColor();
        int k = readCard8();
        int m = readCard8();
        int n = readCard8();
        int i1 = readCard8();
        this.client_.clientFillRectangle(paramInt1 + k, paramInt1 + m, n, i1, localColor2);
      }
    }

    private void readFillRectangles(int paramInt1, int paramInt2, int paramInt3) throws IOException
    {
      int i = this.pixelFormat_.bitsPerPixel_ + 7 >> 3;
      int j = paramInt3 * (i + 2);
      readFully(this.data, 0, j);
      int k = 0;
      for (int m = 0; m < paramInt3; m++)
      {
        Color localColor;
        if (this.pixelFormat_.bitsPerPixel_ == 32) {
          localColor = new Color(this.data[(k + this.bandOffsets32_[0])] & 0xFF, this.data[(k + this.bandOffsets32_[1])] & 0xFF, this.data[(k + this.bandOffsets32_[2])] & 0xFF);

          k += 4;
        } else if (this.pixelFormat_.bitsPerPixel_ == 8) {
          n = this.data[(k++)];
          i1 = this.colorModel8_.getRed(n);
          i2 = this.colorModel8_.getGreen(n);
          i3 = this.colorModel8_.getBlue(n);
          localColor = new Color(i1, i2, i3);
        } else {
          throw new RuntimeException("unexpected bits per pixel");
        }
        int n = this.data[(k++)] & 0xFF;
        int i1 = n >> 4;
        int i2 = n & 0xF;
        int i3 = this.data[(k++)] & 0xFF;
        int i4 = (i3 >> 4) + 1;
        int i5 = (i3 & 0xF) + 1;
        this.client_.clientFillRectangle(paramInt1 + i1, paramInt2 + i2, i4, i5, localColor);
      }
    }

    private void readRectangles(int paramInt1, int paramInt2, int paramInt3, Color paramColor)
      throws IOException
    {
      for (int i = 0; i < paramInt3; i++) {
        int j = readCard8();
        int k = j >> 4;
        int m = j & 0xF;
        int n = readCard8();
        int i1 = (n >> 4) + 1;
        int i2 = (n & 0xF) + 1;
        this.client_.clientFillRectangle(paramInt1 + k, paramInt2 + m, i1, i2, paramColor);
      }
    }

    private void readHextileEncoding(int paramInt1, int paramInt2, int paramInt3, int paramInt4)
      throws IOException
    {
      Color localColor1 = Color.BLACK;
      Color localColor2 = Color.WHITE;
      int i = paramInt3 + 15 >> 4;
      int j = paramInt4 + 15 >> 4;
      for (int k = 0; k < j; k++) {
        int m = paramInt2 + (k << 4);
        int n = k == j - 1 ? paramInt4 & 0xF : 16;
        if (n == 0) {
          n = 16;
        }
        for (int i1 = 0; i1 < i; i1++) {
          int i2 = paramInt1 + (i1 << 4);
          int i3 = i1 == i - 1 ? paramInt3 & 0xF : 16;
          if (i3 == 0) {
            i3 = 16;
          }
          int i4 = readCard8();
          if ((i4 & 0x1) != 0) {
            readRawEncoding(i2, m, i3, n);
          } else {
            if ((i4 & 0x2) != 0) {
              localColor1 = readColor();
            }
            this.client_.clientFillRectangle(i2, m, i3, n, localColor1);
            if ((i4 & 0x4) != 0) {
              localColor2 = readColor();
            }
            if ((i4 & 0x8) != 0) {
              int i5 = readCard8();
              if ((i4 & 0x10) != 0)
                readFillRectangles(i2, m, i5);
              else
                readRectangles(i2, m, i5, localColor2);
            }
          }
        }
      }
    }

    private void readCursorPseudoEncoding(int paramInt1, int paramInt2, int paramInt3, int paramInt4) throws IOException
    {
      try
      {
        this.client_.clientSetCursor(readRawEncoding_(paramInt3, paramInt4, true), paramInt1, paramInt2);
      }
      catch (Throwable localThrowable)
      {
        this._console.writeline(localThrowable.getMessage());
        this._console.writeline("Disabling local cursors for VNC due to exception.");

        synchronized (this.out_) {
          writeSetEncodings(VNCStream.encodings_8);
        }
      }
    }

    private void readFrameBufferUpdate() throws IOException {
      readPadding(1);
      int i = readCard16();

      for (int j = 0; j < i; j++) {
        int k = readCard16();
        int m = readCard16();
        int n = readCard16();
        int i1 = readCard16();
        int i2 = readCard32();

        switch (i2) {
        case 0:
          readRawEncoding(k, m, n, i1);
          break;
        case 2:
          readRREEncoding(k, m, n, i1);
          break;
        case 4:
          readCoRREEncoding(k, m, n, i1);
          break;
        case 1:
          readCopyRectangleEncoding(k, m, n, i1);
          break;
        case 5:
          readHextileEncoding(k, m, n, i1);
          break;
        case -239:
          readCursorPseudoEncoding(k, m, n, i1);
          break;
        case -223:
          desktopSize(n, i1);
          break;
        default:
          throw new VNCStream.VNCException("unimplemented encoding: " + i2);
        }
      }

      this.client_.clientFrameBufferUpdate();
    }

    private void desktopSize(int paramInt1, int paramInt2) throws IOException {
      this.width_ = paramInt1;
      this.height_ = paramInt2;
      this.client_.clientDesktopSize(paramInt1, paramInt2);
    }

    private void readServerCutText() throws IOException {
      readPadding(3);
      String str = readString();
      this.client_.clientCutText(str);
    }

    private void readServerMessage()
      throws IOException
    {
      int i = readCard8();
      switch (i)
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
        throw new VNCStream.VNCException("unknown server message: " + i);
      }
    }

    Helper(VNCClient paramVNCClient, RawHTTP paramRawHTTP, char[] paramArrayOfChar, ConnectionListener paramConnectionListener, ConsoleListener paramConsoleListener)
      throws IOException
    {
      this.client_ = paramVNCClient;
      this.password = paramArrayOfChar;
      this.socket = paramRawHTTP.getSocket();
      this._listener = paramConnectionListener;
      this._console = paramConsoleListener;
      this.socket.setReceiveBufferSize(65536);
      this.socket.setTcpNoDelay(true);

      this.in_ = new DataInputStream(new BufferedInputStream(paramRawHTTP.getInputStream()));

      this.out_ = new DataOutputStream(new BufferedOutputStream(paramRawHTTP.getOutputStream()));
    }

    Helper(VNCClient paramVNCClient, Socket paramSocket, char[] paramArrayOfChar, ConnectionListener paramConnectionListener, ConsoleListener paramConsoleListener)
      throws IOException
    {
      this.client_ = paramVNCClient;
      this.password = paramArrayOfChar;
      this.socket = paramSocket;
      this._listener = paramConnectionListener;
      this._console = paramConsoleListener;
      paramSocket.setReceiveBufferSize(65536);
      paramSocket.setTcpNoDelay(true);

      this.in_ = new DataInputStream(new BufferedInputStream(paramSocket.getInputStream()));

      this.out_ = new DataOutputStream(new BufferedOutputStream(paramSocket.getOutputStream()));
    }

    void connect() throws IOException
    {
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
        boolean bool = false;
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
            writeFramebufferUpdateRequest(0, 0, this.width_, this.height_, bool);

            this.out_.flush();
          }
          bool = true;
          readServerMessage();
        }
      } catch (IOException localIOException) {
        ??? = localIOException != null ? localIOException.getMessage() : "Unknown";
        SwingUtilities.invokeLater(new Runnable((String)???) {
          public void run() {
            VNCStream.Helper.this._listener.ConnectionLost(this.val$message);
          } } );
      }
      catch (InterruptedException localInterruptedException) {
        this._console.writeline(localInterruptedException.getMessage());
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
      catch (IOException localIOException)
      {
      }
      catch (RuntimeException localRuntimeException)
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

    synchronized void setUpdateThumbnail(boolean paramBoolean)
    {
      this.updateThumbnail = paramBoolean;
      notify();
    }

    void terminate()
    {
      this.running = false;
      Thread localThread = this.runThread;
      if (localThread != null)
        localThread.interrupt();
    }
  }

  private static class ProtocolVersion
  {
    int major_;
    int minor_;

    ProtocolVersion(int paramInt1, int paramInt2)
    {
      this.major_ = paramInt1;
      this.minor_ = paramInt2;
    }
  }

  public static class VNCException extends IOException
  {
    static final long serialVersionUID = 0L;

    VNCException(String paramString)
    {
      super();
    }
  }
}