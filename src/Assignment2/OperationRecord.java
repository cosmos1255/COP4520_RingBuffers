import java.util.concurrent.atomic.AtomicReference;

public class OperationRecord {
    AtomicReference<Helper> helper;

    void helpComplete(int indexOfOperationRecord) {
    }

    void fail() {
        helper.compareAndExchange(helper.get(), null);
    }

    //TODO change?...
    boolean getResult() {
        return true;
    }

    boolean notDone() {
        return helper.get() == null;
    }
}
