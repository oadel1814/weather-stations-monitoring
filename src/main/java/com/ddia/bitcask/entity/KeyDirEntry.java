package com.ddia.bitcask.entity;

import lombok.Getter;

/** IndexValue */
@Getter
public class KeyDirEntry {
  private long fileId;
  private int valueSize;
  private long valuePosition;
  private long timestamp;

  public KeyDirEntry(long fileId, int valueSize, long valuePosition, long timestamp) {
    this.fileId = fileId;
    this.valueSize = valueSize;
    this.valuePosition = valuePosition;
    this.timestamp = timestamp;
  }
}
