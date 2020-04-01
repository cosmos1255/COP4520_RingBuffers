public class DequeueOperation extends OperationRecord {
    RingBuffer ringBuffer;

    DequeueOperation(RingBuffer rb)
    {
        ringBuffer = rb;
    }

    @Override
    void helpComplete(int indexOfOperationRecord)
    {
        ringBuffer.easyDequeue(indexOfOperationRecord);
    }

    //TODO implement getResult method?..

}
