package webapp.card;

import java.util.List;

/** A stable, explicitly paginated API response. */
public record CardPage<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
}
