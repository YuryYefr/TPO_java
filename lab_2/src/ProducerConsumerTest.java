import java.util.concurrent.*;

public class ProducerConsumerTest {
    public static void main(String[] args) {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(100);

        Producer producer = new Producer(queue);
        Consumer consumer = new Consumer(queue);

        Thread producerThread = new Thread(producer);
        Thread consumerThread = new Thread(consumer);

        producerThread.start();
        consumerThread.start();

        try {
            Thread.sleep(5000); // Let it run for a while
            producerThread.interrupt();
            consumerThread.interrupt();
        } catch (InterruptedException ignored) {}

        System.out.println("Producer-Consumer simulation completed.");
    }
}

class Producer implements Runnable {
    private final BlockingQueue<Integer> queue;
    public Producer(BlockingQueue<Integer> q) { queue = q; }
    public void run() {
        try {
            for (int i = 0; i < 5000; i++) {
                queue.put(i);
            }
        } catch (InterruptedException ignored) {}
    }
}
class Consumer implements Runnable {
    private final BlockingQueue<Integer> queue;
    public Consumer(BlockingQueue<Integer> q) { queue = q; }
    public void run() {
        try {
            while (true) {
                System.out.println("Consumed: " + queue.take());
            }
        } catch (InterruptedException ignored) {}
    }
}