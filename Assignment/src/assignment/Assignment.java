/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package assignment;

import java.util.Random;

public class Assignment {

    private static final int TotalPlanes=6;
    
    public static void main(String[] args) {
        Airport airport = new Airport();
        
        Thread[] planeThreads = new Thread[TotalPlanes];
        for(int i=0; i<TotalPlanes;i++){
            Planes plane = new Planes(i+1, 50, airport, false);
            planeThreads[i] = new Thread(plane, "Plane: "+ (i+1));
            
            try{
                Thread.sleep(new Random().nextInt(1000));
            }catch (InterruptedException e){}
            
            planeThreads[i].start();
        }
        
        
        //Emergency Plane
        try{
            Thread.sleep(10000);
        }catch(InterruptedException e){}
        
        Planes EPlane = new Planes(TotalPlanes, 50, airport, true);
        planeThreads[TotalPlanes - 1] = new Thread(EPlane, "Plane-" + TotalPlanes);
        planeThreads[TotalPlanes - 1].start();
        
        // Wait for all planes to complete
        for (Thread planeThread : planeThreads) {
            try {
                planeThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Print statistics 
        airport.printStatistics();
        System.out.println("Simulation completed.");
    }
    
    public static synchronized void log(String message) {
        System.out.println(Thread.currentThread().getName() + ": " + message);
    }
    
}
