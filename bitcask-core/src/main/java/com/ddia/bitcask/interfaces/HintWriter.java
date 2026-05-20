package com.ddia.bitcask.interfaces;

import com.ddia.bitcask.entity.HintFileEntry;
import java.io.IOException;

/** HintWriter */
public interface HintWriter {
  void writeHintEntry(HintFileEntry hintFileEntry) throws IOException;

  void close() throws IOException;
}
