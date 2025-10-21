package eu.okaeri.menu.bukkit.unit.navigation;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.navigation.NavigationHistory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for NavigationHistory.
 * Tests navigation stack management and menu history tracking.
 */
class NavigationHistoryTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private PlayerMock player;
    private Menu menu1;
    private Menu menu2;
    private Menu menu3;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
        this.player = this.server.addPlayer();

        // Clear history before each test
        NavigationHistory.clearAll();

        // Create test menus
        this.menu1 = Menu.builder()
            .title("Menu 1")
            .rows(3)
            .plugin(this.plugin)
            .build();

        this.menu2 = Menu.builder()
            .title("Menu 2")
            .rows(3)
            .plugin(this.plugin)
            .build();

        this.menu3 = Menu.builder()
            .title("Menu 3")
            .rows(3)
            .plugin(this.plugin)
            .build();
    }

    @AfterEach
    void tearDown() {
        NavigationHistory.clearAll();
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should have zero depth with no history")
    void testInitialDepth() {
        assertThat(NavigationHistory.depth(this.player)).isEqualTo(0);
        assertThat(NavigationHistory.hasLast(this.player)).isFalse();
    }

    @Test
    @DisplayName("Should track single menu navigation")
    void testSingleNavigation() {
        NavigationHistory.open(this.player, this.menu1);

        assertThat(NavigationHistory.depth(this.player)).isEqualTo(1);
        assertThat(NavigationHistory.hasLast(this.player)).isFalse(); // Only 1 menu, no "last"
    }

    @Test
    @DisplayName("Should track multiple menu navigations")
    void testMultipleNavigations() {
        NavigationHistory.open(this.player, this.menu1);
        NavigationHistory.open(this.player, this.menu2);
        NavigationHistory.open(this.player, this.menu3);

        assertThat(NavigationHistory.depth(this.player)).isEqualTo(3);
        assertThat(NavigationHistory.hasLast(this.player)).isTrue();
    }

    @Test
    @DisplayName("Should retrieve last menu without navigating")
    void testLastMenu() {
        NavigationHistory.open(this.player, this.menu1);
        NavigationHistory.open(this.player, this.menu2);

        NavigationHistory.MenuSnapshot last = NavigationHistory.last(this.player);

        assertThat(last).isNotNull();
        assertThat(last.getMenu()).isEqualTo(this.menu1);
        assertThat(NavigationHistory.depth(this.player)).isEqualTo(2); // Should not modify stack
    }

    @Test
    @DisplayName("Should return null when no last menu exists")
    void testLastMenuWithSingleEntry() {
        NavigationHistory.open(this.player, this.menu1);

        NavigationHistory.MenuSnapshot last = NavigationHistory.last(this.player);

        assertThat(last).isNull();
    }

    @Test
    @DisplayName("Should navigate back through history")
    void testBackNavigation() {
        NavigationHistory.open(this.player, this.menu1);
        NavigationHistory.open(this.player, this.menu2);
        NavigationHistory.open(this.player, this.menu3);

        assertThat(NavigationHistory.depth(this.player)).isEqualTo(3);

        // Go back from menu3 to menu2
        boolean navigated = NavigationHistory.back(this.player);
        assertThat(navigated).isTrue();
        assertThat(NavigationHistory.depth(this.player)).isEqualTo(2);

        // Go back from menu2 to menu1
        navigated = NavigationHistory.back(this.player);
        assertThat(navigated).isTrue();
        assertThat(NavigationHistory.depth(this.player)).isEqualTo(1);

        // No more history, should close
        navigated = NavigationHistory.back(this.player);
        assertThat(navigated).isFalse();
        assertThat(NavigationHistory.depth(this.player)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return false when backing with no history")
    void testBackWithNoHistory() {
        boolean navigated = NavigationHistory.back(this.player);

        assertThat(navigated).isFalse();
    }

    @Test
    @DisplayName("Should store and retrieve menu parameters")
    void testMenuParameters() {
        Map<String, Object> params = Map.of(
            "category", "weapons",
            "page", 1,
            "filter", true
        );

        NavigationHistory.open(this.player, this.menu1, params);
        NavigationHistory.open(this.player, this.menu2); // Open another to make menu1 "last"

        NavigationHistory.MenuSnapshot last = NavigationHistory.last(this.player);

        assertThat(last).isNotNull();
        assertThat(last.getParams()).isEqualTo(params);
        assertThat(last.getParam("category")).isEqualTo("weapons");
        assertThat(last.getParam("page")).isEqualTo(1);
        assertThat(last.getParam("filter")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should retrieve typed parameters")
    void testTypedParameters() {
        Map<String, Object> params = Map.of(
            "category", "weapons",
            "page", 42
        );

        NavigationHistory.open(this.player, this.menu1, params);
        NavigationHistory.open(this.player, this.menu2); // Open another to make menu1 "last"

        NavigationHistory.MenuSnapshot last = NavigationHistory.last(this.player);

        assertThat(last).isNotNull();
        assertThat(last.getParam("category", String.class)).isEqualTo("weapons");
        assertThat(last.getParam("page", Integer.class)).isEqualTo(42);
        assertThat(last.getParam("nonexistent", String.class)).isNull();
        assertThat(last.getParam("category", Integer.class)).isNull(); // Wrong type
    }

    @Test
    @DisplayName("Should handle null parameters")
    void testNullParameters() {
        NavigationHistory.open(this.player, this.menu1, null);

        NavigationHistory.MenuSnapshot last = NavigationHistory.last(this.player);
        // With only 1 menu, last should be null
        assertThat(last).isNull();

        // Add another menu to get a snapshot
        NavigationHistory.open(this.player, this.menu2);
        last = NavigationHistory.last(this.player);

        assertThat(last).isNotNull();
        assertThat(last.getParams()).isEmpty();
    }

    @Test
    @DisplayName("Should clear history for specific player")
    void testClearPlayerHistory() {
        PlayerMock player2 = this.server.addPlayer("Player2");

        NavigationHistory.open(this.player, this.menu1);
        NavigationHistory.open(this.player, this.menu2);
        NavigationHistory.open(player2, this.menu1);

        assertThat(NavigationHistory.depth(this.player)).isEqualTo(2);
        assertThat(NavigationHistory.depth(player2)).isEqualTo(1);

        NavigationHistory.clear(this.player);

        assertThat(NavigationHistory.depth(this.player)).isEqualTo(0);
        assertThat(NavigationHistory.depth(player2)).isEqualTo(1); // Unaffected
    }

    @Test
    @DisplayName("Should clear all navigation history")
    void testClearAllHistory() {
        PlayerMock player2 = this.server.addPlayer("Player2");

        NavigationHistory.open(this.player, this.menu1);
        NavigationHistory.open(player2, this.menu2);

        assertThat(NavigationHistory.depth(this.player)).isEqualTo(1);
        assertThat(NavigationHistory.depth(player2)).isEqualTo(1);

        NavigationHistory.clearAll();

        assertThat(NavigationHistory.depth(this.player)).isEqualTo(0);
        assertThat(NavigationHistory.depth(player2)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should maintain independent history per player")
    void testIndependentPlayerHistory() {
        PlayerMock player2 = this.server.addPlayer("Player2");

        NavigationHistory.open(this.player, this.menu1);
        NavigationHistory.open(this.player, this.menu2);
        NavigationHistory.open(player2, this.menu3);

        assertThat(NavigationHistory.depth(this.player)).isEqualTo(2);
        assertThat(NavigationHistory.depth(player2)).isEqualTo(1);

        NavigationHistory.back(this.player);

        assertThat(NavigationHistory.depth(this.player)).isEqualTo(1);
        assertThat(NavigationHistory.depth(player2)).isEqualTo(1); // Unaffected
    }

    @Test
    @DisplayName("Should return immutable parameters map")
    void testImmutableParameters() {
        Map<String, Object> params = Map.of("key", "value");

        NavigationHistory.open(this.player, this.menu1, params);
        NavigationHistory.open(this.player, this.menu2); // Open another to make menu1 "last"

        NavigationHistory.MenuSnapshot last = NavigationHistory.last(this.player);

        assertThat(last).isNotNull();

        // Should be unmodifiable
        Map<String, Object> retrieved = last.getParams();
        assertThat(retrieved).containsEntry("key", "value");

        // Verify it's actually unmodifiable by attempting modification
        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> retrieved.put("new", "value")
        );
    }

    @Test
    @DisplayName("Should handle deep navigation chains")
    void testDeepNavigationChain() {
        // Create a deep stack
        for (int i = 0; i < 10; i++) {
            Menu menu = Menu.builder()
                .title("Menu " + i)
                .rows(3)
                .plugin(this.plugin)
                .build();
            NavigationHistory.open(this.player, menu);
        }

        assertThat(NavigationHistory.depth(this.player)).isEqualTo(10);

        // Navigate back through entire chain
        for (int i = 9; i > 0; i--) {
            boolean navigated = NavigationHistory.back(this.player);
            assertThat(navigated).isTrue();
            assertThat(NavigationHistory.depth(this.player)).isEqualTo(i);
        }

        // Last back should close and return false
        boolean navigated = NavigationHistory.back(this.player);
        assertThat(navigated).isFalse();
        assertThat(NavigationHistory.depth(this.player)).isEqualTo(0);
    }
}
