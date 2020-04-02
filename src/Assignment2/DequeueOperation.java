import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class DequeueOperation extends OperationRecord {
    RingBuffer ringBuffer;

    DequeueOperation(RingBuffer rb) {
        ringBuffer = rb;
    }

    @Override
    void helpComplete(int indexOfOperationRecord) {
        int head = ringBuffer.getHead().get();
        while (this.notDone()) {
            if (ringBuffer.isEmpty()) {
                this.fail();
                return;
            }
            int seqId = head++;
            int pos = ringBuffer.getPos(seqId);
            AtomicStampedReference<Integer> val = ringBuffer.getElementAtPos(pos);

            while (this.notDone()) {
                if (ringBuffer.readValue(pos, val))
                    continue;

                int valSeqId = val.getReference();
                boolean valIsValueType = ringBuffer.valueIsValueType(valSeqId);
                boolean valIsDelayMarked = ringBuffer.valueIsDelayMarked(valSeqId);

                if (valSeqId > head) {
                    // So check the next position
                    break;
                } else if (valIsDelayMarked) {
                    // We don't want a delayed marked anything, too complicated, let some
                    // one else deal with it.
                    break;
                } else if (valIsValueType) {
                    // it is a valueType, with seqid <= to the one we are working with...
                    // so we take it or try to any way...
                    AtomicReference<Helper> helper = new AtomicReference<>(new Helper(this, val));

                    boolean res = ringBuffer.elements[pos].compareAndSet(val, helper, 0, 100);

                    break;
                } else {
                    // Its an EmptyType with a seqid <= to the one we are working with
                    // so lets mess shit up and set it delayed mark that will show them...
                    // but it is the simplest way to ensure nothing gets enqueued at this pos
                    // which allows us to keep fifo.
                    // If something did get enqueued then, it will be marked and we will check
                    // it on the next round
                    if (valIsDelayMarked) {
                        // its already been marked, so move on to the next pos;
                        break;
                    } else {
                        ringBuffer.atomicDelayMark(pos);
                        continue;  // re-read/process the value
                    }
                }
            }
        }
    }

}
