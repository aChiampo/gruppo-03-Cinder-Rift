package webapp.pack;

/** Distinct resource balances of the authenticated user. */
public record InventoryView(int cobaltShards, int riftCores, int scraps) { }
