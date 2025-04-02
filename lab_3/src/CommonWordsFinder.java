import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class CommonWordsFinder {

    public static void main(String[] args) throws IOException {
        // Список текстових файлів
        List<String> filePaths = List.of("lab_3/mock_text/ci_text_0.txt", "lab_3/mock_text/ci_text_1.txt");

        // Завантаження текстів
        List<Set<String>> wordSets = new ArrayList<>();
        for (String filePath : filePaths) {
            wordSets.add(loadWords(filePath));
        }

        // Паралельний пошук спільних слів
        ForkJoinPool pool = new ForkJoinPool();
        CommonWordsTask task = new CommonWordsTask(wordSets, 0, wordSets.size());
        Set<String> commonWords = pool.invoke(task);

        // Виведення результату
        System.out.println("Спільні слова у всіх текстах: " + commonWords);
    }

    // Метод для завантаження слів з файлу
    private static Set<String> loadWords(String filePath) throws IOException {
        return Files.lines(Path.of(filePath))
                .flatMap(line -> Arrays.stream(line.toLowerCase().split("\\W+")))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toSet());
    }

    // ForkJoinTask для паралельного знаходження спільних слів
    static class CommonWordsTask extends RecursiveTask<Set<String>> {
        private final List<Set<String>> wordSets;
        private final int start, end;

        CommonWordsTask(List<Set<String>> wordSets, int start, int end) {
            this.wordSets = wordSets;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Set<String> compute() {
            if (end - start == 1) {
                return wordSets.get(start);
            } else if (end - start == 2) {
                return intersection(wordSets.get(start), wordSets.get(start + 1));
            } else {
                int mid = (start + end) / 2;
                CommonWordsTask leftTask = new CommonWordsTask(wordSets, start, mid);
                CommonWordsTask rightTask = new CommonWordsTask(wordSets, mid, end);
                leftTask.fork();
                Set<String> rightResult = rightTask.compute();
                Set<String> leftResult = leftTask.join();
                return intersection(leftResult, rightResult);
            }
        }

        // Метод для знаходження перетину двох множин
        private Set<String> intersection(Set<String> set1, Set<String> set2) {
            return set1.stream().filter(set2::contains).collect(Collectors.toSet());
        }
    }
}
