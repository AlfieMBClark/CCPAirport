package assignment;
import java.util.Random;

public class Assignment {
    public static int TOTAL_PLANES = 6;
    
    public static void main(String[] args) {
        System.out.println("\tAsia Pacific Airport Simulation Started");
        System.out.println("\tTotal planes: " + TOTAL_PLANES);
        Airport airport = new Airport();
        Random random = new Random();

        Thread[] planeThreads = new Thread[TOTAL_PLANES];
        
      
        for(int i = 0; i < TOTAL_PLANES; i++) {
            int planeId = i + 1;
            boolean isEmergency = (planeId == 5);
            
            Planes plane = new Planes(planeId, 50, airport, isEmergency);
            planeThreads[i] = new Thread(plane, "Plane-" + planeId);
           
            int spawnPlaneTime;
            if (planeId >= 3 && planeId <= 5) {
                spawnPlaneTime = random.nextInt(500, 1000); 
                if (isEmergency){
                    System.out.println("EMERGENCY PLANE INTRODUCTION COMING");
                }
            } else {
                spawnPlaneTime = random.nextInt(1500, 2000); 
            }
            try {
                Thread.sleep(spawnPlaneTime);
            } catch (InterruptedException e) {}
            
            planeThreads[i].start();
        }
        
        // complete planes
        for (int i = 0; i < TOTAL_PLANES; i++) {
            try {
                planeThreads[i].join();
                System.out.println("\t" + planeThreads[i].getName() + " completed");
            } catch (InterruptedException e) {}
        }
        
        System.out.println("\n\tAll planes have completed");
        
        airport.printStatistics();
    }
}