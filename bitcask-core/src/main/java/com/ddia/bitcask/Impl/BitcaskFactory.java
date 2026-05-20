package com.ddia.bitcask.Impl;

import com.ddia.bitcask.enums.SyncConfig;
import com.ddia.bitcask.interfaces.Bitcask;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class BitcaskFactory {

  // Holds our active database instances mapped by their directory path
  private static final ConcurrentHashMap<String, Bitcask> activeInstances =
      new ConcurrentHashMap<>();

  public static Bitcask getInstance(String directory) {
    return activeInstances.computeIfAbsent(
        directory,
        dir -> {
          try {
            return new BitcaskImpl(dir, SyncConfig.SYNC_ON_EVERY_WRITE);
          } catch (IOException e) {
            // computeIfAbsent doesn't allow checked exceptions, so wrap it
            throw new RuntimeException("Failed to open Bitcask at " + dir, e);
          }
        });
  }

  public static void releaseInstance(String directory) {
    activeInstances.remove(directory);
  }
}
