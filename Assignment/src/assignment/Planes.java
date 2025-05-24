package assignment;

import java.util.Random;

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
        this.atc = airport.getATC();
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
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY LANDING request due to low fuel!");
            } else {
                System.out.println(Thread.currentThread().getName() + ": Requesting landing permission");
            }
            
            // Request to join landing queue
            while (!landingGranted) {
                landingGranted = atc.requestLanding(id, emergency);
                if (!landingGranted) {
                    Thread.sleep(1000);
                }
            }
            
            // Now wait for actual landing clearance (when runway + gate available)
            System.out.println(Thread.currentThread().getName() + ": Added to landing queue, waiting for clearance...");
            while (!atc.hasLandingPermission(id)) {
                Thread.sleep(500); // Wait for landing clearance
            }
            
            // Get assigned gate
            assignedGate = atc.getAssignedGateForPlane(id);
            if (assignedGate == -1) {
                System.out.println(Thread.currentThread().getName() + ": ERROR - No gate assigned for landing!");
                return;
            }
            
            //waiting time
            totalWaitingTime = System.currentTimeMillis() - waitStartTime;
            airport.updateWaitingTime(totalWaitingTime);
            
            //Land
            if (emergency) {
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY LANDING on runway (assigned Gate-" + assignedGate + ")");
            } else {
                System.out.println(Thread.currentThread().getName() + ": Landing on runway (assigned Gate-" + assignedGate + ")");
            }
            Thread.sleep(400); 
            
            atc.completeLanding(id);
            
            // Remove from landing queue
            atc.removePlaneFromQueue(id);
            
            System.out.println(Thread.currentThread().getName() + ": Taxiing to Gate-" + assignedGate + ".");
            Thread.sleep(1000); // Time to taxi to gate
            System.out.println(Thread.currentThread().getName() + ": Arrived at Gate-" + assignedGate + ".");
            
            // Perform ground operations
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
    
    
    private void performGroundOperations() throws InterruptedException {
        
        Thread refuelingThread = new Thread(() -> {
            airport.getRefuelTruck().requestRefueling(id);
        }, "RefuelRequest-" + id);
        refuelingThread.start();
        
        
        Passenger disembarkingPassengers = new Passenger(id, passengers, false);
        Thread PassengerDisembarkThread = new Thread(disembarkingPassengers, "DisembarkThread-" + id);
        PassengerDisembarkThread.start();
        
        try {
            PassengerDisembarkThread.join(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
        
        // Reset passenger count after disembarking
        passengers = 0;
        
        
        CleanRefill cleaningCrew = new CleanRefill(id);
        Thread cleaningThread = new Thread(cleaningCrew, "CleaningThread-" + id);
        cleaningThread.start();
        
        try {
            cleaningThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
        
        
        passengers = Passenger.generatePassengerCount(capacity);
        Passenger boardingPassengers = new Passenger(id, passengers, true);
        Thread PassengerBoardingThread = new Thread(boardingPassengers, "BoardingThread-" + id);
        PassengerBoardingThread.start();
        
        try {
            PassengerBoardingThread.join(); 
        } catch (InterruptedException e) {}
        
        // Update stats
        airport.updatePassengerCount(passengers);
        
        // Wait for refueling to complete (runs concurrently with passenger operations)
        try {
            refuelingThread.join(); // Wait for refueling to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
        
        //Depart
        System.out.println("\t"+Thread.currentThread().getName() + ": Undocking from Gate-" + assignedGate + ".");
        Thread.sleep(500);
        
        // Notify gate release
        System.out.println("\t"+Thread.currentThread().getName() + ": Notifying ATC that Gate-" + assignedGate + " is being released.");
        atc.releaseGate(assignedGate, id);
    }
}