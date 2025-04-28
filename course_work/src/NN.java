import java.util.*;
import java.util.concurrent.*;

public class NN {

    static final int DATA_SIZE = 100_000_00; // Size of the data
    static final int TASK_COUNT = 1000;       // Number of tasks for parallel computation
    static final int THREAD_COUNT = 4;        // Number of threads
    static final int INPUT_SIZE = 10;         // Size of the neural network input layer
    static final int HIDDEN_SIZE = 5;         // Size of the hidden layer
    static final int OUTPUT_SIZE = 1;         // Size of the output layer

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Initialize random data for the neural network
        double[][] inputs = new double[DATA_SIZE][INPUT_SIZE];
        double[][] outputs = new double[DATA_SIZE][OUTPUT_SIZE];
        Random random = new Random(42);
        for (int i = 0; i < DATA_SIZE; i++) {
            for (int j = 0; j < INPUT_SIZE; j++) {
                inputs[i][j] = random.nextDouble();
            }
            outputs[i][0] = random.nextDouble();
        }

        // Initialize and train the neural network
        NeuralNetwork nn = new NeuralNetwork();
        double learningRate = 0.01;
        int epochs = 1000;
        for (int epoch = 0; epoch < epochs; epoch++) {
            nn.backpropagate(inputs, outputs, learningRate);
        }

        // Sequential computation using trained neural network
        long startTime = System.nanoTime();
        double sequentialResult = computeSequential(inputs, nn);
        long endTime = System.nanoTime();
        double sequentialTime = (endTime - startTime) / 1e6;
        System.out.printf("Sequential result: %.4f, time: %.2f ms\n", sequentialResult, sequentialTime);

        // Parallel computation using ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Double>> futures = new ArrayList<>();
        int chunkSize = DATA_SIZE / TASK_COUNT;

        startTime = System.nanoTime();
        for (int i = 0; i < TASK_COUNT; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, DATA_SIZE);
            futures.add(executor.submit(new NNTask(inputs, start, end, nn)));
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

    static class NeuralNetwork {
        double[] weightsInputHidden = new double[INPUT_SIZE * HIDDEN_SIZE];
        double[] weightsHiddenOutput = new double[HIDDEN_SIZE * OUTPUT_SIZE];
        Random random = new Random();

        public NeuralNetwork() {
            // Initialize weights randomly
            for (int i = 0; i < weightsInputHidden.length; i++) {
                weightsInputHidden[i] = random.nextDouble() - 0.5;  // Random values between -0.5 and 0.5
            }
            for (int i = 0; i < weightsHiddenOutput.length; i++) {
                weightsHiddenOutput[i] = random.nextDouble() - 0.5;
            }
        }

        // Forward pass
        public double forward(double[] input) {
            double[] hidden = new double[HIDDEN_SIZE];
            double[] output = new double[OUTPUT_SIZE];

            // Input to hidden layer
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                hidden[i] = 0;
                for (int j = 0; j < INPUT_SIZE; j++) {
                    hidden[i] += input[j] * weightsInputHidden[j * HIDDEN_SIZE + i];
                }
                hidden[i] = Math.tanh(hidden[i]);
            }

            // Hidden to output layer
            for (int i = 0; i < OUTPUT_SIZE; i++) {
                output[i] = 0;
                for (int j = 0; j < HIDDEN_SIZE; j++) {
                    output[i] += hidden[j] * weightsHiddenOutput[j * OUTPUT_SIZE + i];
                }
            }
            return output[0];  // Return the single output value
        }

        // Backpropagation and weight updates
        public void backpropagate(double[][] inputs, double[][] outputs, double learningRate) {
            for (int i = 0; i < inputs.length; i++) {
                // Forward pass
                double predicted = forward(inputs[i]);

                // Calculate error
                double outputError = predicted - outputs[i][0];
                double[] hiddenError = new double[HIDDEN_SIZE];
                for (int j = 0; j < HIDDEN_SIZE; j++) {
                    hiddenError[j] = outputError * weightsHiddenOutput[j];
                }

                // Update weights (simplified for demonstration)
                for (int j = 0; j < INPUT_SIZE; j++) {
                    for (int k = 0; k < HIDDEN_SIZE; k++) {
                        weightsInputHidden[j * HIDDEN_SIZE + k] -= learningRate * hiddenError[k] * inputs[i][j];
                    }
                }
                for (int j = 0; j < HIDDEN_SIZE; j++) {
                    weightsHiddenOutput[j] -= learningRate * outputError * Math.tanh(inputs[i][j]);
                }
            }
        }
    }

    static double computeSequential(double[][] inputs, NeuralNetwork nn) {
        double result = 0;
        for (double[] input : inputs) {
            result += nn.forward(input);  // Apply the trained model on each input
        }
        return result;
    }

    static class NNTask implements Callable<Double> {
        private final double[][] inputs;
        private final int start, end;
        private final NeuralNetwork nn;

        NNTask(double[][] inputs, int start, int end, NeuralNetwork nn) {
            this.inputs = inputs;
            this.start = start;
            this.end = end;
            this.nn = nn;
        }

        @Override
        public Double call() {
            double result = 0;
            for (int i = start; i < end; i++) {
                result += nn.forward(inputs[i]);  // Apply the trained model on each input in the chunk
            }
            return result;
        }
    }
}
