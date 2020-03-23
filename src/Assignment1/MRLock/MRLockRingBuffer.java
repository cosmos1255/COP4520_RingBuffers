import java.util.Arrays;

public class MRLockRingBuffer<T> {

    private int head;
    private int tail;
    private int capacity;
    private Object[] elements;
    private MRLock mrLock;

    public MRLockRingBuffer(int cap) {
        this.mrLock = new MRLock();
        capacity = cap;
        elements = new Object[cap];
        head = 0;
        tail = 0;
        System.out.println("Initializing Ring Buffer.");
    }

    boolean isFull() {
        return (tail - head) == capacity;
    }

    boolean isEmpty() {
        return head == tail;
    }

    boolean enqueue(T v) {
        System.out.println("Enqueuing!");
        mrLock.lock(1); //Lock the tail
        if(isFull()){ //Full
            mrLock.unlock(1);
            return false;
        }
        else {
            elements[tail % capacity] = v;
            tail++;
            mrLock.unlock(1);
        }
        return true;
    }

    boolean dequeue() {
        System.out.println("Dequeuing!");
        mrLock.lock(10);
        if (isEmpty()) { // Empty
            mrLock.unlock(10);
            return false;
        } else { //Otherwise, remove from head
            elements[head % capacity] = null;
            head++;
            mrLock.unlock(10);
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
