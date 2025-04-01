//Two approaches for block lock inside method and full lock on method
class Counter {
    private int count = 0;
    private final Object lock = new Object();

    public synchronized void increment() { count++; }   //  method lock
    public synchronized void decrement() { count--; }

    public void syncIncrement() {   //  block lock
        synchronized (lock) { count++; }
    }
    public void syncDecrement() {
        synchronized (lock) { count--; }
    }

    public int getCount() { return count; }
}
