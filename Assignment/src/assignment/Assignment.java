package assignment;
import java.util.Random;

public class Assignment {
    private static final int TOTAL_PLANES = 6;
    
    public static void main(String[] args) {
        Airport airport = new Airport();
        Random random = new Random();
        
      
        System.out.println("\tAsia Pacific Airport Simulation Started");
        System.out.println("\tTotal planes: " + TOTAL_PLANES);
        
        Thread[] planeThreads = new Thread[TOTAL_PLANES];
        
      
        for(int i = 0; i < TOTAL_PLANES; i++) {
            int planeId = i + 1;
            boolean isEmergency = (planeId == 5);
            
            Planes plane = new Planes(planeId, 50, airport, isEmergency);
            planeThreads[i] = new Thread(plane, "Plane-" + planeId);
           
            int spawnPlaneTime;
            if (planeId >= 3 && planeId <= 5) {
                spawnPlaneTime = random.nextInt(400, 1000); 
                if (isEmergency){
                    System.out.println("EMERGENCY PLANE!!!!");
                }
            } else {
                spawnPlaneTime = random.nextInt(1000, 2000); 
            }
            
            try {
                Thread.sleep(spawnPlaneTime);
            } catch (InterruptedException e) {}
            
            planeThreads[i].start();
        }
        
        // Wait for all planes to complete
        for (int i = 0; i < TOTAL_PLANES; i++) {
            try {
                planeThreads[i].join();
                System.out.println("\t" + planeThreads[i].getName() + " completed operations");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Main thread interrupted while waiting for planes to complete");
            }
        }
        
        System.out.println("\tAll planes have completed their operations");
        
        // Print final statistics
        airport.printStatistics();
    }
}