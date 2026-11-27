package webapp.pack;

/** Immediate purchase result; this is not a persisted receipt or a recoverable purchase resource. */
public record PurchasePacksResponse(int packId, int quantity, int unitPrice, long totalCost,
        int ownedQuantity, int cobaltShards, long reservedCobaltShards, long availableCobaltShards) { }
