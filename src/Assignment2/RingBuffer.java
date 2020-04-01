import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

public class RingBuffer {
    private AtomicInteger head;
    private AtomicInteger tail;
    private int capacity;
    private AtomicStampedReference<Integer>[] elements;
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

    public boolean enqueue(Integer v) {
        progressAssurance.checkForAnnouncement((int) Thread.currentThread().getId()); //TODO change to new thread ID method
        Limit progAssur = new Limit();

        do {
            if(isFull())
                return false;
            int seqId = nextTail();
            int pos = getPos(seqId);
            Integer val = 0;//Does this have to be an AtomicReference?
            do {
                //TODO implement/figure out what readValue method is for...
//                if(!readValue(pos, val)) continue;
                //TODO get the stamp from somewhere and place in valSeqId?
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
                    AtomicStampedReference<Integer> newValue = new AtomicStampedReference<>(v, seqId);
                    if(elements[pos].compareAndSet(val, newValue.getReference(), 0, 0)) //Don't need stamps?, so just make expected and new 0
                        return true;
                }
                else if(!backoff(pos, val)) break;
            } while(progAssur.notDelayed(1));

            //Do I need anything in here for breaking? Pseudocode not clear on what goes in which while loop
        } while(progAssur.notDelayed(0));

        EnqueueOperation op = new EnqueueOperation(v, this);
        progressAssurance.makeAnnouncement(op, (int) Thread.currentThread().getId()); //TODO change to new thread id method
        boolean result = op.getResult(); //How else can I figure out if the operation completed successfully? Method in progressAssurance?
        return result;
    }

    //TODO use CAS or similar
    public void easyEnqueue(Integer v) {
        if(!isFull()) {
            elements[tail.getAndIncrement() % capacity] = new AtomicStampedReference<>(v, newStamp);
        }
    }

    public boolean dequeue() {
        progressAssurance.checkForAnnouncement((int) Thread.currentThread().getId()); //TODO change to new thread ID method
        Limit progAssur = new Limit();
        do {
            if(isEmpty()) return false;
            int seqId = nextHead();
            int pos = getPos(seqId);
            Integer val = 0;//Does this have to be an AtomicReference?
            AtomicStampedReference<Integer> newValue = new AtomicStampedReference<>(nextSeqId(seqId), 0);
            do {
                //TODO implement/figure out what readValue method is for...
//                if(!readValue(pos, val)) continue;
                //TODO get the stamp from somewhere and place in valSeqId
                //Equivalent to getInfo
                long valSeqId = 0L;
                boolean valIsValueType = valueIsValueType(valSeqId);
                boolean valIsDelayMarked = valueIsDelayMarked(valSeqId);

                if(valSeqId > seqId) break;
                if(valIsValueType) {
                    if(valSeqId == seqId) {
                        if(valIsDelayMarked) {
                            newValue = delayMarkValue(newValue); //Why would we do this if the value is already delay marked?
                            elements[pos].compareAndSet(val, newValue.getReference(), 0, 0);
                            return true;
                        }
                    }
                    else {
                        if(!backoff(pos, val)) {
                            if(valIsDelayMarked) break;
                            else atomicDelayMark(pos);
                        }
                    }
                }
                else {
                    if(valIsDelayMarked) {
                        int curHead = head.get();
                        int tempPos = getPos(curHead);
                        curHead += 2 * capacity;
                        curHead += pos - tempPos;
                        elements[pos].compareAndSet(val, curHead, 0, 0);
                    }
                    else if(!backoff(pos, val) && elements[pos].compareAndSet(val, newValue.getReference(), 0, 0)) break;
                }
            } while(progAssur.isDelayed(1));

            //What goes here?..
        } while(progAssur.isDelayed(0));

        DequeueOperation op = new DequeueOperation(this);
        progressAssurance.makeAnnouncement(op, (int) Thread.currentThread().getId()); //TODO change to new thread id method
        boolean result = op.getResult(); //How else can I figure out if the operation completed successfully? Method in progressAssurance?
        return result;


    }

    private void atomicDelayMark(int pos) {
        AtomicStampedReference<Integer> value = elements[pos];
        value.set(value.getReference(), value.getStamp() + 10);
        elements[pos] = value;
    }


    //Do I need this?
//    private int getValueType() { }

    public int nextSeqId(int seqId)
    {
        return seqId+ 1;
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
    boolean backoff(int pos, Integer val) {

        return false;
    }

    //val with a delay mark
    AtomicStampedReference<Integer> delayMarkValue(AtomicStampedReference<Integer> val) {
        int newStamp = val.getStamp();
        Integer newVal = val.getReference();
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
