import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

public class RingBuffer<T> {
    private AtomicInteger head;
    private AtomicInteger tail;
    private int capacity;
    private AtomicStampedReference<Object>[] elements;
    private static final long TERVEL_DEF_BACKOFF_TIME_MS = 1;
    private static final int newStamp = 0;

    public RingBuffer(int cap) {
        capacity = cap;
        elements = new AtomicStampedReference[cap];
        head = new AtomicInteger(0);
        tail = new AtomicInteger(0);
    }

    boolean isFull() {
        return (tail.get() - head.get()) == capacity;
    }

    boolean isEmpty() {
        return head.get() == tail.get();
    }

    public boolean enqueue(T v) {
        //Check for announcement
        //Limit progrAssurance

        if(isFull()){ //Full
            return false;
        }
        else {
            elements[tail.get() % capacity] = (AtomicStampedReference<Object>) v;
            tail.getAndIncrement();
        }
        return true;
    }

    public void easyEnqueue(T v) {
        if(!isFull()) {
            elements[tail.getAndIncrement() % capacity] = new AtomicStampedReference<>(v, newStamp);
        }
    }

    public boolean dequeue() {
        if (isEmpty()) { // Empty
            return false;
        } else { //Otherwise, remove from head
            elements[head.get() % capacity] = null;
            head.getAndIncrement();
        }
        return true;
    }

    public void easyDequeue() {
        if(!isEmpty()) {
            elements[head.getAndIncrement() % capacity] = null;
        }
    }

    int nextHead() {
        return head.incrementAndGet();
    }

    int nextTail() {
        return tail.incrementAndGet();
    }

    int getPos(int v) { return v % capacity; }

    //performs a user-defned back-off procedure, then loads the value held at `pos' and returns whether or not it equals val
    void backoff(int pos, T val) {

    }

    //val with a delay mark
    AtomicStampedReference<T> delayMarkValue(AtomicStampedReference<T> val) {
        int newStamp = val.getStamp();
        T newVal = val.getReference();
        val.set(newVal, newStamp + 10);
        return val;
    }

    //based on `val', this function assigns values to the other
    //passed variables.
    void getInfo(T val, T valSeqid, boolean
            valIsValueType, boolean valIsDelayedMarked) {

    }

    void printElements() {
        System.out.println(Arrays.toString(elements));
    }
}
