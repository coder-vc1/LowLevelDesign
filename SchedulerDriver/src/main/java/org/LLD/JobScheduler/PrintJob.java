package org.LLD.JobScheduler;

public class PrintJob implements Job {

  private final String id;
  private final int durationMs;

  public PrintJob(String id, int durationMs) {
    this.id = id;
    this.durationMs = durationMs;
  }

  @Override
  public String getJobId() {
    return id;
  }

  @Override
  public int getPriority() {
    return 0; // Default
  }

  @Override
  public void run() {
    System.out.println("  -> STARTING " + id + " on " + Thread.currentThread().getName());
    try {
      Thread.sleep(durationMs);
      System.out.println("  -> COMPLETED " + id);
    } catch (InterruptedException e) {
      System.out.println("  -> INTERRUPTED/CANCELLED " + id);
    }
  }

}
