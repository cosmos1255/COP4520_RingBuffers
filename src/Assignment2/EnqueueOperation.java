public class EnqueueOperation extends OperationRecord {
    Integer newValue;
    RingBuffer ringBuffer;

    EnqueueOperation(Integer newVal, RingBuffer rb)
    {
        newValue = newVal;
        ringBuffer = rb;
    }

    @Override
    void helpComplete(int indexOfOperationRecord)
    {
        ringBuffer.easyEnqueue(newValue, indexOfOperationRecord);
    }

    //TODO implement getResult method?..
}
