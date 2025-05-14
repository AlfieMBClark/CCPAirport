
package assignment;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Planes implements Runnable {
    private final int id;
    private final int capacity;
    private int passengers;
    private final boolean emergency;
    private final Airport airport;
    private final ATC atc;
    private int assignedGate = -1;
    
    // Timing and statistics
    private long waitStartTime = 0;
    private long totalWaitingTime = 0;
    
    // Random generator for passengers
    private final Random random = new Random();
    
    // Plane details
    public Planes(int id, int capacity, Airport airport, boolean emergency) {
        this.id = id;
        this.capacity = capacity;
        this.airport = airport;
        this.emergency = emergency;
        this.atc = airport.getATC(); // Get the ATC reference from airport
        
        // Initial passenger count - using Passenger utility class
        this.passengers = Passenger.generatePassengerCount(capacity);
    }
    
    @Override
    public void run() {
        try {
            // Request landing from ATC
            waitStartTime = System.currentTimeMillis();
            boolean landingGranted = false;
            
            if (emergency) {
                System.out.println(Thread.currentThread().getName() + ": Contacting ATC with emergency landing request due to low fuel!");
                landingGranted = atc.requestLanding(id, true);
            } else {
                while (!landingGranted) {
                    System.out.println(Thread.currentThread().getName() + ": Requesting landing permission");
                    landingGranted = atc.requestLanding(id, false);
                    
                    if (!landingGranted) {
                        // Wait before trying again
                        Thread.sleep(1000);
                    }
                }
            }
            
            // Record waiting time
            totalWaitingTime = System.currentTimeMillis() - waitStartTime;
            airport.updateWaitingTime(totalWaitingTime);
            
            // Land
            System.out.println(Thread.currentThread().getName() + ": Landing on runway.");
            Thread.sleep(400); // Time to land
            System.out.println(Thread.currentThread().getName() + ": Landed successfully.");
            atc.completeLanding(id);
            
            // Request gate assignment from ATC
            int gateNum = -1;
            while (gateNum == -1) {
                System.out.println(Thread.currentThread().getName() + ": Requesting gate assignment.");
                gateNum = atc.assignGate(id);
                if (gateNum == -1) {
                    // Wait for a gate to become available
                    Thread.sleep(1000);
                }
            }
            
            assignedGate = gateNum;
            
            // Taxi to gate
            System.out.println(Thread.currentThread().getName() + ": Taxiing to Gate-" + assignedGate + ".");
            Thread.sleep(1000); // Time to taxi to gate
            
            // Dock at gate
            System.out.println(Thread.currentThread().getName() + ": Arrived at Gate-" + assignedGate + ".");
            
            // Start ground operations
            performGroundOperations();
            
            // Request takeoff from ATC
            boolean takeoffGranted = false;
            while (!takeoffGranted) {
                System.out.println(Thread.currentThread().getName() + ": Requesting takeoff permission from ATC.");
                takeoffGranted = atc.requestTakeoff(id);
                
                if (!takeoffGranted) {
                    Thread.sleep(1000);
                }
            }
            
            // Take off
            System.out.println(Thread.currentThread().getName() + ": Taking off from runway.");
            Thread.sleep(400); // Time to take off
            System.out.println(Thread.currentThread().getName() + ": Successfully taken off");
            atc.completeTakeoff(id);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + ": Operation interrupted!");
        }
    }
    
    // Operations at Gate
    private void performGroundOperations() throws InterruptedException {
        Thread disembarkThread = new Thread(() -> {
            try {
                disembarkPassengers();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "DisembarkThread-" + id);
        
        Thread cleaningThread = new Thread(() -> {
            try {
                cleanAndRefillSupplies();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "CleaningThread-" + id);
        
        Thread refuelingThread = new Thread(() -> {
            try {
                refuelAircraft();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "RefuelingThread-" + id);
        
        // Start all threads
        disembarkThread.start();
        cleaningThread.start();
        refuelingThread.start();
        
        try {
            disembarkThread.join();
            cleaningThread.join();
            refuelingThread.join();
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e; // Rethrow to ensure proper handling
        }
        
        // Board new passengers
        boardPassengers();
        
        // Undock from gate
        System.out.println(Thread.currentThread().getName() + ": Undocking from Gate-" + assignedGate + ".");
        Thread.sleep(500); // Time to undock
        
        // Notify ATC that gate is being released
        System.out.println(Thread.currentThread().getName() + ": Notifying ATC that Gate-" + assignedGate + " is being released.");
        atc.releaseGate(assignedGate, id);
    }
    
    // Disembark
    private void disembarkPassengers() throws InterruptedException {
        // Log passenger disembarking
        Passenger.Disembarking(id, passengers);
        
        // Calculate and wait for appropriate disembarking time
        int disembarkTime = Passenger.calculateOperationTime(passengers);
        Thread.sleep(disembarkTime);
        
        System.out.println(Thread.currentThread().getName() + ": All " + passengers + " passengers have disembarked.");
        passengers = 0;
    }
    
    // Clean & Refill
    private void cleanAndRefillSupplies() throws InterruptedException {
        System.out.println(Thread.currentThread().getName() + ": Starting cleaning and supplies refill.");
        Thread.sleep(2000);
        System.out.println(Thread.currentThread().getName() + ": Cleaning and supplies refill completed.");
    }
    
    // Refuel
    private void refuelAircraft() throws InterruptedException {
        airport.getRefuelTruck().requestRefueling(id);
    }
    
    // Boarding
    private void boardPassengers() throws InterruptedException {
        // Passenger count 
        passengers = Passenger.generatePassengerCount(capacity);
        
        // Log collective passenger boarding
        Passenger.Boarding(id, passengers);
        
        // Boarding time
        int boardingTime = Passenger.calculateOperationTime(passengers);
        Thread.sleep(boardingTime);
        
        System.out.println(Thread.currentThread().getName() + ": All " + passengers + " passengers have boarded.");
        
        // Stats
        airport.updatePassengerCount(passengers);
    }
}
