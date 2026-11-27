package webapp.economy;

import java.util.Map;

/** Resource failures are independent of the feature spending the resources. */
public class EconomyFailure extends RuntimeException {
    private final int status;
    private final String code;
    private final Map<String, Object> properties;

    public EconomyFailure(int status, String code, String detail) {
        this(status, code, detail, Map.of());
    }

    public EconomyFailure(int status, String code, String detail, Map<String, Object> properties) {
        super(detail);
        this.status = status;
        this.code = code;
        this.properties = Map.copyOf(properties);
    }

    public int status() { return status; }
    public String code() { return code; }
    public Map<String, Object> properties() { return properties; }
}
