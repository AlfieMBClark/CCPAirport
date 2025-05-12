/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assignment;


public class Gates {
    private final int gateNumber;
    private boolean occupied = false;
    private int occupiedBy = 0; // Plane ID
    
    
    public Gates(int gateNumber) {
        this.gateNumber = gateNumber;
    }
    
   
    public int getGateNumber() {
        return gateNumber;
    }
    
    //Check occupied
    public synchronized boolean isOccupied() {
        return occupied;
    }
    
 
    public synchronized void occupy(int planeId) {
        this.occupied = true;
        this.occupiedBy = planeId;
    }
    
    //Gate Free
    public synchronized void Depart() {
        this.occupied = false;
        this.occupiedBy = 0;
    }
    
    //Plane in Gate
    public synchronized int getOccupiedBy() {
        return occupiedBy;
    }
}
