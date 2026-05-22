package com.ddia.bitcask.interfaces;

import com.ddia.bitcask.entity.KeyDirEntry;
import com.ddia.bitcask.entity.LogFileEntry;
import java.io.IOException;

public interface LogReader {
  LogFileEntry readNextLogEntry() throws IOException;

  String readSpecificEntry(KeyDirEntry keyDirEntry) throws IOException;

  void resetReadPosition() throws IOException;

  void close() throws IOException;
}
