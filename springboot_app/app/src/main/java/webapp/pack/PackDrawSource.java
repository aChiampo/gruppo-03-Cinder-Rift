package webapp.pack;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Server-owned randomness, replaceable in deterministic drawing tests. */
@Component
public class PackDrawSource {
    private final SecureRandom random = new SecureRandom();

    public int nextInt(int bound) { return random.nextInt(bound); }
}
