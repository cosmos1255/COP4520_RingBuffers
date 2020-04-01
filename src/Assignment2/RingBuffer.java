import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

public class RingBuffer<T> {
    private AtomicInteger head;
    private AtomicInteger tail;
    private int capacity;
    private AtomicStampedReference[] elements;
    private static final long TERVEL_DEF_BACKOFF_TIME_MS = 1;
    private static final int newStamp = 0;
    private static ProgressAssurance progressAssurance;

    public RingBuffer(int cap, ProgressAssurance pa) {
        capacity = cap;
        elements = new AtomicStampedReference[cap];
        head = new AtomicInteger(0);
        tail = new AtomicInteger(0);
        progressAssurance = pa;
    }

    boolean isFull() {
        return (tail.get() - head.get()) == capacity;
    }

    boolean isEmpty() {
        return head.get() == tail.get();
    }

    public boolean enqueue(T v) {
        progressAssurance.checkForAnnouncement((int) Thread.currentThread().getId()); //TODO change to new thread ID method
        Limit progAssur = new Limit();

        do {
            if(isFull())
                return false;
            int seqId = nextTail();
            int pos = getPos(seqId);
            T val = null;//Does this have to be an AtomicReference?
            do {
                //TODO implement/figure out what readValue method is for...
//                if(!readValue(pos, val)) continue;
                //TODO get the stamp from somewhere and place in valSeqId
                //Equivalent to getInfo
                long valSeqId = 0L;
                boolean valIsValueType = valueIsValueType(valSeqId);
                boolean valIsDelayMarked = valueIsDelayMarked(valSeqId);

                if(valSeqId > seqId) break;
                if(valIsDelayMarked) {
                    if (backoff(pos, val)) continue;
                    else break;
                }
                else if(!valIsValueType) {
                    if(valSeqId < seqId && backoff(pos, val)) continue;
                    AtomicStampedReference<T> newValue = new AtomicStampedReference<>(v, seqId);
                    if(elements[pos].compareAndSet(val, newValue, 0, 0)) //Don't need stamps?, so just make expected and new 0
                        return true;
                }
                else if(!backoff(pos, val)) break;
            } while(progAssur.notDelayed(1));

            //Do I need anything in here for breaking? Pseudocode not clear on what goes in which while loop
        } while(progAssur.notDelayed(0));

        EnqueueOperation<T> op = new EnqueueOperation<>(v, this);
        progressAssurance.makeAnnouncement(op, (int) Thread.currentThread().getId()); //TODO change to new thread id method
        boolean result = op.getResult(); //How else can I figure out if the operation completed successfully? Method in progressAssurance?
        return result;
    }

    //TODO use CAS or similar
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

    //TODO use CAS or similar
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
    boolean backoff(int pos, T val) {

        return false;
    }

    //val with a delay mark
    AtomicStampedReference<T> delayMarkValue(AtomicStampedReference<T> val) {
        int newStamp = val.getStamp();
        T newVal = val.getReference();
        val.set(newVal, newStamp + 10);
        return val;
    }

    boolean valueIsDelayMarked(long val) {
        //If the value is 11 or 10, then it is delay marked
        return val == 11 || val == 10;
    }

    boolean valueIsValueType(long val) {
        //If the value is 01 or 11, it is delay marked
        return val == 1 || val == 11;
    }

    void printElements() {
        System.out.println(Arrays.toString(elements));
    }
}
