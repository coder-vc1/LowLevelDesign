---
# Async Job Scheduler in Java
---

A lightweight, thread-safe, in-memory Job Scheduler designed to manage concurrent task execution. This project demonstrates core low-level system design concepts including state management, concurrency control, and thread pooling.

## Features

- **Concurrency Control:** Limits the number of active jobs using a Fixed Thread Pool.
- **State Management:** Tracks jobs through `QUEUED`, `RUNNING`, `SUSPENDED`, `COMPLETED`, and `CANCELLED` states.
- **Lifecycle Operations:**
  - `submit()`: Add jobs to the queue.
  - `suspend()`: Hold a queued job from execution.
  - `resume()`: Re-queue a suspended job.
  - `cancel()`: Interrupt a running job or remove a queued job.
- **Thread Safety:** Uses `ConcurrentHashMap` and atomic operations to handle high-concurrency environments.

---

Here is the complete, end-to-end implementation of the Job Scheduler System.

### **Part 1: System Design & Architecture**

Before writing code, we must define the lifecycle of a Job. This ensures our logic for "Suspending" or "Cancelling" is consistent.

**The Job State Machine:**

1. **QUEUED:** Submitted and waiting for a thread.
2. **RUNNING:** Currently executing on a worker thread.
3. **SUSPENDED:** Pulled from the queue (held back); not yet running.
4. **COMPLETED:** Finished execution successfully.
5. **CANCELLED:** Stopped by user or interrupted during execution.

---

### **Part 2: The Implementation (Java)**

This implementation uses standard Java `java.util.concurrent` packages. It is thread-safe and handles race conditions.

#### **1. The Interfaces**

**`Job.java`**
Defines the unit of work.

```java
package com.scheduler;

public interface Job extends Runnable {
    /**
     * Unique identifier for the job.
     */
    String getJobId();

    /**
     * Priority level (Optional extension point).
     * Higher int = Higher priority.
     */
    int getPriority();
}

```

**`Scheduler.java`**
Defines the contract for the system.

```java
package com.scheduler;

public interface Scheduler {
    /**
     * Submits a job for execution.
     * @return The job ID if accepted.
     */
    String submit(Job job);

    /**
     * Permanently stops a job. 
     * If QUEUED -> Removed. 
     * If RUNNING -> Interrupted.
     */
    boolean cancel(String jobId);

    /**
     * Temporarily holds a QUEUED job so it won't be picked up.
     * Cannot suspend a job that is already RUNNING.
     */
    boolean suspend(String jobId);

    /**
     * Returns a SUSPENDED job back to the QUEUE.
     */
    boolean resume(String jobId);
    
    /**
     * Gracefully shuts down the scheduler.
     */
    void shutdown();
}

```

**`JobStatus.java`**
```java

    public enum JobStatus { QUEUED, RUNNING, SUSPENDED, COMPLETED, CANCELLED }

```

#### **2. The Core Logic**

**`SimpleJobScheduler.java`**
This contains the scaffolding, state management, and thread pool logic.

```java

public class SimpleJobScheduler implements Scheduler {

    // Wrapper class to hold Job Metadata + Execution Handle
    private static class JobContext {
        final Job job;
        JobStatus status;
        Future<?> future; // The handle to the underlying thread task

        public JobContext(Job job) {
            this.job = job;
            this.status = JobStatus.QUEUED;
        }
    }

    private final ExecutorService executor;
    // Thread-safe map to store job state by ID
    private final Map<String, JobContext> jobStore;

    /**
     * @param poolSize Number of concurrent jobs allowed.
     */
    public SimpleJobScheduler(int poolSize) {
        // 
        // Fixed Thread Pool limits CPU usage.
        this.executor = Executors.newFixedThreadPool(poolSize);
        this.jobStore = new ConcurrentHashMap<>();
    }

    @Override
    public String submit(Job job) {
        JobContext ctx = new JobContext(job);
        String id = job.getJobId();

        // Atomic Check-and-Act: Prevent duplicate IDs
        jobStore.compute(id, (key, existing) -> {
            if (existing != null && (existing.status == JobStatus.QUEUED || existing.status == JobStatus.RUNNING)) {
                throw new IllegalStateException("Job ID " + id + " is already active.");
            }
            return ctx;
        });

        // Submit to the pool logic
        scheduleInternal(ctx);
        
        return id;
    }

    // Helper to submit the task to the ExecutorService
    private void scheduleInternal(JobContext ctx) {
        // We wrap the user's Runnable to handle State Transitions
        Runnable wrapper = () -> {
            try {
                // Double check: If suspended right before running, stop.
                synchronized (ctx) {
                    if (ctx.status == JobStatus.SUSPENDED || ctx.status == JobStatus.CANCELLED) {
                        return;
                    }
                    ctx.status = JobStatus.RUNNING;
                }

                // Execute the actual job
                ctx.job.run();
                
                // Mark complete if not interrupted
                synchronized (ctx) {
                   if (ctx.status == JobStatus.RUNNING) {
                       ctx.status = JobStatus.COMPLETED;
                   }
                }
            } catch (Exception e) {
                System.err.println("Error in job " + ctx.job.getJobId() + ": " + e.getMessage());
            }
        };

        // Submit returns a Future, which we store to allow cancellation later
        ctx.future = executor.submit(wrapper);
    }

    @Override
    public boolean cancel(String jobId) {
        JobContext ctx = jobStore.get(jobId);
        if (ctx == null) return false;

        synchronized (ctx) {
            if (ctx.status == JobStatus.COMPLETED || ctx.status == JobStatus.CANCELLED) {
                return false;
            }
            
            // 1. Cancel the Future (interruption if running)
            if (ctx.future != null) {
                // true = mayInterruptIfRunning
                ctx.future.cancel(true);
            }
            
            // 2. Update State
            ctx.status = JobStatus.CANCELLED;
            return true;
        }
    }

    @Override
    public boolean suspend(String jobId) {
        JobContext ctx = jobStore.get(jobId);
        if (ctx == null) return false;

        synchronized (ctx) {
            // We can only suspend if it hasn't started yet
            if (ctx.status == JobStatus.QUEUED) {
                // Cancel the *future* so the executor doesn't pick it up, 
                // but keep the *context* in memory.
                if (ctx.future != null) {
                    ctx.future.cancel(false); 
                }
                ctx.status = JobStatus.SUSPENDED;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean resume(String jobId) {
        JobContext ctx = jobStore.get(jobId);
        if (ctx == null) return false;

        synchronized (ctx) {
            if (ctx.status == JobStatus.SUSPENDED) {
                ctx.status = JobStatus.QUEUED;
                // Re-submit to the executor
                scheduleInternal(ctx);
                return true;
            }
        }
        return false;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}

```

#### **3. The Driver (Main Method)**

**`SchedulerDriver.java`**
A complete test harness to verify all functionality.

```java

public class SchedulerDriver {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== INITIALIZING SCHEDULER (Pool Size: 2) ===");
        // Only 2 threads. If we add 3 jobs, the 3rd must wait.
        SimpleJobScheduler scheduler = new SimpleJobScheduler(2);

        // --- 1. HAPPY PATH ---
        System.out.println("\n[1] Submitting Job-A (1s) and Job-B (1s)...");
        scheduler.submit(new MockJob("Job-A", 1000));
        scheduler.submit(new MockJob("Job-B", 1000));

        // --- 2. SUSPENSION ---
        System.out.println("\n[2] Submitting Job-C (Queue blocked by A & B)...");
        scheduler.submit(new MockJob("Job-C", 2000));
        
        System.out.println("[2] Suspending Job-C immediately...");
        boolean suspended = scheduler.suspend("Job-C");
        System.out.println("    -> Job-C Suspended? " + suspended);

        // Sleep to let A and B finish. C should NOT start because it is suspended.
        Thread.sleep(1500);
        System.out.println("    -> (A and B should be done now. C should still be holding)");

        // --- 3. RESUME ---
        System.out.println("\n[3] Resuming Job-C...");
        scheduler.resume("Job-C");

        // --- 4. CANCELLATION ---
        System.out.println("\n[4] Submitting Job-D and Cancelling it while Running...");
        scheduler.submit(new MockJob("Job-D", 3000));
        Thread.sleep(500); // Let it start
        
        boolean cancelled = scheduler.cancel("Job-D");
        System.out.println("    -> Job-D Cancelled? " + cancelled);

        // Cleanup
        Thread.sleep(3000); 
        System.out.println("\n=== SHUTDOWN ===");
        scheduler.shutdown();
    }

    // --- Mock Job Class ---
    static class MockJob implements Job {
        private final String id;
        private final int duration;

        public MockJob(String id, int duration) {
            this.id = id;
            this.duration = duration;
        }

        @Override
        public String getJobId() { return id; }

        @Override
        public int getPriority() { return 0; }

        @Override
        public void run() {
            String tName = Thread.currentThread().getName();
            System.out.println("  START: " + id + " [" + tName + "]");
            try {
                Thread.sleep(duration);
                System.out.println("  DONE : " + id);
            } catch (InterruptedException e) {
                System.out.println("  INTERRUPTED: " + id);
            }
        }
    }
}

```

---
### 5. `scheduler.shutdown()`

**What it is:** The clean-up protocol.
**Why we used it:** Without calling shutdown, the thread pool (and the Java application) will stay alive forever, waiting for new tasks. This method stops accepting new tasks and waits for active ones to finish.



## Technical Concepts & Deep Dive

This implementation relies on five critical Java Concurrency concepts.

### 1. `Runnable` Interface
**What it is:** The blueprint for a task. It represents a command that can be executed.
**Usage:** Our `Job` interface extends `Runnable`. This allows us to pass our jobs directly to Java's threading mechanisms.

### 2. `ExecutorService` (FixedThreadPool)
**What it is:** A manager for a pool of worker threads.
**Why we used it:** Creating a new thread for every job is expensive and dangerous (can crash the OS). A `FixedThreadPool` ensures we never exceed a specific number of active threads (e.g., 2), protecting system resources. Extra jobs simply wait in an internal queue.

### 3. `Future<?>`
**What it is:** A "receipt" or "handle" for an asynchronous task.
**Why we used it:** When we submit a job to the executor, it returns a `Future`. We store this object. Later, if the user asks to **Cancel** the job, we use `future.cancel(true)` to send an interrupt signal to the specific thread running that job.

### 4. `Map.compute()` (Atomic Operations)
**What it is:** A method that allows us to check a value and update it in one atomic step.
**Why we used it:**

jobStore.compute(id, (key, existing) -> { ... });

