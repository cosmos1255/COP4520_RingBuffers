package Assignment1;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

public class MRLockRingBuffer<T> {

    private int head;
    private int tail;
    private int capacity;
    private Object[] elements;
    private BitSet[] queue;
    private int numResources;

    public static void main(String[] args) {
        MRSimpleLock mrSimpleLock = new MRSimpleLock();

        mrSimpleLock.lock(1);
        mrSimpleLock.lock(10);
        mrSimpleLock.unlock(10);
        mrSimpleLock.unlock(1);
    }

    static class MRSimpleLock {

        AtomicInteger mBits;

        MRSimpleLock() {
            mBits = new AtomicInteger();
        }

        public void lock(int resources) {
            for(;;) {
                int bits = mBits.get();
                System.out.println("Locking on " + resources);
                if((bits & resources) == 0) {
                    if(mBits.compareAndSet(bits, bits | resources)) {
                        System.out.println(mBits.get());
                        break;
                    }
                }
            }
        }

        void unlock(int resources) {
            for(;;) {
                int bits = mBits.get();
                if(mBits.compareAndSet(bits, bits & ~resources)) {
                    System.out.println(mBits.get());
                    break;
                }
            }
        }
    }

    //Thread

    public MRLockRingBuffer(int cap) {
        capacity = cap;
        elements = new Object[cap];
        head = 0;
        tail = 0;
    }

    boolean isFull() {
        return (tail - head) == capacity;
    }

    boolean isEmpty() {
        return head == tail;
    }

    boolean enqueue(T v) {
        if(isEmpty()) { //Empty
            elements[0] = v;
            tail++;
        } else if(isFull()){ //Full
            return false;
        }
        else {
            elements[tail++] = v;
        }
        return true;
    }

    boolean dequeue() {
        if(isEmpty()) { //Empty
            return false;
        } else { //Otherwise, remove from head
            elements[head] = null;
            head++;
            if(head==tail) {
                reset();
            }
        }
        return true;
    }

    Object getObjectAtIndex(int v) {
        if(elements[v] != null) {
            return elements[v];
        } else {
            return null;
        }
    }

    int nextHead() {
        return head+1;
    }

    int nextTail() {
        return tail+1;
    }

    int getSize() {
        return tail - head;
    }

    void printElements() {
        System.out.println(Arrays.toString(elements));
    }

    void reset() {
        head = 0;
        tail = 0;
    }
}