package com.ddia.bitcask;

import com.ddia.bitcask.Impl.BitcaskFactory;
import com.ddia.bitcask.enums.SyncConfig;
import com.ddia.bitcask.interfaces.Bitcask;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BitcaskCompactionTest {

  public static void main(String[] args) {
    int numKeys = 1000000;
    int updates = 3;

    // Define a test directory.
    // We use a specific folder so you can watch the files appear and disappear.
    Path dbDirectory = Paths.get("/home/omar/Documents/compaction_test_db");

    System.out.println("\n--- STARTING JAVA COMPACTION TEST ---");
    System.out.println("Target: " + numKeys + " unique keys, updated " + updates + " times each.");
    System.out.println("Total Disk Puts to execute: " + (numKeys * updates) + "\n");

    try {
      // Ensure the directory exists
      if (!Files.exists(dbDirectory)) {
        Files.createDirectories(dbDirectory);
      }

      // Initialize Bitcask directly using your factory
      Bitcask bitcask =
          BitcaskFactory.getInstance(
              "/home/omar/Documents/compaction_test_db",
              SyncConfig.NONE); // Adjust this if your factory method is named differently

      // ---------------------------------------------------------
      // STEP 1: Populate with stale data
      // ---------------------------------------------------------
      System.out.println("[1/4] Populating database with overlapping data...");
      for (int pass = 0; pass < updates; pass++) {
        for (int i = 0; i < numKeys; i++) {
          String key = "test_key_" + i;
          String value = "p".repeat(1000) + "value_version_" + pass + "_for_" + key;

          bitcask.put(key, value);
        }
        System.out.println("      Completed pass " + (pass + 1) + "/" + updates);
      }

      // ---------------------------------------------------------
      // STEP 2: Pre-Merge Pause
      // ---------------------------------------------------------
      System.out.println(
          "\n[2/4] Data populated. Check your '"
              + dbDirectory.toString()
              + "' folder now to see the bloated logs!");
      Thread.sleep(2000); // 2 second pause just like the Python script

      // ---------------------------------------------------------
      // STEP 3: Trigger Merge
      // ---------------------------------------------------------
      System.out.println("\n[3/4] Triggering direct Merge operation...");
      bitcask.merge();
      System.out.println("      [SUCCESS] Compaction/Merge completed successfully.");

      // ---------------------------------------------------------
      // STEP 4: Verify Data Integrity
      // ---------------------------------------------------------
      System.out.println("\n[4/4] Verifying data integrity post-compaction...");
      int errors = 0;

      for (int i = 0; i < numKeys; i++) {
        String key = "test_key_" + i;
        // The expected value is the one from the final update loop
        String expectedValue = "p".repeat(1000) + "value_version_" + (updates - 1) + "_for_" + key;

        String actualValue = bitcask.get(key);

        if (actualValue == null) {
          System.err.println("ERROR: " + key + " is completely missing!");
          errors++;
        } else if (!actualValue.equals(expectedValue)) {
          System.err.println(
              "ERROR: "
                  + key
                  + " holds stale data! Expected '"
                  + expectedValue
                  + "', got '"
                  + actualValue
                  + "'");
          errors++;
        }
      }

      // ---------------------------------------------------------
      // RESULTS
      // ---------------------------------------------------------
      if (errors == 0) {
        System.out.println("\n✅ COMPACTION TEST PASSED!");
        System.out.println(
            "All " + numKeys + " keys survived the merge and retain their latest values.");
      } else {
        System.out.println("\n❌ COMPACTION TEST FAILED!");
        System.out.println(errors + " keys had missing or corrupted data after the merge.");
      }

    } catch (IOException e) {
      System.err.println("\n❌ TEST CRASHED: Disk I/O Exception occurred.");
      e.printStackTrace();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("Test interrupted.");
    }
  }
}
