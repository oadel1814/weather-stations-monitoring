package com.ddia.bitcask.interfaces;

import com.ddia.bitcask.entity.KeyDirEntry;
import com.ddia.bitcask.entity.LogFileEntry;
import java.io.IOException;
import java.nio.file.Path;

/** LogFileInterface */
public interface LogWriter {
  KeyDirEntry writeLogEntry(LogFileEntry logFileEntry) throws IOException;

  long getSize() throws IOException;

  Path getPath();

  void close() throws IOException;
}
