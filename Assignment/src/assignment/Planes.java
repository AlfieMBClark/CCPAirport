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
            int[] gateReference = new int[1]; // store gate assignment
            
            if (emergency) {
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY LANDING request due to low fuel!");
                landingGranted = atc.requestLanding(id, true, gateReference);
                assignedGate = gateReference[0]; // Get assigned gate
            } else {
                while (!landingGranted) {
                    System.out.println(Thread.currentThread().getName() + ": Requesting landing permission");
                    landingGranted = atc.requestLanding(id, false, gateReference);
                    
                    if (landingGranted) {
                        assignedGate = gateReference[0]; // Get assigned gate
                    } else {
                        // Wait
                        Thread.sleep(1000);
                    }
                }
            }
            
            // Record waiting time
            totalWaitingTime = System.currentTimeMillis() - waitStartTime;
            airport.updateWaitingTime(totalWaitingTime);
            
            // Land
            System.out.println(Thread.currentThread().getName() + ": Landed on runway.");
            Thread.sleep(400); 
            
            //If now gate assigned
            if (assignedGate == -1) {
                System.out.println(Thread.currentThread().getName() + ": Landed without a gate assignment, waiting for gate");
                while (assignedGate == -1) {
                    System.out.println(Thread.currentThread().getName() + ": Requesting gate assignment.");
                    assignedGate = atc.assignGate(id);
                    if (assignedGate == -1) {
                        Thread.sleep(1000);
                    }
                }
            }
            
            atc.completeLanding(id);
            
            
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
    
    
    private void performGroundOperations() throws InterruptedException {
        RefuelTruck refuelTruck = new RefuelTruck(id);
        Thread refuelingThread = new Thread(refuelTruck, "RefuelTruck-" + id);
        refuelingThread.start();
        
      
        Passenger disembarkingPassengers = new Passenger(id, passengers, false);
        Thread disembarkThread = new Thread(disembarkingPassengers, "DisembarkThread-" + id);
        // Reset passenger count
        passengers = 0;
        
        CleanRefill cleaningCrew = new CleanRefill(id);
        Thread cleaningThread = new Thread(cleaningCrew, "CleaningThread-" + id);
        
        passengers = Passenger.generatePassengerCount(capacity); // Generate new passenger count
        Passenger boardingPassengers = new Passenger(id, passengers, true);
        Thread boardingThread = new Thread(boardingPassengers, "BoardingThread-" + id);
        
        disembarkThread.start();
        cleaningThread.start();
        boardingThread.start();
        
        
        try{
            disembarkThread.join(); 
            cleaningThread.join();
            boardingThread.join();
        }catch (InterruptedException e){}
        
        //passenger statistics
        airport.updatePassengerCount(passengers);
        
        // Step 5: Wait for refueling to complete (if not already finished)
        refuelingThread.join(); // Wait for refueling to complete
        
        //Depart
        System.out.println("\t"+Thread.currentThread().getName() + ": Undocking from Gate-" + assignedGate + ".");
        Thread.sleep(500);
        
        // Notify gate release
        System.out.println("\t"+Thread.currentThread().getName() + ": Notifying ATC that Gate-" + assignedGate + " is being released.");
        atc.releaseGate(assignedGate, id);
    }
}