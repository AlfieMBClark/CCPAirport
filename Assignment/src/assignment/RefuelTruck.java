
package assignment;

public class RefuelTruck {
    private boolean refuelling = false;
    private int servingPlaneId = 0;
    
    public synchronized void requestRefueling(int planeId) throws InterruptedException {
        System.out.println("\t" + Thread.currentThread().getName()+": Received refueling request from Plane-" + planeId + ".");
        
        // Wait until truck is available
        while (refuelling) {
            System.out.println("\t" + Thread.currentThread().getName()+": Plane-" + planeId + " waiting in queue for refueling.");
            wait();
        }
        
        // Mark as busy and refuel plane
        refuelling = true;
        servingPlaneId = planeId;
        System.out.println("\t" + Thread.currentThread().getName()+": Started refueling Plane-" + planeId + ".");
        Thread.sleep(2000);
        
        // Mark as available
        refuelling = false;
        servingPlaneId = 0;
        System.out.println("\t" + Thread.currentThread().getName()+": Completed refueling Plane-" + planeId + ".");
        
        // Notify waiting planes
        notifyAll();
    }
    
    public synchronized boolean isBusy() {
        return refuelling;
    }
    
    public synchronized int getServingPlaneId() {
        return servingPlaneId;
    }
}
