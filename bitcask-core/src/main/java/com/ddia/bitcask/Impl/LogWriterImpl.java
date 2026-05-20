package com.ddia.bitcask.Impl;

import com.ddia.bitcask.entity.KeyDirEntry;
import com.ddia.bitcask.entity.LogFileEntry;
import com.ddia.bitcask.enums.SyncConfig;
import com.ddia.bitcask.interfaces.LogWriter;
import com.ddia.bitcask.util.BitcaskFileHelpers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** LogWriterImpl */
public class LogWriterImpl implements LogWriter {
  private final FileChannel channel;
  private final Path path;

  public LogWriterImpl(Path path, SyncConfig syncConfig) throws IOException {
    this.path = path;
    this.channel =
        FileChannel.open(
            path, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    configureSync(syncConfig);
  }

  @Override
  public KeyDirEntry writeLogEntry(LogFileEntry logFileEntry) throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.allocate(logFileEntry.getEntrySize());
    byteBuffer.putInt(logFileEntry.getCrc());
    byteBuffer.putLong(logFileEntry.getTimestamp());
    byteBuffer.putInt(logFileEntry.getKeySize());
    byteBuffer.putInt(logFileEntry.getValueSize());
    byteBuffer.put(logFileEntry.getKeyBytes());
    byteBuffer.put(logFileEntry.getValueBytes());
    byteBuffer.flip();

    long entryStartPosition = channel.position();
    long valuePosition =
        entryStartPosition + LogFileEntry.getHeaderSize() + logFileEntry.getKeySize();

    while (byteBuffer.hasRemaining()) {
      channel.write(byteBuffer);
    }
    long fileId = BitcaskFileHelpers.getFileId(path);
    return new KeyDirEntry(
        fileId, logFileEntry.getValueSize(), valuePosition, logFileEntry.getTimestamp());
  }

  private void configureSync(SyncConfig syncConfig) throws IOException {
    switch (syncConfig) {
      case SYNC_ON_EVERY_WRITE:
        channel.force(true);
        break;
      case SYNC_EVERY_SEC:
        break;
      case NONE:
        channel.force(false);
    }
  }

  @Override
  public long getSize() throws IOException {
    return channel.size();
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public void close() throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'close'");
  }
}
