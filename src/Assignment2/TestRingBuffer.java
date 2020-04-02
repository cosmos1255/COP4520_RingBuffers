import java.util.*;
public class TestRingBuffer {

    private static int capacity = 100000;
    private static int percentEnqueue = 30;
    private static int percentDequeue = 70;
    private static final int numValuesToPrepopulateThread = 10000;
    private static int prepopulateOperationsForThreads = 10000;
    private static RingBuffer ringBuffer;
    private static final long operationsToCompletePerThread = 100000;
    private static final int prepopulateRingBuffer = 1000;

    public static void main(String[] args) throws InterruptedException {
        Random random = new Random();
        for (int numThreads = 1; numThreads <= 32; numThreads *= 2) {
            long runTime;
            RingBufferThread[] threads = new RingBufferThread[numThreads];
            HashMap<Long, Integer> mapThreadIdToThreadId = new HashMap<>();
            int[][] prepopulateValues = new int[numThreads][numValuesToPrepopulateThread];
            Boolean[] preopulateOperations = new Boolean[prepopulateOperationsForThreads];

            for (int i = 0; i < numThreads; i++) {
                prepopulateValues[i] = new int[numValuesToPrepopulateThread];
                for (int j = 0; j < numValuesToPrepopulateThread; j++) {
                    prepopulateValues[i][j] = random.nextInt(100);
                }
            }

            int numEnqueues = percentEnqueue * prepopulateOperationsForThreads / 100;
            for (int i = 0; i < numEnqueues; i++) {
                preopulateOperations[i] = true;
            }
            for (int i = numEnqueues; i < prepopulateOperationsForThreads; i++) {
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

            for(int i = 0; i < prepopulateRingBuffer; i++) {
                ringBuffer.prepopulateRingBuffer(random.nextInt(1000));
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < numThreads; i++) {
                threads[i].start();
            }

            long endTime = System.currentTimeMillis();

            runTime = endTime - startTime;

            System.out.println("Number of threads:" + numThreads + " Operations complete: " + operationsToCompletePerThread + " Runtime(ms): " + runTime);
        }
    }

    private static class RingBufferThread extends Thread {
        int[] valuesToInsert;
        Boolean[] operations;

        long opsAttempted = 0;
        long opsComplete = 0;
        RingBufferThread(int[] prepopulateValue, Boolean[] ops) {
            valuesToInsert = prepopulateValue;
            operations = ops;
        }

        @Override
        public void run() {
            while (opsComplete <= operationsToCompletePerThread) {
                boolean currentOperation = operations[(int) (opsAttempted % prepopulateOperationsForThreads)];
                if (currentOperation) { //Enqueue
                    if (ringBuffer.enqueue(valuesToInsert[(int) (opsAttempted % numValuesToPrepopulateThread)])) {
                        opsComplete++;
                    }
                } else { //Dequeue
                    if (ringBuffer.dequeue()) {
                        opsComplete++;
                    }
                }
            }
        }
    }

}
