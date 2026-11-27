package webapp.battle;

/** Internal PvP engine request payload using server-owned snapshots only. */
public record EnginePvpBattleRequest(
        EngineBattleRequest.Challenger playerA,
        EngineBattleRequest.Deck deckA,
        EngineBattleRequest.Challenger playerB,
        EngineBattleRequest.Deck deckB,
        String mode) {
}
