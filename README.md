# Okaeri Menu

Modern, reactive GUI framework for Minecraft (Paper) with pane-based architecture, async data loading, and advanced filtering.

## Features

- **Pane-Based Architecture**: Organize menus into reusable, composable panes with precise bounds
- **Reactive Properties**: Menu items automatically update when data changes
- **Async Support**: Load data asynchronously with built-in loading/error/empty states
- **Smart Pagination**: Filter, sort, and paginate large datasets with database-side filtering
- **Modern API**: Fluent builders, lambdas, and type-safe design
- **i18n Ready**: Built-in MiniMessage support with multi-locale text formatting

## Requirements

### Platform
- **Paper 1.21+** (or any modern Paper fork like Purpur)
  - Older Paper versions may work but are not officially supported
  - Some features may behave differently or break on older versions
  - Test thoroughly if using older versions
- **Spigot/Bukkit**: Not officially supported
  - Adventure API must be shaded and relocated into your plugin
  - See [Adventure Platform Bukkit](https://docs.advntr.dev/platform/bukkit.html) for setup
  - Relocation of Adventure is **required** to avoid conflicts

### Java
- **Java 21 or higher** required

## Installation

![Version](https://img.shields.io/badge/version-2.0.1--beta.1-blue.svg)
![Java](https://img.shields.io/badge/java-21-orange.svg)
![Paper](https://img.shields.io/badge/platform-paper-00ADD8.svg)

### Maven
```xml
<repositories>
    <repository>
        <id>okaeri-repo</id>
        <url>https://storehouse.okaeri.eu/repository/maven-public/</url>
    </repository>
</repositories>
```
```xml
<dependency>
    <groupId>eu.okaeri</groupId>
    <artifactId>okaeri-menu-bukkit</artifactId>
    <version>2.0.1-beta.3</version>
</dependency>
```

### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
}
```
```kotlin
dependencies {
    implementation("eu.okaeri:okaeri-menu-bukkit:2.0.1-beta.3")
}
```

## Quick Start

### Simple Static Menu

```java
import eu.okaeri.menu.Menu;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;

public class SimpleMenuExample {

    public static Menu createMenu(Plugin plugin) {
        return Menu.builder(plugin)
            .title("<green>My First Menu")
            .rows(3)  // Optional - auto-calculated from pane bounds if omitted
            .pane(staticPane("main")
                .bounds(0, 0, 3, 9)  // row, col, height, width
                .item(1, 4, item()   // row, col
                    .material(Material.DIAMOND)
                    .name("<aqua><b>Click Me!")
                    .lore("""
                        <gray>This is a clickable item
                        <yellow>Try it out!""")
                    .onClick(ctx -> {
                        ctx.sendMessage("<green>You clicked the diamond!");
                        ctx.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                    })
                    .build())
                .item(2, 8, item()   // row, col
                    .material(Material.BARRIER)
                    .name("<red>Close")
                    .onClick(ctx -> ctx.closeInventory())
                    .build())
                .build())
            .build();
    }
}
```

### Reactive Menu with Dynamic Updates

```java
public static Menu createReactiveMenu(Plugin plugin) {
    return Menu.builder(plugin)
        .title("<yellow>Reactive Menu")
        // .state(s -> s.define("clickCount", 0))  // Optional - integers default to 0
        .pane(staticPane("main")
            .bounds(0, 0, 3, 9)  // row, col, height, width
            .item(1, 4, item()   // row, col
                .material(ctx -> (ctx.getInt("clickCount") > 10) ? Material.DIAMOND : Material.COAL)
                .name("<gold>Clicks: <count>")
                .vars(ctx -> Map.of("count", ctx.getInt("clickCount")))
                .lore(ctx -> (ctx.getInt("clickCount") > 10)
                    ? "<green>You're a clicking master!"
                    : "<gray>Keep clicking...")
                .onClick(ctx -> {
                    ctx.set("clickCount", ctx.getInt("clickCount") + 1);
                    // No refresh needed - state changes automatically trigger refresh on next tick!
                })
                .build())
            .build())
        .build();
}
```

**Automatic Refresh:** State changes (`ctx.set()`), async data updates, and pagination changes automatically trigger refresh on the next tick. Manual `ctx.refresh()` is only needed for immediate updates within the same tick.

**Update Interval:** Use `.updateInterval(Duration)` only when you need periodic polling of external state (e.g., checking balance from database every 5 seconds). It's not required for reactive state changes!

### Opening Menus

```java
// Immediate open (shows loading states for async components)
menu.open(player);

// Async open with data preloading - wait up to 5 seconds
menu.open(player, Duration.ofSeconds(5))
    .thenAccept(result -> {
        if (result.isSuccess()) {
            player.sendMessage("Menu opened!");

            // Check data quality
            if (result.isFullyLoaded()) {
                player.sendMessage("All data loaded successfully!");
            } else if (result.getStatus() == OpenStatus.TIMEOUT) {
                player.sendMessage("Menu opened on timeout (partial data)");
            }
        } else {
            player.sendMessage("Failed to open menu: " + result.getStatus());
        }
    });

// Simple usage - just check success
menu.open(player, Duration.ofSeconds(5))
    .thenAccept(result -> {
        if (result.isSuccess()) {
            player.sendMessage("Menu opened in " + result.getElapsed().toMillis() + "ms");
        }
    });

// Blocking usage (not recommended for main thread)
MenuOpenResult result = menu.open(player, Duration.ofSeconds(5)).join();
if (result.getStatus() == OpenStatus.PRELOADED) {
    player.sendMessage("All data preloaded!");
}
```

## Advanced Features

### Per-Player State Management

Each player viewing a menu has independent state. Use the state API to store per-player data like filter toggles, counters, or selections.

```java
Menu.builder(plugin)
    .title("<yellow>Stateful Menu")
    // Define menu-level defaults (optional)
    .state(s -> s
        .define("clicks", 0)
        .define("favoriteColor", "blue")
        .define("premium", false))
    .pane("main", staticPane()
        .bounds(0, 0, 3, 9)  // row, col, height, width
        .item(1, 4, item()   // row, col
            // Get state with automatic defaults
            .name(ctx -> "<gold>Clicks: " + ctx.getInt("clicks"))
            .onClick(ctx -> {
                // Set state - automatically triggers refresh on next tick
                ctx.set("clicks", ctx.getInt("clicks") + 1);
            })
            .build())
        .build())
    .build();
```

**State API Methods:**
```java
// Primitive convenience methods (with automatic defaults)
int clicks = ctx.getInt("clicks");              // Returns 0 if not set
boolean premium = ctx.getBool("premium");       // Returns false if not set
String name = ctx.getString("name");            // Returns null if not set

// Generic get/set
Integer value = ctx.get("key", Integer.class);
ctx.set("key", 42);

// Collections
List<String> names = ctx.getList("names");
Map<String, Integer> scores = ctx.getMap("scores");

// Check if explicitly set (not just default)
if (ctx.has("customValue")) {
    // Player has explicitly set this value
}

// Remove state (reverts to default)
ctx.remove("temporaryData");
```

**Default Value Priority:**
1. Explicitly set value via `ctx.set()`
2. Menu-level default from `.state()` block
3. Automatic primitive default (0, false, etc.)
4. null for non-primitive types

**Automatic Refresh Behavior:**

State changes automatically trigger refreshes on the next tick:

- **State changes** (`ctx.set()`) → refresh on next tick
- **Pagination changes** (page navigation, filters) → refresh on next tick

This means you typically don't need manual `ctx.refresh()` calls. The menu automatically updates when data changes!

**When to use updateInterval:**
Use `.updateInterval(Duration)` only for periodic re-evaluation of properties that directly access external state without async caching. For example: `ctx -> player.getLevel()`.

For async reactive polling (like `menu.reactive(() -> database.getBalance(), Duration.ofSeconds(5))`), you **don't need** `updateInterval` - the menu automatically refreshes when TTL expires!

### Pagination with Filtering

```java
import static eu.okaeri.menu.pane.PaginatedPane.pane;

public static Menu createShopMenu(Plugin plugin) {
    return Menu.builder(plugin)
        .title("<yellow>⚡ Shop")
        .pane(pane("items", ShopItem.class)
            .bounds(1, 0, 4, 9)  // row, col, height, width
            .items(() -> loadShopItems())  // Your data source
            .renderer((ctx, item, index) -> item()
                .material(item.getMaterial())
                .name("<yellow><name>")
                .vars(Map.of(
                    "name", item.getName(),
                    "price", item.getPrice()
                ))
                .lore("<gray>Price: <gold><price> coins")
                .onClick(ctx -> purchaseItem(ctx.getEntity(), item))
                .build())
            .build())
        .pane(staticPane("controls")
            .bounds(5, 0, ". . < . I . > . .")  // Template: dimensions auto-derived
            .item('<', previousPageButton("items").build())
            .item('I', pageIndicator("items").build())
            .item('>', nextPageButton("items").build())
            .build())
        .build();
}
```

### Async Data Loading

```java
import static eu.okaeri.menu.pane.AsyncPaginatedPane.paneAsync;

public static Menu createAsyncShopMenu(Plugin plugin) {
    return Menu.builder(plugin)
        .title("<yellow>⚡ Async Shop")
        .pane(paneAsync("items", ShopItem.class)
            .bounds(1, 0, 4, 9)  // row, col, height, width
            .loader(ctx -> {
                // Runs async - fetch from database
                int page = ctx.getCurrentPage();
                int pageSize = ctx.getPageSize();

                // NOTE: Inline SQL for demonstration - use your actual database layer
                return database.query("""
                    SELECT * FROM shop_items
                    LIMIT ? OFFSET ?
                    """, pageSize + 1, page * pageSize);
            })
            .ttl(Duration.ofSeconds(30))  // Cache for 30 seconds
            .renderer((ctx, item, index) -> item()
                .material(item.getMaterial())
                .name("<yellow><name>")
                .vars(Map.of(
                    "name", item.getName(),
                    "price", item.getPrice()
                ))
                .lore("<gray>Price: <gold><price> coins")
                .build())
            .loading(loadingItem(Material.HOPPER, "<gray>Loading...").build())
            .error(errorItem(Material.BARRIER, "<red>Failed to load").build())
            .empty(emptyItem(Material.CHEST, "<gray>No items").build())
            .build())
        .build();
}
```

### Filtering Items

```java
// Filters are declared on menu items (not on the pane)
// State is per-player - each viewer has independent filter state
// No .state() block needed - booleans default to false
Menu.builder(plugin)
    .title("<yellow>Shop with Filters")
    // Filter controls - toggle items with declarative filters
    .pane(staticPane("controls")
        .bounds(0, 0, 1, 9)  // row, col, height, width
        .item(0, 2, item()   // row, col
            .material(ctx -> ctx.getBool("weaponFilter") ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD)
            .name(ctx -> ctx.getBool("weaponFilter") ? "<green>✓ Weapons" : "<gray>Weapons")
            .lore("<gray>Show weapons only\n<yellow>Click to toggle!")
            // Declarative filter - attached to this menu item
            .filter(ItemFilter.<ShopItem>builder()
                .target("items")  // Target pane name
                .id("weapon")
                .when(ctx -> ctx.getBool("weaponFilter"))  // Active when true
                .predicate(item -> item.getCategory().equals("weapon"))
                .build())
            .onClick(ctx -> {
                ctx.set("weaponFilter", !ctx.getBool("weaponFilter"));
                ctx.playSound(Sound.UI_BUTTON_CLICK);
            })
            .build())
        .item(0, 4, item()   // row, col
            .material(ctx -> ctx.getBool("expensiveFilter") ? Material.GOLD_INGOT : Material.IRON_INGOT)
            .name(ctx -> ctx.getBool("expensiveFilter") ? "<green>✓ Expensive" : "<gray>Expensive")
            .lore("<gray>Show items over 100 coins\n<yellow>Click to toggle!")
            .filter(ItemFilter.<ShopItem>builder()
                .target("items")
                .id("expensive")
                .when(ctx -> ctx.getBool("expensiveFilter"))
                .predicate(item -> item.getPrice() > 100)
                .build())
            .onClick(ctx -> {
                ctx.set("expensiveFilter", !ctx.getBool("expensiveFilter"));
                ctx.playSound(Sound.UI_BUTTON_CLICK);
            })
            .build())
        .build())
    // Items pane - filters are applied automatically
    .pane(pane("items" ShopItem.class)
        .bounds(1, 0, 4, 9)  // row, col, height, width
        .items(() -> loadShopItems())
        .renderer((ctx, item, index) -> item()
            .material(item.getMaterial())
            .name("<yellow><name>")
            .vars(Map.of("name", item.getName(), "price", item.getPrice()))
            .lore("<gray>Price: <gold><price> coins")
            .build())
        .build())
    .build();
```

### Database-Side Filtering with Value Filters

For async loaders, use value-only filters to pass filter parameters to your database queries without loading all data into memory first.

```java
// No .state() block needed - primitives have automatic defaults (false, 0)
Menu.builder(plugin)
    .title("<yellow>Database-Filtered Shop")
    // Filter controls with value-only filters
    .pane(staticPane("controls")
        .bounds(0, 0, 1, 9)  // row, col, height, width
        .item(0, 2, item()   // row, col
            .material(ctx -> ctx.getBool("weaponFilter") ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD)
            .name(ctx -> ctx.getBool("weaponFilter") ? "<green>✓ Weapons" : "<gray>Weapons")
            .lore("<gray>Filter by category\n<yellow>Click to toggle!")
            // Value-only filter (no predicate) - passes value to database query
            .filter(ItemFilter.<ShopItem>builder()
                .target("items")
                .id("category")
                .when(ctx -> ctx.getBool("weaponFilter"))
                .value(() -> "WEAPONS")  // Value passed to loader
                .build())
            .onClick(ctx -> ctx.set("weaponFilter", !ctx.getBool("weaponFilter")))
            .build())
        .item(0, 4, item()   // row, col
            .material(Material.GOLD_INGOT)
            .name("<yellow>Min Price: <white><price>")
            .vars(ctx -> Map.of("price", ctx.getInt("minPrice")))
            .lore("<gray>Filter by minimum price\n<yellow>Click: +50 | Shift+Click: Reset")
            .filter(ItemFilter.<ShopItem>builder()
                .target("items")
                .id("minPrice")
                .when(ctx -> ctx.getInt("minPrice") > 0)
                .value(ctx -> ctx.getInt("minPrice"))  // Integer value
                .build())
            .onClick(ctx -> {
                if (ctx.isShiftClick()) {
                    ctx.set("minPrice", 0);
                } else {
                    ctx.set("minPrice", ctx.getInt("minPrice") + 50);
                }
            })
            .build())
        .build())
    // Async pane - filters are extracted in loader
    .pane(paneAsync("items", ShopItem.class)
        .bounds(1, 0, 4, 9)  // row, col, height, width
        .loader(ctx -> {
            // Extract filter values from context
            String category = ctx.getFilter("category", String.class).orElse(null);
            Integer minPrice = ctx.getFilter("minPrice", Integer.class).orElse(0);

            // Build database query with filters
            // NOTE: Use your actual database layer, not inline SQL
            StringBuilder query = new StringBuilder("SELECT * FROM shop_items WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (category != null) {
                query.append(" AND category = ?");
                params.add(category);
            }
            if (minPrice > 0) {
                query.append(" AND price >= ?");
                params.add(minPrice);
            }

            query.append(" LIMIT ? OFFSET ?");
            params.add(ctx.getPageSize() + 1);
            params.add(ctx.getCurrentPage() * ctx.getPageSize());

            return database.query(query.toString(), params.toArray());
        })
        .ttl(Duration.ofSeconds(30))
        .renderer((ctx, item, index) -> item()
            .material(item.getMaterial())
            .name("<yellow><name>")
            .vars(Map.of("name", item.getName(), "price", item.getPrice()))
            .lore("<gray>Price: <gold><price> coins")
            .build())
        .build())
    .build();
```

**Key concepts:**
- **Value-only filters**: Use `.value(() -> ...)` without `.predicate()` on the filter
- **LoaderContext API**: Access filter values in your loader with `ctx.getFilter(id, Type.class)`
- **Database queries**: Use filter values to build WHERE clauses for efficient database-side filtering
- **Type-safe**: Filter values are retrieved with type checking (`String.class`, `Integer.class`, etc.)

### Custom Filter Strategies

```java
// Built-in strategies
pagination.setFilterStrategy(FilterStrategy.AND);  // All filters must match
pagination.setFilterStrategy(FilterStrategy.OR);   // Any filter can match

// Custom strategy
public class PriorityFilterStrategy implements FilterStrategy {
    @Override
    public <T> Predicate<T> combine(Collection<ItemFilter<T>> filters) {
        return item -> {
            // Required filters must ALL match
            boolean requiredPass = filters.stream()
                .filter(ItemFilter::isActive)
                .filter(f -> f.getFilterId().startsWith("required-"))
                .allMatch(f -> f.test(item));

            // Optional filters - ANY must match
            boolean optionalPass = filters.stream()
                .filter(ItemFilter::isActive)
                .filter(f -> f.getFilterId().startsWith("optional-"))
                .anyMatch(f -> f.test(item));

            return requiredPass && optionalPass;
        };
    }

    @Override
    public String getName() {
        return "PRIORITY";
    }
}
```

## Examples

Check out [test-plugin](test-plugin/src/main/java/eu/okaeri/menu/test/example) for comprehensive examples:

- **[SimpleMenuExample.java](test-plugin/src/main/java/eu/okaeri/menu/test/example/SimpleMenuExample.java)** - Basic static menus
- **[PaginationExample.java](test-plugin/src/main/java/eu/okaeri/menu/test/example/PaginationExample.java)** - Pagination and filtering
- **[AsyncShopExample.java](test-plugin/src/main/java/eu/okaeri/menu/test/example/AsyncShopExample.java)** - Async loading, filters, and statistics

---

## "Nooo, I can't use Paper!" 😱

Relax. It's annoying, but here's how to make it _probably_ work on Spigot/Bukkit:

**Maven:**
```xml
<dependencies>
    <dependency>
        <groupId>net.kyori</groupId>
        <artifactId>adventure-platform-bukkit</artifactId>
        <version>4.3.2</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals><goal>shade</goal></goals>
                </execution>
            </executions>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>net.kyori</pattern>
                        <shadedPattern>com.yourplugin.libs.adventure</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Gradle (Kotlin DSL):**
```kotlin
plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation("net.kyori:adventure-platform-bukkit:4.3.2")
}

tasks.shadowJar {
    relocate("net.kyori", "com.yourplugin.libs.adventure")
}
```

Replace `com.yourplugin` with your plugin's package name. Relocation prevents conflicts with other plugins.
