import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A queue that has an optional parent queue of higher priority items that should be returned prior to its own items.
 *
 * This is only intended to be used with a ThreadPoolExecutor so some more exotic operations on this queue may not always
 * behave as intended
 *
 * This is nestable such that a request to lookup the next item in the queue should recurse through all parents taking
 * from the highest parent that has items in it.
 *
 * Most methods only operate on <b>this</b> queue e.g. size will return the size of this queue and not the total including
 * parents
 *
 * Take <b>extreme</b> care to avoid loops in parent queues as this will lead to deadlocks
 * @param <I>
 */
public class PriorityLinkedBlockingQueue<I> extends LinkedBlockingQueue<I> {

    private PriorityLinkedBlockingQueue<I> parentQueue;
    private PriorityLinkedBlockingQueue<I> childQueue;

    private ReentrantLock takeLock = new ReentrantLock();
    private final Condition notEmpty = takeLock.newCondition();

    public PriorityLinkedBlockingQueue() {

    }

    public PriorityLinkedBlockingQueue(PriorityLinkedBlockingQueue<I> parentQueue) {
        this.parentQueue = parentQueue;
        parentQueue.setChildQueue(this);
    }

    public int sizeIncludingParents() {
        if (parentQueue != null) {
            return size() + parentQueue.sizeIncludingParents();
        } else {
            return size();
        }
    }

    @Override
    public I poll() {
        if (parentQueue == null) {
            return super.poll();
        }
        if (sizeIncludingParents() == 0) {
            return null;
        }
        I x = null;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            x = parentQueue.poll();
            if (x == null) {
                x = super.poll();
            }
        } finally {
            takeLock.unlock();
        }
        return x;
    }

    @Override
    public I peek() {
        if (parentQueue == null) {
            return super.peek();
        }
        if (sizeIncludingParents() == 0) {
            return null;
        }
        I x = null;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            x = parentQueue.peek();
            if (x == null) {
                x = super.peek();
            }
        } finally {
            takeLock.unlock();
        }
        return x;
    }

    @Override
    public I take() throws InterruptedException {
        I x;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            if (parentQueue != null) {
                while (sizeIncludingParents() == 0) {
                    this.notEmpty.await();
                }
                x = poll();
                if (sizeIncludingParents() != 0) {
                    this.signalNotEmpty();
                }
            } else {
                x = super.take();
            }
        } finally {
            takeLock.unlock();
        }
        return x;
    }

    @Override
    public int drainTo(Collection<? super I> c) {
        if (parentQueue != null) {
            parentQueue.drainTo(c);
        }
        super.drainTo(c);
        return c.size();
    }

    @Override
    public boolean offer(I i) {
        boolean accepted = super.offer(i);
        if (accepted) {
            signalNotEmpty();
        }
        return accepted;
    }

    @Override
    public void put(I i) throws InterruptedException {
        super.put(i);
        signalNotEmpty();
    }

    @Override
    public I poll(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(I i, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    public void setChildQueue(PriorityLinkedBlockingQueue<I> childQueue) {
        this.childQueue = childQueue;
    }

    public PriorityLinkedBlockingQueue<I> getChildQueue() {
        return childQueue;
    }

    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (childQueue != null) {
            childQueue.signalNotEmpty();
        }
    }
}
