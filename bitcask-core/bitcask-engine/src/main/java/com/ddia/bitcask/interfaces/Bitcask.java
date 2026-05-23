package com.ddia.bitcask.interfaces;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/** Bitcask */
public interface Bitcask {

  public static final Map.Entry<String, String> END = new AbstractMap.SimpleEntry<>("EOF", "EOF");

  String get(String key) throws IOException;

  void put(String key, String value) throws IOException;

  void close() throws IOException;

  void merge() throws IOException;

  BlockingQueue<Map.Entry<String, String>> getAll() throws IOException;
}
