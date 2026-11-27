package webapp.pack;

/** Ownership includes disabled products, which remain openable. */
public record OwnedPackView(int packId, int quantity, PackView pack) { }
