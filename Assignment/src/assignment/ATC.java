package assignment;

import static assignment.Assignment.TOTAL_PLANES;

public class ATC implements Runnable {
    private final Airport airport;
    private volatile boolean running = true;
    
    //comms
    private volatile String request = "";
    private volatile int planeId = 0;
    private volatile boolean emergency = false;
    private volatile int gate = 0;
    private volatile boolean newRequest = false;
    private volatile boolean done = false;
    
    // Response
    private volatile boolean granted = false;
    private volatile int assignedGate = -1;
    
    //Queue
    private final int[] landingQueue = new int[TOTAL_PLANES];
    private int queueSize = 0;
    private int queueFront = 0;
    
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
            if (queueSize > 0 && !airport.isRunwayOccupied() && airport.canAcceptPlane()) {
                processNextLanding();
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
         if ("reqLand".equals(request)) {
            addToLandingQueue(planeId, emergency);
            granted = false;
            assignedGate = -1;

            if (emergency) {
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY request from Plane-" + planeId + " - ADDED TO PRIORITY QUEUE");
            }else {
                System.out.println(Thread.currentThread().getName() + ": Landing request from Plane-" + planeId + " added to queue (Land Order: " + queueSize + ")");
            }
        }else if ("reqTakeoff".equals(request)) {
            if (!airport.isRunwayOccupied()) {
                airport.occupyRunway(planeId);
                granted = true;
                System.out.println(Thread.currentThread().getName() + ": Takeoff Permission GRANTED for Plane-" + planeId);
            } else {
                granted = false;
                System.out.println(Thread.currentThread().getName() + ": Takeoff Permission DENIED for Plane-" + planeId + " - Runway occupied");
            }
        }
        else if ("reqGate".equals(request)) {
            assignedGate = airport.findAvailableGate(planeId);  // Use the plane's ID
            if (assignedGate != -1) {
                airport.occupyGate(assignedGate, planeId);
                System.out.println(Thread.currentThread().getName() + ": Gate-" + assignedGate + " assigned to Plane-" + planeId);
            } else {
                System.out.println(Thread.currentThread().getName() + ": No gates available for Plane-" + planeId);
            }
        }
        else if ("LandComplete".equals(request)) {
            System.out.println(Thread.currentThread().getName() + ": Runway clear");
            airport.clearRunway();  
        }
        else if ("TakeoffComplete".equals(request)) {
            System.out.println(Thread.currentThread().getName() + ": Runway clear");
            airport.clearRunway();
            airport.decrementPlanesOnGround();
            airport.incrementPlanesServed();
        }
        else if ("gateComplete".equals(request)) {
            System.out.println(Thread.currentThread().getName() + ": Gate-" + gate + " Is Free!");
            airport.releaseGate(gate);
        }
    }
    
    private void addToLandingQueue(int planeId, boolean isEmergency) {
        
        if (isEmergency) {
            //add to front
            for (int i = queueSize; i > 0; i--) {
                landingQueue[queueFront + i] = landingQueue[queueFront + i - 1];
            }
            landingQueue[queueFront] = planeId;
            queueSize++;
            System.out.println(Thread.currentThread().getName() + ": EMERGENCY Plane-" + planeId + " moved to FRONT of landing queue");
        } else {
            int position = (queueFront + queueSize) % TOTAL_PLANES;
            landingQueue[position] = planeId;
            queueSize++;
        }
        
        printQueueStatus();
    }
    
    private void processNextLanding() {
        if (queueSize == 0) {
            return;
        }
        int nextPlane = landingQueue[queueFront];
        boolean isEmergency = (nextPlane == 5);
        
        int gateNum = airport.findAvailableGate(nextPlane);
        if (gateNum != -1) {
            //Landing opps
            airport.occupyRunway(nextPlane);
            airport.occupyGate(gateNum, nextPlane);
            airport.incrementPlanesOnGround();
            if (isEmergency) {
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY Landing Permission GRANTED for Plane-" + nextPlane + " Proceed to Gate-"+gateNum+" once landed.");
            } else {
                System.out.println(Thread.currentThread().getName() + ": Landing Permission GRANTED for Plane-" + nextPlane+ " Proceed to Gate-"+gateNum+" once landed.");
            }

            // Rem from queue
            queueFront = (queueFront + 1) % TOTAL_PLANES;
            queueSize--;

            printQueueStatus();

        } else {
            //Remain Wilson!
            if (isEmergency) {
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY Plane-" + nextPlane + " in queue but no gates available - waiting");
            } else {
                System.out.println(Thread.currentThread().getName() + ": Plane-" + nextPlane + " in queue but no gates available - waiting");
            }
        }
    }
    
    private void printQueueStatus() {
        if (queueSize == 0) {
            System.out.println(Thread.currentThread().getName() + ": Landing queue empty");
            return;
        }
        
        System.out.print(Thread.currentThread().getName() + ": Landing queue: [");
        for (int i = 0; i < queueSize; i++) {
            int index = (queueFront + i) % TOTAL_PLANES;
            int planeId = landingQueue[index];
            System.out.print("Plane-" + planeId);
            if (planeId == 5) {
                System.out.print("(E)");
            }
            if (i < queueSize - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("]");
    }
    
    
    public boolean hasLandingPermission(int planeId) {
        return airport.isRunwayOccupied() &&  airport.findGateForPlane(planeId) != -1;
    }
    
    
    // Request to ATC thread
    private void sendToATC(String requestType, int id, boolean isEmergency, int gateNumber) {
        request = requestType;
        planeId = id;
        emergency = isEmergency;
        gate = gateNumber;
        done = false;
        newRequest = true;
        
        // Wait for ATC to process
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

        // After the plane is added to queue, we need to find which gate was actually assigned
        // when the plane landed (this happens in processNextLanding)
        // For now, just return -1 since gate assignment happens during actual landing
        gateRef[0] = -1; // Gate will be assigned when plane actually lands

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