public class Limit {

    private final long PROG_ASSUR_LIMIT = 100000;
    private final long TERVEL_PROG_ASSUR_DELAY = 100000;

    private long counter;

    boolean notDelayed(long val) {
        return !isDelayed(val);
    }

    boolean isDelayed(long val) {
        long temp = counter;
        counter -= val;

        if(temp == 0)
            return true;
        else
            return false;
    }

    void reset() {
        counter = PROG_ASSUR_LIMIT;
    }
}
