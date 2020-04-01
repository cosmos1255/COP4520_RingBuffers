import java.util.ArrayList;
import java.util.Random;

public class TestRingBuffer {
    public static void main(String[] args) throws InterruptedException {
//        testOperationRecords();

    }

    //When creating threads, assign an ID and make a method to get own id, don't rely on built in thread IDs

    //TODO create threads

    static void testMakeAnnouncement() {

    }

    static void testCheckAnnouncement() {

    }

    static void testOperationRecords() throws InterruptedException {
        Random random = new Random();
        RingBuffer<Integer> ringBuffer = new RingBuffer<>(10);
        ArrayList<OperationRecord> opRecords = new ArrayList<>();
        ArrayList<OperationRecord> otherRecords = new ArrayList<>();

        for(int i = 0; i < 5; i++) {
            opRecords.add(new EnqueueOperation<>(random.nextInt(100), ringBuffer));
            opRecords.add(new EnqueueOperation<>(6, ringBuffer));
            otherRecords.add(new DequeueOperation<>(ringBuffer));
        }

        for(OperationRecord operationRecord : opRecords) {
            operationRecord.helpComplete();
            Thread.sleep(250);
            ringBuffer.printElements();
        }

        for(OperationRecord operationRecord : otherRecords) {
            operationRecord.helpComplete();
            Thread.sleep(250);
            ringBuffer.printElements();
        }

        for(OperationRecord operationRecord : opRecords) {
            operationRecord.helpComplete();
            Thread.sleep(250);
            ringBuffer.printElements();
        }
    }
}
