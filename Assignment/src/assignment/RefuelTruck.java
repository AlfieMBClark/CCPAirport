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
        System.out.println("Plane-" + planeId + ": Requesting refueling service.");
        
        // Wait available
        while (Refuelling) {
            System.out.println("Plane-" + planeId + ": Waiting for refueling truck.");
            wait();
        }
        
        // Refuel Plane
        Refuelling = true;
        servingPlaneId = planeId;
        System.out.println("RefuelTruck: Started refueling Plane-" + planeId + ".");
        Thread.sleep(2000);
        
        // available
        Refuelling = false;
        servingPlaneId = 0;
        System.out.println("RefuelTruck: Completed refueling Plane-" + planeId + ".");
        
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
