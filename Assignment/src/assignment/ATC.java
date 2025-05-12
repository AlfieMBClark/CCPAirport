/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assignment;

/**
 *
 * @author alfie
 */
public class ATC implements Runnable{
    private final Airport airport;
    private boolean running = true;
    
   
    public ATC(Airport airport) {
        this.airport = airport;
    }
    
    @Override
    public void run() {
        Assignment.Printmsg("ATC: Air Traffic Control system online.");
        
        // ATC monitoring
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        Assignment.Printmsg("ATC: Air Traffic Control system shutting down.");
    }
    
    //Shutdown
    public void shutdown() {
        running = false;
    }
}
