import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class MRLockRingBuffer<T> {

    private int head;
    private int tail;
    private int capacity; 
    private Object[] elements;
    private MRSimpleLock mrSimpleLock;

    public MRLockRingBuffer(int cap) {
        this.mrSimpleLock = new MRSimpleLock();
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
        if(isFull()){ //Full
            return false;
        }
        else {
            mrSimpleLock.lock(1); //Lock the tail
            elements[(tail++) % capacity] = v;
            mrSimpleLock.unlock(1);
        }
        return true;
    }

    boolean dequeue() {
        if (isEmpty()) { // Empty
            return false;
        } else { //Otherwise, remove from head
            mrSimpleLock.lock(10);
            elements[head++] = null;
            mrSimpleLock.unlock(10);
        }
        return true;
    }

    Object getObjectAtIndex(int v) {
        if (elements[v] != null) {
            return elements[v];
        } else {
            return null;
        }
    }

    int nextHead() {
        return head + 1;
    }

    int nextTail() {
        return tail + 1;
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