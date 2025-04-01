import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicIntegerArray;

interface Bank {
    void transfer(int from, int to, int amount) throws InterruptedException;

    int size();
}

class SynchronizedBank implements Bank {
    private final int[] accounts;
    private final int NTEST = 10000;
    private long nTransacts = 0;

    public SynchronizedBank(int n, int initialBalance) {
        accounts = new int[n];
        Arrays.fill(accounts, initialBalance);
    }

    public synchronized void transfer(int from, int to, int amount) throws InterruptedException {
        while (accounts[from] < amount) {
            wait();
        }
        accounts[from] -= amount;
        accounts[to] += amount;
        nTransacts++;
        notifyAll();
        if (nTransacts % NTEST == 0) test();
    }

    public void test() {
        System.out.println("Running synchronized bank");
        int sum = 0;
        for (int account : accounts) sum += account;
        System.out.println("Transactions: " + nTransacts + " Sum: " + sum);
    }

    public int size() {
        return accounts.length;
    }
}

class LockBank implements Bank {
    private final int[] accounts;
    private final ReentrantLock bankLock = new ReentrantLock();
    private final Condition sufficientFunds;
    private long nTransacts = 0;
    private final int NTEST = 10000;

    public LockBank(int n, int initialBalance) {
        accounts = new int[n];
        Arrays.fill(accounts, initialBalance);
        sufficientFunds = bankLock.newCondition();
    }

    public void transfer(int from, int to, int amount) throws InterruptedException {
        bankLock.lock();
        try {
            while (accounts[from] < amount) {
                sufficientFunds.await();
            }
            accounts[from] -= amount;
            accounts[to] += amount;
            nTransacts++;
            sufficientFunds.signalAll();
            if (nTransacts % NTEST == 0) test();
        } finally {
            bankLock.unlock();
        }
    }

    public void test() {
        int sum = 0;
        for (int account : accounts) sum += account;
        System.out.println("Transactions: " + nTransacts + " Sum: " + sum);
    }

    public int size() {
        return accounts.length;
    }
}


class AtomicBank implements Bank {
    private final AtomicIntegerArray accounts;
    private final AtomicLong nTransacts = new AtomicLong(0);
    private static final long NTEST = 10000;

    public AtomicBank(int n, int initialBalance) {
        accounts = new AtomicIntegerArray(n);
        for (int i = 0; i < n; i++) accounts.set(i, initialBalance);
    }

    public void transfer(int from, int to, int amount) {
        while (true) {
            int fromBalance = accounts.get(from);
            if (fromBalance < amount) {
                try {
                    Thread.sleep(1); // Retry later instead of skipping the transaction
                } catch (InterruptedException ignored) {
                }
                continue;
            }

            if (accounts.compareAndSet(from, fromBalance, fromBalance - amount)) {
                accounts.addAndGet(to, amount);
                long currentTransacts = nTransacts.incrementAndGet();

                if (currentTransacts % NTEST == 0) {
                    printSummary();
                }
                break;
            }
        }
    }

    public int size() {
        return accounts.length();
    }

    public void printSummary() {
        int total = 0;
        int[] snapshot = new int[accounts.length()];

        // Take a snapshot of balances for consistency
        for (int i = 0; i < accounts.length(); i++) {
            snapshot[i] = accounts.get(i);
        }

        // Calculate total balance from snapshot
        for (int balance : snapshot) {
            total += balance;
        }

        System.out.println("Transactions: " + nTransacts.get() + " Sum: " + total);
    }
}


class TransferThread extends Thread {
    private final Bank bank;
    private final int fromAccount;
    private final int maxAmount;
    private static final int REPS = 1000;

    public TransferThread(Bank b, int from, int max) {
        bank = b;
        fromAccount = from;
        maxAmount = max;
    }

    public void run() {
        try {
            for (int i = 0; i < REPS; i++) {
                int toAccount = (int) (bank.size() * Math.random());
                int amount = (int) (maxAmount * Math.random() / REPS);
                bank.transfer(fromAccount, toAccount, amount);
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
