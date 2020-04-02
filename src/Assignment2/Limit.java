public class Limit {

    private final long PROG_ASSUR_LIMIT = 100000;
    private long counter;

    boolean notDelayed(long val) {
        return !isDelayed(val);
    }

    boolean isDelayed(long val) {
        long temp = counter;
        counter -= val;
        return temp == 0;
    }
}
