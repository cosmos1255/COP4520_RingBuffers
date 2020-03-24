import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMRLock {

    static private MRLockRingBuffer<Integer> mrLockRingBuffer;
    static private int numThreads = 4;
    static private AtomicInteger enqueueComplete = new AtomicInteger(0);
    static private AtomicInteger dequeueComplete = new AtomicInteger(0);
    static private AtomicInteger enqueueAttempted = new AtomicInteger(0);
    static private AtomicInteger dequeueAttempted = new AtomicInteger(0);

    static private int ringBufferCapacity = 500000;
    static private int runTimeSeconds = 5;

    static private int percentEnqueue = 50;
    static private int percentDequeue = 50;
    static private final int enqueueOperation = 0;
    static private final int dequeueOperation = 1;
    static private AtomicBoolean keepRunning = new AtomicBoolean(true);

    public static void main(String[] args) throws InterruptedException {
        Random random = new Random();
        if(args.length == 2) {
            percentEnqueue = Integer.parseInt(args[0]);
            percentDequeue = Integer.parseInt(args[1]);
            if(percentDequeue + percentEnqueue != 100) {
                System.out.println("Percentages must add to 100. Changing back to default.");
                percentEnqueue = 50;
                percentDequeue = 50;
            }
        }
        if(args.length == 0){
            System.out.println("Example of running program with arguments: java TestMRLock <intPercentEnqueue> <intPercentDequeue> <runtimeSeconds.\njava TestMrLock 50 50 5");
        }
        if(args.length == 3) {
            runTimeSeconds = Integer.parseInt(args[2]);
        } else {
            System.out.println("You can optionally dictate the runtime in milliseconds as an argument.");
        }
        int[] seed = new int[5000];
        for(int i = 0; i < 5000; i++) {
            seed[i] = random.nextInt(500);
        }

        mrLockRingBuffer = new MRLockRingBuffer<>(ringBufferCapacity, seed);
        Thread[] threads = new Thread[numThreads];
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

        for(int i = 0; i < runTimeSeconds; i++) {
            Thread.sleep(1000);
        }
        keepRunning.set(false);
        System.out.println("Finished running!");

        for(int i = 0; i < numThreads; i++)
        {
            threads[i].join();
        }
        int opsComplete = enqueueComplete.get() + dequeueComplete.get();
        System.out.println("Total operations complete:" + opsComplete);
        System.out.println("Attempted number of enqueue:" + enqueueAttempted.get());
        System.out.println("Attempted number of dequeue:" + dequeueAttempted.get());
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
            while(keepRunning.get()) {
                switch(operations.get(currOp % operationsSize)) {
                    case enqueueOperation:
                        if(mrLockRingBuffer.enqueue(enqueue.get(currOp % enqueueSize))) {
                            currOp++;
                            enqueueComplete.getAndIncrement();
                        }
                        enqueueAttempted.getAndIncrement();
                        break;
                    case dequeueOperation:
                        if(mrLockRingBuffer.dequeue()) {
                            currOp++;
                            dequeueComplete.getAndIncrement();
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
