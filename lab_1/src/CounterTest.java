class CounterTest {
    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        Thread t1 = new Thread(() -> { for (int i = 0; i < 100000; i++) counter.increment(); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 100000; i++) counter.decrement(); });
        Thread t3 = new Thread(() -> { for (int i = 0; i < 100000; i++) counter.syncIncrement(); });
        Thread t4 = new Thread(() -> { for (int i = 0; i < 100000; i++) counter.syncDecrement(); });
        t1.start(); // full method lock
        t2.start();
        t1.join();
        t2.join();
        t3.start(); //  block in method lock
        t4.start();
        t3.join();
        t4.join();
        System.out.println("Final count: " + counter.getCount());
    }
}
