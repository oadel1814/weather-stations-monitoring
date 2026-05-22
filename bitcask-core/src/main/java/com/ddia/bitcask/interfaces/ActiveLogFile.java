package com.ddia.bitcask.interfaces;

import com.ddia.bitcask.entity.KeyDirEntry;
import com.ddia.bitcask.entity.LogFileEntry;
import java.io.IOException;
import java.nio.file.Path;

/** ActiveLogFile */
public interface ActiveLogFile {

  String readSpecificEntry(KeyDirEntry keyDirEntry) throws IOException;

  KeyDirEntry writeLogEntry(LogFileEntry logFileEntry) throws IOException;

  long getSize() throws IOException;

  Path getPath();

  LogReader getReader();

  LogWriter getWriter();

  void close() throws IOException;
}
