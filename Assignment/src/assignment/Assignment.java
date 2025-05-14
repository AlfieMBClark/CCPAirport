/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package assignment;

import java.util.Random;

public class Assignment {
     private static final int TOTAL_PLANES = 6;
    
    public static void main(String[] args) {
        Airport airport = new Airport();
        
        // Print welcome message
        System.out.println("===== Asia Pacific Airport Simulation Started =====");
        System.out.println("Simulating operations with " + TOTAL_PLANES + " planes");
        
        Thread[] planeThreads = new Thread[TOTAL_PLANES];
        
        // Create regular planes (1-5)
        for(int i = 0; i < TOTAL_PLANES - 1; i++) {
            Planes plane = new Planes(i+1, 50, airport, false);
            planeThreads[i] = new Thread(plane, "Plane-" + (i+1));
            
            try {
                Thread.sleep(new Random().nextInt(2000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            planeThreads[i].start();
        }
        
        // Wait before introducing emergency plane
        try {
            Thread.sleep(10000);
            System.out.println("\n=== EMERGENCY SCENARIO STARTING ===");
            System.out.println("Introducing emergency plane with fuel shortage\n");
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Create and start emergency plane (plane 6)
        Planes emergencyPlane = new Planes(TOTAL_PLANES, 50, airport, true);
        planeThreads[TOTAL_PLANES - 1] = new Thread(emergencyPlane, "Plane-" + TOTAL_PLANES);
        planeThreads[TOTAL_PLANES - 1].start();
        
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
        System.out.println("\n===== Simulation completed successfully =====");
    }
}
