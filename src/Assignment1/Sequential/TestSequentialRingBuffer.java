
public class TestSequentialRingBuffer {

    public static void main(String[] args) {
        SequentialRingBuffer<Integer> buffer = new SequentialRingBuffer<>(10);

        System.out.println("Buffer should be empty. IsEmpty Value should be true, and is " + buffer.isEmpty());
        System.out.println("Buffer should be empty. IsFull Value should be false, and is " + buffer.isFull());

        System.out.println("Size should be 0, and is " + buffer.getSize());

        buffer.enqueue(1);
        buffer.enqueue(2);
        buffer.enqueue(3);

        System.out.println("Buffer shouldn't be empty. IsEmpty Value should be false, and is " + buffer.isEmpty());
        System.out.println("Buffer shouldn't be empty. IsFull Value should be false, and is " + buffer.isFull());

        buffer.printElements();

        buffer.dequeue();
        buffer.dequeue();
        buffer.dequeue();
        if(!buffer.dequeue()) {
            System.out.println("Can't dequeue, buffer is empty.");
        }

        System.out.println("Buffer should be empty. IsEmpty Value should be true, and is " + buffer.isEmpty());
        System.out.println("Buffer should be empty. IsFull Value should be false, and is " + buffer.isFull());

        buffer.enqueue(1);
        buffer.enqueue(2);
        buffer.enqueue(3);
        buffer.enqueue(4);
        buffer.enqueue(5);
        buffer.enqueue(6);
        buffer.enqueue(7);
        buffer.enqueue(8);
        buffer.enqueue(9);
        buffer.enqueue(10);

        System.out.println("Buffer should be full. IsFull Value should be full, and is " + buffer.isFull());

        buffer.printElements();
    }
}
