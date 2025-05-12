
package assignment;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Planes implements Runnable {
     private final int id;
    private final int capacity;
    private int passengers;
    private final boolean emergency;
    private final Airport airport;
    private int assignedGate = -1;
    
    // Timing and statistics
    private long waitStartTime = 0;
    private long totalWaitingTime = 0;
    
    // Random generator for passengers
    private final Random random = new Random();
    
    /**
     * Constructor
     * @param id Plane ID
     * @param capacity Maximum passenger capacity
     * @param airport The airport instance
     * @param emergency Whether this is an emergency plane
     */
    public Planes(int id, int capacity, Airport airport, boolean emergency) {
        this.id = id;
        this.capacity = capacity;
        this.airport = airport;
        this.emergency = emergency;
        
        // Initial passenger count
        this.passengers = random.nextInt(capacity + 1);
    }
    
    @Override
    public void run() {
        try {
            // Request landing
            waitStartTime = System.currentTimeMillis();
            boolean landingGranted = false;
            
            if (emergency) {
                Assignment.log("Plane-" + id + ": EMERGENCY! Low fuel, requesting immediate landing!");
                landingGranted = airport.requestLanding(id, true);
            } else {
                while (!landingGranted) {
                    landingGranted = airport.requestLanding(id);
                    
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
            Assignment.log("Plane-" + id + ": Landing.");
            Thread.sleep(1000); // Time to land
            airport.completeLanding(id);
            
            // Request gate
            int gateNum = -1;
            while (gateNum == -1) {
                gateNum = airport.assignGate(id);
                if (gateNum == -1) {
                    // Wait for a gate to become available
                    Thread.sleep(1000);
                }
            }
            
            assignedGate = gateNum;
            
            // Coast to gate
            Assignment.log("Plane-" + id + ": Coasting to Gate-" + assignedGate + ".");
            Thread.sleep(1500); // Time to coast to gate
            
            // Dock at gate
            Assignment.log("Plane-" + id + ": Docked at Gate-" + assignedGate + ".");
            
            // Start ground operations
            performGroundOperations();
            
            // Req takeoff
            boolean takeoffGranted = false;
            while (!takeoffGranted) {
                takeoffGranted = airport.requestTakeoff(id);
                
                if (!takeoffGranted) {
                    Thread.sleep(1000);
                }
            }
            
            // Take off
            Assignment.log("Plane-" + id + ": Taking-off.");
            Thread.sleep(1000); // Time to take off
            airport.completeTakeoff(id);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assignment.log("Plane-" + id + ": Operation interrupted!");
        }
    }
    
    //Operations at Gate
    private void performGroundOperations() throws InterruptedException {
        CountDownLatch operationsComplete = new CountDownLatch(3);
        
        // Start disembarking passengers
        Thread disembarkThread = new Thread(() -> {
            try {
                disembarkPassengers();
                operationsComplete.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Start cleaning and supplies
        Thread cleaningThread = new Thread(() -> {
            try {
                cleanAndRefillSupplies();
                operationsComplete.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Start refueling
        Thread refuelingThread = new Thread(() -> {
            try {
                refuelAircraft();
                operationsComplete.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Start all ground operation threads
        disembarkThread.start();
        cleaningThread.start();
        refuelingThread.start();
        
        // Wait for all operations to complete
        operationsComplete.await();
        
        // Board new passengers
        boardPassengers();
        
        // Undock from gate
        Assignment.log("Plane-" + id + ": Undocking from Gate-" + assignedGate + ".");
        Thread.sleep(500); // Time to undock
        
        // Release the gate
        airport.releaseGate(assignedGate, id);
    }
    
    //Disembark
    private void disembarkPassengers() throws InterruptedException {
        Assignment.log("Plane-" + id + "'s Passengers: Disembarking out of Plane-" + id + ".");
        
        // Create passenger threads if wanted for detailed simulation
        // For simplicity, just sleep proportional to passenger count
        int disembarkTime = 50 * passengers;
        Thread.sleep(disembarkTime);
        
        Assignment.log("Plane-" + id + ": All " + passengers + " passengers have disembarked.");
        passengers = 0;
    }
    
    //Clean & Refill
    private void cleanAndRefillSupplies() throws InterruptedException {
        Assignment.log("Plane-" + id + ": Starting cleaning and supplies refill.");
        Thread.sleep(2000);
        Assignment.log("Plane-" + id + ": Cleaning and supplies refill completed.");
    }
    
    //Refeul
    private void refuelAircraft() throws InterruptedException {
        airport.getRefuelTruck().requestRefueling(id);
    }
    
    //Boarding
    private void boardPassengers() throws InterruptedException {
        //passenger count
        passengers = random.nextInt(capacity + 1);
        
        Assignment.log("Plane-" + id + ": Boarding " + passengers + " new passengers.");
        
        // Create passenger threads if wanted for detailed simulation
        // For simplicity, just sleep proportional to passenger count
        int boardingTime = 50 * passengers;
        Thread.sleep(boardingTime);
        
        Assignment.log("Plane-" + id + ": All " + passengers + " passengers have boarded.");
        
        // airport statistics
        airport.updatePassengerCount(passengers);
    }
}
