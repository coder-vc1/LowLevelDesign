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