/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assignment;

import java.util.Random;

public class Passenger {
    private static final Random random = new Random();
    
    //Random passenger num
    public static int generatePassengerCount(int maxCapacity) {
        return random.nextInt(maxCapacity) + 1;
    }
    
    //Disembark
    public static void Disembarking(int planeId, int count) {
        System.out.println("Passengers: " + count + " passengers disembarking from Plane-" + planeId);
    }
    
    //Board
    public static void Boarding(int planeId, int count) {
        System.out.println("Passengers: " + count + " passengers boarding Plane-" + planeId);
    }
    
    //time for passenger
    public static int calculateOperationTime(int passengerCount) {
        // Base time + time per passenger
        return 500 + (passengerCount * 30);
    }
}
