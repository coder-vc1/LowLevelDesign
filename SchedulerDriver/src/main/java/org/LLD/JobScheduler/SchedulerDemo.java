package org.LLD.JobScheduler;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class SchedulerDemo {

  public static void main(String[] args) throws InterruptedException {

    System.out.println("--- Starting Job Scheduler Test ---");







    // 1. Initialize Scheduler with a thread pool of 2
    // This means if we submit 3 jobs, the 3rd one waits in the queue.
    SimpleJobScheduler scheduler = new SimpleJobScheduler(2);








    // --- SCENARIO 1: Happy Path (Submission) ---
    System.out.println("\n[Scenario 1] Submitting Jobs A and B...");
    scheduler.submit(new PrintJob("Job-A", 1000)); // 1 sec duration
    scheduler.submit(new PrintJob("Job-B", 1000)); // 1 sec duration

    Thread.sleep(500); // Let them start running






    // --- SCENARIO 2: Queueing & Suspension ---
    System.out.println("\n[Scenario 2] Submitting Job C (will be queued) and Suspending it...");


    // Pool is full (A and B are running), so C goes to QUEUE
    scheduler.submit(new PrintJob("Job-C", 2000));

    // Suspend C while it is still waiting
    boolean suspended = scheduler.suspend("Job-C");
    System.out.println("Job-C suspended? " + suspended);

    // Wait for A and B to finish
    Thread.sleep(1500);
    System.out.println(">> A and B should be done. C should NOT start yet (it is suspended).");







    // --- SCENARIO 3: Resuming ---
    System.out.println("\n[Scenario 3] Resuming Job C...");
    scheduler.resume("Job-C");










    // --- SCENARIO 4: Cancellation ---
    System.out.println("\n[Scenario 4] Submitting Job D and Cancelling immediately...");
    scheduler.submit(new PrintJob("Job-D", 5000));
    Thread.sleep(100); // Ensure it gets picked up or queued
    boolean cancelled = scheduler.cancel("Job-D");
    System.out.println("Job-D cancelled? " + cancelled);









    // Shutdown
    Thread.sleep(3000); // Wait for C to finish
    System.out.println("\n--- Shutting down ---");
    scheduler.shutdown();






  }

}