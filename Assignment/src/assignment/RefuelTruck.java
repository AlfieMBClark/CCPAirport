/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assignment;

public class RefuelTruck {
    private boolean refuelling = false;
    private int servingPlaneId = 0;
    
    public synchronized void requestRefueling(int planeId) throws InterruptedException {
        System.out.println("RefuelTruck: Received refueling request from Plane-" + planeId + ".");
        
        // Wait until truck is available
        while (refuelling) {
            System.out.println("RefuelTruck: Plane-" + planeId + " waiting in queue for refueling.");
            wait();
        }
        
        // Mark as busy and refuel plane
        refuelling = true;
        servingPlaneId = planeId;
        System.out.println("RefuelTruck: Started refueling Plane-" + planeId + ".");
        Thread.sleep(2000);
        
        // Mark as available
        refuelling = false;
        servingPlaneId = 0;
        System.out.println("RefuelTruck: Completed refueling Plane-" + planeId + ".");
        
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
