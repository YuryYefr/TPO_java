import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        int NACCOUNTS = 10;
        int INITIAL_BALANCE = 10000;
        Scanner bankType = new Scanner(System.in);

        System.out.println("""
                Choose type of bank transaction
                1.lock
                2.atomic
                Enter for synchronized
                """);

        Bank bank = switch (bankType.nextLine()) {
            case "1" -> new LockBank(NACCOUNTS, INITIAL_BALANCE);
            case "2" -> new AtomicBank(NACCOUNTS, INITIAL_BALANCE);
            default -> new SynchronizedBank(NACCOUNTS, INITIAL_BALANCE);
        };

        for (int i = 0; i < NACCOUNTS; i++) {
            TransferThread t = new TransferThread(bank, i, INITIAL_BALANCE);
            t.setPriority(Thread.NORM_PRIORITY + i % 2);
            t.start();
        }
    }
}
