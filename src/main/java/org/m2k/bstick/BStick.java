package org.m2k.bstick;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class BStick extends JavaPlugin implements Listener {

    private int areaSize;
    private int maxUses;
    public static final Set<Biome> DISALLOWED_BIOMES = Set.of(
            Biome.CUSTOM,
            Biome.THE_END,
            Biome.NETHER_WASTES,
            Biome.SOUL_SAND_VALLEY,
            Biome.CRIMSON_FOREST,
            Biome.WARPED_FOREST,
            Biome.BASALT_DELTAS,
            Biome.SMALL_END_ISLANDS,
            Biome.END_MIDLANDS,
            Biome.END_HIGHLANDS,
            Biome.END_BARRENS
    );

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        areaSize = this.getConfig().getInt("areaSize", 10);
        maxUses = this.getConfig().getInt("maxUses", 10);
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(this.getCommand("bstick")).setExecutor((sender, command, label, args) -> onCommand(sender, args));
        Objects.requireNonNull(this.getCommand("bstick")).setTabCompleter(new BStickTabCompleter());
    }

    // Handle the /bstick command.
    private boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                try {
                    Biome biome = Biome.valueOf(args[0].toUpperCase());
                    if (!DISALLOWED_BIOMES.contains(biome)) {
                        player.getInventory().addItem(createBiomeStick(biome));
                        return true;
                    } else {
                        player.sendMessage(ChatColor.RED + "Disallowed biome type!");
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid biome type!");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Usage: /bstick <biome>");
            }
        }
        return false;
    }

    private boolean biomeIsAllowed(Biome biome) {
        return !DISALLOWED_BIOMES.contains(biome);
    }

    // Creates a biome stick with the specified biome.
    public ItemStack createBiomeStick(Biome biome) {
        ItemStack stick = new ItemStack(Material.STICK, 1);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            // Display name
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Biome Stick [" + maxUses + "]");

            // Persistent data container meta to track usage count.
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(new NamespacedKey(this, "BSTIK"), PersistentDataType.STRING, "true");
            container.set(new NamespacedKey(this, "Biome"), PersistentDataType.STRING, biome.toString());
            container.set(new NamespacedKey(this, "Uses"), PersistentDataType.INTEGER, maxUses);

            // Lore
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.YELLOW + biome.toString());
            lore.add(getUsesLore(maxUses, maxUses));
            meta.setLore(lore);

            stick.setItemMeta(meta);
        }
        return stick;
    }

    // Handles usage of the biome stick.
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // If the right-clicked item is a stick, proceed.
        if (item != null && item.getType() == Material.STICK) {
            ItemMeta meta = item.getItemMeta();

            // If this is a custom stick (has meta), proceed.
            if (meta != null) {
                PersistentDataContainer container = meta.getPersistentDataContainer();

                // If this stick is a biome stick, do the magic.
                if (container.has(new NamespacedKey(this, "BSTIK"), PersistentDataType.STRING)) {
                    int uses = container.getOrDefault(new NamespacedKey(this, "Uses"), PersistentDataType.INTEGER, 0);
                    String biomeStr = container.getOrDefault(new NamespacedKey(this, "Biome"), PersistentDataType.STRING, "PLAINS");
                    Biome biome = Biome.valueOf(biomeStr);

                    if (uses > 0) {
                        changeBiome(player, biome);
                        updateBiomeStick(meta, container, biome, --uses);
                        item.setItemMeta(meta);
                        if (uses <= 0) {
                            item.setAmount(0);
                        }
                    }
                }
            }
        }
    }

    // Changes an N x N area to the specified biome.
    private void changeBiome(Player player, Biome biome) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            for (int x = -areaSize; x <= areaSize; x++) {
                for (int z = -areaSize; z <= areaSize; z++) {
                    world.setBiome(loc.getBlockX() + x, loc.getBlockY(), loc.getBlockZ() + z, biome);
                }
            }
        }
        player.sendMessage(ChatColor.GREEN + "Successfully changed " + areaSize + "x" + areaSize + " area to " + biome.toString());
    }

    // Updates the biome stick item when used.
    private void updateBiomeStick(ItemMeta meta, PersistentDataContainer container, Biome biome, int uses) {
        container.set(new NamespacedKey(this, "Uses"), PersistentDataType.INTEGER, uses);
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Biome Stick [" + uses + "]");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.YELLOW + biome);
        lore.add(getUsesLore(uses, maxUses));
        meta.setLore(lore);
    }

    // Helper method for displaying remaining uses of the biome stick.
    private String getUsesLore(int currentUses, int maxUses) {
        double percentage = (double) currentUses / maxUses * 100;

        ChatColor color;
        if (percentage >= 80) {
            color = ChatColor.GREEN;
        } else if (percentage >= 30) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        return ChatColor.GRAY + "Uses: " + color + currentUses + "/" + maxUses;
    }
}