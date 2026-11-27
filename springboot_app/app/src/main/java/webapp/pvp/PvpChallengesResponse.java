package webapp.pvp;

import java.util.List;

/** Incoming and outgoing direct friend challenges for the authenticated user. */
public record PvpChallengesResponse(
        List<PvpChallengeResponse> incoming,
        List<PvpChallengeResponse> outgoing) {
    public PvpChallengesResponse {
        incoming = incoming == null ? List.of() : List.copyOf(incoming);
        outgoing = outgoing == null ? List.of() : List.copyOf(outgoing);
    }
}
