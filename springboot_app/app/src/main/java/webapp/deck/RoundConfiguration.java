package webapp.deck;

/** Persisted limits for one creature round or the equipment group. */
public record RoundConfiguration(String roundName, int roundNumber, int maxCardEnergy,
        int maxRoundPower, int maxCards) {
}
