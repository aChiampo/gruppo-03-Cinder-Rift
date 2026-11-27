package webapp.pvp;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Application service for recording and querying battle history. */
@Service
public class BattleHistoryService {
    private final BattleHistoryRepository repository;

    public BattleHistoryService(BattleHistoryRepository repository) {
        this.repository = repository;
    }

    /** Lists the authenticated user's battles, optionally filtered by battle mode. */
    public List<BattleHistoryEntry> listHistory(int userId, String mode) {
        String normalizedMode = (mode == null || mode.isBlank()) ? null : mode.trim();
        return repository.findByUserId(userId, normalizedMode);
    }

    /** Returns a single battle history entry by id, if present. */
    public Optional<BattleHistoryEntry> findById(int id) {
        return repository.findById(id);
    }

    /** Persists a completed battle in {@code battle_history}. */
    public BattleHistoryEntry recordBattle(NewBattleHistoryEntry entry) {
        Objects.requireNonNull(entry, "Battle history entry is required");
        if (entry.mode() == null || entry.mode().isBlank()) {
            throw new IllegalArgumentException("Battle mode is required");
        }
        if (entry.outcome() == null || entry.outcome().isBlank()) {
            throw new IllegalArgumentException("Battle outcome is required");
        }
        if (entry.deckASnapshot() == null) {
            throw new IllegalArgumentException("Player A deck snapshot is required");
        }
        return repository.save(entry);
    }
}
