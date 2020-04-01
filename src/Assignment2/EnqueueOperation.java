public class EnqueueOperation extends OperationRecord {
    Integer newValue;
    RingBuffer ringBuffer;

    EnqueueOperation(Integer newVal, RingBuffer rb)
    {
        newValue = newVal;
        ringBuffer = rb;
    }

    @Override
    void helpComplete()
    {
        ringBuffer.easyEnqueue(newValue);
    }

    //TODO implement getResult method?..
}
