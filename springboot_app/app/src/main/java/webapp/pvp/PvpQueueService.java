package webapp.pvp;

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
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

/** Coordinates ranked PvP matchmaking, keeping remote battle execution outside database transactions. */
@Service
public class PvpQueueService {
    private static final Logger LOG = LoggerFactory.getLogger(PvpQueueService.class);
    private static final String RANKED_MODE = "RANKED";

    private final PostgresBattleGuard guards;
    private final BattleSnapshotService snapshots;
    private final BattleEngineClient engine;
    private final BattleHistoryService battleHistory;
    private final PvpQueueRepository repository;
    private final PlatformTransactionManager transactions;
    private final JsonMapper json;

    public PvpQueueService(
            PostgresBattleGuard guards,
            BattleSnapshotService snapshots,
            BattleEngineClient engine,
            BattleHistoryService battleHistory,
            PvpQueueRepository repository,
            PlatformTransactionManager transactions,
            JsonMapper json) {
        this.guards = guards;
        this.snapshots = snapshots;
        this.engine = engine;
        this.battleHistory = battleHistory;
        this.repository = repository;
        this.transactions = transactions;
        this.json = json;
    }

    /** Enters ranked matchmaking, matching an existing WAITING player if available or enqueueing WAITING. */
    public JoinResult joinQueue(UserProfile user, int deckId, BattleBudget budget) {
        try (PostgresBattleGuard.Handle ignored = guards.acquire(user.id(), budget)) {
            EngineBattleRequest prepared = snapshots.prepare(user, deckId, budget);
            QueueClaim claim = claimOpponentOrEnqueue(user.id(), deckId, prepared.deck(), budget);
            if (claim.waitingTicketId() != null) {
                return new JoinResult(HttpStatus.ACCEPTED,
                        new PvpQueueResponse(claim.waitingTicketId(), "WAITING", null));
            }
            PvpQueueRepository.LockedOpponent opponent = claim.opponent();
            int selfTicketId = claim.selfTicketId();
            try {
                EnginePvpBattleRequest request = new EnginePvpBattleRequest(
                        opponent.challenger(), opponent.deck(),
                        prepared.challenger(), prepared.deck(),
                        RANKED_MODE);
                EnginePvpBattleResponse engineResponse = engine.executePvp(request, budget);
                BattleHistoryEntry battle = completeMatch(opponent, user.id(), prepared.deck(), selfTicketId,
                        engineResponse);
                return new JoinResult(HttpStatus.OK, new PvpQueueResponse(selfTicketId, "MATCHED", battle));
            } catch (RuntimeException failure) {
                safelyRevertFailedMatch(opponent.ticketId(), selfTicketId);
                throw failure;
            }
        }
    }

    /** Returns the current ticket status for its owner, hiding internal MATCHING as WAITING. */
    public PvpQueueResponse getTicketStatus(UserProfile user, int ticketId) {
        PvpQueueTicket ticket = requireOwnedTicket(user, ticketId);
        return switch (ticket.status()) {
            case "WAITING", "MATCHING" -> new PvpQueueResponse(ticket.id(), "WAITING", null);
            case "CANCELLED" -> new PvpQueueResponse(ticket.id(), "CANCELLED", null);
            case "MATCHED" -> {
                BattleHistoryEntry battle = ticket.battleHistoryId() == null ? null
                        : battleHistory.findById(ticket.battleHistoryId()).orElse(null);
                yield new PvpQueueResponse(ticket.id(), "MATCHED", battle);
            }
            default -> throw new IllegalStateException("Unexpected queue status: " + ticket.status());
        };
    }

    /** Cancels an owned WAITING ticket by transitioning it to CANCELLED. */
    public PvpQueueResponse cancelTicket(UserProfile user, int ticketId) {
        requireOwnedTicket(user, ticketId);
        if (!repository.cancelWaitingTicket(ticketId, user.id())) {
            throw new BattleFailure(HttpStatus.CONFLICT, "Conflict",
                    "Only a WAITING queue ticket can be cancelled.",
                    Map.of("code", "TICKET_NOT_WAITING"));
        }
        return new PvpQueueResponse(ticketId, "CANCELLED", null);
    }

    private QueueClaim claimOpponentOrEnqueue(
            int userId, int deckId, EngineBattleRequest.Deck deck, BattleBudget budget) {
        TransactionTemplate tx = shortTransaction(budget);
        return tx.execute(status -> {
            if (repository.hasActiveTicket(userId)) {
                throw alreadyInQueue();
            }
            Optional<PvpQueueRepository.LockedOpponent> opponent = repository.lockOldestWaitingOpponent(userId);
            if (opponent.isEmpty()) {
                int waitingTicketId = insertTicketOrConflict(userId, deckId, deck, "WAITING");
                return new QueueClaim(waitingTicketId, null, null);
            }
            PvpQueueRepository.LockedOpponent locked = opponent.get();
            if (!repository.markMatching(locked.ticketId())) {
                throw new IllegalStateException("Could not transition opponent ticket to MATCHING");
            }
            int selfTicketId = insertTicketOrConflict(userId, deckId, deck, "MATCHING");
            return new QueueClaim(null, locked, selfTicketId);
        });
    }

    private BattleHistoryEntry completeMatch(
            PvpQueueRepository.LockedOpponent opponent,
            int playerBUserId,
            EngineBattleRequest.Deck playerBDeck,
            int selfTicketId,
            EnginePvpBattleResponse engineResponse) {
        if (engineResponse == null || engineResponse.outcome() == null || engineResponse.outcome().isBlank()) {
            throw BattleFailure.integration();
        }
        Integer winnerId = parseWinnerId(engineResponse.winnerUserId(), opponent.userId(), playerBUserId);
        NewBattleHistoryEntry historyCommand = new NewBattleHistoryEntry(
                RANKED_MODE,
                opponent.userId(),
                playerBUserId,
                winnerId,
                engineResponse.outcome(),
                json.valueToTree(opponent.deck()),
                json.valueToTree(playerBDeck),
                engineResponse.battleLog(),
                0);
        TransactionTemplate tx = shortTransaction(null);
        return tx.execute(status -> {
            BattleHistoryEntry saved = battleHistory.recordBattle(historyCommand);
            int updated = repository.markMatched(opponent.ticketId(), selfTicketId, saved.id());
            if (updated != 2) {
                throw new IllegalStateException("Expected two MATCHING tickets when completing battle");
            }
            return saved;
        });
    }

    private void safelyRevertFailedMatch(int opponentTicketId, int selfTicketId) {
        boolean interrupted = Thread.interrupted();
        try {
            shortTransaction(null).executeWithoutResult(status ->
                    repository.revertFailedMatch(opponentTicketId, selfTicketId));
        } catch (RuntimeException recoveryFailure) {
            LOG.error("Failed to revert matchmaking tickets ({}, {}): {}",
                    opponentTicketId, selfTicketId, recoveryFailure.getClass().getSimpleName());
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private int insertTicketOrConflict(int userId, int deckId, EngineBattleRequest.Deck deck, String status) {
        try {
            return repository.insertTicket(userId, deckId, deck, status);
        } catch (DuplicateKeyException conflict) {
            throw alreadyInQueue();
        }
    }

    private TransactionTemplate shortTransaction(BattleBudget budget) {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        if (budget != null) {
            tx.setTimeout((int) Math.max(1, budget.remaining().toSeconds()));
        }
        return tx;
    }

    private PvpQueueTicket requireOwnedTicket(UserProfile user, int ticketId) {
        if (ticketId <= 0) {
            throw ticketNotFound();
        }
        PvpQueueTicket ticket = repository.findById(ticketId).orElseThrow(PvpQueueService::ticketNotFound);
        if (ticket.userId() != user.id()) {
            throw new BattleFailure(HttpStatus.FORBIDDEN, "Forbidden",
                    "You do not have access to this queue ticket.");
        }
        return ticket;
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

    private static BattleFailure alreadyInQueue() {
        return new BattleFailure(HttpStatus.CONFLICT, "Conflict",
                "User is already in the matchmaking queue.",
                Map.of("code", "ALREADY_IN_QUEUE"));
    }

    private static BattleFailure ticketNotFound() {
        return new BattleFailure(HttpStatus.NOT_FOUND, "Not Found", "Queue ticket not found.");
    }

    public record JoinResult(HttpStatus status, PvpQueueResponse body) {
    }

    private record QueueClaim(
            Integer waitingTicketId,
            PvpQueueRepository.LockedOpponent opponent,
            Integer selfTicketId) {
    }
}
