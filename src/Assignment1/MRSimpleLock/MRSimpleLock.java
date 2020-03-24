import java.util.concurrent.atomic.AtomicInteger;

public class MRSimpleLock {

    AtomicInteger mBits;

    MRSimpleLock() {
        this.mBits = new AtomicInteger();
    }

    public void lock(int resources) {
        for (;;) {
            int bits = mBits.get();
            if ((bits & resources) == 0) {
                if (mBits.compareAndSet(bits, bits | resources)) {
                    break;
                }
            }
        }
    }

    void unlock(int resources) {
        for (;;) {
            int bits = mBits.get();
            if (mBits.compareAndSet(bits, bits & ~resources)) {
                break;
            }
        }
    }
}