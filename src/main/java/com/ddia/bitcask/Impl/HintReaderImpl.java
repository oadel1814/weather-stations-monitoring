package com.ddia.bitcask.Impl;

import com.ddia.bitcask.entity.HintFileEntry;
import com.ddia.bitcask.interfaces.HintReader;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class HintReaderImpl implements HintReader {

  private final FileChannel channel;
  private final ByteBuffer headerBuffer = ByteBuffer.allocate(HintFileEntry.getHeaderSize());

  public HintReaderImpl(Path path) throws IOException {
    this.channel = FileChannel.open(path, StandardOpenOption.READ);
  }

  @Override
  public HintFileEntry readNextHintEntry() throws IOException {
    headerBuffer.clear();
    int bytesRead = readSegment(channel, headerBuffer);
    if (bytesRead == -1) {
      return null; // clean EOF
    }
    headerBuffer.flip();

    long timestamp = headerBuffer.getLong();
    int keySize = headerBuffer.getInt();
    int valueSize = headerBuffer.getInt();
    long valuePosition = headerBuffer.getLong();

    ByteBuffer keyBuffer = ByteBuffer.allocate(keySize);
    readSegment(channel, keyBuffer);
    keyBuffer.flip();

    byte[] keyBytes = new byte[keySize];
    keyBuffer.get(keyBytes);
    String key = new String(keyBytes, StandardCharsets.UTF_8);

    HintFileEntry entry = new HintFileEntry(key, keySize, valueSize, valuePosition, timestamp);
    return entry;
  }

  private int readSegment(FileChannel channel, ByteBuffer buffer) throws IOException {
    int totalRead = 0;
    while (buffer.hasRemaining()) {
      int read = channel.read(buffer);
      if (read == -1) {
        if (totalRead == 0) return -1; // clean EOF
        throw new EOFException("Unexpected EOF: hint file might be corrupted.");
      }
      totalRead += read;
    }
    return totalRead;
  }

  @Override
  public void close() throws IOException {
	// TODO Auto-generated method stub
	throw new UnsupportedOperationException("Unimplemented method 'close'");
  }
}
