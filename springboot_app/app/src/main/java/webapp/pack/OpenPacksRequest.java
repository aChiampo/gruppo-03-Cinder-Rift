package webapp.pack;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/** Strict integer input: JSON coercion must not choose a pack or quantity. */
public record OpenPacksRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "2147483647") int packId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "10") int quantity) {
    public OpenPacksRequest {
        if (packId <= 0 || quantity < 1 || quantity > 10) throw invalid();
    }

    public static OpenPacksRequest parse(JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 2
                || !integer(body.get("packId")) || !integer(body.get("quantity"))) throw invalid();
        return new OpenPacksRequest(body.get("packId").intValue(), body.get("quantity").intValue());
    }

    private static boolean integer(JsonNode value) {
        return value != null && value.isIntegralNumber() && value.canConvertToInt();
    }

    private static PackOpeningFailure invalid() {
        return new PackOpeningFailure(400, "INVALID_REQUEST",
                "Provide only a positive integer packId and an integer quantity from 1 to 10.");
    }
}
