public class EnqueueOperation<T> extends OperationRecord {
    T newValue;
    RingBuffer<T> ringBuffer;

    EnqueueOperation(T newVal, RingBuffer<T> rb)
    {
        newValue = newVal;
        ringBuffer = rb;
    }

    @Override
    void helpComplete()
    {
        ringBuffer.easyEnqueue(newValue);
    }
}
