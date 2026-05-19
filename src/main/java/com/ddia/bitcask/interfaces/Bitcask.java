package com.ddia.bitcask.interfaces;

import java.io.IOException;

/** Bitcask */
public interface Bitcask {
  String get(String key) throws IOException;

  void put(String key, String value) throws IOException;

  void close() throws IOException;

  void merge() throws IOException;
}
