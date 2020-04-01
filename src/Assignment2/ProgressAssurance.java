import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class ProgressAssurance {

    static int HELP_DELAY = 100000;
    static int numThreads = 0;

    AtomicReference<Object>[] opTable; //Do we need atomic references?

    ProgressAssurance(int numThr) {
        numThreads = numThr;
        opTable = (AtomicReference<Object>[]) new Object[numThreads];
        for(int i = 0; i < numThreads; i++) {
            opTable[i] = new AtomicReference<>();
        }
    }

    void checkForAnnouncement(int helpId) {
        helpId++;
        if(helpId >= numThreads) {
            helpId = 0;
        }

        Object op = opTable[helpId].get();

        if(op instanceof OperationRecord) {
            ((OperationRecord) op).helpComplete();
        }
    }

    void makeAnnouncement(OperationRecord op, int threadId) {
        opTable[threadId] = new AtomicReference<>(op);
        op.helpComplete();
        opTable[threadId] = null;
    }

    Object getOperationRecord(int index) {
        return opTable[index];
    }

}
