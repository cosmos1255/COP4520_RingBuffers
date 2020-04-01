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
        elements = (AtomicStampedReference<Integer>[]) new Object[cap];
        for(int i = 0; i < cap; i++) {
            elements[i] = new AtomicStampedReference<>(0, 0);
        }
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
            AtomicStampedReference<Integer> val;
            do {
                /* readValue */
                val = elements[pos]; //Equivalent to readValue, except possibility of having a Helper class being stored

                /* getInfo */
                long valSeqId = val.getReference(); // We don't seem to need to have a read value function until we need to check if a helper class is loaded here
                boolean valIsValueType = valueIsValueType(valSeqId);
                boolean valIsDelayMarked = valueIsDelayMarked(valSeqId);
                /* end getInfo */

                if(valSeqId > seqId) break;
                if(valIsDelayMarked) {
                    if (backoff(pos, val.getReference())) continue;
                    else break;
                }
                else if(!valIsValueType) {
                    if(valSeqId < seqId && backoff(pos, val.getReference())) continue;
                    AtomicStampedReference<Integer> newValue = new AtomicStampedReference<>(seqId, 1);
                    if(elements[pos].compareAndSet(val.getReference(), newValue.getReference(), 0, 0)) //Don't need stamps?, so just make expected and new 0
                    {
                        return true;
                    }
                }
                else if(!backoff(pos, val.getReference())) break;
            } while(progAssur.notDelayed(1));

            //Do I need anything in here for breaking? Pseudocode not clear on what goes in which while loop
        } while(progAssur.notDelayed(0));

        EnqueueOperation op = new EnqueueOperation(v, this);
        progressAssurance.makeAnnouncement(op, (int) Thread.currentThread().getId()); //TODO change to new thread id method
        boolean result = op.getResult(); //How else can I figure out if the operation completed successfully? Method in progressAssurance?
        return result;
    }

    public boolean dequeue() {
        progressAssurance.checkForAnnouncement((int) Thread.currentThread().getId()); //TODO change to new thread ID method
        Limit progAssur = new Limit();
        do {
            if(isEmpty()) return false;
            int seqId = nextHead();
            int pos = getPos(seqId);
            AtomicStampedReference<Integer> val;
            AtomicStampedReference<Integer> newValue = new AtomicStampedReference<>(nextSeqId(seqId), 0);
            do {
                /* readValue */
                val = elements[pos]; //Equivalent to readValue, except possibility of having a Helper class being stored

                /* getInfo */
                long valSeqId = val.getReference(); // We don't seem to need to have a read value function until we need to check if a helper class is loaded here
                boolean valIsValueType = valueIsValueType(valSeqId);
                boolean valIsDelayMarked = valueIsDelayMarked(valSeqId);
                /* end getInfo */

                if(valSeqId > seqId) break;
                if(valIsValueType) {
                    if(valSeqId == seqId) {
                        if(valIsDelayMarked) {
                            newValue = delayMarkValue(newValue); //Why would we do this if the value is already delay marked?
                            elements[pos].compareAndSet(val.getReference(), newValue.getReference(), 0, 0);
                            return true;
                        }
                    }
                    else {
                        if(!backoff(pos, val.getReference())) {
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
                        elements[pos].compareAndSet(val.getReference(), curHead, 0, 0);
                    }
                    else if(!backoff(pos, val.getReference()) && elements[pos].compareAndSet(val.getReference(), newValue.getReference(), 0, 0)) break;
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

    public void easyDequeue(int indexOfOperationRecord) {
        //TODO whileloop
        while(!(progressAssurance.getOperationRecord(indexOfOperationRecord) instanceof  Helper)) {

        }
        if(!isEmpty()) {
            //TODO CAS
            elements[head.getAndIncrement() % capacity] = null;
        }
        else {
            //Set Enqueue operation to value indicating empty
        }
        //Do a CAS and check if value is instaceof Helper class, if so stop trying
    }

    public void easyEnqueue(Integer v, int indexOfOperationRecord) {
        while(!(progressAssurance.getOperationRecord(indexOfOperationRecord) instanceof  Helper)) {
            if(!isFull()) {
                //TODO CAS
                elements[tail.getAndIncrement() % capacity] = new AtomicStampedReference<>(v, newStamp);
            }
            else {
                //Set Enqueue operation to value indicating full
            }
        }

        //Do a CAS and check if value is instaceof Helper class, if so stop trying
    }

    int nextHead() {
        return head.incrementAndGet();
    }

    int nextTail() {
        return tail.incrementAndGet();
    }

    int getPos(int v) { return v % capacity; }

    //performs a user-defined back-off procedure, then loads the value held at `pos' and returns whether it equals val
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
