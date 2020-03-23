package Assignment1;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class MRLockRingBuffer<T> {

    private int head;
    private int tail;
    private int capacity;
    private Object[] elements;
    private MRSimpleLock mrSimpleLock;

    static class MRLock {
        int bufferSize = 2;
        int maxThreads = 4;
        int bufferMask;
        Cell[] mBuffer;
        AtomicInteger mHead;
        AtomicInteger mTail;

        MRLock() {

            while (bufferSize <= maxThreads) {
                bufferSize = bufferSize << 1;
            }
            mBuffer = new Cell[bufferSize];
            bufferMask = bufferSize - 1;
            for (int i = 0; i < bufferSize; i++) {
                mBuffer[i].mSequence.set(i);

                // m_bits are initialized to all 1s, and will be set to all 1s when dequeued
                // This ensure that after a thread equeue a new request but before it set the
                // m_bits to
                // proper value, the following request will not pass through
                mBuffer[i].setmBits(~0);
            }
            mHead = new AtomicInteger(0);
            mTail = new AtomicInteger(0);
        }

        public int lock(final int resources) {
            Cell cell;
            int position;

            for (;;) {
                position = mTail.get();
                cell = mBuffer[position & bufferMask];
                int sequence = cell.getmSequence().get();
                int difference = sequence - position;

                if (difference == 0) {
                    if (mTail.compareAndSet(position, position + 1)) {
                        break;
                    }
                }
            }
            cell.setmBits(resources);
            cell.setmSequence(position + 1);

            // Spin on all previous locks
            int spinPos = mHead.get();
            while (spinPos != position) {
                // We start from the head moving toward my pos, spin on cell that collide with
                // my request
                // When that cell is freed we move on to the next one util reaching myself
                // we need to check both m_sequence and m_bits, because either of them could be
                // set to
                // indicate a free cell, and we want to move on quickly
                if (position - mBuffer[spinPos & bufferMask].getmSequence().get() > bufferMask
                        || (mBuffer[spinPos & bufferMask].getmBits() & resources) == 0) {
                    spinPos++;
                }
            }

            // Good to go
            return position;
        }

        public void unlock(final int handle) {
            // Release my lock by setting the bits to 0
            mBuffer[handle & bufferMask].setmBits(0);

            // Dequeue cells that have been released
            int position = mHead.get();
            while (mBuffer[position & bufferMask].getmBits() != 0) {
                Cell cell = mBuffer[position & bufferMask];
                int seq = cell.getmSequence().get();
                int difference = seq - (position + 1);

                if (difference == 0) {
                    if (mHead.compareAndSet(position, position + 1)) {
                        cell.setmBits(~0);
                        cell.setmSequence(position + bufferMask + 1);
                    }
                }

                position = mHead.get();
            }

        }
    }

    static class Cell {
        AtomicInteger mSequence;
        int mBits;

        public AtomicInteger getmSequence() {
            return mSequence;
        }

        public void setmSequence(int mSeq) {
            mSequence.set(mSeq);
        }

        public int getmBits() {
            return mBits;
        }

        public void setmBits(int mBits) {
            this.mBits = mBits;
        }
    }

    static class MRSimpleLock {

        AtomicInteger mBits;

        MRSimpleLock() {
            this.mBits = new AtomicInteger();
        }

        public void lock(int resources) {
            for (;;) {
                int bits = mBits.get();
                System.out.println("Locking on " + resources);
                if ((bits & resources) == 0) {
                    if (mBits.compareAndSet(bits, bits | resources)) {
                        System.out.println(mBits.get());
                        break;
                    }
                }
            }
        }

        void unlock(int resources) {
            for (;;) {
                int bits = mBits.get();
                if (mBits.compareAndSet(bits, bits & ~resources)) {
                    System.out.println(mBits.get());
                    break;
                }
            }
        }
    }

    // Thread

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