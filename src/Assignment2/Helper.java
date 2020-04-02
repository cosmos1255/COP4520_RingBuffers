package Assignment2;

import java.util.concurrent.atomic.AtomicStampedReference;

public class Helper {
    OperationRecord operationRecord;
    AtomicStampedReference<Integer> oldValue;

    Helper(OperationRecord or, AtomicStampedReference<Integer> ov) {
        operationRecord = or;
        oldValue = ov;
    }
}
