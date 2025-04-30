import java.util.*;
import java.util.concurrent.*;

public class NNTraining {

    static int inputSize = 3;
    static int hiddenSize = 2;
    static int outputSize = 1;
    static int epochs;
    static int threadCount;
    static int dataSize;

    static double[][] inputs;
    static double[][] targets;

    static Random random = new Random(42);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введіть кількість прикладів для навчання: ");
        dataSize = scanner.nextInt();

        System.out.print("Введіть кількість епох навчання: ");
        epochs = scanner.nextInt();

        System.out.print("Введіть кількість потоків: ");
        threadCount = scanner.nextInt();

        generateData();

        NeuralNetwork sequentialNN = new NeuralNetwork();
        NeuralNetwork parallelNN = new NeuralNetwork();

        // Послідовне навчання
        long startTime = System.nanoTime();
        trainSequential(sequentialNN);
        long endTime = System.nanoTime();
        double sequentialTime = (endTime - startTime) / 1e6;
        System.out.printf("Час послідовного навчання: %.2f ms\n", sequentialTime);

        // Паралельне навчання
        startTime = System.nanoTime();
        trainParallel(parallelNN);
        endTime = System.nanoTime();
        double parallelTime = (endTime - startTime) / 1e6;
        System.out.printf("Час паралельного навчання: %.2f ms\n", parallelTime);

        // Перевірка
        double[] testInput = inputs[0];
        double seqOut = sequentialNN.predict(testInput)[0];
        double parOut = parallelNN.predict(testInput)[0];

        System.out.printf("Результат послідовної моделі: %.4f\n", seqOut);
        System.out.printf("Результат паралельної моделі: %.4f\n", parOut);

        double speedup = sequentialTime / parallelTime;
        System.out.printf("Прискорення: %.2f\n", speedup);

        if (Math.abs(seqOut - parOut) > 0.1) {
            System.out.println("WARNING: Значна різниця між результатами!");
        }
    }

    static void generateData() {
        inputs = new double[dataSize][inputSize];
        targets = new double[dataSize][outputSize];
        for (int i = 0; i < dataSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                inputs[i][j] = random.nextDouble();
            }
            targets[i][0] = random.nextDouble();
        }
    }

    static void trainSequential(NeuralNetwork nn) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            for (int i = 0; i < dataSize; i++) {
                nn.train(inputs[i], targets[i]);
            }
        }
    }

    static void trainParallel(NeuralNetwork nn) throws InterruptedException, ExecutionException {
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {

            for (int epoch = 0; epoch < epochs; epoch++) {
                List<Future<?>> futures = new ArrayList<>();
                int chunkSize = dataSize / threadCount;

                for (int t = 0; t < threadCount; t++) {
                    int start = t * chunkSize;
                    int end = (t == threadCount - 1) ? dataSize : start + chunkSize;

                    futures.add(executor.submit(() -> {
                        for (int i = start; i < end; i++) {
                            nn.train(inputs[i], targets[i]);
                        }
                    }));
                }

                for (Future<?> f : futures) {
                    f.get();
                }
            }

            executor.shutdown();
        }
    }

    static class NeuralNetwork {
        double[][] w1 = new double[hiddenSize][inputSize];
        double[] b1 = new double[hiddenSize];
        double[][] w2 = new double[outputSize][hiddenSize];
        double[] b2 = new double[outputSize];

        NeuralNetwork() {
            for (int i = 0; i < hiddenSize; i++) {
                for (int j = 0; j < inputSize; j++) {
                    w1[i][j] = random.nextGaussian() * 0.1;
                }
                b1[i] = 0;
            }
            for (int i = 0; i < outputSize; i++) {
                for (int j = 0; j < hiddenSize; j++) {
                    w2[i][j] = random.nextGaussian() * 0.1;
                }
                b2[i] = 0;
            }
        }

        double[] predict(double[] input) {
            double[] hidden = new double[hiddenSize];
            for (int i = 0; i < hiddenSize; i++) {
                hidden[i] = b1[i];
                for (int j = 0; j < inputSize; j++) {
                    hidden[i] += w1[i][j] * input[j];
                }
                hidden[i] = Math.tanh(hidden[i]);
            }

            double[] output = new double[outputSize];
            for (int i = 0; i < outputSize; i++) {
                output[i] = b2[i];
                for (int j = 0; j < hiddenSize; j++) {
                    output[i] += w2[i][j] * hidden[j];
                }
            }
            return output;
        }

        void train(double[] input, double[] target) {
            double[] hidden = new double[hiddenSize];
            double[] hiddenRaw = new double[hiddenSize];
            for (int i = 0; i < hiddenSize; i++) {
                hiddenRaw[i] = b1[i];
                for (int j = 0; j < inputSize; j++) {
                    hiddenRaw[i] += w1[i][j] * input[j];
                }
                hidden[i] = Math.tanh(hiddenRaw[i]);
            }

            double[] output = new double[outputSize];
            for (int i = 0; i < outputSize; i++) {
                output[i] = b2[i];
                for (int j = 0; j < hiddenSize; j++) {
                    output[i] += w2[i][j] * hidden[j];
                }
            }

            double[] error = new double[outputSize];
            for (int i = 0; i < outputSize; i++) {
                error[i] = output[i] - target[i];
            }

            // Градієнти для вихідного шару
            double[][] gradW2 = new double[outputSize][hiddenSize];
            double[] gradB2 = new double[outputSize];
            for (int i = 0; i < outputSize; i++) {
                gradB2[i] = error[i];
                for (int j = 0; j < hiddenSize; j++) {
                    gradW2[i][j] = error[i] * hidden[j];
                }
            }

            // Градієнти для прихованого шару
            double[] deltaHidden = new double[hiddenSize];
            for (int i = 0; i < hiddenSize; i++) {
                double sum = 0;
                for (int j = 0; j < outputSize; j++) {
                    sum += error[j] * w2[j][i];
                }
                deltaHidden[i] = sum * (1 - Math.pow(Math.tanh(hiddenRaw[i]), 2));
            }

            double[][] gradW1 = new double[hiddenSize][inputSize];
            double[] gradB1 = new double[hiddenSize];
            for (int i = 0; i < hiddenSize; i++) {
                gradB1[i] = deltaHidden[i];
                for (int j = 0; j < inputSize; j++) {
                    gradW1[i][j] = deltaHidden[i] * input[j];
                }
            }

            double lr = 0.01;
            for (int i = 0; i < outputSize; i++) {
                b2[i] -= lr * gradB2[i];
                for (int j = 0; j < hiddenSize; j++) {
                    w2[i][j] -= lr * gradW2[i][j];
                }
            }
            for (int i = 0; i < hiddenSize; i++) {
                b1[i] -= lr * gradB1[i];
                for (int j = 0; j < inputSize; j++) {
                    w1[i][j] -= lr * gradW1[i][j];
                }
            }
        }
    }
}
