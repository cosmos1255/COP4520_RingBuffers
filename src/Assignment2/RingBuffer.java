import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class RingBuffer {
    private AtomicInteger head;
    private AtomicInteger tail;
    private int capacity;
    public AtomicStampedReference[] elements;
    private ProgressAssurance progressAssurance;
    private static HashMap<Long, Integer> mapThreadIdToThreadId;
    private static final int TERVEL_DEF_BACKOFF_TIME_NS = 100;
    private static final int newStamp = 0;
    private static int bufferIsFull = 1;
    private static int bufferIsEmpty = 2;

    public RingBuffer(int cap, ProgressAssurance pa, HashMap<Long, Integer> mapThreadIds) {
        mapThreadIdToThreadId = mapThreadIds;
        capacity = cap;
        //Initialize the buffer
        elements = new AtomicStampedReference[cap];
        for (int i = 0; i < cap; i++) {
            elements[i] = new AtomicStampedReference<>(0, 0);
        }
        //Initialize the head and tail
        head = new AtomicInteger(0);
        tail = new AtomicInteger(0);
        progressAssurance = pa;
    }

    /**
     * Check if the buffer is full
     *
     * @return returns true if the buffer is full
     */
    boolean isFull() {
        return (tail.get() - head.get()) >= capacity;
    }

    /**
     * Checks if the buffer is empty
     *
     * @return returns true if the buffer is empty
     */
    boolean isEmpty() {
        return (tail.get() - head.get()) <= 0;

    }

    /**
     * First we check if another thread needs assistance in completing an operation in the Operation Record Table for
     * Progress Assurance and wait freedom. After we help one thread, or there are no threads to help, we try to complete
     * our enqueue operation. While our operation is not delayed, which is determined by the Limit class in the number
     * of attempts we will try.
     * <p>
     * We grab the current seqId for the thread with the nextTail() method, and then grab the position appropriate for
     * the thread's seqId. With this value, we try get the information from the element in the buffer, the element's
     * valSeqId, valIsValueType, and valIsDelayMarked.
     * <p>
     * With the information from these three values, we determine if the thread should complete its operation, or
     * backoff and try again later.
     *
     * @param v value we are trying to enqueue at the tail of the buffer
     * @return returns true if the operation is complete
     */
    public boolean enqueue(Integer v) {
//        progressAssurance.checkForAnnouncement(mapThreadIdToThreadId.get(Thread.currentThread().getId()));
        Limit progAssur = new Limit();

        while(progAssur.notDelayed(0)) {
            if (isFull())
                return false;
            int seqId = nextTail();
            int pos = getPos(seqId);
            AtomicStampedReference<Integer> val;
            while (progAssur.notDelayed(1)) {
                /* readValue */
                val = elements[pos]; //Equivalent to readValue, except possibility of having a Helper class being stored

                /* getInfo */
                if (val == null)
                    break;
                long valSeqId = val.getReference(); // We don't seem to need to have a read value function until we need to check if a helper class is loaded here
                boolean valIsValueType = valueIsValueType(valSeqId);
                boolean valIsDelayMarked = valueIsDelayMarked(valSeqId);
                /* end getInfo */

                //To maintain FIFO, if the current val's seqId is greater than the thread's, we should wait until later
                if (valSeqId > seqId) break;
                //If the value is delayed, we backoff, then check if the value has changed
                if (valIsDelayMarked) {
                    if (backoff(pos, val.getReference())) continue;
                    else break;
                }
                //If the current value already has a value loaded
                else if (!valIsValueType) {
                    //Let any delayed threads catch up
                    if (valSeqId < seqId && backoff(pos, val.getReference())) continue;
                    AtomicStampedReference<Integer> newValue = new AtomicStampedReference<>(seqId, 1);
                    //Actual enqueue operation
                    if (elements[pos] != null && elements[pos].compareAndSet(val.getReference(), newValue.getReference(), 0, 1)) {
                        return true;
                    }
                    if (elements[pos] != null && elements[pos].compareAndSet(val.getReference(), newValue.getReference(), 10, 1)) {
                        return true;
                    }
                } else if (!backoff(pos, val.getReference())) break;
            }
            //Do I need anything in here for breaking? Pseudocode not clear on what goes in which while loop
        }

        //Have reached past the limit of tries for enqueue operation, make an announcement for help
//        EnqueueOperation op = new EnqueueOperation(v, this);
//        progressAssurance.makeAnnouncement(op, mapThreadIdToThreadId.get(Thread.currentThread().getId())); //TODO change to new thread id method
//        return op.getResult();
        return false;
    }

    /**
     * First we check if another thread needs assistance in completing an operation in the Operation Record Table for
     * Progress Assurance and wait freedom. After we help one thread, or there are no threads to help, we try to complete
     * our dequeue operation. While our operation is not delayed, which is determined by the Limit class in the number
     * of attempts we will try.
     * <p>
     * We grab the current seqId for the thread with the nextHead() method, and then grab the position appropriate for
     * the thread's seqId. With this value, we try get the information from the element in the buffer, the element's
     * valSeqId, valIsValueType, and valIsDelayMarked.
     * <p>
     * With the information from these three values, we determine if the thread should complete its operation, or
     * backoff and try again later.
     */
    public boolean dequeue() {
//        progressAssurance.checkForAnnouncement(mapThreadIdToThreadId.get(Thread.currentThread().getId()));
        Limit progAssur = new Limit();
        while (progAssur.notDelayed(0)) {
            if (isEmpty()) return false;
            int seqId = nextHead();
            int pos = getPos(seqId);
            AtomicStampedReference<Integer> val;
            AtomicStampedReference<Integer> newValue = new AtomicStampedReference<>(nextSeqId(seqId), 0);
            while (progAssur.notDelayed(1)) {
                /* readValue */
                val = elements[pos]; //Equivalent to readValue, except possibility of having a Helper class being stored

                /* getInfo */
                if (val == null)
                    break;
                long valSeqId = val.getReference(); // We don't seem to need to have a read value function until we need to check if a helper class is loaded here
                boolean valIsValueType = valueIsValueType(valSeqId);
                boolean valIsDelayMarked = valueIsDelayMarked(valSeqId);
                /* end getInfo */

                //To maintain FIFO, if the current val's seqId is greater than the thread's, we should wait until later
                if (valSeqId > seqId) break;
                //Check if the buffer element has a value stored
                if (valIsValueType) {
                    if (valSeqId == seqId) {
                        //Best case, when val's seqId and threads seqId match up and the val is delay marked
                        if (valIsDelayMarked) {
                            newValue = delayMarkValue(newValue); //Why would we do this if the value is already delay marked?
                            boolean res = elements[pos].compareAndSet(val.getReference(), newValue.getReference(), 0, 0);
                            if (!res)
                                elements[pos].compareAndSet(val.getReference(), newValue.getReference(), 1, 0);
                            return true;
                        }
                    } else {
                        //Backoff to let delayed threads catch up
                        if (!backoff(pos, val.getReference())) {
                            if (valIsDelayMarked) break;
                            else atomicDelayMark(pos);
                        }
                    }
                } else {
                    //Change empty type val with a greater seqId than the current tail counter
                    if (valIsDelayMarked) {
                        int curHead = head.get();
                        int tempPos = getPos(curHead);
                        curHead += 2 * capacity;
                        curHead += pos - tempPos;
                        elements[pos].compareAndSet(val.getReference(), curHead, 0, 0);
                    } else if (elements[pos] == null && val == null && newValue == null && !backoff(pos, val.getReference()) && elements[pos].compareAndSet(val.getReference(), newValue.getReference(), 0, 0))
                        break;
                    else if (elements[pos] == null && val == null && newValue == null && !backoff(pos, val.getReference()) && elements[pos].compareAndSet(val.getReference(), newValue.getReference(), 1, 0))
                        break;
                }
            }
        }

        //Have reached past the limit of tries for dequeue operation, make an announcement for help

//        DequeueOperation op = new DequeueOperation(this);
//        progressAssurance.makeAnnouncement(op, mapThreadIdToThreadId.get(Thread.currentThread().getId())); //TODO change to new thread id method
//        return op.getResult();
        return false;
    }

     void atomicDelayMark(int pos) {
        AtomicStampedReference<Integer> value = elements[pos];
        value.set(value.getReference(), value.getStamp() | 10);
        elements[pos] = value;
    }

    public int nextSeqId(int seqId) {
        return seqId + capacity;
    }

//    public void easyDequeue(int indexOfOperationRecord) {
//        while (!(progressAssurance.getOperationRecord(indexOfOperationRecord) instanceof Helper) || !(progressAssurance.getOperationRecord(indexOfOperationRecord) instanceof OperationRecord)) {
//            if (!isEmpty()) {
//                int index = capacity % head.get();
//                elements[index] = null; //TODO replace with CAS
//                head.getAndIncrement();
//            } else {
//                progressAssurance.setOperationRecord(indexOfOperationRecord, bufferIsEmpty);
//            }
//        }
//    }
//
//    public void easyEnqueue(Integer v, int indexOfOperationRecord) {
//        while (!(progressAssurance.getOperationRecord(indexOfOperationRecord) instanceof Helper) || !(progressAssurance.getOperationRecord(indexOfOperationRecord) instanceof OperationRecord)) {
//            if (!isFull()) {
//                int index = capacity % tail.get();
//                elements[index] = new AtomicStampedReference<>(v, newStamp); //TODO replace with CAS
//                tail.getAndIncrement();
//            } else {
//                progressAssurance.setOperationRecord(indexOfOperationRecord, bufferIsFull);
//            }
//        }
//    }

    int nextHead() {
        return head.incrementAndGet();
    }

    int nextTail() {
        return tail.incrementAndGet();
    }

    int getPos(int v) {
        return v % capacity;
    }

    //performs a user-defined back-off procedure, then loads the value held at `pos' and returns whether it equals val
    boolean backoff(int pos, Integer val) {
        try {
            Thread.sleep(0,TERVEL_DEF_BACKOFF_TIME_NS);
        } catch (InterruptedException ignored) {
        }
        if (elements[pos] == null) {
            return false;
        } else {
            return elements[pos].getReference() != val;
        }
    }

    //val with a delay mark
    AtomicStampedReference<Integer> delayMarkValue(AtomicStampedReference<Integer> val) {
        int newStamp = val.getStamp();
        Integer newVal = val.getReference();
        val.set(newVal, newStamp + 10);
        return val;
    }

    /**
     * Return if the given stamp is delay marked(10 or 11) or not (01 or 0)
     *
     * @param val to check for delay marking
     * @return returns true if the value is delay marked
     */
    boolean valueIsDelayMarked(long val) {
        //If the value is 11 or 10, then it is delay marked
        return val == 11 || val == 10;
    }

    /**
     * Returns if the given stamp is of value type (01 or 11) or not (00 or 10)
     *
     * @param val to check for value type
     * @return returns true if the value is value type
     */
    boolean valueIsValueType(long val) {
        //If the value is 01 or 11, it is delay marked
        return val == 1 || val == 11;
    }

    /**
     * Returns if the given stamp is of helper type (100 or 110 or 111)
     *
     * @param val to check for helper type
     * @return returns true if the value is helper type
     */
    boolean isHelperType(long val) {
        return val == 100 || val == 110 || val == 111;
    }

    /**
     * Print all the elements in the buffer that have a stamp value that's not default (0)
     */
    void printElements() {
        for (AtomicStampedReference reference : elements) {
            if (reference.getStamp() != 0)
                System.out.print(reference.getReference() + ",");
        }
    }

    AtomicInteger getTail() {
        return this.tail;
    }

    AtomicInteger getHead() {
        return this.head;
    }

    AtomicStampedReference<Integer> getElementAtPos(int pos) {
        return elements[pos];
    }

    boolean readValue(int pos, AtomicStampedReference<Integer> val) {
        long stamp = elements[pos].getStamp();
        return !isHelperType(stamp);
    }

    void prepopulateRingBuffer(Integer value) {
        int currTail = tail.get();
        int seqId = currTail++;
        int pos = getPos(seqId);
        elements[pos] = new AtomicStampedReference(value, 1);
    }

}
