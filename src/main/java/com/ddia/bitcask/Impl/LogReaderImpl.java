package com.ddia.bitcask.Impl;

import com.ddia.bitcask.entity.KeyDirEntry;
import com.ddia.bitcask.entity.LogFileEntry;
import com.ddia.bitcask.interfaces.LogReader;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** LogReaderImpl */
public class LogReaderImpl implements LogReader {

  private final FileChannel channel;
  private final ByteBuffer headerBuffer = ByteBuffer.allocate(LogFileEntry.getHeaderSize());

  public LogReaderImpl(Path path) throws IOException {
    this.channel = FileChannel.open(path, StandardOpenOption.READ);
  }

  @Override
  public LogFileEntry readNextLogEntry() throws IOException {
    headerBuffer.clear();
    int bytesRead = readSegment(channel, headerBuffer);
    if (bytesRead == -1) {
      return null;
    }
    headerBuffer.flip();
    int crc = headerBuffer.getInt();
    long timestamp = headerBuffer.getLong();
    int keySize = headerBuffer.getInt();
    int valueSize = headerBuffer.getInt();
    // after the readFully function now the position is right after the header
    long valuePosition = channel.position() + keySize;

    ByteBuffer dataBuffer = ByteBuffer.allocate(keySize + valueSize);
    readSegment(channel, dataBuffer);
    dataBuffer.flip();
    // get key
    byte[] keyBytes = new byte[keySize];
    dataBuffer.get(keyBytes);
    String key = new String(keyBytes, StandardCharsets.UTF_8);
    // get value
    byte[] valueBytes = new byte[valueSize];
    dataBuffer.get(valueBytes);
    String value = new String(valueBytes, StandardCharsets.UTF_8);

    return new LogFileEntry(crc, timestamp, keySize, valueSize, key, value, valuePosition);
  }

  private int readSegment(FileChannel channel, ByteBuffer buffer) throws IOException {
    int totalRead = 0;
    while (buffer.hasRemaining()) {
      int read = channel.read(buffer);
      if (read == -1) {
        if (totalRead == 0) return -1; // Clean EOF
        throw new EOFException("Unexpected EOF: File might be corrupted.");
      }
      totalRead += read;
    }
    return totalRead;
  }

  @Override
  public String readSpecificEntry(KeyDirEntry keyDirEntry) throws IOException {
    int valueSize = keyDirEntry.getValueSize();
    long valuePosition = keyDirEntry.getValuePosition();
    ByteBuffer valueBuffer = ByteBuffer.allocate(valueSize);

    int totalRead = 0;
    while (valueBuffer.hasRemaining()) {
      int read = channel.read(valueBuffer, valuePosition + totalRead);
      if (read == -1) {
        throw new EOFException("Unexpected EOF during point read.");
      }
      totalRead += read;
    }
    valueBuffer.flip();
    byte[] valueBytes = new byte[valueSize];
    valueBuffer.get(valueBytes);
    String value = new String(valueBytes, StandardCharsets.UTF_8);
    return value;
  }

  @Override
  public void resetReadPosition() throws IOException {
    channel.position(0);
  }

  @Override
  public void close() throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'close'");
  }
}
