package assignment;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;

public class Airport {
    private static final int NUM_GATES = 3;
    private static final int MAX_PLANES = 3;
    //Runway
    //False = free -- True = occupied
    private AtomicBoolean runway = new AtomicBoolean(false);
    private int runwayOccupiedBy = 0;
    
    private final Gates[] gates = new Gates[NUM_GATES];
    private final RefuelTruck refuelTruck = new RefuelTruck();
    
    //SEMAPHORE FOR GATES
    private final Semaphore gateSemaphore = new Semaphore(NUM_GATES, true); 
    
    // ATC
    private final ATC atc;
    private Thread atcThread;

    private final AtomicInteger planesOnGround = new AtomicInteger(0);
  
    // Stats
    private long totalWaitingTime = 0;
    private long maxWaitingTime = 0;
    private long minWaitingTime = Long.MAX_VALUE;
    private int planesServed = 0;
    private int passengersBoarded = 0;
    
    public Airport() {
        // Init gates
        for (int i = 0; i < NUM_GATES; i++) {
            gates[i] = new Gates(i + 1);
        }
        
        atc = new ATC(this);
        atcThread = new Thread(atc, "ATC");
        atcThread.start();
    }
    
    public ATC getATC() {
        return atc;
    }
    
    //Check airport
    public boolean canAcceptPlane() {
        return planesOnGround.get() < MAX_PLANES;
    }
    
    public boolean isRunwayOccupied() {
        return runway.get();
    }
    
    //Assign 
    public void occupyRunway(int planeId) {
        runway.set(true);
        runwayOccupiedBy = planeId;
    }
    
    public void clearRunway() {
        int previousOccupant = runwayOccupiedBy;
        runway.set(false);
        runwayOccupiedBy = 0;
    }
    
    public void incrementPlanesOnGround() {
        int newCount = planesOnGround.incrementAndGet();
    }
    
    public void decrementPlanesOnGround() {
        int newCount = planesOnGround.decrementAndGet();
    }
    
    //Planes fin
    public void incrementPlanesServed() {
        planesServed++;
    }
    

    public boolean areGatesAvailable() {
        return gateSemaphore.availablePermits() > 0;
    }
    
  
    public int findAvailableGate(int planeId) {
        try {
            gateSemaphore.acquire(); // Wait available
            for (Gates gate : gates) {
                if (!gate.isOccupied()) {
                    return gate.getGateNumber();
                }
            }
            gateSemaphore.release(); // Rel if no gate found
            return -1;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
    
    //Assign Gate
    public void occupyGate(int gateNumber, int planeId) {
        gates[gateNumber - 1].occupy(planeId);
    }
    
    public void releaseGate(int gateNumber) {
        if (gateNumber < 1 || gateNumber > NUM_GATES) {
            System.out.println("Aw Hell nah: Invalid gate number " + gateNumber);
            return;
        }
        gates[gateNumber - 1].Depart();
        gateSemaphore.release(); // Rel
    }
    
    public int findGateForPlane(int planeId) {
        for (Gates gate : gates) {
            if (gate.isOccupied() && gate.getOccupiedBy() == planeId) {
                return gate.getGateNumber();
            }
        }
        return -1; // Plane not found at any gate
    }
    
    //Get Truck
    public RefuelTruck getRefuelTruck() {
        return refuelTruck;
    }
   
    public synchronized void updatePassengerCount(int count) {
        passengersBoarded += count;
    }
    
    //Wait time stats
    public synchronized void updateWaitingTime(long waitTime) {
        totalWaitingTime += waitTime;
        if (waitTime > maxWaitingTime) {
            maxWaitingTime = waitTime;
        }
        if (waitTime < minWaitingTime) {
            minWaitingTime = waitTime;
        }
    }
    
    
    
    public void printStatistics() {
        System.out.println("\tAirport: Shutting down ATC system...");
        atc.shutdown();
        try {
            atcThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n========== AIRPORT STATISTICS ==========");
        System.out.println("Planes Served: "+ planesServed);
        
        //Check gates
        //boolean allGatesEmpty = true;
        //for (Gates gate : gates) {
        //   if (gate.isOccupied()) {
        //        allGatesEmpty = false;
        //        System.out.println(" Gate-" + gate.getGateNumber() + " is still occupied!");
        //    }
        //}
        
        if (planesServed==6  ) {
            System.out.println("All Planes have been served!");
        }
        
        // Print stat
        if (planesServed > 0) {
            System.out.println("\nWaiting Time Statistics:");
            System.out.println("- Maximum waiting time: " + maxWaitingTime/1000.0 + " seconds");
            System.out.println("- Average waiting time: " + (totalWaitingTime/planesServed)/1000.0 + " seconds");
            System.out.println("- Minimum waiting time: " + minWaitingTime/1000.0 + " seconds");
        }
        
        // Print service statistics
        System.out.println("\nService Stats:");
        System.out.println("- Number of planes served: " + planesServed);
        System.out.println("- Number of passengers boarded: " + passengersBoarded);
        
        System.out.println("=========================================");
    }
}