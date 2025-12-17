package org.LLD.JobScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class SimpleJobScheduler implements Scheduler {

  private final ExecutorService executor;
  // Map to track all jobs by ID for O(1) access
  private final Map<String, JobContext> jobStore;

  public SimpleJobScheduler(int poolSize) {
    this.executor = Executors.newFixedThreadPool(poolSize);
    this.jobStore = new ConcurrentHashMap<>();
  }








  @Override
  public String submit(Job job) {
    String id = job.getJobId();
    JobContext ctx = new JobContext(job);

    // Critical section: Avoid race condition if submitting same ID twice
    jobStore.compute(id, (key, existing) -> {
      if (existing != null && existing.status == JobStatus.RUNNING) {
        throw new IllegalStateException("Job with this ID is already running");
      }
      return ctx;
    });

    // Submit to thread pool
    // We wrap the job to update status upon completion
    Future<?> future = executor.submit(() -> {
      try {
        ctx.status = JobStatus.RUNNING;
        job.run();
        ctx.status = JobStatus.COMPLETED;
      } catch (Exception e) {
        // Log error
      }
    });

    ctx.future = future;
    return id;
  }









  @Override
  public boolean cancel(String jobId) {
    JobContext ctx = jobStore.get(jobId);
    if (ctx == null || ctx.status == JobStatus.COMPLETED) {
      return false;
    }

    // true = mayInterruptIfRunning
    boolean cancelled = ctx.future.cancel(true);
    if (cancelled) {
      ctx.status = JobStatus.CANCELLED;
      jobStore.remove(jobId); // Optional: Clean up memory
    }
    return cancelled;
  }










  @Override
  public boolean suspend(String jobId) {
    JobContext ctx = jobStore.get(jobId);
    if (ctx == null) {
      return false;
    }

    synchronized (ctx) {
      // We can only suspend if it hasn't started yet (is still in queue)
      // Note: Suspending a *running* thread is generally unsafe in Java
      // without cooperative cancellation logic inside the Job itself.
      if (ctx.status == JobStatus.QUEUED) {
        // Cancel the future execution but keep the data in our store
        ctx.future.cancel(false);
        ctx.status = JobStatus.SUSPENDED;
        return true;
      }
    }
    return false;
  }









  @Override
  public boolean resume(String jobId) {
    JobContext ctx = jobStore.get(jobId);
    if (ctx == null) {
      return false;
    }

    synchronized (ctx) {
      if (ctx.status == JobStatus.SUSPENDED) {
        // Resubmit the job to the executor
        ctx.status = JobStatus.QUEUED;
        ctx.future = executor.submit(() -> {
          try {
            ctx.status = JobStatus.RUNNING;
            ctx.job.run();
            ctx.status = JobStatus.COMPLETED;
          } catch (Exception e) {
            // Handle error
          }
        });
        return true;
      }
    }
    return false;
  }






  public void shutdown() {
    executor.shutdown();
  }









  // Internal wrapper to hold job details and execution handle
  private static class JobContext {

    Job job;
    JobStatus status;
    Future<?> future; // The handle to the running/queued task

    public JobContext(Job job) {
      this.job = job;
      this.status = JobStatus.QUEUED;
    }
  }





}