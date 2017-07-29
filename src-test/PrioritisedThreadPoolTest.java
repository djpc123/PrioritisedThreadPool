import org.junit.Test;

import static org.junit.Assert.*;

public class PrioritisedThreadPoolTest {

    class EndlessRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void execute() throws Exception {
        PrioritisedThreadPool threadPool = new PrioritisedThreadPool();

        threadPool.execute(new EndlessRunnable(), PrioritisedThreadPool.Priority.HIGH);
        threadPool.execute(new EndlessRunnable(), PrioritisedThreadPool.Priority.LOW);

        return;
    }

}