/*
 * This file is part of CommandManager.
 *
 * CommandManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CommandManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CommandManager.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dsh105.command.registration;

import com.captainbern.reflection.Reflection;
import com.captainbern.reflection.accessor.FieldAccessor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;

/**
 * Thanks @CaptainBern <3
 */
public class CommandRegistry {

    protected static final FieldAccessor<CommandMap> SERVER_COMMAND_MAP = new Reflection().reflect(Bukkit.getServer().getPluginManager().getClass()).getSafeFieldByNameAndType("commandMap", CommandMap.class).getAccessor();

    private CommandMap fallback;

    private final Plugin plugin;

    public CommandRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(DynamicPluginCommand command) {
        getCommandMap().register(this.plugin.getName(), command);
    }

    public CommandMap getCommandMap() {
        CommandMap map;

        try {
            map = SERVER_COMMAND_MAP.get(Bukkit.getPluginManager());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to retrieve the CommandMap! Using fallback instead...");
            map = null;
        }

        if (map == null) {
            if (fallback != null) {
                return fallback;
            } else {
                fallback = map = new SimpleCommandMap(Bukkit.getServer());
                Bukkit.getPluginManager().registerEvents(new FallbackCommandRegistrationListener(fallback), this.plugin);
            }
        }
        return map;
    }
}
