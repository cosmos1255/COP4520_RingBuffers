import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

public class MRLock {
    int bufferSize = 2;
    int maxThreads = 4;
    int bufferMask;
    Cell[] mBuffer;
    AtomicInteger mHead;
    AtomicInteger mTail;

    MRLock(int resources) {

        while (bufferSize <= maxThreads) {
            bufferSize = bufferSize << 1;
        }
        mBuffer = new Cell[bufferSize];
        bufferMask = bufferSize - 1;
        for (int i = 0; i < bufferSize; i++) {
            Cell cell = new Cell();
            cell.mSequence.set(i);

            // m_bits are initialized to all 1s, and will be set to all 1s when dequeued
            // This ensure that after a thread equeue a new request but before it set the
            // m_bits to
            // proper value, the following request will not pass through
            cell.initializeBitSet(resources);

            mBuffer[i] = cell;
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

//        System.out.println("Spinning on previous locks with thread " + Thread.currentThread());
        // Spin on all previous locks
        int spinPos = mHead.get();
        long whileStartTime = System.currentTimeMillis();
        int timesPrinted = 0;
        while (spinPos != position) {
            // We start from the head moving toward my pos, spin on cell that collide with
            // my request
            // When that cell is freed we move on to the next one util reaching myself
            // we need to check both m_sequence and m_bits, because either of them could be
            // set to
            // indicate a free cell, and we want to move on quickly
            int yeet = position - mBuffer[spinPos & bufferMask].getmSequence().get();
            int yote = (mBuffer[spinPos & bufferMask].getmBits() & resources);
//            System.out.println("Yeet is " + yeet + " bufferMask is " + bufferMask + " yote is " + yote);
            if (yeet > bufferMask ||  yote == 0) {
                spinPos = (spinPos + 1 ) % bufferSize;
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
        long whileStartTime = System.currentTimeMillis();
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

	static class Cell {
		AtomicInteger mSequence;
		int mBits = 0;

		Cell() {
		    mSequence = new AtomicInteger();
        }

        //Set all bits to 1
        public void initializeBitSet(int numResources) {
		    int start = 1;
            for(int i = 0; i < numResources; i++) {
                mBits += start;
                start *= 10;
            }
        }

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
}

    