package eu.okaeri.menu.item;

import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.async.AsyncLoader;
import eu.okaeri.menu.pagination.ItemFilter;
import eu.okaeri.menu.reactive.ReactiveProperty;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a menu item with reactive properties.
 * All properties can be static or dynamic (supplier-based).
 */
@Getter
public class MenuItem {

    private final ReactiveProperty<ItemStack> baseItem;
    private final ReactiveProperty<Material> material;
    private final ReactiveProperty<Component> name;
    private final ReactiveProperty<List<Component>> lore;
    private final ReactiveProperty<Integer> amount;
    private final ReactiveProperty<Boolean> visible;
    private final ReactiveProperty<Boolean> glint;
    private final ReactiveProperty<Boolean> hideAttributes;
    private final Map<Enchantment, Integer> enchantments = new HashMap<>();
    private final List<ItemFlag> itemFlags = new ArrayList<>();

    // Handlers
    private Consumer<MenuItemClickContext> clickHandler;
    private Consumer<MenuItemClickContext> rightClickHandler;
    private Consumer<MenuItemClickContext> leftClickHandler;
    private Consumer<MenuItemClickContext> middleClickHandler;

    // Interactive slot properties
    private final boolean allowPickup;
    private final boolean allowPlacement;
    private Consumer<MenuItemChangeContext> itemChangeHandler;

    // Declarative filters for pagination
    private final List<ItemFilter<?>> filters = new ArrayList<>();

    protected MenuItem(Builder builder) {
        this.baseItem = builder.baseItem;
        this.material = builder.material;
        this.name = builder.name;
        this.lore = builder.lore;
        this.amount = builder.amount;
        this.visible = builder.visible;
        this.glint = builder.glint;
        this.hideAttributes = builder.hideAttributes;
        this.enchantments.putAll(builder.enchantments);
        this.itemFlags.addAll(builder.itemFlags);
        this.clickHandler = builder.clickHandler;
        this.rightClickHandler = builder.rightClickHandler;
        this.leftClickHandler = builder.leftClickHandler;
        this.middleClickHandler = builder.middleClickHandler;
        this.allowPickup = builder.allowPickup;
        this.allowPlacement = builder.allowPlacement;
        this.itemChangeHandler = builder.itemChangeHandler;
        this.filters.addAll(builder.filters);
    }

    /**
     * Renders this menu item to an ItemStack.
     *
     * @param context The reactive context
     * @return The rendered ItemStack, or null if not visible
     */
    public ItemStack render(@NonNull MenuContext context) {
        if (!this.visible.get(context)) {
            return null;
        }

        ItemStack base = this.baseItem.get(context);
        ItemStack stack;

        if (base != null && base.getType() != Material.AIR) {
            // Start from base ItemStack (preserves NBT, meta, custom model data, etc.)
            stack = base.clone();

            // Override material if explicitly set
            Material explicitMaterial = this.material.get(context);
            if (explicitMaterial != null && explicitMaterial != Material.AIR) {
                stack.setType(explicitMaterial);
            }
        } else {
            // No base - create from material
            Material mat = this.material.get(context);
            if ((mat == null) || (mat == Material.AIR)) {
                return null;
            }
            stack = new ItemStack(mat);
        }

        stack.setAmount(Math.max(1, Math.min(64, this.amount.get(context))));

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            // Override name if set (Component.empty() means keep base name)
            Component displayName = this.name.get(context);
            if ((displayName != null) && !displayName.equals(Component.empty())) {
                meta.displayName(displayName);
            }

            // Override lore if set (empty list means keep base lore)
            List<Component> loreLines = this.lore.get(context);
            if ((loreLines != null) && !loreLines.isEmpty()) {
                meta.lore(loreLines);
            }

            // Add enchantments (merge with base enchantments)
            for (Map.Entry<Enchantment, Integer> entry : this.enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }

            // Add item flags (merge with base flags)
            if (!this.itemFlags.isEmpty()) {
                meta.addItemFlags(this.itemFlags.toArray(new ItemFlag[0]));
            }

            // Glowing effect
            if (this.glint.get(context) && this.enchantments.isEmpty()) {
                meta.setEnchantmentGlintOverride(true);
            }

            // Hide attributes
            if (this.hideAttributes.get(context)) {
                meta.addItemFlags(Arrays.stream(ItemFlag.values())
                    .filter(it -> it.name().startsWith("HIDE_"))
                    .toArray(ItemFlag[]::new));
            }

            stack.setItemMeta(meta);
        }

        return stack;
    }

    /**
     * Invalidates all reactive properties, forcing re-evaluation on next render.
     */
    public void invalidate() {
        this.material.invalidate();
        this.name.invalidate();
        this.lore.invalidate();
        this.amount.invalidate();
        this.visible.invalidate();
        this.glint.invalidate();
    }

    /**
     * Handles a click event on this item.
     *
     * @param context The click context
     */
    public void handleClick(@NonNull MenuItemClickContext context) {
        if (this.clickHandler != null) {
            this.clickHandler.accept(context);
        }

        if (context.isLeftClick() && (this.leftClickHandler != null)) {
            this.leftClickHandler.accept(context);
        } else if (context.isRightClick() && (this.rightClickHandler != null)) {
            this.rightClickHandler.accept(context);
        } else if (context.isMiddleClick() && (this.middleClickHandler != null)) {
            this.middleClickHandler.accept(context);
        }
    }

    /**
     * Checks if this item is interactive (allows item placement/pickup).
     *
     * @return true if the item allows any interaction
     */
    public boolean isInteractive() {
        return this.allowPickup || this.allowPlacement;
    }

    /**
     * Checks if this item should be re-rendered on refresh.
     * Interactive items should not be re-rendered as they manage their own state.
     *
     * @return true if the item should be re-rendered
     */
    public boolean shouldRender() {
        return !this.isInteractive();
    }

    /**
     * Handles an item change event on this interactive slot.
     *
     * @param context The change context
     */
    public void handleItemChange(@NonNull MenuItemChangeContext context) {
        if (this.itemChangeHandler != null) {
            this.itemChangeHandler.accept(context);
        }
    }

    @NonNull
    public static Builder item() {
        return new Builder();
    }

    /**
     * Creates an empty placeholder item (AIR material).
     * Useful for empty slots in menus where no item should be displayed.
     *
     * @return An empty MenuItem with AIR material
     */
    @NonNull
    public static MenuItem empty() {
        return new Builder().material(Material.AIR).build();
    }

    /**
     * Creates a builder for AsyncMenuItem with async data loading and suspense states.
     * Convenient shorthand for AsyncMenuItem.async().
     *
     * @return AsyncMenuItem builder
     */
    @NonNull
    public static AsyncMenuItem.Builder itemAsync() {
        return AsyncMenuItem.itemAsync();
    }

    public static class Builder {
        private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

        private ReactiveProperty<ItemStack> baseItem = ReactiveProperty.of((ItemStack) null);
        private ReactiveProperty<Material> material = ReactiveProperty.of(Material.AIR);
        private ReactiveProperty<Component> name = ReactiveProperty.of(Component.empty());
        private ReactiveProperty<List<Component>> lore = ReactiveProperty.of(new ArrayList<>());
        private ReactiveProperty<Integer> amount = ReactiveProperty.of(1);
        private ReactiveProperty<Boolean> visible = ReactiveProperty.of(true);
        private ReactiveProperty<Boolean> glint = ReactiveProperty.of(false);
        private ReactiveProperty<Boolean> hideAttributes = ReactiveProperty.of(true);  // Default true for clean appearance
        private Map<Enchantment, Integer> enchantments = new HashMap<>();
        private List<ItemFlag> itemFlags = new ArrayList<>();
        private Consumer<MenuItemClickContext> clickHandler;
        private Consumer<MenuItemClickContext> rightClickHandler;
        private Consumer<MenuItemClickContext> leftClickHandler;
        private Consumer<MenuItemClickContext> middleClickHandler;

        // Interactive slot properties
        private boolean allowPickup = false;
        private boolean allowPlacement = false;
        private Consumer<MenuItemChangeContext> itemChangeHandler;

        // Item-level variables shared across name, lore, etc.
        private ReactiveProperty<Map<String, Object>> itemLevelVars = ReactiveProperty.of(Map.of());

        // Declarative filters
        private List<ItemFilter<?>> filters = new ArrayList<>();

        // Track if display properties were explicitly set (for interactive item validation)
        private boolean materialExplicitlySet = false;
        private boolean nameExplicitlySet = false;
        private boolean loreExplicitlySet = false;

        // Reactive data sources for async loading
        private final AsyncLoader asyncLoader = new AsyncLoader();

        // ========================================
        // HELPER METHODS (INTERNAL)
        // ========================================

        /**
         * Merges item-level variables with method-level variables.
         * Method-level variables override item-level variables if keys conflict.
         *
         * @param ctx        The menu context for evaluating reactive vars
         * @param methodVars Method-level variables (can be null or empty)
         * @return Merged variables map
         */
        @NonNull
        private Map<String, Object> mergeVars(@NonNull MenuContext ctx, Map<String, Object> methodVars) {
            Map<String, Object> itemVars = this.itemLevelVars.get(ctx);
            if (itemVars.isEmpty()) {
                return (methodVars != null) ? methodVars : Map.of();
            }
            if ((methodVars == null) || methodVars.isEmpty()) {
                return itemVars;
            }
            Map<String, Object> merged = new HashMap<>(itemVars);
            merged.putAll(methodVars);  // Method-level overrides
            return merged;
        }

        /**
         * Helper method to resolve a template using the menu's MessageProvider.
         *
         * @param ctx        The menu context
         * @param template   The template string to resolve
         * @param methodVars Optional method-level variables (can be null)
         * @return List of resolved Components
         */
        @NonNull
        private List<Component> resolve(@NonNull MenuContext ctx, @NonNull String template, Map<String, Object> methodVars) {
            Map<String, Object> vars = this.mergeVars(ctx, methodVars);
            return ctx.getMenu().getMessageProvider().resolve(ctx.getEntity(), template, vars);
        }

        /**
         * Helper method to resolve a locale map using the menu's MessageProvider.
         *
         * @param ctx        The menu context
         * @param localeMap  The locale-to-template map
         * @param methodVars Optional method-level variables (can be null)
         * @return List of resolved Components
         */
        @NonNull
        private List<Component> resolve(@NonNull MenuContext ctx, @NonNull Map<Locale, String> localeMap, Map<String, Object> methodVars) {
            Map<String, Object> vars = this.mergeVars(ctx, methodVars);
            return ctx.getMenu().getMessageProvider().resolve(ctx.getEntity(), localeMap, vars);
        }

        /**
         * Helper method to join multiple Components into a single Component.
         * If there's only one Component, returns it directly.
         * Otherwise, joins with newlines.
         *
         * @param components The list of components to join
         * @return A single Component
         */
        @NonNull
        private Component joinComponents(@NonNull List<Component> components) {
            return components.size() == 1 ? components.get(0) : Component.join(JoinConfiguration.newlines(), components);
        }

        // ========================================
        // BASE ITEM
        // ========================================

        /**
         * Sets a base ItemStack to use as a template.
         * Properties from the base ItemStack (NBT, custom model data, enchantments, etc.) are preserved,
         * unless explicitly overridden by builder methods.
         *
         * <p>Priority order:
         * <ol>
         *   <li>Explicit builder calls (.name(), .lore(), .material()) - HIGHEST</li>
         *   <li>Base ItemStack properties - MEDIUM</li>
         *   <li>Default values - LOWEST</li>
         * </ol>
         *
         * <p>Example:
         * <pre>{@code
         * MenuItem.item()
         *     .from(offer.getItem().clone())  // Base item with lore, NBT
         *     .lore("New lore line")          // Override lore
         *     // name, material, NBT from base are preserved
         *     .build()
         * }</pre>
         *
         * @param base The base ItemStack to clone (will be cloned automatically)
         * @return This builder
         */
        @NonNull
        public Builder from(@NonNull ItemStack base) {
            this.baseItem = ReactiveProperty.of(base.clone());
            return this;
        }

        /**
         * Sets a context-aware base ItemStack to use as a template.
         * The base ItemStack is resolved dynamically for each render.
         *
         * <p>Example:
         * <pre>{@code
         * MenuItem.item()
         *     .from(ctx -> questService.getQuestIcon(questId))
         *     .name("Quest Name")
         *     .build()
         * }</pre>
         *
         * @param baseFunction Function to provide base ItemStack based on context
         * @return This builder
         */
        @NonNull
        public Builder from(@NonNull Function<MenuContext, ItemStack> baseFunction) {
            this.baseItem = ReactiveProperty.ofContext(ctx -> {
                ItemStack base = baseFunction.apply(ctx);
                return (base != null) ? base.clone() : null;
            });
            return this;
        }

        // ========================================
        // MATERIAL
        // ========================================

        /**
         * Sets the item material (type).
         *
         * @param material The Bukkit material type
         * @return This builder
         */
        @NonNull
        public Builder material(@NonNull Material material) {
            this.material = ReactiveProperty.of(material);
            // Only mark as explicitly set if it's not AIR (AIR is the default "no material")
            if (material != Material.AIR) {
                this.materialExplicitlySet = true;
            }
            return this;
        }

        /**
         * Sets the material from a dynamic supplier.
         * The supplier is called each time the item is rendered.
         *
         * @param supplier Supplier providing the material
         * @return This builder
         */
        @NonNull
        public Builder material(@NonNull Supplier<Material> supplier) {
            this.material = ReactiveProperty.of(supplier);
            this.materialExplicitlySet = true;
            return this;
        }

        /**
         * Sets the material with context-aware function.
         * Useful for dynamic materials based on player state or conditions.
         *
         * <p>Example usage:
         * <pre>{@code
         * .material(ctx -> ctx.getBool("unlocked") ? Material.DIAMOND : Material.IRON_INGOT)
         * }</pre>
         *
         * @param function Function that receives MenuContext and returns the material
         * @return This builder
         */
        @NonNull
        public Builder material(@NonNull Function<MenuContext, Material> function) {
            this.material = ReactiveProperty.ofContext(function::apply);
            this.materialExplicitlySet = true;
            return this;
        }

        // ========================================
        // NAME
        // ========================================

        /**
         * Sets the item name from a template string.
         * Template is processed by menu's MessageProvider (supports §, &, MiniMessage).
         * Uses item-level variables set via {@link #vars(Map)}.
         *
         * @param template The template string
         * @return This builder
         */
        @NonNull
        public Builder name(@NonNull String template) {
            this.name = ReactiveProperty.ofContext(ctx -> this.joinComponents(this.resolve(ctx, template, null)));
            this.nameExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item name from a template with additional variables.
         * These variables are merged with item-level variables (method-level overrides item-level).
         *
         * @param template The template string
         * @param vars     Additional variables for placeholder replacement
         * @return This builder
         */
        @NonNull
        public Builder name(@NonNull String template, Map<String, Object> vars) {
            this.name = ReactiveProperty.ofContext(ctx -> this.joinComponents(this.resolve(ctx, template, vars)));
            this.nameExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item name from a template with dynamic context-aware variables.
         * These variables are merged with item-level variables (method-level overrides item-level).
         *
         * @param template     The template string
         * @param varsSupplier Function to provide variables based on context
         * @return This builder
         */
        @NonNull
        public Builder name(@NonNull String template, @NonNull Function<MenuContext, Map<String, Object>> varsSupplier) {
            this.name = ReactiveProperty.ofContext(ctx -> {
                Map<String, Object> vars = varsSupplier.apply(ctx);
                return this.joinComponents(this.resolve(ctx, template, vars));
            });
            this.nameExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item name from a dynamic supplier.
         * The supplier's result is processed through MessageProvider (supports §, &, MiniMessage).
         *
         * @param supplier Supplier that provides the name template
         * @return This builder
         */
        @NonNull
        public Builder name(@NonNull Supplier<String> supplier) {
            this.name = ReactiveProperty.ofContext(ctx -> {
                String template = supplier.get();
                if ((template == null) || template.isEmpty()) {
                    return Component.empty();
                }
                return this.joinComponents(this.resolve(ctx, template, null));
            });
            this.nameExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item name from a context-aware function.
         * The function's result is processed through MessageProvider (supports §, &, MiniMessage).
         *
         * @param function Function that provides the name template based on context
         * @return This builder
         */
        @NonNull
        public Builder name(@NonNull Function<MenuContext, String> function) {
            this.name = ReactiveProperty.ofContext(ctx -> this.joinComponents(this.resolve(ctx, function.apply(ctx), null)));
            this.nameExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item name from a locale-specific map.
         * The menu's MessageProvider selects the appropriate locale for each viewer.
         * Uses item-level variables set via {@link #vars(Map)}.
         *
         * <p>Example:
         * <pre>{@code
         * .name(Map.of(
         *     Locale.ENGLISH, "Diamond Sword",
         *     new Locale("pl"), "Diamentowy miecz"
         * ))
         * }</pre>
         *
         * @param localeMap Map of locale to template string
         * @return This builder
         */
        @NonNull
        public Builder name(@NonNull Map<Locale, String> localeMap) {
            this.name = ReactiveProperty.ofContext(ctx -> this.joinComponents(this.resolve(ctx, localeMap, null)));
            this.nameExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item name from a locale-specific map with additional variables.
         * These variables are merged with item-level variables (method-level overrides item-level).
         *
         * <p>Example:
         * <pre>{@code
         * .name(Map.of(
         *     Locale.ENGLISH, "Price: {price} coins",
         *     new Locale("pl"), "Cena: {price} monet"
         * ), Map.of("price", 100))
         * }</pre>
         *
         * @param localeMap Map of locale to template string
         * @param vars      Additional variables for placeholder replacement
         * @return This builder
         */
        @NonNull
        public Builder name(@NonNull Map<Locale, String> localeMap, Map<String, Object> vars) {
            this.name = ReactiveProperty.ofContext(ctx -> this.joinComponents(this.resolve(ctx, localeMap, vars)));
            this.nameExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item name from a locale-specific map with dynamic context-aware variables.
         * These variables are merged with item-level variables (method-level overrides item-level).
         *
         * @param localeMap    Map of locale to template string
         * @param varsSupplier Function to provide variables based on context
         * @return This builder
         */
        @NonNull
        public Builder name(@NonNull Map<Locale, String> localeMap, @NonNull Function<MenuContext, Map<String, Object>> varsSupplier) {
            this.name = ReactiveProperty.ofContext(ctx -> {
                Map<String, Object> vars = varsSupplier.apply(ctx);
                return this.joinComponents(this.resolve(ctx, localeMap, vars));
            });
            this.nameExplicitlySet = true;
            return this;
        }

        // ========================================
        // LORE
        // ========================================

        /**
         * Sets the item lore from a multiline template string.
         * Template is processed by menu's MessageProvider (supports §, &, MiniMessage).
         * Lines are split on {@code \n} character.
         * Uses item-level variables set via {@link #vars(Map)}.
         *
         * @param template The multiline template string
         * @return This builder
         */
        @NonNull
        public Builder lore(@NonNull String template) {
            this.lore = ReactiveProperty.ofContext(ctx -> this.resolve(ctx, template, null));
            this.loreExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item lore from a multiline template with additional variables.
         * These variables are merged with item-level variables (method-level overrides item-level).
         *
         * @param template The multiline template string
         * @param vars     Additional variables for placeholder replacement
         * @return This builder
         */
        @NonNull
        public Builder lore(@NonNull String template, Map<String, Object> vars) {
            this.lore = ReactiveProperty.ofContext(ctx -> this.resolve(ctx, template, vars));
            this.loreExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item lore from a multiline template with dynamic context-aware variables.
         * These variables are merged with item-level variables (method-level overrides item-level).
         *
         * @param template     The multiline template string
         * @param varsSupplier Function to provide variables based on context
         * @return This builder
         */
        @NonNull
        public Builder lore(@NonNull String template, @NonNull Function<MenuContext, Map<String, Object>> varsSupplier) {
            this.lore = ReactiveProperty.ofContext(ctx -> {
                Map<String, Object> vars = varsSupplier.apply(ctx);
                return this.resolve(ctx, template, vars);
            });
            this.loreExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item lore from a reactive supplier.
         * The supplier is called each time the item is rendered.
         *
         * <p>Example:
         * <pre>{@code
         * .lore(() -> filterActive ?
         *     "<gray>Status: <green>Active\n<yellow>Click to toggle!" :
         *     "<gray>Status: <gray>Inactive\n<yellow>Click to toggle!")
         * }</pre>
         *
         * @param supplier Supplier providing the lore template (split on \n)
         * @return This builder
         */
        @NonNull
        public Builder lore(@NonNull Supplier<String> supplier) {
            this.lore = ReactiveProperty.ofContext(ctx -> {
                String template = supplier.get();
                return this.resolve(ctx, template, null);
            });
            this.loreExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item lore from a context-aware function.
         * The function receives MenuContext and returns a multiline template string.
         * Lines are split on {@code \n} character.
         *
         * @param function Function that receives MenuContext and returns lore template
         * @return This builder
         */
        @NonNull
        public Builder lore(@NonNull Function<MenuContext, String> function) {
            this.lore = ReactiveProperty.ofContext(ctx -> this.resolve(ctx, function.apply(ctx), null));
            this.loreExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item lore from a locale-specific map.
         * The menu's MessageProvider selects the appropriate locale for each viewer.
         * The locale-specific template is split into lines on {@code \n} character.
         * Uses item-level variables set via {@link #vars(Map)}.
         *
         * <p>Example:
         * <pre>{@code
         * .lore(Map.of(
         *     Locale.ENGLISH, """
         *         Price: 100 coins
         *         Stock: 50 items
         *
         *         Click to purchase!""",
         *     new Locale("pl"), """
         *         Cena: 100 monet
         *         Zapas: 50 przedmiotów
         *
         *         Kliknij aby kupić!"""
         * ))
         * }</pre>
         *
         * @param localeMap Map of locale to multiline template string
         * @return This builder
         */
        @NonNull
        public Builder lore(@NonNull Map<Locale, String> localeMap) {
            this.lore = ReactiveProperty.ofContext(ctx -> this.resolve(ctx, localeMap, null));
            this.loreExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item lore from a locale-specific map with additional variables.
         * These variables are merged with item-level variables (method-level overrides item-level).
         *
         * <p>Example:
         * <pre>{@code
         * .lore(Map.of(
         *     Locale.ENGLISH, """
         *         Price: {price} coins
         *         Stock: {stock} items
         *
         *         Click to purchase!""",
         *     new Locale("pl"), """
         *         Cena: {price} monet
         *         Zapas: {stock} przedmiotów
         *
         *         Kliknij aby kupić!"""
         * ), Map.of("price", 100, "stock", 50))
         * }</pre>
         *
         * @param localeMap Map of locale to multiline template string
         * @param vars      Additional variables for placeholder replacement
         * @return This builder
         */
        @NonNull
        public Builder lore(@NonNull Map<Locale, String> localeMap, Map<String, Object> vars) {
            this.lore = ReactiveProperty.ofContext(ctx -> this.resolve(ctx, localeMap, vars));
            this.loreExplicitlySet = true;
            return this;
        }

        /**
         * Sets the item lore from a locale-specific map with dynamic context-aware variables.
         * These variables are merged with item-level variables (method-level overrides item-level).
         *
         * @param localeMap    Map of locale to multiline template string
         * @param varsSupplier Function to provide variables based on context
         * @return This builder
         */
        @NonNull
        public Builder lore(@NonNull Map<Locale, String> localeMap, @NonNull Function<MenuContext, Map<String, Object>> varsSupplier) {
            this.lore = ReactiveProperty.ofContext(ctx -> {
                Map<String, Object> vars = varsSupplier.apply(ctx);
                return this.resolve(ctx, localeMap, vars);
            });
            this.loreExplicitlySet = true;
            return this;
        }

        // ========================================
        // AMOUNT
        // ========================================

        /**
         * Sets the item stack amount (count).
         *
         * @param amount The stack size (1-64)
         * @return This builder
         */
        @NonNull
        public Builder amount(int amount) {
            this.amount = ReactiveProperty.of(amount);
            return this;
        }

        /**
         * Sets the amount from a dynamic supplier.
         * The supplier is called each time the item is rendered.
         *
         * @param supplier Supplier providing the stack size
         * @return This builder
         */
        @NonNull
        public Builder amount(@NonNull Supplier<Integer> supplier) {
            this.amount = ReactiveProperty.of(supplier);
            return this;
        }

        /**
         * Sets the amount with context-aware function.
         * Useful for dynamic amounts based on player state.
         *
         * @param amountFunction Function that receives MenuContext and returns the amount
         * @return This builder
         */
        @NonNull
        public Builder amount(@NonNull Function<MenuContext, Integer> amountFunction) {
            this.amount = ReactiveProperty.ofContext(amountFunction);
            return this;
        }

        // ========================================
        // VISIBILITY
        // ========================================

        /**
         * Sets the item visibility to a static value.
         * When false, the item will not be displayed in the menu.
         *
         * @param visible Whether the item should be visible
         * @return This builder
         */
        @NonNull
        public Builder visible(boolean visible) {
            this.visible = ReactiveProperty.of(visible);
            return this;
        }

        /**
         * Sets the item visibility from a dynamic supplier.
         * The supplier is called each time the item is rendered.
         *
         * @param supplier Supplier providing the visibility state
         * @return This builder
         */
        @NonNull
        public Builder visible(@NonNull Supplier<Boolean> supplier) {
            this.visible = ReactiveProperty.of(supplier);
            return this;
        }

        /**
         * Sets the item visibility with context-aware function.
         * Useful for conditional visibility based on player permissions, state, or filters.
         *
         * <p>Example usage:
         * <pre>{@code
         * .visible(ctx -> ctx.getEntity().hasPermission("shop.vip"))
         * .visible(ctx -> ctx.getBool("showHidden"))
         * }</pre>
         *
         * @param function Function that receives MenuContext and returns visibility state
         * @return This builder
         */
        @NonNull
        public Builder visible(@NonNull Function<MenuContext, Boolean> function) {
            this.visible = ReactiveProperty.ofContext(function::apply);
            return this;
        }

        // ========================================
        // GLINT
        // ========================================

        /**
         * Sets the glint (enchantment glow) to a static value.
         *
         * @param glint Whether to show glint
         * @return This builder
         */
        @NonNull
        public Builder glint(boolean glint) {
            this.glint = ReactiveProperty.of(glint);
            return this;
        }

        /**
         * Sets the glint (enchantment glow) with context-aware function.
         * Useful for dynamic glint based on player state, permissions, or conditions.
         *
         * <p>Example usage:
         * <pre>{@code
         * .glint(ctx -> ctx.getBool("isSelected"))
         * .glint(ctx -> ctx.getEntity().hasPermission("vip.glint"))
         * .glint(ctx -> ctx.getInt("level") >= 10)
         * }</pre>
         *
         * @param glintFunction Function that receives MenuContext and returns whether to show glint
         * @return This builder
         */
        @NonNull
        public Builder glint(@NonNull Function<MenuContext, Boolean> glintFunction) {
            this.glint = ReactiveProperty.ofContext(glintFunction);
            return this;
        }

        // ========================================
        // HIDE ATTRIBUTES
        // ========================================

        /**
         * Sets whether to hide all item attributes (armor values, enchantment descriptions, etc.).
         * Adds all HIDE_* ItemFlags when enabled.
         *
         * <p>Note: Defaults to {@code true} for clean menu appearance.
         * Set to {@code false} to show item attributes.
         *
         * @param hideAttributes Whether to hide all attributes
         * @return This builder
         */
        @NonNull
        public Builder hideAttributes(boolean hideAttributes) {
            this.hideAttributes = ReactiveProperty.of(hideAttributes);
            return this;
        }

        /**
         * Sets whether to hide all item attributes with context-aware function.
         * Useful for dynamic attribute hiding based on player state or conditions.
         *
         * <p>Note: Defaults to {@code true} for clean menu appearance.
         *
         * @param hideAttributesFunction Function that receives MenuContext and returns whether to hide attributes
         * @return This builder
         */
        @NonNull
        public Builder hideAttributes(@NonNull Function<MenuContext, Boolean> hideAttributesFunction) {
            this.hideAttributes = ReactiveProperty.ofContext(hideAttributesFunction);
            return this;
        }

        // ========================================
        // DECORATIONS
        // ========================================

        /**
         * Adds an enchantment to the item.
         *
         * @param enchantment The enchantment type
         * @param level       The enchantment level
         * @return This builder
         */
        @NonNull
        public Builder enchant(@NonNull Enchantment enchantment, int level) {
            this.enchantments.put(enchantment, level);
            return this;
        }

        /**
         * Adds item flags to hide specific item attributes.
         * Common flags: HIDE_ENCHANTS, HIDE_ATTRIBUTES, HIDE_UNBREAKABLE, etc.
         *
         * @param flags The item flags to add
         * @return This builder
         */
        @NonNull
        public Builder itemFlag(@NonNull ItemFlag... flags) {
            this.itemFlags.addAll(Arrays.asList(flags));
            return this;
        }

        // ========================================
        // CLICK HANDLERS
        // ========================================

        /**
         * Sets a handler for any click type.
         * This handler is called for all click types (left, right, middle, etc.).
         *
         * @param handler The click handler
         * @return This builder
         */
        @NonNull
        public Builder onClick(@NonNull Consumer<MenuItemClickContext> handler) {
            this.clickHandler = handler;
            return this;
        }

        /**
         * Sets a handler specifically for right-clicks.
         * If set, this overrides the general onClick handler for right-clicks.
         *
         * @param handler The right-click handler
         * @return This builder
         */
        @NonNull
        public Builder onRightClick(@NonNull Consumer<MenuItemClickContext> handler) {
            this.rightClickHandler = handler;
            return this;
        }

        /**
         * Sets a handler specifically for left-clicks.
         * If set, this overrides the general onClick handler for left-clicks.
         *
         * @param handler The left-click handler
         * @return This builder
         */
        @NonNull
        public Builder onLeftClick(@NonNull Consumer<MenuItemClickContext> handler) {
            this.leftClickHandler = handler;
            return this;
        }

        /**
         * Sets a handler specifically for middle-clicks (mouse wheel click).
         * If set, this overrides the general onClick handler for middle-clicks.
         *
         * @param handler The middle-click handler
         * @return This builder
         */
        @NonNull
        public Builder onMiddleClick(@NonNull Consumer<MenuItemClickContext> handler) {
            this.middleClickHandler = handler;
            return this;
        }

        // ========================================
        // INTERACTIVE SLOTS
        // ========================================

        /**
         * Allows players to pick up the item from this slot.
         * By default, all menu items are locked and cannot be picked up.
         *
         * @param allowPickup true to allow picking up
         * @return This builder
         */
        @NonNull
        public Builder allowPickup(boolean allowPickup) {
            this.allowPickup = allowPickup;
            return this;
        }

        /**
         * Allows players to place items into this slot.
         * By default, all menu slots block item placement.
         *
         * @param allowPlacement true to allow placing items
         * @return This builder
         */
        @NonNull
        public Builder allowPlacement(boolean allowPlacement) {
            this.allowPlacement = allowPlacement;
            return this;
        }

        /**
         * Makes this slot fully interactive (allows both pickup and placement).
         * Shorthand for {@code .allowPickup(true).allowPlacement(true)}.
         *
         * @return This builder
         */
        @NonNull
        public Builder interactive() {
            this.allowPickup = true;
            this.allowPlacement = true;
            return this;
        }

        /**
         * Sets a handler for when the item in this slot changes.
         * Called after items are placed, removed, or swapped.
         *
         * <p>Use {@link MenuItemChangeContext} methods to check what changed:
         * <ul>
         *   <li>{@code wasItemPlaced()} - Item was placed into empty slot</li>
         *   <li>{@code wasItemRemoved()} - Item was removed from slot</li>
         *   <li>{@code wasItemSwapped()} - Item was swapped with another</li>
         * </ul>
         *
         * @param handler The change handler
         * @return This builder
         */
        @NonNull
        public Builder onItemChange(@NonNull Consumer<MenuItemChangeContext> handler) {
            this.itemChangeHandler = handler;
            return this;
        }

        // ========================================
        // ADVANCED FEATURES
        // ========================================

        /**
         * Sets item-level variables that are shared across name, lore, and other properties.
         * These variables are merged with method-level variables (method-level overrides item-level).
         *
         * @param vars Variables map
         * @return This builder
         */
        @NonNull
        public Builder vars(@NonNull Map<String, Object> vars) {
            // For static vars, we need to merge with existing vars
            Map<String, Object> currentVars = this.itemLevelVars.get(null);
            Map<String, Object> merged = new HashMap<>(currentVars);
            merged.putAll(vars);
            this.itemLevelVars = ReactiveProperty.of(merged);
            return this;
        }

        /**
         * Sets item-level variables with context-aware function.
         * Useful for dynamic variables based on player state that are shared across name/lore.
         *
         * @param varsFunction Function that receives MenuContext and returns variables map
         * @return This builder
         */
        @NonNull
        public Builder vars(@NonNull Function<MenuContext, Map<String, Object>> varsFunction) {
            this.itemLevelVars = ReactiveProperty.ofContext(varsFunction);
            return this;
        }

        /**
         * Adds a declarative filter to this item.
         * The filter will be applied to the target pane when active.
         *
         * <p>Example:
         * <pre>{@code
         * .filter(ItemFilter.builder()
         *     .target("items")
         *     .when(ctx -> filterActive)
         *     .predicate(item -> item.getRarity() == Rarity.EPIC)
         *     .build())
         * }</pre>
         *
         * @param filter The item filter
         * @return This builder
         */
        @NonNull
        public Builder filter(@NonNull ItemFilter<?> filter) {
            this.filters.add(filter);
            return this;
        }

        /**
         * Declares an async data dependency for this item.
         * Data will be loaded asynchronously and cached per-viewer.
         * Access the loaded data using {@code ctx.computed(key)} in property functions.
         *
         * <p>Example:
         * <pre>{@code
         * MenuItem.builder()
         *     .reactive("stats", () -> database.getPlayerStats(player), Duration.ofSeconds(30))
         *     .reactive("guild", () -> guildService.getGuild(player), Duration.ofMinutes(5))
         *     .name(ctx -> ctx.computed("stats")
         *         .map(PlayerStats::getTitle)
         *         .loading("&7Loading...")
         *         .orElse("&7Unknown"))
         *     .lore(ctx -> {
         *         String statsLine = ctx.computed("stats")
         *             .map(s -> "&7Level: " + s.getLevel())
         *             .loading("&7Level: Loading...")
         *             .orElse("&7Level: Unknown");
         *         String guildLine = ctx.computed("guild")
         *             .map(g -> "&7Guild: " + g.getName())
         *             .loading("&7Guild: Loading...")
         *             .orElse("&7Guild: None");
         *         return statsLine + "\n" + guildLine;
         *     })
         *     .build()
         * }</pre>
         *
         * @param key    Unique key for this data source
         * @param loader Async data loader
         * @param ttl    Time-to-live for cached data
         * @return This builder
         */
        @NonNull
        public Builder reactive(@NonNull String key, @NonNull Supplier<?> loader, @NonNull Duration ttl) {
            // Convert Supplier to Function for ReactiveLoader
            this.asyncLoader.register(key, ctx -> loader.get(), ttl);
            return this;
        }

        /**
         * Attaches reactive data source with default TTL of 1 second.
         *
         * @param key    The data key
         * @param loader The data loader
         * @return This builder
         */
        @NonNull
        public Builder reactive(@NonNull String key, @NonNull Supplier<?> loader) {
            return this.reactive(key, loader, Duration.ofSeconds(1));
        }

        // ========================================
        // BUILD
        // ========================================

        @NonNull
        public MenuItem build() {
            // Start async loads for all reactive sources on first render
            if (!this.asyncLoader.isEmpty()) {
                // Wrap material property to trigger async loads on first access
                ReactiveProperty<Material> originalMaterial = this.material;
                this.material = ReactiveProperty.ofContext(ctx -> {
                    // Trigger all reactive data sources
                    this.asyncLoader.trigger(ctx);
                    return originalMaterial.get(ctx);
                });
            }

            // Validate: interactive items should not have display properties
            boolean isInteractive = this.allowPickup || this.allowPlacement;

            if (isInteractive) {
                // Check if material was explicitly set
                if (this.materialExplicitlySet) {
                    throw new IllegalStateException(
                        "Interactive items (allowPickup/allowPlacement) should not have a material() set. " +
                            "Interactive items display whatever is in the inventory slot. " +
                            "Remove material() from this interactive item."
                    );
                }

                // Check if name was explicitly set
                if (this.nameExplicitlySet) {
                    throw new IllegalStateException(
                        "Interactive items (allowPickup/allowPlacement) should not have a name() set. " +
                            "Interactive items display whatever is in the inventory slot. " +
                            "Remove name() from this interactive item."
                    );
                }

                // Check if lore was explicitly set
                if (this.loreExplicitlySet) {
                    throw new IllegalStateException(
                        "Interactive items (allowPickup/allowPlacement) should not have lore() set. " +
                            "Interactive items display whatever is in the inventory slot. " +
                            "Remove lore() from this interactive item."
                    );
                }
            }

            return new MenuItem(this);
        }
    }
}
