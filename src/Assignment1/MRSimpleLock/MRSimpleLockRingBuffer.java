import java.util.Arrays;

public class MRSimpleLockRingBuffer<T> {

    private int head;
    private int tail;
    private int capacity; 
    private Object[] elements;
    private MRSimpleLock mrSimpleLock;

    public MRSimpleLockRingBuffer(int cap) {
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
        mrSimpleLock.lock(1); //Lock the tail
        if(isFull()){ //Full
            mrSimpleLock.unlock(1);
            return false;
        }
        else {
            elements[tail % capacity] = v;
            tail++;
            mrSimpleLock.unlock(1);
        }
        return true;
    }

    boolean dequeue() {
        mrSimpleLock.lock(10);
        if (isEmpty()) { // Empty
            mrSimpleLock.unlock(10);
            return false;
        } else { //Otherwise, remove from head
            elements[head % capacity] = null;
            head++;
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