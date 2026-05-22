package com.ddia.bitcask.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** HintFileEntry */
@Getter
@RequiredArgsConstructor
public class HintFileEntry {
  private final String key;
  private final int keySize;
  private final int valueSize;
  private final long valuePosition;
  private final long timestamp;

  public static short getHeaderSize() {
    return 24;
  }

  public int getDataSize() {
    return keySize;
  }

  public int getEntrySize() {
    return getHeaderSize() + getDataSize();
  }
}
