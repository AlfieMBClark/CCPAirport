/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assignment;

/**
 *
 * @author alfie
 */
public class Passenger {
    private final int id;
    private final int planeId;
    private final boolean boarding; // true = boarding, false = disembarking
    
  
    public Passenger(int id, int planeId, boolean boarding) {
        this.id = id;
        this.planeId = planeId;
        this.boarding = boarding;
    }
    
    @Override
    public void run() {
        try {
            if (boarding) {
                Assignment.log("Passenger-" + id + ": I'm boarding Plane-" + planeId + " now.");
                Thread.sleep(200);
            } else {
                Assignment.log("Passenger-" + id + ": I'm disembarking from Plane-" + planeId + " now.");
                Thread.sleep(200); 
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
