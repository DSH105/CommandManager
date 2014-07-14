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

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thanks @CaptainBern <3
 */
public class CommandRegistry {

    protected static Field SERVER_COMMAND_MAP;
    private ArrayList<String> REGISTERED_COMMANDS;

    static {
        Bukkit.getHelpMap().registerHelpTopicFactory(DynamicPluginCommand.class, new DynamicPluginCommandHelpTopicFactory());

        try {
            SERVER_COMMAND_MAP = Bukkit.getServer().getPluginManager().getClass().getDeclaredField("commandMap");
            SERVER_COMMAND_MAP.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private CommandMap fallback;

    private final Plugin plugin;

    public CommandRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public CommandMap getCommandMap() {
        CommandMap map;

        try {
            map = (CommandMap) SERVER_COMMAND_MAP.get(Bukkit.getPluginManager());
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

    public void register(DynamicPluginCommand command) {
        if (REGISTERED_COMMANDS.contains(command.getName())) {
            // Already registered with CommandManager -> no need to do so again
            return;
        }
        getCommandMap().register(this.plugin.getName(), command);
        REGISTERED_COMMANDS.add(command.getName());
    }

    public void unregister(DynamicPluginCommand command) {
        command.unregister(getCommandMap());
        REGISTERED_COMMANDS.remove(command.getName());
    }

    public void unregister(String command) {
        Command bukkitCommand = getCommandMap().getCommand(command);
        if (bukkitCommand != null && bukkitCommand instanceof DynamicPluginCommand) {
            unregister((DynamicPluginCommand) bukkitCommand);
        }
    }

    public List<String> getRegisteredCommands() {
        return Collections.unmodifiableList(REGISTERED_COMMANDS);
    }
}
