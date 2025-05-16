
package assignment;


public class ATC implements Runnable{
    private final Airport airport;
    private boolean running = true;
    
    public ATC(Airport airport) {
        this.airport = airport;
    }
    
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + ":System online.");
        
        // ATC monitoring
        while (running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println(Thread.currentThread().getName() + ":Shutting down.");
    }
    
  
    public synchronized boolean requestLanding(int planeId, boolean emergency, int[] assignedGateRef) {
        Thread currentThread = Thread.currentThread();
        String originalThreadName = currentThread.getName();
        try {
            currentThread.setName("ATC");
            
            // Print that ATC received the request
            System.out.println(Thread.currentThread().getName() + ": Received landing request from Plane-" + planeId);
            
            // Check runway and airport capacity
            boolean runwayFree = !airport.isRunwayOccupied();
            boolean airportHasCapacity = airport.canAcceptPlane();
            
            // Check gate availability (for non-emergency landings)
            int gateNum = -1;
            if (!emergency) {
                gateNum = airport.findAvailableGate();
                if (gateNum == -1) {
                    System.out.println(Thread.currentThread().getName() + ": Landing Permission Denied for Plane-" + planeId + ", No Available Gates.");
                    return false;
                }
            }
            
            if (emergency) {
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY for Plane-" + planeId + ". Clearing runway for emergency landing.");
                
                // Emergency landing always granted
                if (!runwayFree) {
                    // Clear runway for emergency landing
                    try {
                        wait(100); // Wait a bit before forcing clear
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    airport.clearRunway();
                    System.out.println(Thread.currentThread().getName() + ": Runway cleared for emergency landing of Plane-" + planeId);
                }
                
                // Mark runway as occupied
                airport.occupyRunway(planeId);
                airport.incrementPlanesOnGround();
                
                // For emergency, try to find a gate after granting permission
                gateNum = airport.findAvailableGate();
                if (gateNum != -1) {
                    airport.occupyGate(gateNum, planeId);
                    System.out.println(Thread.currentThread().getName() + ": Gate-" + gateNum + " assigned for emergency Plane-" + planeId + ".");
                } else {
                    System.out.println(Thread.currentThread().getName() + ": No gates available for emergency Plane-" + planeId + ". Will wait for gate.");
                }
                
                // Store gate assignment in reference parameter
                assignedGateRef[0] = gateNum;
                
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY Landing Permission granted for Plane-" + planeId + ".");
                return true;
            }
            
            // Normal landing request
            if (!runwayFree || !airportHasCapacity) {
                if (!airportHasCapacity) {
                    System.out.println(Thread.currentThread().getName() + ": Landing Permission Denied for Plane-" + planeId + ", Airport Full.");
                } else {
                    System.out.println(Thread.currentThread().getName() + ": Landing Permission Denied for Plane-" + planeId + ", Runway Occupied.");
                }
                return false;
            }
            
            //Land & Gates
            System.out.println(Thread.currentThread().getName() + ": Landing Permission granted for Plane-" + planeId + ".");
            airport.occupyRunway(planeId);
            System.out.println(Thread.currentThread().getName() + ": Plane "+ planeId +" Assinged to Gate-" + gateNum);
            airport.occupyGate(gateNum, planeId);
            airport.incrementPlanesOnGround();
            
 
            
            // Store gate assignment in reference parameter
            assignedGateRef[0] = gateNum;
            
            return true;
        } finally {
            // Restore original thread name
            currentThread.setName(originalThreadName);
        }
    }
    
 
    public synchronized void completeLanding(int planeId) {
        // Switch to ATC thread context for printing
        Thread currentThread = Thread.currentThread();
        String originalThreadName = currentThread.getName();
        try {
            currentThread.setName("ATC");
            
            System.out.println(Thread.currentThread().getName() + ": Runway is clear");
            airport.clearRunway();
            notifyAll();
        } finally {
            // Restore original thread name
            currentThread.setName(originalThreadName);
        }
    }
    
    public synchronized boolean requestTakeoff(int planeId) {
        // Switch to ATC thread context for printing
        Thread currentThread = Thread.currentThread();
        String originalThreadName = currentThread.getName();
        try {
            currentThread.setName("ATC");
            
            System.out.println(Thread.currentThread().getName() + ": Received takeoff request from Plane-" + planeId);
            
            // Check runway availability
            if (!airport.isRunwayOccupied()) {
                // Mark runway as occupied for takeoff
                airport.occupyRunway(planeId);
                System.out.println(Thread.currentThread().getName() + ": Takeoff Permission granted for Plane-" + planeId + ".");
                return true;
            } else {
                System.out.println(Thread.currentThread().getName() + ": Takeoff Permission Denied for Plane-" + planeId + ", Runway Occupied.");
                return false;
            }
        } finally {
            // Restore original thread name
            currentThread.setName(originalThreadName);
        }
    }
    
    public synchronized void completeTakeoff(int planeId) {
        // Switch to ATC thread context for printing
        Thread currentThread = Thread.currentThread();
        String originalThreadName = currentThread.getName();
        try {
            currentThread.setName("ATC");
            
            System.out.println(Thread.currentThread().getName() + ": Runway is clear");
            airport.clearRunway();
            airport.decrementPlanesOnGround();
            airport.incrementPlanesServed();
            notifyAll(); // Notify waiting planes
        } finally {
            // Restore original thread name
            currentThread.setName(originalThreadName);
        }
    }
    
    public synchronized int assignGate(int planeId) {
        // Switch to ATC thread context for printing
        Thread currentThread = Thread.currentThread();
        String originalThreadName = currentThread.getName();
        try {
            currentThread.setName("ATC");
            
            System.out.println(Thread.currentThread().getName() + ": Received gate assignment request from Plane-" + planeId);
            
            int gateNum = airport.findAvailableGate();
            
            if (gateNum != -1) {
                airport.occupyGate(gateNum, planeId);
                System.out.println(Thread.currentThread().getName() + ": Gate-" + gateNum + " assigned for Plane-" + planeId + ".");
            } else {
                System.out.println(Thread.currentThread().getName() + ": No gates available for Plane-" + planeId + ".");
            }
            
            return gateNum;
        } finally {
            currentThread.setName(originalThreadName);
        }
    }
    
    public synchronized void releaseGate(int gateNumber, int planeId) {
        // Switch to ATC thread context for printing
        Thread currentThread = Thread.currentThread();
        String originalThreadName = currentThread.getName();
        try {
            currentThread.setName("ATC");
            
            System.out.println(Thread.currentThread().getName() + ": Received gate release notification from Plane-" + planeId);
            airport.releaseGate(gateNumber);
            System.out.println( Thread.currentThread().getName() + ": Confirming Gate-" + gateNumber + " has been released by Plane-" + planeId + ".");
            notifyAll(); // Notify planes waiting for gates
        } finally {
            // Restore original thread name
            currentThread.setName(originalThreadName);
        }
    }
    
    public void shutdown() {
        running = false;
    }
}