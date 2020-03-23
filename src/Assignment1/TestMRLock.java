import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMRLock {

    static private MRLockRingBuffer<Integer> mrLockRingBuffer;
    static private Thread[] threads;
    static private int numThreads = 4;
    static private int numRandom = 1000;
    static private int operationsComplete = 0;
    static private AtomicInteger enqueueComplete = new AtomicInteger(0);
    static private AtomicInteger deqeueueComplete = new AtomicInteger(0);
    static private AtomicInteger enqueueAttemped = new AtomicInteger(0);
    static private AtomicInteger dequeueAttempted = new AtomicInteger(0);

    static private int ringBufferCapacity = 5000000;
    static private int runTimeInMilliseconds = 5000;

    static private int percentEnqueue = 50;
    static private int percentDequeue = 50;
    static private final int enqueueOperation = 0;
    static private final int dequeueOperation = 1;
    static private final int initRingBuffer = 1000;
    static private boolean keepRunning = true;

    public static void main(String[] args) throws InterruptedException {
        Random random = new Random();
        //Pass in percent of enqueue and dequeue optionally
        if(args.length == 2) {
            percentEnqueue = Integer.parseInt(args[0]);
            percentDequeue = Integer.parseInt(args[1]);
            if(percentDequeue + percentEnqueue != 100) {
                System.out.println("Percentages must add to 100. Changing back to default.");
                percentEnqueue = 50;
                percentDequeue = 50;
            }
        } else if(args.length == 0){
            System.out.println("You can optionally dictate the percentage of enqueue and dequeues. The default is 50% enqeue and 50% dequeue.");
        }
        if(args.length == 3) {
            runTimeInMilliseconds = Integer.parseInt(args[2]);
        } else {
            System.out.println("You can optionally dictate the runtime in milliseconds as an argument.");
        }

        mrLockRingBuffer = new MRLockRingBuffer<>(ringBufferCapacity);
        threads = new Thread[numThreads];
        for(int i = 0; i < initRingBuffer; i++) {
            mrLockRingBuffer.enqueue(random.nextInt(100));
        }
        ArrayList<Integer> randomItems = new ArrayList<Integer>();
        ArrayList<Integer> operations = new ArrayList<Integer>();

        int enqueueOperationsPerThread = 10000 / 4 * percentEnqueue / 100;
        for(int i = 0; i < enqueueOperationsPerThread ; i++) {
            randomItems.add(random.nextInt(100));
            operations.add(enqueueOperation);
        }

        int dequeueOperationsPerThread = 10000 / 4 * percentDequeue / 100;
        for(int i = 0; i < dequeueOperationsPerThread; i++) {
            operations.add(dequeueOperation);
        }

        for(int i = 0; i < numThreads; i++)
        {
            Collections.shuffle(randomItems);
            Collections.shuffle(operations);
            threads[i] = new RingBufferThread(randomItems, operations);
        }

        for(int i = 0; i < numThreads; i++)
        {
            threads[i].start();
        }

        Thread.sleep(runTimeInMilliseconds);
        keepRunning = false;

        for(int i = 0; i < numThreads; i++)
        {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        operationsComplete = enqueueComplete.get() + deqeueueComplete.get();
        System.out.println("Total operations complete:" + operationsComplete);
        System.out.println("Attempted number of enqueues:" + enqueueAttemped.get());
        System.out.println("Attempted number of dequeues:" + dequeueAttempted.get());
    }

    static class RingBufferThread extends Thread {
        List<Integer> enqueue;
        List<Integer> operations;


        public RingBufferThread(ArrayList<Integer> toAdd, ArrayList<Integer> operations) {
            this.enqueue = new ArrayList<>(toAdd);
            this.operations = new ArrayList<>(operations);
        }

        //Initialize things to enqueue/dequeue
        @Override
        public void run() {
            int enqueueSize = enqueue.size();
            int currOp = 0;
            int operationsSize = operations.size();
            while(keepRunning) {
                switch(operations.get(currOp % operationsSize)) {
                    case enqueueOperation:
                        if(mrLockRingBuffer.enqueue(enqueue.get(currOp % enqueueSize))) {
                            currOp++;
                            enqueueComplete.getAndIncrement();
                        }
                        enqueueAttemped.getAndIncrement();
                        break;
                    case dequeueOperation:
                        if(mrLockRingBuffer.dequeue()) {
                            currOp++;
                            deqeueueComplete.getAndIncrement();
                        }
                        dequeueAttempted.getAndIncrement();
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
