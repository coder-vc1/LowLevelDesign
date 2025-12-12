package org.LLD;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomLogger {

  // 1. Singleton Instance
  private static CustomLogger instance;

  // Configuration
  private static final String LOG_FILE_PATH = "application.log";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // PrintWriter for efficient file writing
  private PrintWriter fileWriter;

  // 2. Private Constructor (prevents direct instantiation)
  private CustomLogger() {
    try {
      // true = append mode (don't overwrite file on restart)
      FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
      BufferedWriter bw = new BufferedWriter(fw);
      this.fileWriter = new PrintWriter(bw, true); // true = auto-flush
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Failed to initialize file logger.");
    }
  }

  // 3. Public access to the Singleton instance (Thread Safe)
  public static synchronized CustomLogger getInstance() {
    if (instance == null) {
      instance = new CustomLogger();
    }
    return instance;
  }

  // 4. Core Logging Logic
  public void log(LogLevel level, String message) {
    String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
    String formattedMessage = String.format("[%s] [%s] %s", timestamp, level, message);

    // Print to Console
    System.out.println(formattedMessage);

    // Write to File (Synchronized for thread safety)
    logToFile(formattedMessage);
  }

  private synchronized void logToFile(String message) {
    if (fileWriter != null) {
      fileWriter.println(message);
    }
  }

  // 5. Convenience Methods
  public void info(String message) {
    log(LogLevel.INFO, message);
  }

  public void debug(String message) {
    log(LogLevel.DEBUG, message);
  }

  public void error(String message) {
    log(LogLevel.ERROR, message);
  }

  public void warning(String message) {
    log(LogLevel.WARNING, message);
  }
}
