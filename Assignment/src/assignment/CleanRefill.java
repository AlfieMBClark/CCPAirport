package assignment;

public class CleanRefill implements Runnable {
    private final int planeId;
    
    public CleanRefill(int planeId) {
        this.planeId = planeId;
    }
    
    @Override
    public void run() {
        try {
            performCleaningAndRefill();
        } catch (InterruptedException e) {}
    }
    
    private void performCleaningAndRefill() throws InterruptedException {
        System.out.println("\t\t" + Thread.currentThread().getName() + ": Starting cleaning and supplies refill for Plane-" + planeId + ".");
        Thread.sleep(4000); // Cleaning time
        System.out.println("\t\t" + Thread.currentThread().getName() + ": Cleaning and supplies refill completed for Plane-" + planeId + ".");
    }
}