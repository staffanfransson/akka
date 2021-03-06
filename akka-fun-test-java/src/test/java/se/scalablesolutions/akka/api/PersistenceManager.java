package se.scalablesolutions.akka.api;

public class PersistenceManager {
  private static volatile boolean isRunning = false;
  public static void init() {
    if (!isRunning) {
      se.scalablesolutions.akka.Kernel.startRemoteService();
      isRunning = true;
    }
  }
}
