package webapp.pack;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Uses identical eligibility rules for administrative writes and actual openings. */
@Service
public class PackConfigurationValidationService {
    private final PackDrawRepository catalog;

    public PackConfigurationValidationService(PackDrawRepository catalog) { this.catalog = catalog; }

    public void validateForWrite(PackWriteRequest pack) {
        try { prepare(pack); }
        catch (PackOpeningFailure failure) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, failure.getMessage());
        }
    }

    PackOpeningConfiguration prepare(PackWriteRequest pack) {
        if (pack.allowedFactions().isEmpty() || pack.allowedTypes().isEmpty()
                || pack.allowedExpansions().isEmpty() || pack.allowedRarities().isEmpty()) {
            throw invalid("Empty filters allow no cards; list all allowed codes to remove a restriction.");
        }
        if (pack.packSlots().isEmpty() || pack.packSlots().size() != pack.description().cardNumber()) {
            throw invalid("The pack must have exactly one slot per configured card.");
        }
        ArtworkPullRates artwork = catalog.artwork(pack.artworkPullRatesId())
                .orElseThrow(() -> invalid("The artwork configuration is unavailable."));
        requireDistribution(List.of(artwork.standard(), artwork.extendedArt(), artwork.fullArt(), artwork.alternateArt()));
        var types = catalog.slotTypes(pack.packSlots().stream().distinct().sorted().toList());
        var cards = catalog.eligibleCards(pack);
        var pools = cards.stream().collect(Collectors.groupingBy(c -> c.rarity().id()));
        var mercy = pack.mercyRarity() == null ? null : catalog.rarity(pack.mercyRarity())
                .orElseThrow(() -> invalid("The mercy rarity is unavailable."));
        boolean active = pack.mercyThreshold() != null && mercy != null;
        var reachableRarities = new HashSet<Integer>();
        if (active && pack.mercyThreshold() <= 0) throw invalid("The mercy threshold must be positive.");
        for (int id : pack.packSlots()) {
            SlotPullType type = types.get(id);
            if (type == null) throw invalid("A slot distribution is unavailable.");
            requireDistribution(type.pullRates().stream().map(SlotPullType.RarityPullRate::probability).toList());
            for (var rate : type.pullRates()) {
                if (rate.probability().signum() == 0) continue;
                reachableRarities.add(rate.rarityId());
                if (!pools.containsKey(rate.rarityId())) {
                    throw invalid("Slot type " + id + " has no eligible cards for rarity " + rate.rarityCode() + ".");
                }
                if (active) rank(rate.rarityCode());
            }
        }
        if (active) {
            rank(mercy.code());
            reachableRarities.add(mercy.id());
            if (!pools.containsKey(mercy.id())) throw invalid("No eligible cards for the exact mercy rarity.");
        }
        // Only rarities reachable by a slot or the mercy replacement require the fallback.
        for (var card : cards) {
            if (reachableRarities.contains(card.rarity().id())
                    && card.artworks().stream().noneMatch(a -> a.artCode().equals("STANDARD"))) {
                throw invalid("Eligible card " + card.id() + " has no STANDARD artwork fallback.");
            }
        }
        Map<Integer, List<PackOpeningConfiguration.DrawCard>> immutablePools = pools.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
        return new PackOpeningConfiguration(pack.packSlots().stream().map(types::get).toList(),
                immutablePools, artwork, pack.mercyThreshold(), mercy);
    }

    static int rank(String code) {
        return switch (code) {
            case "COMMON" -> 0;
            case "RARE" -> 1;
            case "EPIC" -> 2;
            case "LEGENDARY" -> 3;
            default -> throw invalid("Mercy requires the configured Common, Rare, Epic, Legendary rarity order.");
        };
    }

    static int weight(BigDecimal probability) {
        try {
            int result = probability.movePointRight(4).intValueExact();
            if (result < 0 || result > 1_000_000) throw invalid("Invalid probability.");
            return result;
        } catch (ArithmeticException failure) { throw invalid("Probabilities require at most four decimal places."); }
    }

    private static void requireDistribution(List<BigDecimal> probabilities) {
        if (probabilities.stream().mapToInt(PackConfigurationValidationService::weight).sum() != 1_000_000) {
            throw invalid("Probabilities must total exactly 100.0000 percent.");
        }
    }

    private static PackOpeningFailure invalid(String detail) { return PackOpeningFailure.invalidConfiguration(detail); }
}
