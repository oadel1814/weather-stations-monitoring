package com.ddia.bitcask.Impl;

import com.ddia.bitcask.entity.KeyDirEntry;
import com.ddia.bitcask.entity.LogFileEntry;
import com.ddia.bitcask.enums.SyncConfig;
import com.ddia.bitcask.interfaces.ActiveLogFile;
import com.ddia.bitcask.interfaces.LogReader;
import com.ddia.bitcask.interfaces.LogWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** ActiveLogFile */
public class ActiveLogFileImpl implements ActiveLogFile {
  private final LogReaderImpl logReaderImpl;
  private final LogWriterImpl logWriterImpl;
  private long fileSize;

  public ActiveLogFileImpl(Path path, SyncConfig syncConfig) throws IOException {
    // should init the writer first
    this.logWriterImpl = new LogWriterImpl(path, syncConfig);
    this.logReaderImpl = new LogReaderImpl(path);
    fileSize = Files.size(path);
  }

  @Override
  public String readSpecificEntry(KeyDirEntry keyDirEntry) throws IOException {
    return logReaderImpl.readSpecificEntry(keyDirEntry);
  }

  @Override
  public KeyDirEntry writeLogEntry(LogFileEntry logFileEntry) throws IOException {
    KeyDirEntry keyDirEntry = logWriterImpl.writeLogEntry(logFileEntry);
    fileSize += logFileEntry.getEntrySize();
    return keyDirEntry;
  }

  @Override
  public long getSize() throws IOException {
    return logWriterImpl.getSize();
  }

  @Override
  public Path getPath() {
    return logWriterImpl.getPath();
  }

  @Override
  public void close() throws IOException {
    logReaderImpl.close();
    logWriterImpl.close();
  }

  @Override
  public LogReader getReader() {
    return logReaderImpl;
  }

  @Override
  public LogWriter getWriter() {
    return logWriterImpl;
  }
}
