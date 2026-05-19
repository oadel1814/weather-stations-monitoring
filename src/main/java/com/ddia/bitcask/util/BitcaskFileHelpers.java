package com.ddia.bitcask.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BitcaskFileHelpers {

  public static final String DATA_EXT = ".data";
  public static final String MERGE_EXT = ".merge";
  public static final String HINT_EXT = ".hint";

  public static long getFileId(Path path) {
    String fileName = path.getFileName().toString();
    String idString = fileName.substring(0, fileName.indexOf('.'));
    return Long.parseLong(idString);
  }

  // return all .data + .merge including the active file
  public static List<Path> getAllReadableFilesChronologically(Path directory) throws IOException {
    try (Stream<Path> stream = Files.list(directory)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(
              p -> {
                String name = p.toString();
                return name.endsWith(DATA_EXT) || name.endsWith(MERGE_EXT);
              })
          // Sort ascending: Oldest timestamps first, newest timestamps last.
          .sorted((p1, p2) -> Long.compare(getFileId(p1), getFileId(p2)))
          .collect(Collectors.toList());
    }
  }

  // return .hint files only
  public static List<Path> getHintFilesChronologically(Path directory) throws IOException {
    try (Stream<Path> stream = Files.list(directory)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(HINT_EXT))
          .sorted((p1, p2) -> Long.compare(getFileId(p1), getFileId(p2)))
          .collect(Collectors.toList());
    }
  }

  public static Optional<Path> getActiveFile(Path directory) throws IOException {
    try (Stream<Path> stream = Files.list(directory)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(DATA_EXT)) // ONLY look at .data files
          // Find the one with the absolute highest ID
          .max((p1, p2) -> Long.compare(getFileId(p1), getFileId(p2)));
    }
  }

  // return .data + .merge and eliminate active file
  public static List<Path> getReadOnlyFiles(Path directory) throws IOException {
    List<Path> allFiles = getAllReadableFilesChronologically(directory);
    Optional<Path> activeFile = getActiveFile(directory);

    // If an active file exists, remove it from the read-only list.
    activeFile.ifPresent(allFiles::remove);

    return allFiles;
  }

  // return .merge files that doesn't have hint files and .data files
  public static List<Path> getFilesNeedingFullParse(Path directory) throws IOException {
    // Start with the perfectly sorted chronological list
    List<Path> allReadable = getAllReadableFilesChronologically(directory);

    return allReadable.stream()
        .filter(
            path -> {
              String name = path.toString();

              if (name.endsWith(DATA_EXT)) {
                // .data files never have hints, so we ALWAYS need to parse them
                return true;
              } else if (name.endsWith(MERGE_EXT)) {
                // If it's a .merge file, we only parse it if the .hint is MISSING
                long fileId = getFileId(path);
                Path expectedHint = directory.resolve(fileId + HINT_EXT);
                return !Files.exists(expectedHint);
              }

              return false;
            })
        .collect(Collectors.toList());
  }

  private static synchronized long getUniqueTimestampId() {
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return System.currentTimeMillis();
  }

  public static Path createNewActiveFilePath(Path directory) {
    return directory.resolve(getUniqueTimestampId() + DATA_EXT);
  }

  public static Path createNewMergeFilePath(Path directory) {
    return directory.resolve(getUniqueTimestampId() + MERGE_EXT);
  }

  public static Path createMatchingHintFilePath(Path directory) {
    return directory.resolve(getFileId(directory) + HINT_EXT);
  }
}
