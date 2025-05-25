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
    
    // Time + stat
    private long waitStartTime = 0;
    private long totalWaitingTime = 0;
    
    // gen pass num
    private final Random random = new Random();
    
    
    // Plane deets
    public Planes(int id, int capacity, Airport airport, boolean emergency) {
        this.id = id;
        this.capacity = capacity;
        this.airport = airport;
        this.emergency = emergency;
        this.atc = airport.getATC();
        
        //Init pass count
        this.passengers = Passenger.generatePassengerCount(capacity);
    }
    
    @Override
    public void run() {
        try {
            // Req landing
            waitStartTime = System.currentTimeMillis();
            int[] gateReference = new int[1]; //gate assignment

            if (emergency) {
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY LANDING request due to low fuel!");
                atc.requestLanding(id, true, gateReference);

                while (!atc.hasLandingPermission(id)) {
                    Thread.sleep(500); 
                }

            }else {
                System.out.println(Thread.currentThread().getName() + ": Requesting landing permission");
                atc.requestLanding(id, false, gateReference);

                while (!atc.hasLandingPermission(id)) {
                    Thread.sleep(500); 
                }
            }

            //Record waiting time
            totalWaitingTime = System.currentTimeMillis() - waitStartTime;
            airport.updateWaitingTime(totalWaitingTime);

            //land
            System.out.println(Thread.currentThread().getName() + ": Received landing clearance - Landing on runway.");
            Thread.sleep(400); 
    
            //Find assigned gate
            assignedGate = airport.findGateForPlane(id);
           
            //if (assignedGate == -1) {
            //    System.out.println("ERROR: " + Thread.currentThread().getName() + " landed but no gate found!");
            //    return;
            //}

         
            // Taxi to gate
            System.out.println(Thread.currentThread().getName() + ": Landed. Moving to Gate-" + assignedGate + ".");
            if(id == 2){
                Thread.sleep(4000); 
                atc.completeLanding(id);
            }else{
                atc.completeLanding(id);
                Thread.sleep(1000); 
            } 
            System.out.println(Thread.currentThread().getName() + ": Arrived at Gate-" + assignedGate + ".");

            //Opps
            performGroundOperations();

            //takeoff
            boolean takeoffGranted = false;
            while (!takeoffGranted) {
                System.out.println(Thread.currentThread().getName() + ": Requesting takeoff permission from ATC.");
                takeoffGranted = atc.requestTakeoff(id);

                if (!takeoffGranted) {
                    Thread.sleep(1000);
                }
            }
            //Depart
            System.out.println("\t"+Thread.currentThread().getName() + ": Departing from Gate-" + assignedGate + ". Heading to Runway.");
            Thread.sleep(500);

            atc.releaseGate(assignedGate, id);
            
            System.out.println(Thread.currentThread().getName() + ":V1, Rotate, Positive Rate. Gear up.");
            Thread.sleep(400); 
            System.out.println(Thread.currentThread().getName() + ": Successfully taken off");
            atc.completeTakeoff(id);

        } catch (InterruptedException e) {}
    }

    
    private void performGroundOperations(){
        
        RefuelTruck refuelingCrew = new RefuelTruck(id);
        Thread refuelingThread = new Thread(refuelingCrew, "RefuelThread-" + id);
        refuelingThread.start();
        
        
        Passenger disembarkingPassengers = new Passenger(id, passengers, false);
        Thread PassengerDisembarkThread = new Thread(disembarkingPassengers, "DisembarkThread-" + id);
        PassengerDisembarkThread.start();
        
        try {
            PassengerDisembarkThread.join();
        } catch (InterruptedException e) {}
        
        // Reset passenger count
        passengers = 0;
        
        
        CleanRefill cleaningCrew = new CleanRefill(id);
        Thread cleaningThread = new Thread(cleaningCrew, "CleaningThread-" + id);
        cleaningThread.start();
        
        try {
            cleaningThread.join();
        } catch (InterruptedException e) {}
        
        
        passengers = Passenger.generatePassengerCount(capacity);
        Passenger boardingPassengers = new Passenger(id, passengers, true);
        Thread PassengerBoardingThread = new Thread(boardingPassengers, "BoardingThread-" + id);
        PassengerBoardingThread.start();
        
        try {
            PassengerBoardingThread.join(); 
        } catch (InterruptedException e) {}
        
        // Update stats
        airport.updatePassengerCount(passengers);
        
        // Wait for refueling to complete
        try {
            refuelingThread.join();
        } catch (InterruptedException e) {}
    }
}