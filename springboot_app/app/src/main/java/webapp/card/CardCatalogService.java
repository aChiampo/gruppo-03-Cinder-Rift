package webapp.card;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides filtered and paginated catalog reads. */
@Service
public class CardCatalogService {
    private final CardCatalogRepository cards;

    public CardCatalogService(CardCatalogRepository cards) {
        this.cards = cards;
    }

    @Transactional(readOnly = true)
    public CardPage<CardCatalogItem> search(CardSearchCriteria criteria) {
        long total = cards.count(criteria);
        int totalPages = total == 0 ? 0 : Math.toIntExact((total + criteria.size() - 1) / criteria.size());
        return new CardPage<>(cards.findPage(criteria), criteria.page(), criteria.size(), total, totalPages);
    }
}
