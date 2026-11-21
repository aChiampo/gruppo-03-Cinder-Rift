package webapp.card;

/** A catalog value with its stable integer ID and display code. */
public record CardReference(Integer id, String code, String name) {
}
