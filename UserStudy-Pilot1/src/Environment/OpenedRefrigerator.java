package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class OpenedRefrigerator implements AutoCloseable {
    OpenedRefrigerator() {
    }

    public Food get(String kind) throws UnavailableException {
        return new Food() {
            @Override
            public String toString() {
                return kind;
            }
        };
    }

    @Override
    public void close() {

    }
}
