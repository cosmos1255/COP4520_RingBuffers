import java.util.concurrent.atomic.AtomicReference;

public class ProgressAssurance {

    static int HELP_DELAY = 100000;
    static int numThreads = 0;

    static AtomicReference[] opTable; //Do we need atomic references?

    ProgressAssurance(int numThr) {
        numThreads = numThr;
        opTable = new AtomicReference[numThreads];
        for (int i = 0; i < numThreads; i++) {
            opTable[i] = new AtomicReference<>();
        }
    }

    static void checkForAnnouncement(int helpId) {
        helpId++;
        if (helpId >= numThreads) {
            helpId = 0;
        }

        Object op = opTable[helpId].get();

        if (op instanceof OperationRecord) {
            ((OperationRecord) op).helpComplete(helpId);
        }
    }

    static void makeAnnouncement(OperationRecord op, int threadId) {
        opTable[threadId] = new AtomicReference<>(op);
        op.helpComplete(threadId);
        opTable[threadId] = null;
    }

    static Object getOperationRecord(int index) {
        return opTable[index];
    }

    static void setOperationRecord(int index, Integer value) {
        opTable[index] = new AtomicReference<>(value);
    }

}
