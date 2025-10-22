package eu.okaeri.menu.bukkit.unit.message;

import eu.okaeri.menu.message.DefaultMessageProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.HumanEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for DefaultMessageProvider.
 * Tests universal message support (§, &, MiniMessage) and placeholder replacement.
 */
class DefaultMessageProviderTest {

    private DefaultMessageProvider provider;
    private HumanEntity player;

    @BeforeEach
    void setUp() {
        this.provider = new DefaultMessageProvider();
        this.player = mock(HumanEntity.class);
    }

    @Test
    @DisplayName("Should parse simple text")
    void testSimpleText() {
        Component result = this.provider.resolve(this.player, "Hello World", Map.of());

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("Should support § color codes")
    void testSectionColorCodes() {
        Component result = this.provider.resolve(this.player, "§cRed §bBlue §aGreen", Map.of());

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("Red Blue Green");
        assertThat(result.children()).isNotEmpty(); // Has color changes
    }

    @Test
    @DisplayName("Should support & color codes")
    void testAmpersandColorCodes() {
        Component result = this.provider.resolve(this.player, "&cRed &bBlue &aGreen", Map.of());

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("Red Blue Green");
    }

    @Test
    @DisplayName("Should support MiniMessage tags")
    void testMiniMessageTags() {
        Component result = this.provider.resolve(this.player, "<red>Red <blue>Blue <green>Green", Map.of());

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("Red Blue Green");
    }

    @Test
    @DisplayName("Should mix § and & codes")
    void testMixedLegacyCodes() {
        Component result = this.provider.resolve(this.player, "§cSection &bAmpersand", Map.of());

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("Section Ampersand");
    }

    @Test
    @DisplayName("Should mix legacy codes and MiniMessage tags")
    void testMixedFormats() {
        Component result = this.provider.resolve(this.player, "§cLegacy <gradient:red:blue>MiniMessage</gradient> &aMore", Map.of());

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("Legacy MiniMessage More");
    }

    @Test
    @DisplayName("Should optimize pure legacy messages (no MiniMessage)")
    void testLegacyOptimization() {
        // Pure legacy message without < or > should use fast path
        Component result = this.provider.resolve(this.player, "§cRed &bBlue", Map.of());

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("Red Blue");
    }

    @Test
    @DisplayName("Should replace placeholders")
    void testPlaceholderReplacement() {
        Component result = this.provider.resolve(this.player, "Hello <name>!", Map.of("name", "Steve"));

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("Hello Steve!");
    }

    @Test
    @DisplayName("Should replace multiple placeholders")
    void testMultiplePlaceholders() {
        Component result = this.provider.resolve(this.player,
            "Player <name> has <coins> coins",
            Map.of("name", "Steve", "coins", 100)
        );

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("Player Steve has 100 coins");
    }

    @Test
    @DisplayName("Should keep placeholder if no value provided")
    void testMissingPlaceholder() {
        Component result = this.provider.resolve(this.player, "Hello <name>!", Map.of());

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("Hello <name>!");
    }

    @Test
    @DisplayName("Should handle empty template")
    void testEmptyTemplate() {
        Component result = this.provider.resolve(this.player, "", Map.of());

        assertThat(result).isEqualTo(Component.empty());
    }

    @Test
    @DisplayName("Should NOT resolve i18n keys")
    void testI18nNotResolved() {
        Component result = this.provider.resolve(this.player, "${some.key}", Map.of());

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("${some.key}");
    }

    @Test
    @DisplayName("Should resolve list of templates")
    void testResolveList() {
        List<Component> result = this.provider.resolveList(
            this.player,
            List.of("Line 1: <value>", "Line 2: <value>"),
            Map.of("value", "test")
        );

        assertThat(result).hasSize(2);

        String line1 = PlainTextComponentSerializer.plainText().serialize(result.get(0));
        String line2 = PlainTextComponentSerializer.plainText().serialize(result.get(1));

        assertThat(line1).isEqualTo("Line 1: test");
        assertThat(line2).isEqualTo("Line 2: test");
    }

    @Test
    @DisplayName("Should escape MiniMessage tags in placeholder values")
    void testPlaceholderEscaping() {
        // Placeholder value contains MiniMessage tag - should be escaped
        Component result = this.provider.resolve(this.player,
            "Message: <msg>",
            Map.of("msg", "<red>Dangerous</red>")
        );

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        // The <red> tags should be escaped and appear as plain text
        assertThat(plain).isEqualTo("Message: <red>Dangerous</red>");
    }

    @Test
    @DisplayName("Should handle numeric placeholder values")
    void testNumericPlaceholders() {
        Component result = this.provider.resolve(this.player,
            "You have <coins> coins and <level> level",
            Map.of("coins", 100, "level", 5)
        );

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertThat(plain).isEqualTo("You have 100 coins and 5 level");
    }
}
