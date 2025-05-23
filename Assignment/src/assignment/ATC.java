package assignment;

public class ATC implements Runnable {
    private final Airport airport;
    private volatile boolean running = true;
    
    // Simple communication
    private volatile String request = "";
    private volatile int planeId = 0;
    private volatile boolean emergency = false;
    private volatile int gate = 0;
    private volatile boolean newRequest = false;
    private volatile boolean done = false;
    
    // Response
    private volatile boolean granted = false;
    private volatile int assignedGate = -1;
    
    public ATC(Airport airport) {
        this.airport = airport;
    }
    
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + ": System online.");
        
        while (running) {
            if (newRequest) {
                handleRequest();
                newRequest = false;
                done = true;
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println(Thread.currentThread().getName() + ": Shutting down.");
    }
    
    private void handleRequest() {
        // ATC responds (not acknowledges) - this runs in ATC thread
        
        if ("reqLand".equals(request)) {
            if (emergency) {
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY - Clearing runway for Plane-" + planeId);
                if (!airport.isRunwayOccupied() && airport.canAcceptPlane()) {
                    assignedGate = airport.findAvailableGate();
                    if (assignedGate != -1) {
                        airport.occupyRunway(planeId);
                        airport.occupyGate(assignedGate, planeId);
                        airport.incrementPlanesOnGround();
                        granted = true;
                        System.out.println(Thread.currentThread().getName() + ": Landing Permission GRANTED for Emergency Plane-" + planeId);
                        System.out.println(Thread.currentThread().getName() + ": Emergency Plane-" + planeId + " assigned to Gate-" + assignedGate);
                    }else {
                        granted = false;
                        System.out.println(Thread.currentThread().getName() + ": Landing Permission DENIED for Emergency Plane-" + planeId + " - No gates available");
                    }
                }else {
                    granted = false;
                    System.out.println(Thread.currentThread().getName() + ": Landing Permission DENIED for Emergency Plane-" + planeId + " - Runway occupied or airport full");
                }
            }else {
                // Normal landing
                if (!airport.isRunwayOccupied() && airport.canAcceptPlane()) {
                    assignedGate = airport.findAvailableGate();
                    if (assignedGate != -1) {
                        airport.occupyRunway(planeId);
                        airport.occupyGate(assignedGate, planeId);
                        airport.incrementPlanesOnGround();
                        granted = true;
                        System.out.println(Thread.currentThread().getName() + ": Landing Permission GRANTED for Plane-" + planeId);
                        System.out.println(Thread.currentThread().getName() + ": Plane-" + planeId + " assigned to Gate-" + assignedGate);
                    }else {
                        granted = false;
                        System.out.println(Thread.currentThread().getName() + ": Landing Permission DENIED for Plane-" + planeId + " - No gates available");
                    }
                }else {
                    granted = false;
                    System.out.println(Thread.currentThread().getName() + ": Landing Permission DENIED for Plane-" + planeId + " - Runway occupied or airport full");
                }
            }
        }else if ("reqTakeoff".equals(request)) {
            if (!airport.isRunwayOccupied()) {
                airport.occupyRunway(planeId);
                granted = true;
                System.out.println(Thread.currentThread().getName() + ": Takeoff Permission GRANTED for Plane-" + planeId);
            }else {
                granted = false;
                System.out.println(Thread.currentThread().getName() + ": Takeoff Permission DENIED for Plane-" + planeId + " - Runway occupied");
            }
        }else if ("reqGate".equals(request)) {
            assignedGate = airport.findAvailableGate();
            if (assignedGate != -1) {
                airport.occupyGate(assignedGate, planeId);
                System.out.println(Thread.currentThread().getName() + ": Gate-" + assignedGate + " assigned to Plane-" + planeId);
            } else {
                System.out.println(Thread.currentThread().getName() + ": No gates available for Plane-" + planeId);
            }
        }else if ("LandComplete".equals(request)) {
            System.out.println(Thread.currentThread().getName() + ": Runway clear");
            airport.clearRunway();  
        }else if ("TakeoffComplete".equals(request)) {
            System.out.println(Thread.currentThread().getName() + ": Runway clear");
            airport.clearRunway();
            airport.decrementPlanesOnGround();
            airport.incrementPlanesServed();
        
        }else if ("gateComplete".equals(request)) {
            System.out.println(Thread.currentThread().getName() + ": Gate-" + gate + " released");
            airport.releaseGate(gate);
        }
    }
    
    //request to ATC thread
    private void sendToATC(String requestType, int id, boolean isEmergency, int gateNumber) {
        request = requestType;
        planeId = id;
        emergency = isEmergency;
        gate = gateNumber;
        done = false;
        newRequest = true;
        
        //DO IT
        while (!done) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
    
    public boolean requestLanding(int id, boolean isEmergency, int[] gateRef) {
        sendToATC("reqLand", id, isEmergency, 0);
        gateRef[0] = assignedGate;
        return granted;
    }
    
    public void completeLanding(int id) {
        sendToATC("LandComplete", id, false, 0);
    }
    
    public boolean requestTakeoff(int id) {
        sendToATC("reqTakeoff", id, false, 0);
        return granted;
    }
    
    public void completeTakeoff(int id) {
        sendToATC("TakeoffComplete", id, false, 0);
    }
    
    public int assignGate(int id) {
        sendToATC("reqGate", id, false, 0);
        return assignedGate;
    }
    
    public void releaseGate(int gateNumber, int id) {
        sendToATC("gateComplete", id, false, gateNumber);
    }
    
    public void shutdown() {
        running = false;
    }
}