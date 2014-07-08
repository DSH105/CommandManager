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

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

/**
 * Thanks @CaptainBern <3
 */
public class DynamicPluginCommand extends Command implements PluginIdentifiableCommand {

    protected final CommandExecutor registeredWith;
    protected final Plugin owningPlugin;

    public DynamicPluginCommand(String name, String[] aliases, String desc, String usage, CommandExecutor registeredWith, Plugin plugin) {
        super(name, desc, usage, Arrays.asList(aliases));
        this.registeredWith = registeredWith;
        this.owningPlugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        return owningPlugin.isEnabled() && registeredWith.onCommand(sender, this, label, args);

    }

    public CommandExecutor getRegisteredWith() {
        return registeredWith;
    }

    @Override
    public Plugin getPlugin() {
        return owningPlugin;
    }
}
