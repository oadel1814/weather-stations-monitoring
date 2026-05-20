package com.ddia.bitcask;

import com.ddia.bitcask.Impl.BitcaskFactory;
import com.ddia.bitcask.interfaces.Bitcask;
import java.util.Scanner;

public class App {
  public static void main(String[] args) {
    try {
      // Initialize your actual database engine
      Bitcask bitcask = BitcaskFactory.getInstance("/media/omar/New Volume/College/Term 8/Designing Data Intensive Applications/weather-stations-monitoring/bitcask-data");
      Scanner scanner = new Scanner(System.in);

      System.out.println("\n=== Bitcask Interactive CLI ===");

      while (true) {
        System.out.println("\nAvailable commands: [put, get, merge, exit]");
        System.out.print("bitcask> ");

        // Read the whole line and standardize it to lowercase
        String option = scanner.nextLine().trim().toLowerCase();

        if (option.isEmpty()) continue;

        switch (option) {
          case "put":
            System.out.print("Enter key: ");
            String putKey = scanner.nextLine().trim();
            System.out.print("Enter value: ");
            String putValue = scanner.nextLine().trim();

            bitcask.put(putKey, putValue);
            System.out.println("[SUCCESS] Saved '" + putKey + "'.");
            break;

          case "get":
            System.out.print("Enter key to retrieve: ");
            String getKey = scanner.nextLine().trim();

            String result = bitcask.get(getKey);
            if (result != null) {
              System.out.println("[RESULT] " + result);
            } else {
              System.out.println("[RESULT] KEY NOT FOUND");
            }
            break;

          case "merge":
            System.out.println("[SYSTEM] Starting compaction merge...");
            // Assuming your interface exposes a merge() method
            bitcask.merge();
            System.out.println("[SYSTEM] Merge completed successfully.");
            break;

          case "exit":
            System.out.println("[SYSTEM] Shutting down cleanly...");
            scanner.close();
            // If your Bitcask interface has a close() method, call it here!
            System.exit(0);
            break;

          default:
            System.out.println("[ERROR] Unknown command. Please try again.");
        }
      }
    } catch (Exception e) {
      System.err.println("[FATAL ERROR] " + e.getMessage());
      e.printStackTrace();
    }
  }
}
