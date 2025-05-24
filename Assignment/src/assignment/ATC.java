package assignment;

import static assignment.Assignment.TOTAL_PLANES;
import java.util.concurrent.Semaphore;

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
    
    // Gate management using Semaphore
    private final Semaphore gatesSemaphore;
    
    // Landing Queue System - using arrays as requested
    private final int[] landingQueue = new int[TOTAL_PLANES];
    private final boolean[] isEmergencyQueue = new boolean[TOTAL_PLANES];
    private final boolean[] landingGranted = new boolean[TOTAL_PLANES]; // Track if plane can land
    private final int[] assignedGates = new int[TOTAL_PLANES]; // Store gate assignments
    private int queueSize = 0;
    private int queueFront = 0;
    
    public ATC(Airport airport) {
        this.airport = airport;
        this.gatesSemaphore = new Semaphore(airport.getNumGates(), true); // Fair semaphore
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
            processLandingQueue();
            
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
            handleLandingRequest();
        }
        else if ("reqTakeoff".equals(request)) {
            handleTakeoffRequest();
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
            gatesSemaphore.release(); // Release the semaphore permit
        }
    }
    
    private void handleLandingRequest() {
        // Check if airport can accept the plane
        if (!airport.canAcceptPlane()) {
            granted = false;
            assignedGate = -1;
            System.out.println(Thread.currentThread().getName() + ": Landing request DENIED for Plane-" + planeId + " - Airport at capacity");
            return;
        }
        
        // Check if plane is already in queue
        if (isPlaneInQueue(planeId)) {
            granted = true; // Already in queue
            assignedGate = -1; // Will be assigned when ready to land
            return;
        }
        
        // Add to landing queue
        addToLandingQueue(planeId, emergency);
        granted = true;
        assignedGate = -1; // Will be assigned when ready to land
        
        if (emergency) {
            System.out.println(Thread.currentThread().getName() + ": EMERGENCY request from Plane-" + planeId + " - PRIORITY QUEUE POSITION");
        } else {
            System.out.println(Thread.currentThread().getName() + ": Landing request from Plane-" + planeId + " added to queue (Position: " + queueSize + ")");
        }
        
        printQueueStatus();
    }
    
    private void handleTakeoffRequest() {
        if (!airport.isRunwayOccupied()) {
            airport.occupyRunway(planeId);
            granted = true;
            System.out.println(Thread.currentThread().getName() + ": Takeoff Permission GRANTED for Plane-" + planeId);
        } else {
            granted = false;
            System.out.println(Thread.currentThread().getName() + ": Takeoff Permission DENIED for Plane-" + planeId + " - Runway occupied");
        }
    }
    
    private void addToLandingQueue(int planeId, boolean isEmergency) {
        if (queueSize >= TOTAL_PLANES) {
            System.out.println(Thread.currentThread().getName() + ": Landing queue full! Cannot add Plane-" + planeId);
            return;
        }
        
        if (isEmergency) {
            // Emergency plane goes to front of queue
            // Shift everything back one position
            for (int i = queueSize; i > 0; i--) {
                int frontIndex = queueFront;
                int backIndex = (frontIndex + i) % TOTAL_PLANES;
                int prevIndex = (frontIndex + i - 1) % TOTAL_PLANES;
                
                landingQueue[backIndex] = landingQueue[prevIndex];
                isEmergencyQueue[backIndex] = isEmergencyQueue[prevIndex];
                landingGranted[backIndex] = landingGranted[prevIndex];
                assignedGates[backIndex] = assignedGates[prevIndex];
            }
            
            // Put emergency plane at front
            landingQueue[queueFront] = planeId;
            isEmergencyQueue[queueFront] = true;
            landingGranted[queueFront] = false;
            assignedGates[queueFront] = -1;
            queueSize++;
            
            System.out.println(Thread.currentThread().getName() + ": EMERGENCY Plane-" + planeId + " moved to FRONT of landing queue");
        } else {
            // Normal plane goes to back of queue
            int position = (queueFront + queueSize) % TOTAL_PLANES;
            landingQueue[position] = planeId;
            isEmergencyQueue[position] = false;
            landingGranted[position] = false;
            assignedGates[position] = -1;
            queueSize++;
        }
    }
    
    private void processLandingQueue() {
        if (queueSize == 0) {
            return;
        }
        
        // Check if runway is available
        if (airport.isRunwayOccupied()) {
            return; // Wait for runway to be free
        }
        
        // Get next plane from front of queue
        int nextPlane = landingQueue[queueFront];
        boolean isEmergency = isEmergencyQueue[queueFront];
        
        // Check if this plane already has landing permission
        if (landingGranted[queueFront]) {
            return; // Already processed
        }
        
        // Check if gate is available (try to acquire semaphore permit)
        if (!gatesSemaphore.tryAcquire()) {
            // No gates available
            return;
        }
        
        // Both runway and gate are available - process next plane
        // Find and assign available gate
        int availableGate = airport.findAvailableGate();
        if (availableGate == -1) {
            // This shouldn't happen since we acquired semaphore permit
            gatesSemaphore.release(); // Release the permit
            System.out.println(Thread.currentThread().getName() + ": ERROR - Semaphore acquired but no gate available!");
            return;
        }
        
        // Assign gate and runway
        airport.occupyGate(availableGate, nextPlane);
        airport.occupyRunway(nextPlane);
        airport.incrementPlanesOnGround();
        
        // Mark as granted and store gate assignment
        landingGranted[queueFront] = true;
        assignedGates[queueFront] = availableGate;
        
        if (isEmergency) {
            System.out.println(Thread.currentThread().getName() + ": EMERGENCY Landing Permission GRANTED for Plane-" + nextPlane);
            System.out.println(Thread.currentThread().getName() + ": Emergency Plane-" + nextPlane + " cleared to land at Gate-" + availableGate);
        } else {
            System.out.println(Thread.currentThread().getName() + ": Landing Permission GRANTED for Plane-" + nextPlane);
            System.out.println(Thread.currentThread().getName() + ": Plane-" + nextPlane + " cleared to land at Gate-" + availableGate);
        }
        
        printQueueStatus();
    }
    
    private boolean isPlaneInQueue(int planeId) {
        for (int i = 0; i < queueSize; i++) {
            int index = (queueFront + i) % TOTAL_PLANES;
            if (landingQueue[index] == planeId) {
                return true;
            }
        }
        return false;
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
            boolean isEmergency = isEmergencyQueue[index];
            
            System.out.print("Plane-" + planeId);
            if (isEmergency) {
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
    
    public boolean requestLanding(int id, boolean isEmergency) {
        sendToATC("reqLand", id, isEmergency, 0);
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
    
    public void releaseGate(int gateNumber, int id) {
        sendToATC("gateComplete", id, false, gateNumber);
    }
    
    public void shutdown() {
        running = false;
    }
    
    // Method for planes to check if they can land (runway + gate available)
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
    
    // Method to get assigned gate for a plane (only when they're about to land)
    public int getAssignedGateForPlane(int planeId) {
        // Check if plane is in queue and has been assigned a gate
        for (int i = 0; i < queueSize; i++) {
            int index = (queueFront + i) % TOTAL_PLANES;
            if (landingQueue[index] == planeId && landingGranted[index]) {
                return assignedGates[index];
            }
        }
        return -1; // No gate assigned yet
    }
    
    // Method to remove plane from queue after landing is complete
    public void removePlaneFromQueue(int planeId) {
        for (int i = 0; i < queueSize; i++) {
            int index = (queueFront + i) % TOTAL_PLANES;
            if (landingQueue[index] == planeId) {
                // Found the plane - remove it from queue
                if (i == 0) {
                    // Plane is at front, just move front pointer
                    queueFront = (queueFront + 1) % TOTAL_PLANES;
                } else {
                    // Plane is in middle/back, shift everything forward
                    for (int j = i; j < queueSize - 1; j++) {
                        int currentIndex = (queueFront + j) % TOTAL_PLANES;
                        int nextIndex = (queueFront + j + 1) % TOTAL_PLANES;
                        
                        landingQueue[currentIndex] = landingQueue[nextIndex];
                        isEmergencyQueue[currentIndex] = isEmergencyQueue[nextIndex];
                        landingGranted[currentIndex] = landingGranted[nextIndex];
                        assignedGates[currentIndex] = assignedGates[nextIndex];
                    }
                }
                queueSize--;
                printQueueStatus();
                break;
            }
        }
    }
}