package assignment;

import java.util.Random;

public class Passenger implements Runnable {
    private static final Random random = new Random();
    private final int planeId;
    private final int passengerCount;
    private final boolean isBoarding; // true for boarding, false for disembarking
    
    public Passenger(int planeId, int passengerCount, boolean isBoarding) {
        this.planeId = planeId;
        this.passengerCount = passengerCount;
        this.isBoarding = isBoarding;
    }
    
    @Override
    public void run() {
        try {
            if (isBoarding) {
                performBoarding();
            } else {
                performDisembarking();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("\t" + Thread.currentThread().getName() + ": Passenger operation interrupted!");
        }
    }
    
    private void performBoarding() throws InterruptedException {
        System.out.println("\tPassengers: " + passengerCount + " passengers boarding Plane-" + planeId);
        // Boarding time
        int boardingTime = calculateOperationTime(passengerCount);
        Thread.sleep(boardingTime);
        
        System.out.println("\t" + Thread.currentThread().getName() + ": All " + passengerCount + " passengers have boarded Plane-"+ planeId);
    }
    
    private void performDisembarking() throws InterruptedException {
        System.out.println("\tPassengers: " + passengerCount + " passengers disembarking from Plane-" + planeId);
        
        // Disembarking time
        int disembarkTime = calculateOperationTime(passengerCount);
        Thread.sleep(disembarkTime);
        
        System.out.println("\t" + Thread.currentThread().getName() + ": All " + passengerCount + " passengers have disembarked from Plane-"+planeId);
    }
    
    //Random passenger num
    public static int generatePassengerCount(int maxCapacity) {
        return random.nextInt(maxCapacity) + 1;
    }
    
    public static void Disembarking(int planeId, int count) {
        System.out.println("\tPassengers: " + count + " passengers disembarking from Plane-" + planeId);
    }
   
    public static void Boarding(int planeId, int count) {
        System.out.println("\tPassengers: " + count + " passengers boarding Plane-" + planeId);
    }
    
    //time
    public static int calculateOperationTime(int passengerCount) {
        return 500 + (passengerCount * 30);
    }
}