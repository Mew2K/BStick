package org.m2k.bstick;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.block.Biome;

import java.util.ArrayList;
import java.util.List;

public class BStickTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (Biome biome : Biome.values()) {
                if (!BStick.DISALLOWED_BIOMES.contains(biome)) {
                    suggestions.add(biome.name());
                }
            }
            return suggestions;
        }
        return null;
    }
}
