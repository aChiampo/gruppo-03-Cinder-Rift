package webapp.pack;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import webapp.card.CardArtwork;
import webapp.card.CardReference;
import webapp.pack.PackOpeningConfiguration.DrawCard;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** Verifies probability boundaries and mercy transitions without statistical/flaky assertions. */
class PackDrawingServiceTests {
    private static final Map<Integer, List<DrawCard>> CARDS = Map.of(
            1, List.of(card(11, 1, "COMMON")), 2, List.of(card(22, 2, "RARE")),
            3, List.of(card(33, 3, "EPIC")), 4, List.of(card(44, 4, "LEGENDARY")));
    private static final ArtworkPullRates ART = new ArtworkPullRates(1, decimal("81"), decimal("10"), decimal("7.5"), decimal("1.5"));

    @Test
    void honorsFourDecimalProbabilityBoundariesAndZeroProbability() {
        var type = new SlotPullType(1, "", List.of(rate(4, "LEGENDARY", "0"),
                rate(1, "COMMON", "0.0001"), rate(2, "RARE", "99.9999")));
        var config = config(List.of(type), null, CARDS);
        assertThat(drawWith(0, 0, 0).openOnePack(config, null).cards().getFirst().rarity().code()).isEqualTo("COMMON");
        assertThat(drawWith(1, 0, 0).openOnePack(config, null).cards().getFirst().rarity().code()).isEqualTo("RARE");
        assertThat(drawWith(999999, 0, 0).openOnePack(config, null).cards().getFirst().rarity().code()).isEqualTo("RARE");
    }

    @Test
    void appliesArtworkBoundariesIndependentlyAndFallsBackToStandardWithoutChangingCard() {
        var arts = List.of(new CardArtwork(101, "STANDARD", null), new CardArtwork(102, "EXTENDED_ART", null),
                new CardArtwork(103, "FULL_ART", null), new CardArtwork(104, "ALTERNATE_ART", null));
        var config = config(List.of(type(1, "COMMON")), null,
                Map.of(1, List.of(new DrawCard(11, "Test", new CardReference(1, "COMMON", "Common"), arts))));
        for (var entry : Map.of(809999, "STANDARD", 810000, "EXTENDED_ART", 910000, "FULL_ART", 985000, "ALTERNATE_ART").entrySet()) {
            assertThat(drawWith(0, 0, entry.getKey()).openOnePack(config, null).cards().getFirst().artCode()).isEqualTo(entry.getValue());
        }
        var fallback = drawWith(0, 0, 999999).openOnePack(config(List.of(type(1, "COMMON")), null, CARDS), null).cards().getFirst();
        assertThat(fallback.cardId()).isEqualTo(11);
        assertThat(fallback.cardArtId()).isEqualTo(111);
        assertThat(fallback.artCode()).isEqualTo("STANDARD");
    }

    @Test
    void selectsUniformlyFromTheEntireCardPoolAndAllowsDuplicates() {
        var source = mock(PackDrawSource.class);
        when(source.nextInt(anyInt())).thenAnswer(i -> (int) i.getArgument(0) - 1);
        var config = config(List.of(type(1, "COMMON"), type(1, "COMMON")), null,
                Map.of(1, List.of(card(11, 1, "COMMON"), card(12, 1, "COMMON"))));
        var result = new PackDrawingService(source).openOnePack(config, null);
        assertThat(result.cards()).extracting(OpenPacksResponse.PulledCardView::cardId).containsExactly(12, 12);
        verify(source, times(2)).nextInt(2);
    }

    @Test
    void theSameCardCanAppearWithDifferentArtworksInOnePack() {
        var card = new DrawCard(11, "Test", new CardReference(1, "COMMON", "Common"),
                List.of(new CardArtwork(101, "STANDARD", null), new CardArtwork(102, "EXTENDED_ART", null)));
        var config = config(List.of(type(1, "COMMON"), type(1, "COMMON")), null, Map.of(1, List.of(card)));
        var result = drawWith(0, 0, 0, 0, 0, 810000).openOnePack(config, null);
        assertThat(result.cards()).extracting(OpenPacksResponse.PulledCardView::cardId).containsExactly(11, 11);
        assertThat(result.cards()).extracting(OpenPacksResponse.PulledCardView::artCode).containsExactly("STANDARD", "EXTENDED_ART");
    }

    @Test
    void carriesMercySequentiallyAndReplacesOnlyTheLastSlotAtExactRarity() {
        var service = drawWith();
        var config = config(List.of(type(1, "COMMON"), type(1, "COMMON")), 10, CARDS);
        Integer state = 3;
        for (int expected : new int[]{2, 1, 10, 9}) {
            var result = service.openOnePack(config, state);
            assertThat(result.nextMercy()).isEqualTo(expected);
            assertThat(result.cards()).hasSize(2);
            assertThat(result.cards().getFirst().rarity().code()).isEqualTo("COMMON");
            assertThat(result.cards().getLast().rarity().code()).isEqualTo(expected == 10 ? "EPIC" : "COMMON");
            state = result.nextMercy();
        }
        assertThat(service.openOnePack(config, 0).cards().getLast().rarity().code()).isEqualTo("EPIC");
    }

    @Test
    void naturalHigherRarityInAnEarlierOrLastSlotResetsWithoutReplacement() {
        var config = config(List.of(type(4, "LEGENDARY"), type(1, "COMMON")), 10, CARDS);
        var result = drawWith().openOnePack(config, 1);
        assertThat(result.nextMercy()).isEqualTo(10);
        assertThat(result.cards()).extracting(c -> c.rarity().code()).containsExactly("LEGENDARY", "COMMON");
        result = drawWith().openOnePack(config(List.of(type(1, "COMMON"), type(3, "EPIC")), 10, CARDS), 7);
        assertThat(result.nextMercy()).isEqualTo(10);
        assertThat(result.cards()).extracting(c -> c.rarity().code()).containsExactly("COMMON", "EPIC");
    }

    @Test
    void initializesFromThresholdAndIgnoresInactiveProgress() {
        var service = drawWith();
        assertThat(service.openOnePack(config(List.of(type(1, "COMMON")), 10, CARDS), null).nextMercy()).isEqualTo(9);
        assertThat(service.openOnePack(config(List.of(type(1, "COMMON")), null, CARDS), 0).nextMercy()).isNull();
        assertThatThrownBy(() -> service.openOnePack(config(List.of(type(1, "COMMON")), 10, CARDS), 11))
                .isInstanceOf(PackOpeningFailure.class);
    }

    private static PackDrawingService drawWith(int... rolls) {
        var random = mock(PackDrawSource.class);
        var position = new AtomicInteger();
        when(random.nextInt(anyInt())).thenAnswer(i -> {
            int index = position.getAndIncrement();
            int result = index < rolls.length ? rolls[index] : 0;
            assertThat(result).isBetween(0, (int) i.getArgument(0) - 1);
            return result;
        });
        return new PackDrawingService(random);
    }

    private static PackOpeningConfiguration config(List<SlotPullType> slots, Integer threshold, Map<Integer, List<DrawCard>> cards) {
        return new PackOpeningConfiguration(slots, cards, ART, threshold,
                threshold == null ? null : new CardReference(3, "EPIC", "Epic"));
    }

    private static DrawCard card(int id, int rarity, String code) {
        return new DrawCard(id, "Test " + id, new CardReference(rarity, code, code),
                List.of(new CardArtwork(id + 100, "STANDARD", null)));
    }

    private static SlotPullType type(int rarity, String code) { return new SlotPullType(rarity, "", List.of(rate(rarity, code, "100"))); }
    private static SlotPullType.RarityPullRate rate(int id, String code, String probability) {
        return new SlotPullType.RarityPullRate(id, code, decimal(probability));
    }
    private static BigDecimal decimal(String value) { return new BigDecimal(value); }
}
