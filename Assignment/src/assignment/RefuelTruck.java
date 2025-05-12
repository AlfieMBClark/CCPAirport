/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assignment;

/**
 *
 * @author alfie
 */
public class RefuelTruck {
    private boolean Refuelling = false;
    private int servingPlaneId = 0;
    
    public synchronized void requestRefueling(int planeId) throws InterruptedException {
        Assignment.log("Plane-" + planeId + ": Requesting refueling service.");
        
        // Wait available
        while (Refuelling) {
            Assignment.log("Plane-" + planeId + ": Waiting for refueling truck.");
            wait();
        }
        
        // Refuel Plane
        Refuelling = true;
        servingPlaneId = planeId;
        Assignment.log("RefuelTruck: Started refueling Plane-" + planeId + ".");
        Thread.sleep(2000);
        
        // available
        Refuelling = false;
        servingPlaneId = 0;
        Assignment.log("RefuelTruck: Completed refueling Plane-" + planeId + ".");
        
        // Notify
        notifyAll();
    }
    
   
    //Check busy
    public synchronized boolean isBusy() {
        return Refuelling;
    }
    
    //PlaneRefueled
    public synchronized int RefullinbgPlaneId() {
        return servingPlaneId;
    }
}
