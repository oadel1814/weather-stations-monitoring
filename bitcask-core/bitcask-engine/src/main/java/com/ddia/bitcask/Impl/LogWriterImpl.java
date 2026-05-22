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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** LogWriterImpl */
public class LogWriterImpl implements LogWriter {
  private final FileChannel channel;
  private final Path path;
  private ScheduledExecutorService scheduler;
  private final SyncConfig syncConfig;

  public LogWriterImpl(Path path, SyncConfig syncConfig) throws IOException {
    this.path = path;
    this.channel =
        FileChannel.open(
            path, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    this.syncConfig = syncConfig;
    configureSchedular();
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
    sync();
    long fileId = BitcaskFileHelpers.getFileId(path);
    return new KeyDirEntry(
        fileId, logFileEntry.getValueSize(), valuePosition, logFileEntry.getTimestamp());
  }

  private void sync() throws IOException {
    if (syncConfig == SyncConfig.SYNC_ON_EVERY_WRITE) channel.force(true);
  }

  private void configureSchedular() throws IOException {

    if (syncConfig == SyncConfig.SYNC_EVERY_SEC) {
      scheduler = Executors.newScheduledThreadPool(1);

      Runnable syncTask =
          () -> {
            try {
              channel.force(true);
              System.out.println("Forced Write at: " + System.currentTimeMillis());

            } catch (IOException ex) {
              System.out.println("couldn't open " + path.toUri());
            }
          };
      scheduler.scheduleAtFixedRate(syncTask, 1, 1, TimeUnit.SECONDS);
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
    if (scheduler != null) {
      scheduler.shutdown();
      try {
        scheduler.awaitTermination(2, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    channel.close();
  }
}
