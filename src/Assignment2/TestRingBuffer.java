import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


public class TestRingBuffer {

    private static int numThreads = 32;
    private static int capacity = 100000;
    private static int executionTime = 5000;
    private static int percentEnqueue = 50;
    private static int percentDequeue = 50;
    private static final int numValuesToPrepopulateThread = 10000;
    private static int prepopulateOperationsForThreads = 10000;
    private static RingBuffer ringBuffer;
    private static AtomicLong operationsComplete = new AtomicLong(0);
    private static AtomicLong operationsAttempted = new AtomicLong(0);
    private static AtomicLong enqueuesComplete = new AtomicLong(0);
    private static AtomicLong dequeuesComplete = new AtomicLong(0);
    private static AtomicLong enqueuesAttempted = new AtomicLong(0);
    private static AtomicLong dequeuesAttempted = new AtomicLong(0);
    private static boolean keepGoing = true;

    public static void main(String[] args) throws InterruptedException {
        Random random = new Random();
        RingBufferThread[] threads = new RingBufferThread[numThreads];
        HashMap<Long, Integer> mapThreadIdToThreadId = new HashMap<>();
        int[][] prepopulateValues = new int[numThreads][numValuesToPrepopulateThread];
        Boolean[] preopulateOperations = new Boolean[prepopulateOperationsForThreads];

        for(int i = 0; i < numThreads; i++) {
            for(int j = 0; j < numValuesToPrepopulateThread; j++) {
                prepopulateValues[i] = new int[numValuesToPrepopulateThread];
                prepopulateValues[i][j] = random.nextInt(100);
            }
        }

        int numEnqueues = percentEnqueue * prepopulateOperationsForThreads / 100;
        for(int i = 0; i < numEnqueues; i++) {
            preopulateOperations[i] = true;
        }
        for(int i = numEnqueues; i < prepopulateOperationsForThreads; i++) {
            preopulateOperations[i] = false;
        }

        List<Boolean> tempList = Arrays.asList(preopulateOperations);

        Collections.shuffle(tempList);
        tempList.toArray(preopulateOperations);

        for (int i = 0; i < numThreads; i++) {
            RingBufferThread rbt = new RingBufferThread(prepopulateValues[i], preopulateOperations);
            threads[i] = rbt;
            mapThreadIdToThreadId.put(rbt.getId(), i);
        }

        ringBuffer = new RingBuffer(capacity, new ProgressAssurance(numThreads), mapThreadIdToThreadId);

        for (int i = 0; i < numThreads; i++) {
            threads[i].start();
        }


        try {
            Thread.sleep(executionTime);
        }
        catch (InterruptedException ignored) {
        }
        keepGoing = false;

        System.out.println("Operations attempted: " + operationsAttempted + " Operations complete " + operationsComplete.get());
    }

    private static class RingBufferThread extends Thread {
        int[] valuesToInsert;
        Boolean[] operations;

        long opsAttempted = 0;
        RingBufferThread(int[] prepopulateValue, Boolean[] ops) {
            valuesToInsert = prepopulateValue;
            operations = ops;
        }

        @Override
        public void run() {
            while(keepGoing) {
                operationsAttempted.getAndIncrement();
                opsAttempted++;
                boolean currentOperation = operations[(int) (opsAttempted % prepopulateOperationsForThreads)];
                if(currentOperation) { //Enqueue
//                    enqueuesAttempted.getAndIncrement();
                    if(ringBuffer.enqueue(valuesToInsert[(int) (opsAttempted % numValuesToPrepopulateThread)])) {
//                        enqueuesComplete.getAndIncrement();
                        operationsComplete.getAndIncrement();
                    }
                }
                else { //Dequeue
//                    dequeuesAttempted.getAndIncrement();
                    if(ringBuffer.dequeue()) {
//                        dequeuesComplete.getAndIncrement();
                        operationsComplete.getAndIncrement();
                    }
                }
            }
        }
    }

}
