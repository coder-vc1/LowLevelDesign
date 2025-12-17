package org.LLD.JobScheduler;

public interface Scheduler {

  // Returns a tracking ID
  String submit(Job job);

  // Returns true if successful
  boolean cancel(String jobId);

  // Moves job to a 'holding' state
  boolean suspend(String jobId);

  // Moves job back to the ready queue
  boolean resume(String jobId);

}
