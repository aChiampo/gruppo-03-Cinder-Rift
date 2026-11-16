package webapp.card;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides an authenticated user's own filtered and paginated collection. */
@Service
public class CollectionService {
    private final CollectionRepository collections;

    public CollectionService(CollectionRepository collections) {
        this.collections = collections;
    }

    @Transactional(readOnly = true)
    public CardPage<CollectionItem> findOwnCollection(int authenticatedUserId, CardSearchCriteria criteria) {
        long total = collections.count(authenticatedUserId, criteria);
        int totalPages = total == 0 ? 0 : Math.toIntExact((total + criteria.size() - 1) / criteria.size());
        return new CardPage<>(
                collections.findPage(authenticatedUserId, criteria),
                criteria.page(),
                criteria.size(),
                total,
                totalPages);
    }
}
