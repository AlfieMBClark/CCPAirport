/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assignment;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Airport {
    private static final int NUM_GATES = 2;
    private static final int MAX_PLANES = 3;
    
    // Runway
    // False = free -- True = occupied
    private AtomicBoolean runway = new AtomicBoolean(false);
    private int runwayOccupiedBy = 0;
    
    // Gates
    private final Gates[] gates = new Gates[NUM_GATES];
    
    // RefuelTruck
    private final RefuelTruck refuelTruck = new RefuelTruck();
    
    // ATC
    private final ATC atc;
    private Thread atcThread;
    
    // Airport capacity tracking
    private final AtomicInteger planesOnGround = new AtomicInteger(0);
    
    // Statistics
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
        // Init ATC
        atc = new ATC(this);
        atcThread = new Thread(atc, "ATC-Thread");
        atcThread.start();
    }
    
    
    public ATC getATC() {
        return atc;
    }
    
    //Check airport
    public boolean canAcceptPlane() {
        return planesOnGround.get() < MAX_PLANES;
    }
    
    //Check Runway
    public boolean isRunwayOccupied() {
        return runway.get();
    }
    
    //Assign Runway
    public void occupyRunway(int planeId) {
        runway.set(true);
        runwayOccupiedBy = planeId;
    }
    
    //Clear Runway
    public void clearRunway() {
        runway.set(false);
        runwayOccupiedBy = 0;
    }
    
    //Increment & Decrement Ground
    public void incrementPlanesOnGround() {
        planesOnGround.incrementAndGet();
    }
    
    public void decrementPlanesOnGround() {
        planesOnGround.decrementAndGet();
    }
    
    //Planes finished
    public void incrementPlanesServed() {
        planesServed++;
    }
    
    //Avail Gate
    public int findAvailableGate() {
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
    
    //Gate Finished
    public void releaseGate(int gateNumber) {
        gates[gateNumber - 1].Depart();
    }
    
    //Get Truck
    public RefuelTruck getRefuelTruck() {
        return refuelTruck;
    }
    
    //Count passangers
    public synchronized void updatePassengerCount(int count) {
        passengersBoarded += count;
    }
    
    //Wiat time stats
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
        atc.shutdown();
        try {
            atcThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n========== AIRPORT STATISTICS ==========");
        
        // Check gates are empty
        boolean allGatesEmpty = true;
        for (Gates gate : gates) {
            if (gate.isOccupied()) {
                allGatesEmpty = false;
                System.out.println("Gate-" + gate.getGateNumber() + " is still occupied!");
            }
        }
        
        if (allGatesEmpty) {
            System.out.println("All gates are empty.");
        }
        
        // Print waiting time statistics
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
