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
        if (DisemBoard) {
            performBoarding();
        } else {
            performDisembarking();
            }
    }
    
    private void performBoarding(){
        System.out.println("\t\t" + Thread.currentThread().getName() +":\t" + passengerCount + " passengers boarding Plane-" + planeId);
        // Boardinggg
        int boardingTime = DisBorTime(passengerCount);
        try{
            Thread.sleep(boardingTime);
        }catch (InterruptedException e){}
        
        System.out.println("\t\t" + Thread.currentThread().getName() + ": All " + passengerCount + " passengers have boarded Plane-"+ planeId);
    }
    
    private void performDisembarking(){
        System.out.println("\t\t" + Thread.currentThread().getName() +":\t" + passengerCount + " passengers disembarking from Plane-" + planeId);
        
        // Disembarkinggg
        int disembarkTime = DisBorTime(passengerCount);
       try{
           Thread.sleep(disembarkTime);
       }catch (InterruptedException e){}
      
        System.out.println("\t\t" + Thread.currentThread().getName() + ": All " + passengerCount + " passengers have disembarked from Plane-"+planeId);
    }
    
    //Random passenger num
    public static int generatePassengerCount(int maxCapacity) {
        return random.nextInt(maxCapacity) + 1;
    }
    
    public static int DisBorTime(int passengerCount) {
        return 1000 + (passengerCount * 30);
    }
}