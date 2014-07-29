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

package com.dsh105.command;

import com.dsh105.command.registration.CommandRegistry;
import org.bukkit.plugin.Plugin;

/**
 * For the convenience of the {@link #getPlugin()} method
 */
public class PluginCommandManager<T extends Plugin> extends CommandManager {

    public PluginCommandManager(T owningPlugin) {
        super(owningPlugin);
    }

    public PluginCommandManager(T owningPlugin, String responsePrefix) {
        super(owningPlugin, responsePrefix);
    }

    public PluginCommandManager(T owningPlugin, CommandRegistry commandRegistry, boolean enableHelpService, String responsePrefix) {
        super(owningPlugin, commandRegistry, enableHelpService, responsePrefix);
    }

    @Override
    public T getPlugin() {
        return (T) super.getPlugin();
    }
}