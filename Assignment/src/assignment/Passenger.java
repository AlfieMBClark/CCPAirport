package assignment;

import java.util.Random;

public class Passenger implements Runnable {
    private static final Random random = new Random();
    private final int planeId;
    private final int passengerCount;
    private final boolean DisemBoard; // true = boarding, false = disembarking
    
    public Passenger(int planeId, int passengerCount, boolean isBoarding) {
        this.planeId = planeId;
        this.passengerCount = passengerCount;
        this.DisemBoard = isBoarding;
    }
    
    @Override
    public void run() {
        try {
            if (DisemBoard) {
                performBoarding();
            } else {
                performDisembarking();
            }
        } catch (InterruptedException e) {}
    }
    
    private void performBoarding() throws InterruptedException {
        System.out.println("\t\t" + Thread.currentThread().getName() +":\t" + passengerCount + " passengers boarding Plane-" + planeId);
        // Boarding time
        int boardingTime = calculateOperationTime(passengerCount);
        Thread.sleep(boardingTime);
        
        System.out.println("\t\t" + Thread.currentThread().getName() + ": All " + passengerCount + " passengers have boarded Plane-"+ planeId);
    }
    
    private void performDisembarking() throws InterruptedException {
        System.out.println("\t\t" + Thread.currentThread().getName() +":\t" + passengerCount + " passengers disembarking from Plane-" + planeId);
        
        // Disembarking time
        int disembarkTime = calculateOperationTime(passengerCount);
        Thread.sleep(disembarkTime);
        
        System.out.println("\t\t" + Thread.currentThread().getName() + ": All " + passengerCount + " passengers have disembarked from Plane-"+planeId);
    }
    
    //Random passenger num
    public static int generatePassengerCount(int maxCapacity) {
        return random.nextInt(maxCapacity) + 1;
    }
    
    
    //time
    public static int calculateOperationTime(int passengerCount) {
        return 1000 + (passengerCount * 30);
    }
}