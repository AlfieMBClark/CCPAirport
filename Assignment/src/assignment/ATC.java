package assignment;

import static assignment.Assignment.TOTAL_PLANES;

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
    
    // Landing Queue System
    private final int[] landingQueue = new int[TOTAL_PLANES];
    private final int[] assignedGates = new int[TOTAL_PLANES]; // Store gate assignments
    private final boolean[] landingGranted = new boolean[TOTAL_PLANES]; // Track if plane can land
    private int queueSize = 0;
    private int queueFront = 0;
    private int noGateCounter = 0; // To reduce spam
    
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
            
            // Check if we can process landing queue
            if (queueSize > 0 && !airport.isRunwayOccupied()) {
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
            // Check if airport can accept the plane BEFORE adding to queue
            if (!airport.canAcceptPlane()) {
                granted = false;
                assignedGate = -1;
                System.out.println(Thread.currentThread().getName() + ": Landing request DENIED for Plane-" + planeId + " - Airport at capacity");
                return;
            }
            
            // Try to assign a gate, but allow queueing even if no gate available
            int availableGate = airport.findAvailableGate();
            if (availableGate != -1) {
                // Gate available - reserve it immediately
                airport.occupyGate(availableGate, planeId);
                assignedGate = availableGate;
            } else {
                // No gate available - plane can still join queue without gate
                assignedGate = -1;
            }
            
            // Add to landing queue (with or without gate)
            addToLandingQueue(planeId, emergency, assignedGate);
            
            // Always grant the request (plane is now in queue)
            granted = true;
            
            if (emergency) {
                if (assignedGate != -1) {
                    System.out.println(Thread.currentThread().getName() + ": EMERGENCY request from Plane-" + planeId + " - PRIORITY QUEUE POSITION with Gate-" + assignedGate);
                } else {
                    System.out.println(Thread.currentThread().getName() + ": EMERGENCY request from Plane-" + planeId + " - PRIORITY QUEUE POSITION (no gate assigned yet)");
                }
            } else {
                if (assignedGate != -1) {
                    System.out.println(Thread.currentThread().getName() + ": Landing request from Plane-" + planeId + " added to queue (Position: " + queueSize + ") with Gate-" + assignedGate);
                } else {
                    System.out.println(Thread.currentThread().getName() + ": Landing request from Plane-" + planeId + " added to queue (Position: " + queueSize + ") (no gate assigned yet)");
                }
            }
        }
        else if ("reqTakeoff".equals(request)) {
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
            assignedGate = airport.findAvailableGate();
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
            System.out.println(Thread.currentThread().getName() + ": Gate-" + gate + " released");
            airport.releaseGate(gate);
        }
    }
    
    private void addToLandingQueue(int planeId, boolean isEmergency, int gateNumber) {
        if (queueSize >= TOTAL_PLANES) {
            System.out.println(Thread.currentThread().getName() + ": Landing queue full! Cannot add Plane-" + planeId);
            return;
        }
        
        if (isEmergency) {
            // Emergency plane goes to front of queue
            // Shift everything back one position
            for (int i = queueSize; i > 0; i--) {
                landingQueue[queueFront + i] = landingQueue[queueFront + i - 1];
                assignedGates[queueFront + i] = assignedGates[queueFront + i - 1];
                landingGranted[queueFront + i] = landingGranted[queueFront + i - 1];
            }
            
            // Put emergency plane at front
            landingQueue[queueFront] = planeId;
            assignedGates[queueFront] = gateNumber;
            landingGranted[queueFront] = false;
            queueSize++;
            
            System.out.println(Thread.currentThread().getName() + ": EMERGENCY Plane-" + planeId + " moved to FRONT of landing queue");
        } else {
            // Normal plane goes to back of queue
            int position = (queueFront + queueSize) % TOTAL_PLANES;
            landingQueue[position] = planeId;
            assignedGates[position] = gateNumber;
            landingGranted[position] = false;
            queueSize++;
        }
        
        // Print current queue status
        printQueueStatus();
    }
    
    private void processNextLanding() {
        if (queueSize == 0) {
            return;
        }
        
        // Check if runway is available
        if (airport.isRunwayOccupied()) {
            return; // Wait for runway to be free
        }
        
        // Get next plane from front of queue
        int nextPlane = landingQueue[queueFront];
        boolean isEmergency = (nextPlane == 5); // Plane-5 is emergency
        
        // Check if this plane already has landing permission
        if (landingGranted[queueFront]) {
            // Plane already cleared to land but hasn't landed yet
            return;
        }
        
        // Try to assign a gate now
        int availableGate = airport.findAvailableGate();
        if (availableGate != -1) {
            // Gate now available - assign it
            airport.occupyGate(availableGate, nextPlane);
            assignedGates[queueFront] = availableGate;
            landingGranted[queueFront] = true;
            
            if (isEmergency) {
                System.out.println(Thread.currentThread().getName() + ": Gate-" + availableGate + " assigned to EMERGENCY Plane-" + nextPlane);
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY Landing Permission GRANTED for Plane-" + nextPlane);
                System.out.println(Thread.currentThread().getName() + ": Emergency Plane-" + nextPlane + " cleared to land at Gate-" + availableGate);
            } else {
                System.out.println(Thread.currentThread().getName() + ": Gate-" + availableGate + " assigned to Plane-" + nextPlane);
                System.out.println(Thread.currentThread().getName() + ": Landing Permission GRANTED for Plane-" + nextPlane);
                System.out.println(Thread.currentThread().getName() + ": Plane-" + nextPlane + " cleared to land at Gate-" + availableGate);
            }
            
            // Grant landing (runway will be occupied when plane actually lands)
            airport.occupyRunway(nextPlane);
            airport.incrementPlanesOnGround();
            
            // Remove from queue
            queueFront = (queueFront + 1) % TOTAL_PLANES;
            queueSize--;
            
        } else {
            // No gate available - reduce spam by only printing occasionally
            noGateCounter++;
            if (noGateCounter % 20 == 0) { // Print every 20 checks (1 second)
                if (isEmergency) {
                    System.out.println(Thread.currentThread().getName() + ": EMERGENCY Plane-" + nextPlane + " waiting for gate (position 1 in queue)");
                } else {
                    System.out.println(Thread.currentThread().getName() + ": Plane-" + nextPlane + " waiting for gate (position 1 in queue)");
                }
            }
        }
    }
    
    private void printQueueStatus() {
        if (queueSize == 0) {
            System.out.println(Thread.currentThread().getName() + ": Landing queue is empty");
            return;
        }
        
        System.out.print(Thread.currentThread().getName() + ": Landing queue: [");
        for (int i = 0; i < queueSize; i++) {
            int index = (queueFront + i) % TOTAL_PLANES;
            int planeId = landingQueue[index];
            int gateId = assignedGates[index];
            
            System.out.print("Plane-" + planeId);
            if (gateId != -1) {
                System.out.print("(G" + gateId + ")");
            } else {
                System.out.print("(No Gate)");
            }
            if (planeId == 5) { // Emergency plane
                System.out.print("(E)");
            }
            if (i < queueSize - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("]");
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
        // Check if plane is already in queue
        for (int i = 0; i < queueSize; i++) {
            int index = (queueFront + i) % TOTAL_PLANES;
            if (landingQueue[index] == id) {
                // Plane already in queue - return current status
                gateRef[0] = assignedGates[index];
                return landingGranted[index];
            }
        }
        
        // Plane not in queue - process new request
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
    
    // Method for planes to check if they can land
    public boolean hasLandingPermission(int planeId) {
        // Check if plane is in queue and has been granted landing
        for (int i = 0; i < queueSize; i++) {
            int index = (queueFront + i) % TOTAL_PLANES;
            if (landingQueue[index] == planeId) {
                return landingGranted[index];
            }
        }
        return false; // Plane not in queue
    }
    
    // Method to get assigned gate for a plane
    public int getAssignedGateForPlane(int planeId) {
        for (int i = 0; i < queueSize; i++) {
            int index = (queueFront + i) % TOTAL_PLANES;
            if (landingQueue[index] == planeId) {
                return assignedGates[index];
            }
        }
        return -1; // Plane not found
    }
}