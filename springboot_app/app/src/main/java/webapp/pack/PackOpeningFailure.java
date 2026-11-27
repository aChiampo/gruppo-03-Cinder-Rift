package webapp.pack;

import java.util.Map;

/** An opening failure safe to expose after the transaction has rolled back. */
public class PackOpeningFailure extends RuntimeException {
    private final int status;
    private final String code;
    private final Map<String, Object> properties;

    public PackOpeningFailure(int status, String code, String detail) {
        this(status, code, detail, Map.of());
    }

    public PackOpeningFailure(int status, String code, String detail, Map<String, Object> properties) {
        super(detail);
        this.status = status;
        this.code = code;
        this.properties = Map.copyOf(properties);
    }

    public int status() { return status; }
    public String code() { return code; }
    public Map<String, Object> properties() { return properties; }

    static PackOpeningFailure invalidConfiguration(String detail) {
        return new PackOpeningFailure(409, "PACK_CONFIGURATION_INVALID", detail);
    }
}
