public class DequeueOperation<T> extends OperationRecord {
    RingBuffer<T> ringBuffer;

    DequeueOperation(RingBuffer<T> rb)
    {
        ringBuffer = rb;
    }

    @Override
    void helpComplete()
    {
        ringBuffer.easyDequeue();
    }

}
