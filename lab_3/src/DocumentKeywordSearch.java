import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocumentKeywordSearch {

    static class KeywordSearchTask extends RecursiveTask<List<String>> {
        private final Map<String, List<String>> documents;
        private final Set<String> keywords;
        private final List<String> filenames;
        private final int start, end;

        KeywordSearchTask(Map<String, List<String>> documents, List<String> filenames, int start, int end, Set<String> keywords) {
            this.documents = documents;
            this.filenames = filenames;
            this.start = start;
            this.end = end;
            this.keywords = keywords;
        }

        @Override
        protected List<String> compute() {
            List<String> result = new ArrayList<>();
            for (int i = start; i < end; i++) {
                String filename = filenames.get(i);
                List<String> content = documents.get(filename);

                for (String line : content) {
                    for (String keyword : keywords) {
                        if (line.contains(keyword)) {
                            result.add("File: " + filename + " contains keyword: " + keyword);
                            break;
                        }
                    }
                }
            }
            return result;
        }
    }

    public static Map<String, List<String>> readMultipleFiles() throws IOException {
        Path directoryPath = Path.of("lab_3/mock_text/kdf_search");
        Map<String, List<String>> fileContents = new HashMap<>();

        try (Stream<Path> paths = Files.list(directoryPath)) {
            fileContents = paths
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toMap(
                            file -> file.getFileName().toString(), // Store filename
                            file -> {
                                try {
                                    return Files.readAllLines(file); // Store content as list of lines
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return List.of();
                                }
                            }
                    ));
        }
        return fileContents;
    }

    public static void main(String[] args) throws IOException {
        Map<String, List<String>> documents = readMultipleFiles();
        List<String> filenames = new ArrayList<>(documents.keySet()); // Extract filenames for indexing

        Set<String> keywords = new HashSet<>(Arrays.asList("technology", "java", "AI"));

        ForkJoinPool pool = new ForkJoinPool();
        KeywordSearchTask task = new KeywordSearchTask(documents, filenames, 0, filenames.size(), keywords);
        List<String> matchingDocs = pool.invoke(task);

        System.out.println("Matching Documents:");
        matchingDocs.forEach(System.out::println);
    }
}
