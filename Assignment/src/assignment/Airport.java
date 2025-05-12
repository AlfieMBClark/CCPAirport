/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assignment;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Airport {
     private static final int NumGates = 2;
    private static final int MaxPlanes = 3;
    
    // Runway
    // False = free -- True = occupied
    public static AtomicBoolean Runway = new AtomicBoolean(false);
    private int runwayOccupiedBy = 0;
    
    // Gates
    private final Gates[] gates = new Gates[NumGates];
    
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
        //Gates
        for (int i = 0; i < NumGates; i++) {
            gates[i] = new Gates(i + 1);
        }
        
        // start ATC
        atc = new ATC(this);
        atcThread = new Thread(atc, "ATC-Thread");
        atcThread.start();
    }
    
    
    //Check if airport can accept a new plane
    public boolean canAcceptPlane() {
        return planesOnGround.get() < MaxPlanes;
    }
    
    //Req Landing
    public synchronized boolean requestLanding(int planeId, boolean emergency) {
        log("Plane-" + planeId + ": Requesting Landing.");
        
        if (emergency) {
            log("ATC: EMERGENCY for Plane-" + planeId + ". Clearing runway for emergency landing.");
            
            // Emergency landing always granted
            if (Runway.get()) {
                // Wait a bit and then force clear
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Runway.set(false);
            }
            
            Runway.set(true);
            runwayOccupiedBy = planeId;
            planesOnGround.incrementAndGet();
            log("ATC: EMERGENCY Landing Permission granted for Plane-" + planeId + ".");
            return true;
        }
        
        // Normal landing request
        if (Runway.get() || !canAcceptPlane()) {
            if (!canAcceptPlane()) {
                log("ATC: Landing Permission Denied for Plane-" + planeId + ", Airport Full.");
            } else {
                log("ATC: Landing Permission Denied for Plane-" + planeId + ", Runway Occupied.");
            }
            return false;
        }
        
        Runway.set(true);
        runwayOccupiedBy = planeId;
        planesOnGround.incrementAndGet();
        log("ATC: Landing Permission granted for Plane-" + planeId + ".");
        return true;
    }
    
    // request landing
    public synchronized boolean requestLanding(int planeId) {
        // Check
        if (!Runway.get() && canAcceptPlane()) {
            // occupied
            Runway.set(true);
            runwayOccupiedBy = planeId;
            planesOnGround.incrementAndGet();
            log("ATC: Landing Permission granted for Plane-" + planeId + ".");
            return true;
        } else {
            if (!canAcceptPlane()) {
                log("ATC: Landing Permission Denied for Plane-" + planeId + ", Airport Full.");
            } else {
                log("ATC: Landing Permission Denied for Plane-" + planeId + ", Runway Occupied.");
            }
            return false;
        }
    }
    
    // release after landing
    public synchronized void completeLanding(int planeId) {
        Runway.set(false);
        runwayOccupiedBy = 0;
        log("Plane-" + planeId + ": Landed and cleared runway.");
        notifyAll(); // Notify waiting planes
    }
    
    // request takeoff
    public synchronized boolean requestTakeoff(int planeId) {
        // Check
        if (!Runway.get()) {
            // occupied
            Runway.set(true);
            runwayOccupiedBy = planeId;
            log("ATC: Takeoff Permission granted for Plane-" + planeId + ".");
            return true;
        } else {
            log("ATC: Takeoff Permission Denied for Plane-" + planeId + ", Runway Occupied.");
            return false;
        }
    }
    
    // release after takeoff
    public synchronized void completeTakeoff(int planeId) {
        Runway.set(false);
        runwayOccupiedBy = 0;
        planesOnGround.decrementAndGet();
        planesServed++;
        log("Plane-" + planeId + ": Took off and cleared runway.");
        notifyAll(); // Notify waiting planes
    }
    
    //Asign Plane to gate
    public synchronized int assignGate(int planeId) {
        for (Gates gate : gates) {
            if (!gate.isOccupied()) {
                gate.occupy(planeId);
                log("ATC: Gate-" + gate.getGateNumber() + " assigned for Plane-" + planeId + ".");
                return gate.getGateNumber();
            }
        }
        log("ATC: No gates available for Plane-" + planeId + ".");
        return -1;
    }
    
    //Gate Empty
    public synchronized void releaseGate(int gateNumber, int planeId) {
        gates[gateNumber - 1].Depart();
        log("Plane-" + planeId + ": Undocked from Gate-" + gateNumber + ".");
        notifyAll(); // Notify planes waiting for gates
    }
    
    //Assign Truck
    public RefuelTruck getRefuelTruck() {
        return refuelTruck;
    }
    
    //Passange stats
    public synchronized void updatePassengerCount(int count) {
        passengersBoarded += count;
    }
    
    //Wait Time stats
    public synchronized void updateWaitingTime(long waitTime) {
        totalWaitingTime += waitTime;
        if (waitTime > maxWaitingTime) {
            maxWaitingTime = waitTime;
        }
        if (waitTime < minWaitingTime) {
            minWaitingTime = waitTime;
        }
    }
    
    //Print Stats
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
                System.out.println("SANITY CHECK FAILED: Gate-" + gate.getGateNumber() + " is still occupied!");
            }
        }
        
        if (allGatesEmpty) {
            System.out.println("SANITY CHECK PASSED: All gates are empty.");
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
    
    //Log info
    private void log(String message) {
        System.out.println(Thread.currentThread().getName() + ": " + message);
    }
}
