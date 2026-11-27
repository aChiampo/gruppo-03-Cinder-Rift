package webapp.pack;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/** Purchase quantities are limited by affordability and integer storage, not the opening batch limit. */
public record PurchasePacksRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "2147483647") int packId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "2147483647") int quantity) {
    public PurchasePacksRequest {
        if (packId <= 0 || quantity <= 0) throw invalid();
    }

    public static PurchasePacksRequest parse(JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 2
                || !integer(body.get("packId")) || !integer(body.get("quantity"))) throw invalid();
        return new PurchasePacksRequest(body.get("packId").intValue(), body.get("quantity").intValue());
    }

    private static boolean integer(JsonNode value) {
        return value != null && value.isIntegralNumber() && value.canConvertToInt();
    }

    private static PackOpeningFailure invalid() {
        return new PackOpeningFailure(400, "INVALID_REQUEST", "Provide only positive 32-bit integer packId and quantity.");
    }
}
