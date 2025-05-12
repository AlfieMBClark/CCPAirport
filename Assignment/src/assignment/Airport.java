/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assignment;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Airport {
    private static final int NumGates = 2;
    private static final int MaxPlanes = 3;
    
    //Runway
    //False = free -- True = occupied
    public static AtomicBoolean Runway = new AtomicBoolean(false);
    private int runwayOccupiedBy = 0;
    
    //Gates
    
     //request landing 
    public synchronized boolean requestLanding(int planeId) {
        // Check
        if (!Runway.get()) {
            // occupied
            Runway.set(true);
            System.out.println("ATC: Landing Permission granted for Plane-" + planeId + ".");
            return true;
        } else {
            System.out.println("ATC: Landing Permission Denied for Plane-" + planeId + ", Runway Occupied.");
            return false;
        }
    }
    
    // release after landing
    public void completeLanding(int planeId) {
        Runway.set(false);
        System.out.println("Plane-" + planeId + ": Landed and cleared runway.");
    }
    
    //request takeoff
    public synchronized boolean requestTakeoff(int planeId) {
        // Check
        if (!Runway.get()) {
            //occupied
            Runway.set(true);
            System.out.println("ATC: Takeoff Permission granted for Plane-" + planeId + ".");
            return true;
        } else {
            System.out.println("ATC: Takeoff Permission Denied for Plane-" + planeId + ", Runway Occupied.");
            return false;
        }
    }
    
    //release after takeoff
    public void completeTakeoff(int planeId) {
        Runway.set(false);
        System.out.println("Plane-" + planeId + ": Took off and cleared runway.");
    }
}
