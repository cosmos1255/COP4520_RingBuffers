import java.util.Arrays;

public class MRLockRingBuffer<T> {

    private int head;
    private int tail;
    private int capacity;
    private Object[] elements;
    private MRLock mrLock;

    public MRLockRingBuffer(int cap, int[] initialElements) {
        this.mrLock = new MRLock(2);
        capacity = cap;
        elements = new Object[cap];
        for(int i = 0; i < initialElements.length; i++) {
            elements[i] = initialElements[i];
            tail++;
        }
        head = 0;
//        tail = 0;
    }

    boolean isFull() {
        return (tail - head) == capacity;
    }

    boolean isEmpty() {
        return head == tail;
    }

    boolean enqueue(T v) {
        int pos = mrLock.lock(1); //Lock the tail
        if(isFull()){ //Full
            mrLock.unlock(pos);
            return false;
        }
        else {
            elements[tail % capacity] = v;
            tail++;
            mrLock.unlock(pos);
        }
        return true;
    }

    boolean dequeue() {
        int pos = mrLock.lock(10); //Lock the head
        if (isEmpty()) { // Empty
            mrLock.unlock(pos);
            return false;
        } else { //Otherwise, remove from head
            elements[head % capacity] = null;
            head++;
            mrLock.unlock(pos);
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
