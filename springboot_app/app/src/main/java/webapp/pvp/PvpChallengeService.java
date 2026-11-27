package webapp.pvp;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import webapp.auth.UserProfile;
import webapp.battle.BattleBudget;
import webapp.battle.BattleEngineClient;
import webapp.battle.BattleFailure;
import webapp.battle.BattleSnapshotService;
import webapp.battle.EngineBattleRequest;
import webapp.battle.EnginePvpBattleRequest;
import webapp.battle.EnginePvpBattleResponse;
import webapp.battle.PostgresBattleGuard;
import webapp.friendship.FriendshipService;

/** Coordinates direct friendly PvP challenges, executing remote battles outside database transactions. */
@Service
public class PvpChallengeService {
    private static final String FRIENDLY_MODE = "FRIENDLY";

    private final PostgresBattleGuard guards;
    private final BattleSnapshotService snapshots;
    private final BattleEngineClient engine;
    private final BattleHistoryService battleHistory;
    private final FriendshipService friendships;
    private final PvpChallengeRepository repository;
    private final PlatformTransactionManager transactions;
    private final JsonMapper json;

    public PvpChallengeService(
            PostgresBattleGuard guards,
            BattleSnapshotService snapshots,
            BattleEngineClient engine,
            BattleHistoryService battleHistory,
            FriendshipService friendships,
            PvpChallengeRepository repository,
            PlatformTransactionManager transactions,
            JsonMapper json) {
        this.guards = guards;
        this.snapshots = snapshots;
        this.engine = engine;
        this.battleHistory = battleHistory;
        this.friendships = friendships;
        this.repository = repository;
        this.transactions = transactions;
        this.json = json;
    }

    /** Creates a PENDING friendly challenge against an accepted friend after snapshotting the challenger's deck. */
    public PvpChallengeResponse createChallenge(UserProfile challenger, int opponentUserId, int deckId,
            BattleBudget budget) {
        if (opponentUserId <= 0 || deckId <= 0) {
            throw new BattleFailure(HttpStatus.BAD_REQUEST, "Bad Request",
                    "opponentUserId and deckId must be positive integers.");
        }
        if (opponentUserId == challenger.id()) {
            throw new BattleFailure(HttpStatus.BAD_REQUEST, "Bad Request",
                    "You cannot challenge yourself.",
                    Map.of("code", "SELF_CHALLENGE"));
        }
        boolean acceptedFriend = friendships.list(challenger.id(), "friends").stream()
                .anyMatch(friend -> friend.otherUserId() == opponentUserId);
        if (!acceptedFriend) {
            throw new BattleFailure(HttpStatus.FORBIDDEN, "Forbidden",
                    "You can only challenge accepted friends.",
                    Map.of("code", "NOT_FRIENDS"));
        }
        EngineBattleRequest prepared = snapshots.prepare(challenger, deckId, budget);
        int challengeId = repository.insertPending(challenger.id(), opponentUserId, deckId, prepared.deck());
        return repository.findById(challengeId).orElseThrow(BattleFailure::integration);
    }

    /** Lists incoming and outgoing PENDING challenges for the authenticated user. */
    public PvpChallengesResponse listChallenges(UserProfile user) {
        return new PvpChallengesResponse(
                repository.findIncomingPending(user.id()),
                repository.findOutgoingPending(user.id()));
    }

    /** Returns one challenge owned by either participant, including its battle report once completed. */
    public PvpChallengeResponse getChallenge(UserProfile user, int challengeId) {
        if (challengeId <= 0) {
            throw challengeNotFound();
        }
        PvpChallengeResponse challenge = repository.findById(challengeId)
                .orElseThrow(PvpChallengeService::challengeNotFound);
        if (challenge.challengerId() != user.id() && challenge.opponentId() != user.id()) {
            throw new BattleFailure(HttpStatus.FORBIDDEN, "Forbidden",
                    "You do not have access to this challenge.");
        }
        BattleHistoryEntry battle = challenge.battleHistoryId() == null ? null
                : battleHistory.findById(challenge.battleHistoryId()).orElse(null);
        return withBattle(challenge, battle);
    }

    /** Accepts a PENDING challenge as its opponent, runs the engine outside DB transactions, and saves history. */
    public PvpChallengeResponse acceptChallenge(UserProfile opponent, int challengeId, int deckId,
            BattleBudget budget) {
        if (challengeId <= 0) {
            throw challengeNotFound();
        }
        if (deckId <= 0) {
            throw new BattleFailure(HttpStatus.BAD_REQUEST, "Bad Request",
                    "deckId must be a positive integer.");
        }
        try (PostgresBattleGuard.Handle ignored = guards.acquire(opponent.id(), budget)) {
            PvpChallengeRepository.ChallengeExecutionData challenge = repository.findForExecution(challengeId)
                    .orElseThrow(PvpChallengeService::challengeNotFound);
            if (challenge.opponentId() != opponent.id()) {
                throw new BattleFailure(HttpStatus.FORBIDDEN, "Forbidden",
                        "Only the challenged opponent can accept this challenge.");
            }
            if (!"PENDING".equals(challenge.status())) {
                throw notPending();
            }
            if (challenge.challengerDeleted()) {
                throw new BattleFailure(HttpStatus.CONFLICT, "Conflict",
                        "The challenger account is no longer active.",
                        Map.of("code", "CHALLENGER_UNAVAILABLE"));
            }

            EngineBattleRequest opponentPrepared = snapshots.prepare(opponent, deckId, budget);
            var challengerPlayer = new EngineBattleRequest.Challenger(
                    Integer.toString(challenge.challengerId()),
                    challenge.challengerNickname(),
                    challenge.challengerNickname());
            EnginePvpBattleRequest engineRequest = new EnginePvpBattleRequest(
                    challengerPlayer,
                    challenge.challengerDeck(),
                    opponentPrepared.challenger(),
                    opponentPrepared.deck(),
                    FRIENDLY_MODE);

            // Remote engine execution happens strictly outside any database transaction.
            EnginePvpBattleResponse engineResponse = engine.executePvp(engineRequest, budget);
            return completeChallenge(challenge, opponent.id(), deckId, opponentPrepared.deck(), engineResponse);
        }
    }

    /** Declines a PENDING challenge as the opponent or cancels it as the challenger. */
    public PvpChallengeResponse declineChallenge(UserProfile actor, int challengeId) {
        if (challengeId <= 0) {
            throw challengeNotFound();
        }
        PvpChallengeResponse challenge = repository.findById(challengeId)
                .orElseThrow(PvpChallengeService::challengeNotFound);
        String newStatus;
        if (challenge.opponentId() == actor.id()) {
            newStatus = "DECLINED";
        } else if (challenge.challengerId() == actor.id()) {
            newStatus = "CANCELLED";
        } else {
            throw new BattleFailure(HttpStatus.FORBIDDEN, "Forbidden",
                    "You do not have access to this challenge.");
        }
        if (!"PENDING".equals(challenge.status())
                || !repository.markDeclinedOrCancelled(challengeId, actor.id(), newStatus)) {
            throw notPending();
        }
        return repository.findById(challengeId).orElseThrow(PvpChallengeService::challengeNotFound);
    }

    private PvpChallengeResponse completeChallenge(
            PvpChallengeRepository.ChallengeExecutionData challenge,
            int opponentId,
            int opponentDeckId,
            EngineBattleRequest.Deck opponentDeck,
            EnginePvpBattleResponse engineResponse) {
        if (engineResponse == null || engineResponse.outcome() == null || engineResponse.outcome().isBlank()) {
            throw BattleFailure.integration();
        }
        Integer winnerId = parseWinnerId(engineResponse.winnerUserId(), challenge.challengerId(), opponentId);
        NewBattleHistoryEntry historyCommand = new NewBattleHistoryEntry(
                FRIENDLY_MODE,
                challenge.challengerId(),
                opponentId,
                winnerId,
                engineResponse.outcome(),
                json.valueToTree(challenge.challengerDeck()),
                json.valueToTree(opponentDeck),
                engineResponse.battleLog(),
                0);

        TransactionTemplate tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return tx.execute(status -> {
            String currentStatus = repository.lockStatus(challenge.id())
                    .orElseThrow(PvpChallengeService::challengeNotFound);
            if (!"PENDING".equals(currentStatus)) {
                throw notPending();
            }
            BattleHistoryEntry saved = battleHistory.recordBattle(historyCommand);
            if (!repository.markCompleted(challenge.id(), opponentId, opponentDeckId, saved.id())) {
                throw notPending();
            }
            PvpChallengeResponse updated = repository.findById(challenge.id())
                    .orElseThrow(BattleFailure::integration);
            return withBattle(updated, saved);
        });
    }

    private static PvpChallengeResponse withBattle(PvpChallengeResponse challenge, BattleHistoryEntry battle) {
        return new PvpChallengeResponse(
                challenge.id(),
                challenge.challengerId(),
                challenge.challengerNickname(),
                challenge.opponentId(),
                challenge.opponentNickname(),
                challenge.challengerDeckId(),
                challenge.opponentDeckId(),
                challenge.status(),
                challenge.battleHistoryId(),
                challenge.createdAt(),
                challenge.respondedAt(),
                battle);
    }

    private static Integer parseWinnerId(String winnerUserId, int playerAId, int playerBId) {
        if (winnerUserId == null || winnerUserId.isBlank()) {
            return null;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(winnerUserId.trim());
        } catch (NumberFormatException invalid) {
            throw BattleFailure.integration();
        }
        if (parsed != playerAId && parsed != playerBId) {
            throw BattleFailure.integration();
        }
        return parsed;
    }

    private static BattleFailure challengeNotFound() {
        return new BattleFailure(HttpStatus.NOT_FOUND, "Not Found", "Challenge not found.");
    }

    private static BattleFailure notPending() {
        return new BattleFailure(HttpStatus.CONFLICT, "Conflict",
                "Challenge is no longer pending.",
                Map.of("code", "CHALLENGE_NOT_PENDING"));
    }
}
