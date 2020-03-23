import java.util.concurrent.atomic.AtomicInteger;

public class MRSimpleLock {

    AtomicInteger mBits;

    MRSimpleLock() {
        this.mBits = new AtomicInteger();
    }

    public void lock(int resources) {
        for (;;) {
            int bits = mBits.get();
            System.out.println("Locking on " + resources);
            if ((bits & resources) == 0) {
                if (mBits.compareAndSet(bits, bits | resources)) {
                    System.out.println(mBits.get());
                    break;
                }
            }
        }
    }

    void unlock(int resources) {
        for (;;) {
            int bits = mBits.get();
            if (mBits.compareAndSet(bits, bits & ~resources)) {
                System.out.println(mBits.get());
                break;
            }
        }
    }
}