package com.ddia.bitcask.Impl;

import com.ddia.bitcask.entity.HintFileEntry;
import com.ddia.bitcask.entity.KeyDirEntry;
import com.ddia.bitcask.entity.LogFileEntry;
import com.ddia.bitcask.enums.SyncConfig;
import com.ddia.bitcask.interfaces.ActiveLogFile;
import com.ddia.bitcask.interfaces.Bitcask;
import com.ddia.bitcask.interfaces.HintReader;
import com.ddia.bitcask.interfaces.HintWriter;
import com.ddia.bitcask.interfaces.LogReader;
import com.ddia.bitcask.interfaces.LogWriter;
import com.ddia.bitcask.util.BitcaskFileHelpers;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** BitcaskEngine */
class BitcaskImpl implements Bitcask {
  // 2GB
  private static final long MAX_FILE_SIZE_BYTES = Integer.MAX_VALUE;
  private static final long COMPACTION_FREQUENCY_IN_SECONDS = TimeUnit.DAYS.toSeconds(1);
  private final Path directory;
  private final SyncConfig syncConfig;
  // the index
  private final ConcurrentHashMap<String, KeyDirEntry> keyDir = new ConcurrentHashMap<>();
  // fileId to the object itself
  private final ConcurrentHashMap<Long, LogReader> readableFiles = new ConcurrentHashMap<>();
  private ActiveLogFile activeFile;

  public BitcaskImpl(String directory, SyncConfig syncConfig) throws IOException {
    this.directory = Path.of(directory);
    this.syncConfig = syncConfig;
    init();
  }

  private void init() throws IOException {
    // init the active file object
    Path activeFilePath =
        BitcaskFileHelpers.getActiveFile(directory)
            .orElseGet(() -> BitcaskFileHelpers.createNewActiveFilePath(directory));

    activeFile = new ActiveLogFileImpl(activeFilePath, syncConfig);

    // init the log files objects
    List<Path> logFilesPath = BitcaskFileHelpers.getReadOnlyFiles(directory);
    for (Path path : logFilesPath) {
      readableFiles.put(BitcaskFileHelpers.getFileId(path), new LogReaderImpl(path));
    }
    readableFiles.put(BitcaskFileHelpers.getFileId(activeFilePath), activeFile.getReader());

    // fill the keyDir
    fillKeyDirWithHintFiles();
    fillKeyDirWithoutHintFiles();
    configureCompactionSchedular();
  }

  private void configureCompactionSchedular() {
    // we will make it starts at midnight
    LocalTime targetTime = LocalTime.MIDNIGHT;
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nextRun = now.with(targetTime);

    // If the time has already passed today, schedule it for tomorrow
    if (now.compareTo(nextRun) > 0) {
      nextRun = nextRun.plusDays(1);
    }

    Duration duration = Duration.between(now, nextRun);
    long delay = duration.getSeconds();
    ScheduledExecutorService schedular = Executors.newScheduledThreadPool(1);
    Runnable compactionTask =
        () -> {
          try {
            this.merge();
          } catch (IOException ex) {
            System.out.println("Failed in the compaction process");
          }
        };
    schedular.scheduleAtFixedRate(
        compactionTask, delay, COMPACTION_FREQUENCY_IN_SECONDS, TimeUnit.SECONDS);
  }

  @Override
  public String get(String key) throws IOException {
    KeyDirEntry keyDirEntry = keyDir.get(key);
    if (keyDirEntry == null) {
      return null;
    }
    long fileId = keyDirEntry.getFileId();
    LogReader logFile = readableFiles.get(fileId);
    return logFile.readSpecificEntry(keyDirEntry);
  }

  @Override
  public void put(String key, String value) throws IOException {
    LogFileEntry logFileEntry = new LogFileEntry(key, value);
    KeyDirEntry newEntry = activeFile.writeLogEntry(logFileEntry);
    keyDir.put(key, newEntry);
    if (activeFile.getSize() >= MAX_FILE_SIZE_BYTES) {
      rotateActiveFile();
    }
  }

  @Override
  public void close() throws IOException {
    activeFile.close();
    for (LogReader reader : readableFiles.values()) {
      reader.close();
    }
  }

  @Override
  public void merge() throws IOException {
    List<Path> filesToMerge = BitcaskFileHelpers.getReadOnlyFiles(directory);
    if (filesToMerge.isEmpty()) return;

    int numThreads = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<Future<Void>> futures = new ArrayList<>();

    List<List<Path>> batches = new ArrayList<>();
    for (int i = 0; i < numThreads; i++) {
      batches.add(new ArrayList<>());
    }
    for (int i = 0; i < filesToMerge.size(); i++) {
      batches.get(i % numThreads).add(filesToMerge.get(i));
    }

    for (List<Path> batch : batches) {
      if (batch.isEmpty()) continue;
      futures.add(
          executor.submit(
              () -> {
                processBatchOfFiles(batch);
                return null;
              }));
    }

    try {
      for (Future<Void> future : futures) {
        future.get();
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException("Merge was interrupted", e);
    } finally {
      executor.shutdown();
    }

    deleteObsoleteFiles(filesToMerge);
  }

  private void processBatchOfFiles(List<Path> batch) throws IOException {
    LogWriter compactedLogWriter = openFreshCompactedFile();
    HintWriter compactedHintWriter = openMatchingHintFile(compactedLogWriter);

    try {
      for (Path path : batch) {
        long fileId = BitcaskFileHelpers.getFileId(path);
        LogReader oldLogFile = new LogReaderImpl(path);
        oldLogFile.resetReadPosition();

        while (true) {
          LogFileEntry logFileEntry = oldLogFile.readNextLogEntry();
          if (logFileEntry == null) break;

          KeyDirEntry currentEntry = keyDir.get(logFileEntry.getKey());

          if (currentEntry != null
              && currentEntry.getFileId() == fileId
              && currentEntry.getValuePosition() == logFileEntry.getValuePosition()) {

            KeyDirEntry newKeyDirEntry =
                writeToCompactedFiles(compactedLogWriter, compactedHintWriter, logFileEntry);
            keyDir.replace(logFileEntry.getKey(), currentEntry, newKeyDirEntry);

            // Only rotate if this thread's dense file hits the max size
            if (compactedLogWriter.getSize() >= MAX_FILE_SIZE_BYTES) {
              closeCompactedPair(compactedLogWriter, compactedHintWriter);
              compactedLogWriter = openFreshCompactedFile();
              compactedHintWriter = openMatchingHintFile(compactedLogWriter);
            }
          }
        }
        oldLogFile.close();
      }
    } finally {
      closeCompactedPair(compactedLogWriter, compactedHintWriter);
    }
  }

  // --------------------helper functions--------------------------------
  private void fillKeyDirWithHintFiles() throws IOException {
    List<Path> hintFiles = BitcaskFileHelpers.getHintFilesChronologically(directory);
    for (Path hintFilepPath : hintFiles) {
      HintReader hintReader = new HintReaderImpl(hintFilepPath);
      while (true) {

        HintFileEntry hintFileEntry = hintReader.readNextHintEntry();
        if (hintFileEntry == null) break;
        KeyDirEntry newKeyDirEntry =
            new KeyDirEntry(
                BitcaskFileHelpers.getFileId(hintFilepPath),
                hintFileEntry.getValueSize(),
                hintFileEntry.getValuePosition(),
                hintFileEntry.getTimestamp());
        keyDir.put(hintFileEntry.getKey(), newKeyDirEntry);
      }
    }
  }

  private void fillKeyDirWithoutHintFiles() throws IOException {

    List<Path> logFiles = BitcaskFileHelpers.getFilesNeedingFullParse(directory);
    for (Path logFilepPath : logFiles) {
      LogReader logReader = new LogReaderImpl(logFilepPath);
      while (true) {

        LogFileEntry logFileEntry = logReader.readNextLogEntry();
        if (logFileEntry == null) break;
        KeyDirEntry newKeyDirEntry =
            new KeyDirEntry(
                BitcaskFileHelpers.getFileId(logFilepPath),
                logFileEntry.getValueSize(),
                logFileEntry.getValuePosition(),
                logFileEntry.getTimestamp());
        keyDir.put(logFileEntry.getKey(), newKeyDirEntry);
      }
    }
  }

  private void rotateActiveFile() throws IOException {
    LogWriter activeFileWriter = activeFile.getWriter();
    activeFileWriter.close();
    openFreshActiveFile();
  }

  private void openFreshActiveFile() throws IOException {
    Path newPath = BitcaskFileHelpers.createNewActiveFilePath(directory);
    activeFile = new ActiveLogFileImpl(newPath, syncConfig);
    // also register it as readable immediately so get() can find it
    readableFiles.put(BitcaskFileHelpers.getFileId(newPath), activeFile.getReader());
  }

  private LogWriter openFreshCompactedFile() throws IOException {
    Path path = BitcaskFileHelpers.createNewMergeFilePath(directory);
    LogWriter writer = new LogWriterImpl(path, SyncConfig.NONE);
    readableFiles.put(BitcaskFileHelpers.getFileId(path), new LogReaderImpl(path));
    return writer;
  }

  private HintWriter openMatchingHintFile(LogWriter compactedWriter) throws IOException {
    Path hintPath = BitcaskFileHelpers.createMatchingHintFilePath(compactedWriter.getPath());
    return new HintWriterImpl(hintPath);
  }

  private KeyDirEntry writeToCompactedFiles(
      LogWriter logWriter, HintWriter hintWriter, LogFileEntry entry) throws IOException {
    KeyDirEntry compactedEntry = logWriter.writeLogEntry(entry);

    HintFileEntry hintFileEntry =
        new HintFileEntry(
            entry.getKey(),
            entry.getKeySize(),
            entry.getValueSize(),
            compactedEntry.getValuePosition(),
            entry.getTimestamp());
    hintWriter.writeHintEntry(hintFileEntry);
    return new KeyDirEntry(
        compactedEntry.getFileId(),
        compactedEntry.getValueSize(),
        compactedEntry.getValuePosition(),
        entry.getTimestamp());
  }

  private void closeCompactedPair(LogWriter logWriter, HintWriter hintWriter) throws IOException {
    logWriter.close();
    hintWriter.close();
  }

  private void deleteObsoleteFiles(List<Path> paths) throws IOException {
    for (Path path : paths) {
      long fileId = BitcaskFileHelpers.getFileId(path);

      LogReader reader = readableFiles.remove(fileId);
      if (reader != null) reader.close();

      java.nio.file.Files.deleteIfExists(path);

      String hintFileName = path.getFileName().toString().replace(".data", ".hint");
      Path hintPath = path.resolveSibling(hintFileName);
      java.nio.file.Files.deleteIfExists(hintPath);
    }
  }

  public BlockingQueue<Map.Entry<String, String>> getAll() {
    // limit the queue to 1,000 items. BackPressure
    BlockingQueue<Map.Entry<String, String>> queue = new LinkedBlockingQueue<>(1000);

    new Thread(
            () -> {
              try {
                for (Map.Entry<String, KeyDirEntry> entry : keyDir.entrySet()) {
                  String key = entry.getKey();
                  KeyDirEntry keyDirEntry = entry.getValue();
                  LogReader logFile = readableFiles.get(keyDirEntry.getFileId());
                  String value = logFile.readSpecificEntry(keyDirEntry);

                  // .put() will block if the queue hits 1,000 items
                  queue.put(new AbstractMap.SimpleEntry<>(key, value));
                }
              } catch (Exception e) {
                System.err.println("Error reading disk during getAll: " + e.getMessage());
              } finally {
                try {
                  queue.put(Bitcask.END);
                } catch (InterruptedException ie) {
                  Thread.currentThread().interrupt();
                }
              }
            },
            "Bitcask-Disk-Reader")
        .start();

    return queue;
  }
}
