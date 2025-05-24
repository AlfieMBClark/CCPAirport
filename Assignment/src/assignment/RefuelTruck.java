package assignment;

public class RefuelTruck implements Runnable {
    private boolean refuelling = false;
    private int servingPlaneId = 0;
    private final int requestingPlaneId;
    
    private static final int[] queue = new int[6]; //Planes
    private static int NumInQueue = 0; 
    private static int currentServing = 0; // Index of plane currently being served
    
    public RefuelTruck(int planeId) {
        this.requestingPlaneId = planeId;
    }
   
    public RefuelTruck() {
        this.requestingPlaneId = -1;
    }
    
    @Override
    public void run() {
        try {
            Refueling();
        } catch (InterruptedException e) {}
    }
    
    private void Refueling() throws InterruptedException {
        System.out.println("\t" + Thread.currentThread().getName() + ": Started refueling Plane-" + requestingPlaneId + ".");
        Thread.sleep(2000); // Refueling time
        System.out.println("\t" + Thread.currentThread().getName() + ": Completed refueling Plane-" + requestingPlaneId + ".");
    }
    
    
    private static synchronized int addToQueue(int planeId) {
        queue[NumInQueue] = planeId;
        NumInQueue++;
        System.out.println("\t" + Thread.currentThread().getName() + ": Plane-" + planeId + " request received. Added to the queue (Position: " + NumInQueue + ")");
        return NumInQueue; 
    }
    
    private static synchronized boolean isPlanesTurn(int planeId) {
        if (currentServing < NumInQueue) {
            return queue[currentServing] == planeId;
        }
        return false;
    }
    
    
    private static synchronized void moveToNextPlane() {
        currentServing++;
        System.out.println("\t" + Thread.currentThread().getName() +": Availble");
    }
    
    /**
     * Main refueling request method
     */
    public synchronized void requestRefueling(int planeId){
        System.out.println("\t" + Thread.currentThread().getName() + ": Plane-" + planeId + " requesting refueling");
        
        // Add
        int position;
        synchronized (RefuelTruck.class) {
            position = addToQueue(planeId);
        }
        
        //plane's turn
        while (true) {
            synchronized (RefuelTruck.class) {
                // Check 
                if (isPlanesTurn(planeId) && !refuelling) {
                    refuelling = true;
                    servingPlaneId = planeId;
                    
                    System.out.println("\t" + Thread.currentThread().getName() +"\t: Starting refueling for Plane-" + planeId + " (Position: " + position + ")");
                    try{
                        Thread.sleep(2000); 
                    }catch (InterruptedException e){}
                    System.out.println("\t" + Thread.currentThread().getName() +": Completed refueling for Plane-" + planeId);
                    
                    moveToNextPlane();
                    refuelling = false;
                    servingPlaneId = 0;
                    
                    RefuelTruck.class.notifyAll();
                    break;
                }
            }
            
            //Wait check
            synchronized (RefuelTruck.class) {
                try{
                    RefuelTruck.class.wait(100); // Wait 100ms or until notified
                }catch (InterruptedException e){}
            }
        }
        
        System.out.println("\t" + Thread.currentThread().getName() + ": Plane-" + planeId + " refueling completed!");
    }
    
   
    public static synchronized int getQueueSize() {
        return Math.max(0, NumInQueue - currentServing);
    }
    
    
    public static synchronized int getTotalRequests() {
        return NumInQueue;
    }
    
    //is it bussy
    public synchronized boolean isBusy() {
        return refuelling || (currentServing < NumInQueue);
    }
    
    
    public synchronized int getServingPlaneId() {
        return servingPlaneId;
    }
    
    
  
 }