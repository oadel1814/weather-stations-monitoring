package com.ddia.bitcask.Impl;

import com.ddia.bitcask.entity.HintFileEntry;
import com.ddia.bitcask.interfaces.HintWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class HintWriterImpl implements HintWriter {

  private final FileChannel channel;

  public HintWriterImpl(Path path) throws IOException {
    this.channel =
        FileChannel.open(
            path, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
  }

  @Override
  public void writeHintEntry(HintFileEntry entry) throws IOException {
    byte[] keyBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
    int entrySize = HintFileEntry.getHeaderSize() + keyBytes.length;

    ByteBuffer buffer = ByteBuffer.allocate(entrySize);
    buffer.putLong(entry.getTimestamp());
    buffer.putInt(entry.getKeySize());
    buffer.putInt(entry.getValueSize());
    buffer.putLong(entry.getValuePosition());
    buffer.put(keyBytes);
    buffer.flip();

    while (buffer.hasRemaining()) {
      channel.write(buffer);
    }
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }
}
