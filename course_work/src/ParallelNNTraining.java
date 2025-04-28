import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class ParallelNNTraining {

        static final int DATA_SIZE = 100_000_000;
        static final int TASK_COUNT = 1000;
        static final int THREAD_COUNT = 8;

        public static void main(String[] args) throws InterruptedException, ExecutionException {
            double[] array = new double[DATA_SIZE];
            Random random = new Random(42);
            for (int i = 0; i < DATA_SIZE; i++) {
                array[i] = random.nextDouble();
            }

            // Sequential computation
            long startTime = System.nanoTime();
            double sequentialResult = computeSequential(array);
            long endTime = System.nanoTime();
            double sequentialTime = (endTime - startTime) / 1e6;
            System.out.printf("Sequential result: %.4f, time: %.2f ms\n", sequentialResult, sequentialTime);

            // Parallel computation
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            List<Future<Double>> futures = new ArrayList<>();
            int chunkSize = DATA_SIZE / TASK_COUNT;

            startTime = System.nanoTime();
            for (int i = 0; i < TASK_COUNT; i++) {
                int start = i * chunkSize;
                int end = (i == TASK_COUNT - 1) ? DATA_SIZE : start + chunkSize;
                futures.add(executor.submit(new SumTask(array, start, end)));
            }

            double parallelResult = 0;
            for (Future<Double> future : futures) {
                parallelResult += future.get();
            }
            endTime = System.nanoTime();
            double parallelTime = (endTime - startTime) / 1e6;
            executor.shutdown();

            System.out.printf("Parallel result: %.4f, time: %.2f ms\n", parallelResult, parallelTime);

            double speedup = sequentialTime / parallelTime;
            System.out.printf("Speedup: %.2f\n", speedup);

            if (Math.abs(sequentialResult - parallelResult) > 1e-3) {
                System.out.println("WARNING: Results differ significantly. Check correctness.");
            }
        }

        static double computeSequential(double[] array) {
            double sum = 0;
            for (double x : array) {
                sum += Math.tanh(x) * Math.exp(-x); // more computational load
            }
            return sum;
        }

        static class SumTask implements Callable<Double> {
            private final double[] array;
            private final int start, end;

            SumTask(double[] array, int start, int end) {
                this.array = array;
                this.start = start;
                this.end = end;
            }

            @Override
            public Double call() {
                double sum = 0;
                for (int i = start; i < end; i++) {
                    sum += Math.tanh(array[i]) * Math.exp(-array[i]);
                }
                return sum;
            }
        }
    }
