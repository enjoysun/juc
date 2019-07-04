class SpinLock {
    SpinLock() {
    }

    private static final AtomicReference<Thread> flag = new AtomicReference<Thread>();

    public boolean lock() {
        for (; ; ) {
            if (flag.compareAndSet(null, Thread.currentThread())) {
                System.out.println("获取锁");
                System.out.println("do something");
                return true;
            }
        }
    }

    public boolean relase() {
        if (flag.get() == null) {
            return false;
        }
        for (; ; ) {
            if (flag.compareAndSet(Thread.currentThread(), null)) {
                System.out.println("释放锁");
                return true;
            }
        }
    }
}
