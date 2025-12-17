package org.LLD.JobScheduler;

public interface Job extends Runnable {

  String getJobId();

  int getPriority(); //OPTIONAL

}
