package test;

import java.io.IOException;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.time.*;

public class WordLengthAnalysisTest {

    public static String formatDecimal(double number) {
        // для читабельності
        DecimalFormat df = new DecimalFormat("#.00");
        return df.format(number);
    }

    public static void main(String[] args) throws IOException {
        // Підготовка текстових файлів різного розміру
        List<String> testFiles = List.of(
                "lab_3/mock_text/small_text.txt",   // ~10 000 слів
                "lab_3/mock_text/medium_text.txt",  // ~100 000 слів
                "lab_3/mock_text/large_text.txt"   // ~1 000 000 слів
        );

        for (String fileName : testFiles) {
            System.out.println("\nTesting file: " + fileName);
            List<String> words = loadWords(fileName);

            // Тест 1: Послідовний алгоритм (однопотоковий)
            Instant startSeq = Instant.now();
            double[] sequentialResult = analyzeSequential(words);
            Instant endSeq = Instant.now();
            System.out.println("Sequential execution time: " + Duration.between(startSeq, endSeq).toMillis() + " ms");

            // Тест 2: Паралельний алгоритм (ForkJoinPool)
            ForkJoinPool pool = new ForkJoinPool();
            Instant startPar = Instant.now();
            double[] parallelResult = pool.invoke(new WordLengthTask(words, 0, words.size()));
            Instant endPar = Instant.now();
            System.out.println("Parallel execution time: " + Duration.between(startPar, endPar).toMillis() + " ms");

            // Перевірка правильності обчислень
            System.out.println("Mean (Sequential): " + formatDecimal(sequentialResult[0]) + " | Mean (Parallel): "
                    + formatDecimal(parallelResult[0]));
            System.out.println("Variance (Sequential): " + formatDecimal(sequentialResult[1]) + " | Variance (Parallel): "
                    + formatDecimal(parallelResult[1]));
        }
    }

    // Метод для завантаження слів з файлу
    private static List<String> loadWords(String filePath) throws IOException {
        return Files.lines(Path.of(filePath))
                .flatMap(line -> Arrays.stream(line.split("\\W+")))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
    }

    // Однопотоковий аналіз довжини слів
    private static double[] analyzeSequential(List<String> words) {
        int[] lengths = words.stream().mapToInt(String::length).toArray();
        double mean = Arrays.stream(lengths).average().orElse(0);
        double variance = Arrays.stream(lengths).mapToDouble(l -> Math.pow(l - mean, 2)).average().orElse(0);
        return new double[]{mean, variance};
    }

    // ForkJoinTask для паралельного аналізу
    static class WordLengthTask extends RecursiveTask<double[]> {
        private final List<String> words;
        private final int start, end;

        WordLengthTask(List<String> words, int start, int end) {
            this.words = words;
            this.start = start;
            this.end = end;
        }

        @Override
        protected double[] compute() {
            if (end - start <= 5000) {
                int[] lengths = new int[end - start];
                for (int i = start; i < end; i++) {
                    lengths[i - start] = words.get(i).length();
                }
                double mean = Arrays.stream(lengths).average().orElse(0);
                double variance = Arrays.stream(lengths).mapToDouble(l -> Math.pow(l - mean, 2)).average().orElse(0);
                return new double[]{mean, variance};
            } else {
                int mid = (start + end) / 2;
                WordLengthTask left = new WordLengthTask(words, start, mid);
                WordLengthTask right = new WordLengthTask(words, mid, end);
                left.fork();
                double[] rightResult = right.compute();
                double[] leftResult = left.join();
                return new double[]{
                        (leftResult[0] + rightResult[0]) / 2, // Середнє значення
                        (leftResult[1] + rightResult[1]) / 2  // Дисперсія
                };
            }
        }
    }
}
