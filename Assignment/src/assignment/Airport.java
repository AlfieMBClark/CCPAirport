package assignment;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Airport {
    private static final int NUM_GATES = 3;
    private static final int MAX_PLANES = 3;
    
    //Runway
    //False = free -- True = occupied
    private AtomicBoolean runway = new AtomicBoolean(false);
    private int runwayOccupiedBy = 0;
    
    
    private final Gates[] gates = new Gates[NUM_GATES];
    private final RefuelTruck refuelTruck = new RefuelTruck();
    
    // ATC
    private final ATC atc;
    private Thread atcThread;
    
    //capacity
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
        
        //System.out.println("\tAirport: Initialized with " + NUM_GATES + " gates, max " + MAX_PLANES + " planes on ground");
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
        //System.out.println("\t Planes on ground " + newCount);
    }
    
    public void decrementPlanesOnGround() {
        int newCount = planesOnGround.decrementAndGet();
        //System.out.println("\t Planes on ground" + newCount);
    }
    
    //Planes finished
    public void incrementPlanesServed() {
        planesServed++;
        //System.out.println("\tAirport: Total planes served: " + planesServed);
    }
    
    //Avail Gate
    public int findAvailableGate(int planeId) {
        for (Gates gate : gates) {
            if (!gate.isOccupied()) {
                return gate.getGateNumber();
            }
        }
        return -1;
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
    
    //print stats
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
        
        // Check gates empty
        boolean allGatesEmpty = true;
        for (Gates gate : gates) {
            if (gate.isOccupied()) {
                allGatesEmpty = false;
                System.out.println(" Gate-" + gate.getGateNumber() + " is still occupied!");
            }
        }
        
        if (allGatesEmpty) {
            System.out.println("All gates clear.");
        }
        
        // Print stat
        if (planesServed > 0) {
            System.out.println("\nWaiting Time Statistics:");
            System.out.println("- Maximum waiting time: " + maxWaitingTime/1000.0 + " seconds");
            System.out.println("- Average waiting time: " + (totalWaitingTime/planesServed)/1000.0 + " seconds");
            System.out.println("- Minimum waiting time: " + minWaitingTime/1000.0 + " seconds");
        }
        
        // Print service statistics
        System.out.println("\nService Statistics:");
        System.out.println("- Number of planes served: " + planesServed);
        System.out.println("- Number of passengers boarded: " + passengersBoarded);
        
        System.out.println("=========================================");
    }
}