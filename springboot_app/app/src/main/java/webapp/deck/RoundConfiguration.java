package webapp.deck;

/** Persisted limits for one creature round or the equipment group. */
public record RoundConfiguration(String roundName, int roundNumber, int maxEnergy, int maxCards) {
}
