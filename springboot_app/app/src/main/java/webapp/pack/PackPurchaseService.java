package webapp.pack;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import webapp.economy.DebitService;

/** The shared debit and unopened-pack credit have exactly one commit boundary. */
@Service
public class PackPurchaseService {
    private final PackPurchaseRepository purchases;
    private final DebitService debit;

    public PackPurchaseService(PackPurchaseRepository purchases, DebitService debit) {
        this.purchases = purchases;
        this.debit = debit;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PurchasePacksResponse purchase(int userId, PurchasePacksRequest request) {
        var offer = purchases.lockOffer(request.packId());
        if (!offer.acquirable()) throw new PackOpeningFailure(409, "PACK_NOT_ACQUIRABLE", "This pack cannot be acquired.");
        if (offer.price() < 0) throw new PackOpeningFailure(500, "INTERNAL_ERROR", "The pack price is invalid.");
        purchases.lockBuyer(userId);
        long cost = (long) offer.price() * request.quantity();
        var balance = debit.debitScaglie(userId, cost);
        int owned = purchases.credit(balance.inventoryId(), request.packId(), request.quantity());
        return new PurchasePacksResponse(request.packId(), request.quantity(), offer.price(), cost, owned,
                balance.cobaltShards(), balance.reservedCobaltShards(), balance.availableCobaltShards());
    }
}
