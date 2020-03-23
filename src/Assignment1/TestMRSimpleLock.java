import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestMRSimpleLock {

    static private MRLockRingBuffer<Integer> mrLockRingBuffer;
    static private Thread[] threads;
    static private int numThreads = 4;

    public static void main(String[] args) {
        mrLockRingBuffer = new MRLockRingBuffer<>(20);
        threads = new Thread[numThreads];

        int counter = 0;

        for(int i = 0; i < numThreads; i++)
        {
            threads[i] = new RingBufferThread(new ArrayList<>(Arrays.asList(++counter,++counter,++counter)));
        }

        for(int i = 0; i < numThreads; i++)
        {
            threads[i].start();
        }

        for(int i = 0; i < numThreads; i++)
        {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mrLockRingBuffer.printElements();
    }

    static class RingBufferThread extends Thread {
        List<Integer> enqueue;


        public RingBufferThread(ArrayList<Integer> toAdd) {
            enqueue = new ArrayList<>(toAdd);
        }

        //Initialize things to enqueue/dequeue
        @Override
        public void run() {
            for(Integer integer : enqueue) {
                mrLockRingBuffer.enqueue(integer);
            }
        }
    }
}
