package eu.okaeri.menu.item;

import lombok.NonNull;
import org.bukkit.Material;

/**
 * Utility class providing common filler items for menu backgrounds.
 * Fillers are purely decorative and non-interactive.
 */
@SuppressWarnings("FieldNamingConvention")
public final class Fillers {

    // Glass panes - commonly used for menu backgrounds
    public static final MenuItem WHITE_GLASS_PANE = glassPane(Material.WHITE_STAINED_GLASS_PANE);
    public static final MenuItem ORANGE_GLASS_PANE = glassPane(Material.ORANGE_STAINED_GLASS_PANE);
    public static final MenuItem MAGENTA_GLASS_PANE = glassPane(Material.MAGENTA_STAINED_GLASS_PANE);
    public static final MenuItem LIGHT_BLUE_GLASS_PANE = glassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
    public static final MenuItem YELLOW_GLASS_PANE = glassPane(Material.YELLOW_STAINED_GLASS_PANE);
    public static final MenuItem LIME_GLASS_PANE = glassPane(Material.LIME_STAINED_GLASS_PANE);
    public static final MenuItem PINK_GLASS_PANE = glassPane(Material.PINK_STAINED_GLASS_PANE);
    public static final MenuItem GRAY_GLASS_PANE = glassPane(Material.GRAY_STAINED_GLASS_PANE);
    public static final MenuItem LIGHT_GRAY_GLASS_PANE = glassPane(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    public static final MenuItem CYAN_GLASS_PANE = glassPane(Material.CYAN_STAINED_GLASS_PANE);
    public static final MenuItem PURPLE_GLASS_PANE = glassPane(Material.PURPLE_STAINED_GLASS_PANE);
    public static final MenuItem BLUE_GLASS_PANE = glassPane(Material.BLUE_STAINED_GLASS_PANE);
    public static final MenuItem BROWN_GLASS_PANE = glassPane(Material.BROWN_STAINED_GLASS_PANE);
    public static final MenuItem GREEN_GLASS_PANE = glassPane(Material.GREEN_STAINED_GLASS_PANE);
    public static final MenuItem RED_GLASS_PANE = glassPane(Material.RED_STAINED_GLASS_PANE);
    public static final MenuItem BLACK_GLASS_PANE = glassPane(Material.BLACK_STAINED_GLASS_PANE);

    // Other common fillers
    public static final MenuItem AIR = MenuItem.builder()
        .material(Material.AIR)
        .build();

    /**
     * Creates a glass pane filler with the specified material.
     *
     * @param material The glass pane material
     * @return The filler item
     */
    @NonNull
    private static MenuItem glassPane(@NonNull Material material) {
        return MenuItem.builder()
            .material(material)
            .name(" ")
            .build();
    }
}
