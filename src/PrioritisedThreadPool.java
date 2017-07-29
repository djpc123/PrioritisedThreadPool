import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PrioritisedThreadPool {

    private final ThreadPoolExecutor highThreadPool;
    private final ThreadPoolExecutor mediumThreadPool;
    private final ThreadPoolExecutor lowThreadPool;

    public PrioritisedThreadPool() {
        PriorityLinkedBlockingQueue<Runnable> highQueue = new PriorityLinkedBlockingQueue<>();
        highThreadPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, highQueue, new ThreadFactory("RG", "High"));
        highThreadPool.prestartAllCoreThreads();

        PriorityLinkedBlockingQueue<Runnable> mediumQueue = new PriorityLinkedBlockingQueue<>(highQueue);
        mediumThreadPool = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, mediumQueue, new ThreadFactory("RG", "Medium"));
        mediumThreadPool.prestartAllCoreThreads();

        PriorityLinkedBlockingQueue<Runnable> lowQueue = new PriorityLinkedBlockingQueue<>(mediumQueue);
        lowThreadPool = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, lowQueue, new ThreadFactory("RG", "Low"));
        lowThreadPool.prestartAllCoreThreads();
    }

    public void execute(Runnable command, Priority priority) {
        switch (priority) {
            case HIGH:
                highThreadPool.execute(command);
                break;
            case MEDIUM:
                mediumThreadPool.execute(command);
                break;
            case LOW:
                lowThreadPool.execute(command);
                break;
        }
    }

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW
    }

    private class ThreadFactory implements java.util.concurrent.ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        ThreadFactory(String poolNamePrefix, String priority) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = poolNamePrefix + "-pool-" + priority + "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
