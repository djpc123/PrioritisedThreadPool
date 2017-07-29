import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

public class PriorityLinkedBlockingQueueTest {

    @Test
    public void testThatElementsAreDrainedInOrderQueued() {
        PriorityLinkedBlockingQueue<String> queue = new PriorityLinkedBlockingQueue<>();
        queue.offer("1");
        queue.offer("2");
        queue.offer("3");
        queue.offer("4");

        ArrayList<String> sink = new ArrayList<>();
        queue.drainTo(sink);

        List<String> expected = Arrays.asList("1", "2", "3", "4");
        assertThat(sink, is(expected));
    }


    @Test
    public void testParentQueueItemsAreDrainedFirst() {
        PriorityLinkedBlockingQueue<String> highQueue = new PriorityLinkedBlockingQueue<>();
        PriorityLinkedBlockingQueue<String> lowQueue = new PriorityLinkedBlockingQueue<>(highQueue);

        lowQueue.offer("1-low");
        lowQueue.offer("2-low");

        highQueue.offer("1-high");

        ArrayList<String> sink = new ArrayList<>();
        lowQueue.drainTo(sink);

        List<String> expected = Arrays.asList("1-high", "1-low", "2-low");
        assertThat(sink, is(expected));
    }

    @Test
    public void testParentQueueItemsAreTakenFirst() throws InterruptedException {
        PriorityLinkedBlockingQueue<String> highQueue = new PriorityLinkedBlockingQueue<>();
        PriorityLinkedBlockingQueue<String> lowQueue = new PriorityLinkedBlockingQueue<>(highQueue);

        lowQueue.offer("1-low");
        lowQueue.offer("2-low");

        highQueue.offer("1-high");

        String taken = lowQueue.take();

        assertThat(taken, is("1-high"));
    }

    @Test
    public void testParentQueueDoesNotTakeFromChild() throws InterruptedException {
        PriorityLinkedBlockingQueue<String> highQueue = new PriorityLinkedBlockingQueue<>();
        PriorityLinkedBlockingQueue<String> lowQueue = new PriorityLinkedBlockingQueue<>(highQueue);

        lowQueue.offer("1-low");
        lowQueue.offer("2-low");

        highQueue.offer("1-high");

        ArrayList<String> sink = new ArrayList<>();
        highQueue.drainTo(sink);

        List<String> expected = Collections.singletonList("1-high");
        assertThat(sink, is(expected));
    }

    @Test
    public void testAddingItemToHighQueueSignalsLowQueueTake() throws Exception {
        PriorityLinkedBlockingQueue<String> highQueue = new PriorityLinkedBlockingQueue<>();
        PriorityLinkedBlockingQueue<String> lowQueue = new PriorityLinkedBlockingQueue<>(highQueue);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String taken = lowQueue.take();
                    assertThat(taken, is("1-high"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        Thread.sleep(50);

        highQueue.offer("1-high");

        Thread.sleep(50);
        if (thread.isAlive()) {
            assertThat("Thread never signaled", null, notNullValue());
        }
    }

    @Test
    public void testAddingItemToLowQueueSignalsLowQueueTake() throws Exception {
        PriorityLinkedBlockingQueue<String> highQueue = new PriorityLinkedBlockingQueue<>();
        PriorityLinkedBlockingQueue<String> lowQueue = new PriorityLinkedBlockingQueue<>(highQueue);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String taken = lowQueue.take();
                    assertThat(taken, is("1-low"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        Thread.sleep(50);

        lowQueue.offer("1-low");

        Thread.sleep(50);
        if (thread.isAlive()) {
            assertThat("Thread never signaled", null, notNullValue());
        }
    }

    @Test
    public void testHighQueueOnlySignaledOnHighQueue() throws Exception {
        PriorityLinkedBlockingQueue<String> highQueue = new PriorityLinkedBlockingQueue<>();
        PriorityLinkedBlockingQueue<String> lowQueue = new PriorityLinkedBlockingQueue<>(highQueue);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String taken = highQueue.take();
                    assertThat(taken, is("1-high"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        Thread.sleep(50);

        lowQueue.offer("1-low");

        Thread.sleep(50);

        highQueue.offer("1-high");

        Thread.sleep(50);
        if (thread.isAlive()) {
            assertThat("Thread never signaled", null, notNullValue());
        }
    }

}