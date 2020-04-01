import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class ProgressAssurance {

    static int HELP_DELAY = 100000;
    static int numThreads = 0;

    OperationRecord[] opTable; //Do we need atomic references?

    ProgressAssurance(int numThr) {
        numThreads = numThr;
        opTable = new OperationRecord[numThreads];
    }

    void checkForAnnouncement(int helpId) {
        helpId++;
        if(helpId >= numThreads) {
            helpId = 0;
        }

        OperationRecord op = opTable[helpId];

        if(op != null) {
            //TODO implement similarity to hazard pointer?
            op.helpComplete();
        }
    }

    void makeAnnouncement(OperationRecord op, int threadId) {
        opTable[threadId] = op;
        op.helpComplete();
        opTable[threadId] = null;
    }

}
