import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;



public class TestRingBuffer {

    private static int numThreads = 4;
    private static int capacity = 100;
    private static int executionTime = 5000;
    private static Thread[] threads;



    public static void main(String[] args) throws InterruptedException {
        Random random = new Random();
        threads = new Thread[numThreads];
        HashMap<Long, Integer> mapThreadIdToThreadId = new HashMap<>();

        for (int i = 0; i < numThreads; i++) {
            RingBufferThread rbt = new RingBufferThread();
            threads[i] = rbt;
            mapThreadIdToThreadId.put(rbt.getId(), i);
        }

        RingBuffer ringBuffer = new RingBuffer(100, new ProgressAssurance(numThreads), mapThreadIdToThreadId);

        for (int i = 0; i < numThreads; i++) {
            threads[i].start();
        }

        try {
            Thread.sleep(executionTime);
        }
        catch (InterruptedException ignored) {
        }

        for(int i = 0; i < numThreads; i++) {
            threads[i].join();
        }

    }

    private static class RingBufferThread extends Thread {
        @Override
        public void run() {
        }
    }

    private static void initializeOperations() {

    }



}
