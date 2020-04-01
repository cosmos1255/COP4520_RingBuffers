public class DequeueOperation extends OperationRecord {
    RingBuffer ringBuffer;

    DequeueOperation(RingBuffer rb)
    {
        ringBuffer = rb;
    }

    @Override
    void helpComplete()
    {
        ringBuffer.easyDequeue();
    }

    //TODO implement getResult method?..

}
