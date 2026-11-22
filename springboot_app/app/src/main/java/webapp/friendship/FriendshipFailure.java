package webapp.friendship;

/** An expected F13 failure with a stable client-facing code. */
public class FriendshipFailure extends RuntimeException {
    private final int status;
    private final String code;

    FriendshipFailure(int status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
