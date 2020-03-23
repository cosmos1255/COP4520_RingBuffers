import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMRSimpleLock {

    static private MRLockRingBuffer<Integer> mrLockRingBuffer;
    static private Thread[] threads;
    static private int numThreads = 4;
	static private int numRandom = 1000;
	static private AtomicInteger operationsComplete = new AtomicInteger(0);
	static private int totalOperationsToComplete = 10000;

	static private int percentEnqueue = 50;
	static private int percentDequeue = 50;
	static private final int enqueueOperation = 0;
	static private final int dequeueOperation = 1;

    public static void main(String[] args) {
		Random random = new Random();
		//Pass in percent of enqueue and dequeue optionally
		if(args.length == 3) {
			percentEnqueue = Integer.getInteger(args[0]);
			percentDequeue = Integer.getInteger(args[1]);
		} else {
			System.out.println("You can optionally dictate the percentage of enqueue and dequeues. The default is 50% enqeue and 50% dequeue.");
		}

        mrLockRingBuffer = new MRLockRingBuffer<>(20);
        threads = new Thread[numThreads];
		ArrayList<Integer> randomItems = new ArrayList<Integer>();
		ArrayList<Integer> operations = new ArrayList<Integer>();

		private int enqueueOperationsPerThread = totalOperationsToComplete / 4 * percentEnqueue / 100;
		for(int i = 0; i < enqueueOperationsPerThread ; i++) {
			randomItems.add(random.nextInt(100));
			operations.add(enqueueOperation);
		}

		private int dequeueOperationsPerThread = totalOperationsToComplete / 4 * percentDequeue / 100;
		for(int i = 0; i < dequeueOperationsPerThread; i++) {
			operations.add(dequeueOperation);
		}

        int counter = 0;

        for(int i = 0; i < numThreads; i++)
        {
			Collections.shuffle(randomItems);
            threads[i] = new RingBufferThread(randomItems);
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
		List<Integer> operations;


        public RingBufferThread(ArrayList<Integer> toAdd, ArrayList<Integer> operations) {
            enqueue = new ArrayList<>(toAdd);
			operations = new ArrayList<>(operations);
        }

        //Initialize things to enqueue/dequeue
        @Override
        public void run() {

			int operationsCompleted = 0;
			int numEnqueues = enqueue.size();

			for(int currOp = 0; currOp < operations.size(); i++){
				switch(operations.get(currOp)) {
					case enqueueOperation:
						mrLockRingBuffer.enqueue(enqueue.get(currOp));
						break;
					case dequeueOperation:
						mrLockRingBuffer.dequeue();
						break;
					default:
						break;
				}
				operationsComplete.getAndIncrement();
			}
        }
    }
}
