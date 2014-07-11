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

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MockCommandEvent<T extends CommandSender> extends CommandEvent<T> {

    public MockCommandEvent(CommandManager manager, String args, T sender) {
        super(manager, args, sender);
    }

    public MockCommandEvent(CommandManager manager, T sender, String... args) {
        super(manager, sender, args);
    }

    public MockCommandEvent(CommandManager manager, String command, T sender, String... args) {
        super(manager, command, sender, args);
    }

    @Override
    public void respond(String response, ChatColor formatColour, ChatColor highlightColour) {
        System.out.println(response);
    }

    @Override
    public boolean canPerform(String... permission) {
        return true;
    }
}