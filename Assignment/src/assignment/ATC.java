/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assignment;


public class ATC implements Runnable{
    private final Airport airport;
    private boolean running = true;
    
    public ATC(Airport airport) {
        this.airport = airport;
    }
    
    @Override
    public void run() {
        Assignment.Printmsg("ATC: Air Traffic Control system online.");
        
        // ATC monitoring
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        Assignment.Printmsg("ATC: Air Traffic Control system shutting down.");
    }
    
    //Req land
    public synchronized boolean requestLanding(int planeId, boolean emergency) {
        Assignment.Printmsg("Plane-" + planeId + ": Requesting Landing.");
        
        // Check runway and airport capacity
        boolean runwayFree = !airport.isRunwayOccupied();
        boolean airportHasCapacity = airport.canAcceptPlane();
        
        if (emergency) {
            Assignment.Printmsg("ATC: EMERGENCY for Plane-" + planeId + ". Clearing runway for emergency landing.");
            
            // Emergency landing always granted
            if (!runwayFree) {
                // Clear runway for emergency landing
                try {
                    wait(1000); // Wait a bit before forcing clear
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                airport.clearRunway();
            }
            
            //  runway occupied
            airport.occupyRunway(planeId);
            airport.incrementPlanesOnGround();
            Assignment.Printmsg("ATC: EMERGENCY Landing Permission granted for Plane-" + planeId + ".");
            return true;
        }
        
        // Normal landing request
        if (!runwayFree || !airportHasCapacity) {
            if (!airportHasCapacity) {
                Assignment.Printmsg("ATC: Landing Permission Denied for Plane-" + planeId + ", Airport Full.");
            } else {
                Assignment.Printmsg("ATC: Landing Permission Denied for Plane-" + planeId + ", Runway Occupied.");
            }
            return false;
        }
        
        // Grant landing permission
        airport.occupyRunway(planeId);
        airport.incrementPlanesOnGround();
        Assignment.Printmsg("ATC: Landing Permission granted for Plane-" + planeId + ".");
        return true;
    }
    
    //Notify Land Completed
    public synchronized void completeLanding(int planeId) {
        airport.clearRunway();
        Assignment.Printmsg("Plane-" + planeId + ": Landed and cleared runway.");
        notifyAll(); // Notify waiting planes
    }
    
    //Req Takeoff
    public synchronized boolean requestTakeoff(int planeId) {
        Assignment.Printmsg("Plane-" + planeId + ": Requesting Takeoff.");
        
        // runway availability
        if (!airport.isRunwayOccupied()) {
            // runway occupied for takeoff
            airport.occupyRunway(planeId);
            Assignment.Printmsg("ATC: Takeoff Permission granted for Plane-" + planeId + ".");
            return true;
        } else {
            Assignment.Printmsg("ATC: Takeoff Permission Denied for Plane-" + planeId + ", Runway Occupied.");
            return false;
        }
    }
    
   //Plane completed takeoff
    public synchronized void completeTakeoff(int planeId) {
        airport.clearRunway();
        airport.decrementPlanesOnGround();
        airport.incrementPlanesServed();
        Assignment.Printmsg("Plane-" + planeId + ": Took off successfully.");
        notifyAll(); // Notify waiting planes
    }
    
    //Assign Gate
    public synchronized int assignGate(int planeId) {
        int gateNum = airport.findAvailableGate();
        
        if (gateNum != -1) {
            airport.occupyGate(gateNum, planeId);
            Assignment.Printmsg("ATC: Gate-" + gateNum + " assigned for Plane-" + planeId + ".");
        } else {
            Assignment.Printmsg("ATC: No gates available for Plane-" + planeId + ".");
        }
        
        return gateNum;
    }
    
    //Plane Leaves gate
    public synchronized void releaseGate(int gateNumber, int planeId) {
        airport.releaseGate(gateNumber);
        Assignment.Printmsg("Plane-" + planeId + ": Undocked from Gate-" + gateNumber + ".");
        notifyAll(); // Notify planes waiting for gates
    }
    
    //ShutDown
    public void shutdown() {
        running = false;
    }
}