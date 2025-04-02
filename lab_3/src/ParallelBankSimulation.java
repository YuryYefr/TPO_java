import java.util.concurrent.*;

public class ParallelBankSimulation {

    static class TransferTask extends RecursiveTask<Void> {
        private final Bank bank;
        private final int fromAccount;
        private final int maxAmount;
        private static final int REPS = 10000;

        public TransferTask(Bank bank, int fromAccount, int maxAmount) {
            this.bank = bank;
            this.fromAccount = fromAccount;
            this.maxAmount = maxAmount;
        }

        @Override
        protected Void compute() {
            try {
                for (int i = 0; i < REPS; i++) {
                    int toAccount = (int) (bank.size() * Math.random());
                    int amount = (int) (maxAmount * Math.random() / REPS);
                    bank.transfer(fromAccount, toAccount, amount);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int NACCOUNTS = 10;
        int INITIAL_BALANCE = 10000;

        // Вимірюємо час для synchronized
        Bank syncBank = new SynchronizedBank(NACCOUNTS, INITIAL_BALANCE);
        long startSync = System.nanoTime();
        Thread[] threads = new Thread[NACCOUNTS];
        for (int i = 0; i < NACCOUNTS; i++) {
            int from = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 10000; j++) {
                        int to = (int) (NACCOUNTS * Math.random());
                        int amount = (int) (INITIAL_BALANCE * Math.random() / 10000);
                        syncBank.transfer(from, to, amount);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long endSync = System.nanoTime();
        double timeSync = (endSync - startSync) / 1e9;

        // Вимірюємо час для ForkJoinPool
        Bank parallelBank = new SynchronizedBank(NACCOUNTS, INITIAL_BALANCE);
        ForkJoinPool pool = new ForkJoinPool();
        long startParallel = System.nanoTime();
        for (int i = 0; i < NACCOUNTS; i++) {
            TransferTask task = new TransferTask(parallelBank, i, INITIAL_BALANCE);
            pool.submit(task);
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        long endParallel = System.nanoTime();
        double timeParallel = (endParallel - startParallel) / 1e9;

        // Виведення результатів
        System.out.printf("Час виконання synchronized: %.3f сек\n", timeSync);
        System.out.printf("Час виконання ForkJoinPool: %.3f сек\n", timeParallel);
        System.out.printf("Прискорення: %.2f рази\n", timeSync / timeParallel);
    }
}
