class SymbolThreads {
    // chain threads
    public static void main(String[] args) {
        Runnable printDash = () -> { for (int i = 0; i < 100; i++) System.out.print("-"); };
        Runnable printPipe = () -> { for (int i = 0; i < 100; i++) System.out.print("|"); };
        Thread t1 = new Thread(printDash);
        Thread t2 = new Thread(printPipe);
        t1.start();
        t2.start();
    }
}
