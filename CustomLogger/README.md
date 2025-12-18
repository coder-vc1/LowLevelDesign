Here is the **README.md** file focusing on the High-Level Design (HLD) and Low-Level Design (LLD) of the architecture.

***


## 1. High-Level Design (HLD)

### 1.1 System Overview
The Custom Logger is a lightweight, thread-safe utility designed to capture application runtime events. It acts as a centralized service that accepts log messages from various client modules and routes them to persistent storage (File System) and immediate display (Console) simultaneously.

### 1.2 Algorithm: Logic Flow

1.  **Define Log Levels:** Create an enumeration to define severity (e.g., `INFO`, `DEBUG`, `ERROR`, `WARNING`).
2.  **Initialize Singleton:** Ensure only one instance of the logger class is created to prevent resource conflicts (especially with file I/O).
3.  **Configuration:** In the constructor, set up the output file path and format.
4.  **Formatting:** Create a standardized format for messages: `[TIMESTAMP] [LEVEL] [MESSAGE]`.
5.  **Output Handling:**
      * **Console:** Print the formatted string to `System.out`.
      * **File:** Append the formatted string to a log file (ensure thread safety).
6.  **Public Interface:** Expose methods like `info()`, `error()`, and `debug()` for easy usage by client code.



### 1.3 Data Flow
1.  **Client Trigger:** Any module in the application (e.g., Auth Service, Payment Processor) invokes the logger instance.
2.  **Message Formatting:** The raw message is intercepted and enriched with metadata:
    * *Timestamp:* Precise time of event ($T_0$).
    * *Severity Level:* INFO, DEBUG, WARNING, ERROR.
3.  **Synchronization Layer:** A thread-safe barrier ensures that concurrent logs from different threads do not interleave or corrupt the output.
4.  **Output Dispatch:** The formatted message is broadcasted to:
    * **Standard Output (stdout):** For real-time monitoring.
    * **File I/O Stream:** Appended to `application.log` for historical auditing.

---

## 2. Low-Level Design (LLD)

### 2.1 Design Pattern Strategy
We utilize the **Singleton Design Pattern**.
* **Reasoning:** Logging requires a shared resource (the output file). Creating multiple logger instances would lead to "file locking" issues where multiple objects try to write to the same file simultaneously, causing data corruption or IOExceptions.
* **Implementation:** The class constructor is `private`, and a static `getInstance()` method controls access.

### 2.2 Component Definitions

#### A. Log Levels (Enum)
A strictly typed enumeration to categorize message severity.
* `INFO`: General operational events.
* `DEBUG`: Granular information for development.
* `WARNING`: Unexpected events that do not stop execution.
* **ERROR:** Critical failures requiring attention.

#### B. The `CustomLogger` Class
The core class managing the logic.

**Attributes:**
| Attribute | Type | Visibility | Purpose |
| :--- | :--- | :--- | :--- |
| `instance` | `CustomLogger` | `private static` | Holds the single instance of the class. |
| `fileWriter` | `PrintWriter` | `private` | Handles the buffered character-output stream. |
| `DATE_FMT` | `DateTimeFormatter` | `private final` | Standardizes timestamp format (yyyy-MM-dd). |

**Method Signatures:**

1.  **`getInstance()`**
    * *Return:* `CustomLogger`
    * *Logic:* Double-checked locking or synchronized block to return the singleton instance.

2.  **`log(LogLevel level, String message)`**
    * *Input:* Severity level, Raw text.
    * *Logic:*
        1.  Capture current timestamp.
        2.  Format string: `[Time] [Level] Message`.
        3.  Call `System.out.println`.
        4.  Call `logToFile()`.

3.  **`logToFile(String formattedMessage)`**
    * *Modifier:* `synchronized`
    * *Logic:* Writes the string to the persistent file buffer and flushes the stream.
  



```java

public enum LogLevel {
  INFO,
  DEBUG,
  WARNING,
  ERROR
}


```
```java
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

```
```java
package org.LLD;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) {
      // Get the logger instance
      CustomLogger logger = CustomLogger.getInstance();

      logger.info("Application started successfully.");

      // Simulating some logic
      int userCount = 42;
      logger.debug("User count fetched: " + userCount);

      try {
        // Simulating an error
        int result = 10 / 0;
      } catch (Exception e) {
        logger.error("Calculation failed: " + e.getMessage());
      }

      logger.warning("Memory usage is high.");
      logger.info("Application shutting down.");
      logger.info("Vicky");
    }

}
```


### 2.3 Handling Concurrency
* **Problem:** If Thread A and Thread B call `log()` at the exact same millisecond, the lines in the text file could become mixed (e.g., "Log A start... Log B start... Log A end").
* **Solution:** The `logToFile` method is marked `synchronized`. This creates a "monitor lock," forcing Thread B to wait until Thread A has finished writing its distinct line to the file.

---

### 3. Future Scalability (Non-Functional Requirements)
* **Log Rotation:** The architecture allows for an extension where the `logToFile` method checks file size before writing; if size > 5MB, it renames the current file and starts a new one.
* **Async Logging:** For high-throughput systems, the `log()` method can push messages to a `Queue`, and a separate background thread can handle the heavy I/O writing to prevent blocking the main application flow.

-----

### Key Features of this Implementation

  * **Singleton Pattern:** `getInstance()` ensures that multiple parts of your application don't try to open the same file simultaneously, which causes locking issues.
  * **Thread Safety:** The `synchronized` keyword is used on the `logToFile` method. If multiple threads try to log at the exact same millisecond, they will queue up rather than corrupt the file.
  * **Auto-Flush:** The `PrintWriter` is set to auto-flush, meaning logs are written immediately. If the app crashes, you won't lose the most recent log lines that were stuck in the buffer.
  * **Standardized Formatting:** Using `LocalDateTime` ensures every log entry is easily searchable and sortable.

### Expected Output

**Console & `application.log` file:**

```text
[2023-10-27 10:15:30] [INFO] Application started successfully.
[2023-10-27 10:15:30] [DEBUG] User count fetched: 42
[2023-10-27 10:15:30] [ERROR] Calculation failed: / by zero
[2023-10-27 10:15:30] [WARNING] Memory usage is high.
[2023-10-27 10:15:30] [INFO] Application shutting down.
```

-----
