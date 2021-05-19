
public class Data {

    private int packet;

    // True if receiver should wait
    // False if sender should wait
    private boolean transfer = true;

    public synchronized void send(int packet) {
        while (!transfer) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        transfer = false;
        this.packet = packet;
        notifyAll();
    }

    public synchronized int receive() {
        while (transfer) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        transfer = true;
        notifyAll();
        return packet;
    }
}
