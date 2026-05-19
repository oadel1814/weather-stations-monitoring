package com.ddia.bitcask.entity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** LogFileEntry */
@Getter
@AllArgsConstructor
public class LogFileEntry {
  private final int crc;
  private final long timestamp;
  private final int keySize;
  private final int valueSize;
  private final String key;
  private final String value;
  private long valuePosition;

  public LogFileEntry(String key, String value) {
    this.timestamp = System.currentTimeMillis();
    this.key = key;
    this.value = value;
    byte[] keyBytes = getKeyBytes();
    byte[] valueBytes = getValueBytes();
    if (keyBytes.length > Integer.MAX_VALUE) throw new IllegalArgumentException("Key too large");
    if (valueBytes.length > Integer.MAX_VALUE)
      throw new IllegalArgumentException("Value too large");
    this.keySize = keyBytes.length;
    this.valueSize = valueBytes.length;
    this.crc = calculateCRC();
  }

  private int calculateCRC() {
    CRC32 crc = new CRC32();
    // substract 8 to eliminate crc from calc
    ByteBuffer byteBuffer = ByteBuffer.allocate(getEntrySize() - 4);
    byteBuffer.putLong(timestamp);
    byteBuffer.putInt(keySize);
    byteBuffer.putInt(valueSize);
    byteBuffer.put(getKeyBytes());
    byteBuffer.put(getValueBytes());
    crc.update(byteBuffer.array());

    return (int) crc.getValue();
  }

  public static short getHeaderSize() {
    return 20;
  }

  public int getDataSize() {
    return keySize + valueSize;
  }

  public int getEntrySize() {
    return getHeaderSize() + getDataSize();
  }

  public byte[] getKeyBytes() {
    return key.getBytes(StandardCharsets.UTF_8);
  }

  public byte[] getValueBytes() {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  public boolean isValid() {
    return crc == calculateCRC();
  }
}
