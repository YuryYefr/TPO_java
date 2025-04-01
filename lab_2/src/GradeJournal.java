import java.util.concurrent.*;
import java.util.*;

class GradeBook {
    private final ConcurrentHashMap<String, List<Integer>> grades = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void addGrade(String group, int grade) {
        grades.computeIfAbsent(group, k -> Collections.synchronizedList(new ArrayList<>())).add(grade);
    }

    public void printGrades() {
        grades.forEach((group, gradesList) -> {
            System.out.println(group + " : " + gradesList);
        });
    }
}

public class GradeJournal {
    public static void main(String[] args) throws InterruptedException {
        GradeBook journal = new GradeBook();
        String[] groups = {"Group A", "Group B", "Group C"};
        ExecutorService executor = Executors.newFixedThreadPool(4); // Лектор + 3 асистенти

        Runnable grader = () -> {
            for (int i = 0; i < 100; i++) {
                String group = groups[new Random().nextInt(groups.length)];
                journal.addGrade(group, new Random().nextInt(101));
            }
        };

        for (int i = 0; i < 4; i++) {
            executor.submit(grader);
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        journal.printGrades();
    }
}
