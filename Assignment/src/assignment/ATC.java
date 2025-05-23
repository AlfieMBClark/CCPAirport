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
            
            // Check if gate is available BEFORE adding to queue
            int availableGate = airport.findAvailableGate();
            if (availableGate == -1) {
                granted = false;
                assignedGate = -1;
                System.out.println(Thread.currentThread().getName() + ": Landing request DENIED for Plane-" + planeId + " - No gates available");
                return;
            }
            
            // Reserve the gate immediately
            airport.occupyGate(availableGate, planeId);
            
            // Add to landing queue with assigned gate
            addToLandingQueue(planeId, emergency, availableGate);
            
            // Grant the request (plane is now in queue with reserved gate)
            granted = true;
            assignedGate = availableGate;
            
            if (emergency) {
                System.out.println(Thread.currentThread().getName() + ": EMERGENCY request from Plane-" + planeId + " - PRIORITY QUEUE POSITION with Gate-" + availableGate);
            } else {
                System.out.println(Thread.currentThread().getName() + ": Landing request from Plane-" + planeId + " added to queue (Position: " + queueSize + ") with Gate-" + availableGate);
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
            }
            
            // Put emergency plane at front
            landingQueue[queueFront] = planeId;
            assignedGates[queueFront] = gateNumber;
            queueSize++;
            
            System.out.println(Thread.currentThread().getName() + ": EMERGENCY Plane-" + planeId + " moved to FRONT of landing queue");
        } else {
            // Normal plane goes to back of queue
            int position = (queueFront + queueSize) % TOTAL_PLANES;
            landingQueue[position] = planeId;
            assignedGates[position] = gateNumber;
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
        int assignedGate = assignedGates[queueFront];
        boolean isEmergency = (nextPlane == 5); // Plane-5 is emergency
        
        // Grant landing (gate already assigned)
        airport.occupyRunway(nextPlane);
        airport.incrementPlanesOnGround();
        
        if (isEmergency) {
            System.out.println(Thread.currentThread().getName() + ": EMERGENCY Landing Permission GRANTED for Plane-" + nextPlane);
            System.out.println(Thread.currentThread().getName() + ": Emergency Plane-" + nextPlane + " cleared to land at Gate-" + assignedGate);
        } else {
            System.out.println(Thread.currentThread().getName() + ": Landing Permission GRANTED for Plane-" + nextPlane);
            System.out.println(Thread.currentThread().getName() + ": Plane-" + nextPlane + " cleared to land at Gate-" + assignedGate);
        }
        
        // Remove from queue
        queueFront = (queueFront + 1) % TOTAL_PLANES;
        queueSize--;
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
            System.out.print("Plane-" + planeId + "(G" + gateId + ")");
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