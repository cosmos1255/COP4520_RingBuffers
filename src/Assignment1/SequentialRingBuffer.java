package Assignment1;

import java.util.Arrays;

public class SequentialRingBuffer<T> {

    private int head;
    private int tail;
    private int capacity;
    private Object[] elements;

    public SequentialRingBuffer(int cap) {
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
        return head++;
    }

    int nextTail() {
        return tail++;
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