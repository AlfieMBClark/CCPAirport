package assignment;

public class ATC implements Runnable {
    private final Airport airport;
    private volatile boolean running = true;
    
    public ATC(Airport airport) {
        this.airport = airport;
    }
    
    @Override
    public void run() {
        System.out.println("\t" + Thread.currentThread().getName() + ": System online.");
        
        // ATC monitoring loop
        while (running) {
            try {
                Thread.sleep(1000); // Monitor every second
                // Could add periodic status reports here if needed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("\t" + Thread.currentThread().getName() + ": Shutting down.");
    }
    
    public synchronized boolean requestLanding(int planeId, boolean emergency, int[] assignedGateRef) {
        // Print that ATC received the request - this will show the plane's thread name
        System.out.println("\t" + Thread.currentThread().getName() + ": Requesting landing permission from ATC");
        
        // Check runway and airport capacity
        boolean runwayFree = !airport.isRunwayOccupied();
        boolean airportHasCapacity = airport.canAcceptPlane();
        
        // Check gate availability (for non-emergency landings)
        int gateNum = -1;
        if (!emergency) {
            gateNum = airport.findAvailableGate();
            if (gateNum == -1) {
                System.out.println("\t" + Thread.currentThread().getName() + ": Landing Permission Denied - No Available Gates");
                return false;
            }
        }
        
        if (emergency) {
            System.out.println("\t" + Thread.currentThread().getName() + ": EMERGENCY LANDING REQUEST - Clearing runway");
            
            // Emergency landing always granted
            if (!runwayFree) {
                // Clear runway for emergency landing
                try {
                    wait(100); // Wait a bit before forcing clear
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                airport.clearRunway();
                System.out.println("\t" + Thread.currentThread().getName() + ": Runway cleared for emergency landing");
            }
            
            // Mark runway as occupied
            airport.occupyRunway(planeId);
            airport.incrementPlanesOnGround();
            
            // For emergency, try to find a gate after granting permission
            gateNum = airport.findAvailableGate();
            if (gateNum != -1) {
                airport.occupyGate(gateNum, planeId);
                System.out.println("\t" + Thread.currentThread().getName() + ": Gate-" + gateNum + " assigned for emergency");
            } else {
                System.out.println("\t" + Thread.currentThread().getName() + ": No gates available - Will wait for gate");
            }
            
            // Store gate assignment in reference parameter
            assignedGateRef[0] = gateNum;
            
            System.out.println("\t" + Thread.currentThread().getName() + ": EMERGENCY Landing Permission GRANTED");
            return true;
        }
        
        // Normal landing request
        if (!runwayFree || !airportHasCapacity) {
            if (!airportHasCapacity) {
                System.out.println("\t" + Thread.currentThread().getName() + ": Landing Permission Denied - Airport Full");
            } else {
                System.out.println("\t" + Thread.currentThread().getName() + ": Landing Permission Denied - Runway Occupied");
            }
            return false;
        }
        
        // Grant landing and assign gate
        System.out.println("\t" + Thread.currentThread().getName() + ": Landing Permission GRANTED");
        airport.occupyRunway(planeId);
        System.out.println("\t" + Thread.currentThread().getName() + ": Assigned to Gate-" + gateNum);
        airport.occupyGate(gateNum, planeId);
        airport.incrementPlanesOnGround();
        
        // Store gate assignment in reference parameter
        assignedGateRef[0] = gateNum;
        
        return true;
    }
    
    public synchronized void completeLanding(int planeId) {
        System.out.println("\t" + Thread.currentThread().getName() + ": Landing completed - Runway is now clear");
        airport.clearRunway();
        notifyAll(); // Notify waiting planes
    }
    
    public synchronized boolean requestTakeoff(int planeId) {
        System.out.println("\t" + Thread.currentThread().getName() + ": Requesting takeoff permission from ATC");
        
        // Check runway availability
        if (!airport.isRunwayOccupied()) {
            // Mark runway as occupied for takeoff
            airport.occupyRunway(planeId);
            System.out.println("\t" + Thread.currentThread().getName() + ": Takeoff Permission GRANTED");
            return true;
        } else {
            System.out.println("\t" + Thread.currentThread().getName() + ": Takeoff Permission Denied - Runway Occupied");
            return false;
        }
    }
    
    public synchronized void completeTakeoff(int planeId) {
        System.out.println("\t" + Thread.currentThread().getName() + ": Takeoff completed - Runway is now clear");
        airport.clearRunway();
        airport.decrementPlanesOnGround();
        airport.incrementPlanesServed();
        notifyAll(); // Notify waiting planes
    }
    
    public synchronized int assignGate(int planeId) {
        System.out.println("\t" + Thread.currentThread().getName() + ": Requesting gate assignment from ATC");
        
        int gateNum = airport.findAvailableGate();
        
        if (gateNum != -1) {
            airport.occupyGate(gateNum, planeId);
            System.out.println("\t" + Thread.currentThread().getName() + ": Gate-" + gateNum + " assigned");
        } else {
            System.out.println("\t" + Thread.currentThread().getName() + ": No gates available");
        }
        
        return gateNum;
    }
    
    public synchronized void releaseGate(int gateNumber, int planeId) {
        System.out.println("\t" + Thread.currentThread().getName() + ": Releasing Gate-" + gateNumber);
        airport.releaseGate(gateNumber);
        System.out.println("\t" + Thread.currentThread().getName() + ": Gate-" + gateNumber + " released successfully");
        notifyAll(); // Notify planes waiting for gates
    }
    
    public void shutdown() {
        running = false;
    }
}