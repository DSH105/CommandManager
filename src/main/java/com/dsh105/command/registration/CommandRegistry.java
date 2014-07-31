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

import com.dsh105.command.Command;
import com.dsh105.command.CommandHandler;
import com.dsh105.command.ICommandManager;
import com.dsh105.commodus.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Thanks @CaptainBern <3
 */
public class CommandRegistry {

    protected static Field SERVER_COMMAND_MAP;
    private ArrayList<String> REGISTERED_COMMANDS = new ArrayList<>();

    static {
        Bukkit.getHelpMap().registerHelpTopicFactory(DynamicPluginCommand.class, new DynamicPluginCommandHelpTopicFactory());

        try {
            SERVER_COMMAND_MAP = Bukkit.getServer().getPluginManager().getClass().getDeclaredField("commandMap");
            SERVER_COMMAND_MAP.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private ICommandManager manager;
    private CommandMap fallback;

    public CommandRegistry(ICommandManager manager) {
        this.manager = manager;
    }

    public CommandMap getCommandMap() {
        CommandMap map;

        try {
            map = (CommandMap) SERVER_COMMAND_MAP.get(Bukkit.getPluginManager());
        } catch (Exception e) {
            manager.getPlugin().getLogger().warning("Failed to retrieve the CommandMap! Using fallback instead...");
            map = null;
        }

        if (map == null) {
            if (fallback != null) {
                return fallback;
            } else {
                fallback = map = new SimpleCommandMap(Bukkit.getServer());
                Bukkit.getPluginManager().registerEvents(new FallbackCommandRegistrationListener(fallback), manager.getPlugin());
            }
        }
        return map;
    }

    public void register(Collection<CommandHandler> registrationQueue) {
        if (!registrationQueue.isEmpty()) {
            for (CommandHandler handler : registrationQueue) {
                Command command = handler.getCommand();

                // Build a list of aliases that Bukkit can use
                List<String> aliases = new ArrayList<>();
                for (String alias : command.aliases()) {
                    aliases.add(alias.split("\\s")[0]);
                }

                register(new DynamicPluginCommand(handler.getCommandName().split("\\s")[0], aliases.toArray(StringUtil.EMPTY_STRING_ARRAY), manager.getMessenger().format(command.description()), manager.getMessenger().format(command.usage()), manager, manager.getPlugin()));
            }
        }
    }

    public void register(DynamicPluginCommand command) {
        if (REGISTERED_COMMANDS.contains(command.getName().toLowerCase().trim())) {
            // Already registered with CommandManager -> no need to do so again
            return;
        }
        if (!getCommandMap().register(manager.getPlugin().getName(), command)) {
            // More of a backup for above
            unregister(command);
        } else {
            REGISTERED_COMMANDS.add(command.getName().toLowerCase().trim());
        }
    }

    public void unregister(DynamicPluginCommand command) {
        command.unregister(getCommandMap());
        REGISTERED_COMMANDS.remove(command.getName().toLowerCase().trim());
    }

    public void unregister(String command) {
        org.bukkit.command.Command bukkitCommand = getCommandMap().getCommand(command);
        if (bukkitCommand != null && bukkitCommand instanceof DynamicPluginCommand) {
            unregister((DynamicPluginCommand) bukkitCommand);
        }
    }

    public List<String> getRegisteredCommands() {
        return Collections.unmodifiableList(REGISTERED_COMMANDS);
    }
}
