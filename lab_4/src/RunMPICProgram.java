import java.io.*;
import java.util.Scanner;

public class RunMPICProgram {

    public static void cParser(String filePath, String mpName) {
        try {
            System.out.println("Running " + mpName);
            // Compile the C program using gcc
            ProcessBuilder compileProcess = new ProcessBuilder("mpicc", "-o", "lab_4/MPI_lab", filePath);
            compileProcess.redirectErrorStream(true);
            Process compile = compileProcess.start();
            compile.waitFor(); // Wait for compilation to complete

            // Print compiler output
            printProcessOutput(compile);
            System.out.println("Compilation finished.");

            // Run the compiled C executable
            ProcessBuilder runProcess = new ProcessBuilder("mpirun", "./lab_4/MPI_lab");
            runProcess.redirectErrorStream(true);
            Process run = runProcess.start();

            // Read and print the output of the C program
            BufferedReader reader = new BufferedReader(new InputStreamReader(run.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            run.waitFor(); // Wait for execution to complete
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        Scanner userInput = new Scanner(System.in);
        System.out.println("""
                1.For blocking
                2.For nonblocking
                """);
        switch (userInput.nextLine()) {
            case "1" -> cParser("lab_4/src/MPI_lab_blocking.c", "Blocking message pass");
            case "2" -> cParser("lab_4/src/MPI_lab_blocking.c", "Nonblocking message pass");
            default -> System.out.println("Try to choose something");
        }
    }

    private static void printProcessOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}
