package assignment;

public class RefuelTruck implements Runnable {
    private static boolean refuelling = false;
    private static int servingPlaneId = 0;
    private final int requestingPlaneId;
    private final String planeThreadName;
    
    private static final int[] queue = new int[6];
    private static int NumInQueue = 0; 
    private static int currentServing = 0; //plane served rn
    
    public RefuelTruck(int planeId) {
        this.requestingPlaneId = planeId;
        this.planeThreadName = "Plane-" + planeId;
    }
   
    public RefuelTruck() {
        this.requestingPlaneId = -1;
        this.planeThreadName = null;
    }
    
    @Override
    public void run() {
        if (requestingPlaneId != -1) {
            requestRefueling(requestingPlaneId, planeThreadName);
        }
    }
    
    private static synchronized int addToQueue(int planeId) {
        queue[NumInQueue] = planeId;
        NumInQueue++;
        System.out.println("\t" + Thread.currentThread().getName() + ": Plane-" + planeId + " request received. Added to the queue (Ref Order: " + NumInQueue + ")");
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
        System.out.println("\t" + Thread.currentThread().getName() +": Available");
    }
    
    public static synchronized void requestRefueling(int planeId, String planeThreadName){
        
        // Add queue
        int position;
        synchronized (RefuelTruck.class) {
            position = addToQueue(planeId);
        }
        
        //Wait for plane's turn
        while (true) { 
           synchronized (RefuelTruck.class) {
                if (isPlanesTurn(planeId) && !refuelling) {
                    refuelling = true;
                    servingPlaneId = planeId;
                    
                    System.out.println("\t" + Thread.currentThread().getName() +": Starting refueling for Plane-" + planeId);
                    try{
                        Thread.sleep(6000); 
                    }catch (InterruptedException e){}
                    System.out.println("\t" + Thread.currentThread().getName() +": Completed refueling for Plane-" + planeId);
                    
                    moveToNextPlane();
                    refuelling = false;
                    servingPlaneId = 0;
                    
                    RefuelTruck.class.notifyAll();
                    break;
                }
            }
            
            //Wait and check again
            synchronized (RefuelTruck.class) {
                try{
                    RefuelTruck.class.wait(100); // Wait or notified
                }catch (InterruptedException e){}
            }
            System.out.println("\t" + planeThreadName + ": Plane-" + planeId + " refueling completed!");
        }
    }
    
    public static synchronized int getQueueSize() {
        return Math.max(0, NumInQueue - currentServing);
    }
    
    public static synchronized int getTotalRequests() {
        return NumInQueue;
    }
    
    public static synchronized boolean isBusy() {
        return refuelling || (currentServing < NumInQueue);
    }
    
    public static synchronized int getServingPlaneId() {
        return servingPlaneId;
    }
}