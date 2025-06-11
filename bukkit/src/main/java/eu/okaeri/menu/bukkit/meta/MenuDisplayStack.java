package eu.okaeri.menu.bukkit.meta;

import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MenuDisplayStack implements Function<HumanEntity, ItemStack> {

    private Function<HumanEntity, String> name;
    private Function<HumanEntity, String> description;

    private Supplier<ItemStack> from;
    private Supplier<Material> material;
    private List<Map.Entry<BooleanSupplier, Map.Entry<Enchantment, Integer>>> enchants;
    private Supplier<Integer> amount;
    private Supplier<Short> durability;

    private List<Runnable> hooks;
    private boolean hideAttributes = true;

    public MenuDisplayStack when(@NonNull BooleanSupplier condition, @NonNull Consumer<MenuDisplayStack> ifTrue) {
        return this.whenElse(condition, ifTrue, Function.identity()::apply);
    }

    public MenuDisplayStack whenElse(@NonNull BooleanSupplier condition, @NonNull Consumer<MenuDisplayStack> ifTrue, @NonNull Consumer<MenuDisplayStack> ifFalse) {

        if (this.hooks == null) {
            // wow; such memory efficient; footprint so low
            this.hooks = new ArrayList<>();
        }

        this.hooks.add(() -> {
            if (condition.getAsBoolean()) {
                ifTrue.accept(this);
            } else {
                ifFalse.accept(this);
            }
        });

        return this;
    }

    public MenuDisplayStack name(@NonNull Function<HumanEntity, String> name) {
        this.name = name;
        return this;
    }

    public MenuDisplayStack name(@NonNull String name) {
        return this.name(viewer -> name);
    }

    public MenuDisplayStack description(@NonNull Function<HumanEntity, String> description) {
        this.description = description;
        return this;
    }

    public MenuDisplayStack description(@NonNull String description) {
        return this.description(viewer -> description);
    }

    public MenuDisplayStack from(@NonNull Supplier<ItemStack> from) {
        if (this.material != null) {
            throw new IllegalStateException("Cannot use from after setting material");
        }
        this.from = from;
        return this;
    }

    public MenuDisplayStack from(@NonNull ItemStack from) {
        return this.from(() -> from);
    }

    public MenuDisplayStack material(@NonNull Supplier<Material> material) {
        if (this.from != null) {
            throw new IllegalStateException("Cannot use material after setting from");
        }
        this.material = material;
        return this;
    }

    public MenuDisplayStack material(@NonNull Material material) {
        return this.material(() -> material);
    }

    public MenuDisplayStack enchant(@NonNull BooleanSupplier when, @NonNull Enchantment enchant, int level) {
        if (this.enchants == null) {
            this.enchants = new ArrayList<>();
        }
        this.enchants.add(new AbstractMap.SimpleEntry<>(
            when,
            new AbstractMap.SimpleEntry<>(enchant, level)
        ));
        return this;
    }

    public MenuDisplayStack enchant(@NonNull Enchantment enchant, int level) {
        return this.enchant(() -> true, enchant, level);
    }

    public MenuDisplayStack amount(@NonNull Supplier<Integer> amount) {
        this.amount = amount;
        return this;
    }

    public MenuDisplayStack amount(int amount) {
        return this.amount(() -> amount);
    }

    public MenuDisplayStack durability(@NonNull Supplier<Short> durability) {
        this.durability = durability;
        return this;
    }

    public MenuDisplayStack durability(short durability) {
        return this.durability(() -> durability);
    }

    public MenuDisplayStack hideAttributes(boolean hideAttributes) {
        this.hideAttributes = hideAttributes;
        return this;
    }

    @Override
    public ItemStack apply(@NonNull HumanEntity viewer) {

        if ((this.from == null) && (this.material == null)) {
            throw new IllegalStateException("Cannot display without from or material");
        }

        ItemStack stack = (this.from == null)
            ? new ItemStack(this.material.get())
            : this.from.get();

        if (this.amount != null) {
            stack.setAmount(this.amount.get());
        }

        if (this.durability != null) {
            stack.setDurability(this.durability.get());
        }

        Optional.ofNullable(this.name)
            .map(fn -> fn.apply(viewer))
            .ifPresent(name -> {
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName(name);
                stack.setItemMeta(meta);
            });

        Optional.ofNullable(this.description)
            .map(fn -> fn.apply(viewer))
            .ifPresent(description -> {
                ItemMeta meta = stack.getItemMeta();
                meta.setLore(Arrays.asList(description.split("\n")));
                stack.setItemMeta(meta);
            });

        if (this.hideAttributes) {
            ItemMeta meta = stack.getItemMeta();
            meta.addItemFlags(Arrays.stream(ItemFlag.values())
                .filter(it -> it.name().startsWith("HIDE_"))
                .toArray(ItemFlag[]::new));
            stack.setItemMeta(meta);
        }

        if (this.enchants != null) {
            for (Map.Entry<BooleanSupplier, Map.Entry<Enchantment, Integer>> entry : this.enchants) {

                if (!entry.getKey().getAsBoolean()) {
                    continue;
                }

                Enchantment enchant = entry.getValue().getKey();
                int level = entry.getValue().getValue();

                stack.addUnsafeEnchantment(enchant, level);
            }
        }

        if (this.hooks != null) {
            this.hooks.forEach(Runnable::run);
        }

        return stack;
    }
}
