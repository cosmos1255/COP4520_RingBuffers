import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class EnqueueOperation extends OperationRecord {
    Integer newValue;
    RingBuffer ringBuffer;

    EnqueueOperation(Integer newVal, RingBuffer rb) {
        newValue = newVal;
        ringBuffer = rb;
    }

    @Override
    void helpComplete(int indexOfOperationRecord) {
        int tail = ringBuffer.getTail().get();
        while (this.notDone()) {
            if (ringBuffer.isFull()) {
                this.fail();
                return;
            }
            int seqId = tail++;
            int pos = ringBuffer.getPos(seqId);
            AtomicStampedReference<Integer> val = ringBuffer.getElementAtPos(pos);

            while (this.notDone()) {
                if (ringBuffer.readValue(pos, val))
                    continue;

                int valSeqId = val.getReference();
                boolean valIsValueType = ringBuffer.valueIsValueType(valSeqId);
                boolean valIsDelayMarked = ringBuffer.valueIsDelayMarked(valSeqId);

                if (valSeqId > tail) {
                    // We are lagging, so iterate until we find a matching seqId.
                    break;
                } else if (valIsDelayMarked) {
                    // We don't want a delayed marked anything, too complicated, let some
                    // one else deal with it.
                    break;
                } else if (valIsValueType) {
                    // it is a valueType, with seqid <= to the one we are working with...
                    // skip
                    break;
                } else {
                    // Its an EmptyType with a seqid <= to the one we are working with
                    // so lets take it!
                    AtomicReference<Helper> helper = new AtomicReference<>(new Helper(this, val));

                    boolean result = ringBuffer.elements[pos].compareAndSet(val, helper, 0, 100);
                    if (result) {
                        // Success!
                        return;
                    }
                }
            }
        }
    }
}
