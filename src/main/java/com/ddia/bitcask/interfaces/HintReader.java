package com.ddia.bitcask.interfaces;

import com.ddia.bitcask.entity.HintFileEntry;
import java.io.IOException;

/** HintReader */
public interface HintReader {

  HintFileEntry readNextHintEntry() throws IOException;

  void close() throws IOException;
}
