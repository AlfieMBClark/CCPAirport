package assignment;

public class CleanRefill implements Runnable {
    private final int planeId;
    
    public CleanRefill(int planeId) {
        this.planeId = planeId;
    }
    
    @Override
    public void run() {
        performCleaningAndRefill(); 
    }
    
    private void performCleaningAndRefill(){
        System.out.println("\t\t" + Thread.currentThread().getName() + ": Starting cleaning and supplies refill for Plane-" + planeId + ".");
        
        try{
            Thread.sleep(4000);
        }catch (InterruptedException e) {}
       
        System.out.println("\t\t" + Thread.currentThread().getName() + ": Cleaning and supplies refill completed for Plane-" + planeId + ".");
    }
}