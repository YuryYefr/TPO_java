import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class WordLengthAnalysis {

    static class WordLengthTask extends RecursiveTask<int[]> {
        private final List<String> words;
        private final int start, end;

        WordLengthTask(List<String> words, int start, int end) {
            this.words = words;
            this.start = start;
            this.end = end;
        }

        @Override
        protected int[] compute() {
            if (end - start <= 1000) {
                int[] lengths = new int[end - start];
                for (int i = start; i < end; i++) {
                    lengths[i - start] = words.get(i).length();
                }
                return lengths;
            } else {
                int mid = (start + end) / 2;
                WordLengthTask left = new WordLengthTask(words, start, mid);
                WordLengthTask right = new WordLengthTask(words, mid, end);
                left.fork();
                int[] rightResult = right.compute();
                int[] leftResult = left.join();
                return IntStream.concat(Arrays.stream(leftResult), Arrays.stream(rightResult)).toArray();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String content = Files.readString(Path.of("lab_3/mock_text/small_text.txt"));
        if (content.isEmpty()) {
            System.out.println("File is empty");
            return;
        }
        List<String> words = Arrays.asList(content.split("\\W+"));
        ForkJoinPool pool = new ForkJoinPool();
        int[] lengths = pool.invoke(new WordLengthTask(words, 0, words.size()));

        double mean = Arrays.stream(lengths).average().orElse(0);
        double variance = Arrays.stream(lengths).mapToDouble(l -> Math.pow(l - mean, 2)).average().orElse(0);
        double stddev = Math.sqrt(variance);

        System.out.println("Mean: " + String.format("%.2f", mean));
        System.out.println("Variance: " + String.format("%.2f", variance));
        System.out.println("Std Dev: " + String.format("%.2f", stddev));
    }
}
