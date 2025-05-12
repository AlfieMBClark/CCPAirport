
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
     * Plane ID
     * capacity Maximum passenger capacity
     * airport The airport instance
     * emergency Whether this is an emergency plane
     */
     public Planes(int id, int capacity, Airport airport, boolean emergency) {
        this.id = id;
        this.capacity = capacity;
        this.airport = airport;
        this.emergency = emergency;
        
        // Initial passenger count - using Passenger utility class
        this.passengers = Passenger.generatePassengerCount(capacity);
    }
    
    @Override
    public void run() {
        try {
            // Request landing
            waitStartTime = System.currentTimeMillis();
            boolean landingGranted = false;
            
            if (emergency) {
                Assignment.Printmsg("Plane-" + id + ": EMERGENCY! Low fuel, requesting immediate landing!");
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
            Assignment.Printmsg("Plane-" + id + ": Landing.");
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
            
            //Taxi to gate
            Assignment.Printmsg("Plane-" + id + ": Taxi to Gate-" + assignedGate + ".");
            Thread.sleep(1500); // Time to coast to gate
            
            // Dock at gate
            Assignment.Printmsg("Plane-" + id + ": At Gate-" + assignedGate + ".");
            
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
            Assignment.Printmsg("Plane-" + id + ": Taking-off.");
            Thread.sleep(1000); // Time to take off
            airport.completeTakeoff(id);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assignment.Printmsg("Plane-" + id + ": Operation interrupted!");
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
        Assignment.Printmsg("Plane-" + id + ": Undocking from Gate-" + assignedGate + ".");
        Thread.sleep(500); // Time to undock
        
        // Release the gate
        airport.releaseGate(assignedGate, id);
    }
    
    //Disembark
    private void disembarkPassengers() throws InterruptedException {
        // Log  passenger disembarking
        Passenger.Disembarking(id, passengers);
        
        // Calculate and wait for appropriate disembarking time
        int disembarkTime = Passenger.calculateOperationTime(passengers);
        Thread.sleep(disembarkTime);
        
        Assignment.Printmsg("Plane-" + id + ": All " + passengers + " passengers have disembarked.");
        passengers = 0;
    }
    
    //Clean & Refill
    private void cleanAndRefillSupplies() throws InterruptedException {
        Assignment.Printmsg("Plane-" + id + ": Starting cleaning and supplies refill.");
        Thread.sleep(2000);
        Assignment.Printmsg("Plane-" + id + ": Cleaning and supplies refill completed.");
    }
    
    //Refeul
    private void refuelAircraft() throws InterruptedException {
        airport.getRefuelTruck().requestRefueling(id);
    }
    
    //Boarding
    private void boardPassengers() throws InterruptedException {
        // passenger count 
        passengers = Passenger.generatePassengerCount(capacity);
        
        // Log collective passenger boarding
        Passenger.Boarding(id, passengers);
        
        //boarding time
        int boardingTime = Passenger.calculateOperationTime(passengers);
        Thread.sleep(boardingTime);
        
        Assignment.Printmsg("Plane-" + id + ": All " + passengers + " passengers have boarded.");
        
        //stats
        airport.updatePassengerCount(passengers);
    }
}
