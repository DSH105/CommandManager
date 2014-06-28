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

import com.dsh105.commodus.StringUtil;
import com.dsh105.powermessage.markup.MarkupBuilder;
import com.dsh105.command.CommandManager;
import com.dsh105.command.ResponseLevel;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandEvent<T extends CommandSender> {

    private CommandManager manager;
    private String command;
    private T sender;
    private String[] args;

    public CommandEvent(CommandManager manager, String args, T sender) {
        this(manager, sender, args.split("\\s"));
    }

    public CommandEvent(CommandManager manager, T sender, String... args) {
        this(manager, args[0], sender, StringUtil.combineArray(1, " ", args));
    }

    public CommandEvent(CommandManager manager, String command, T sender, String... args) {
        this.manager = manager;
        this.command = command;
        this.sender = sender;
        this.args = args;
    }

    public CommandManager getManager() {
        return manager;
    }

    public String command() {
        return command;
    }

    public T sender() {
        return sender;
    }

    public String[] args() {
        return args;
    }

    public int argsLength() {
        return args().length;
    }

    public String arg(int index) {
        return args[index];
    }

    public boolean canPerform(String permission) {
        return canPerform(permission, (Class<? extends T>) Player.class);
    }

    public <S extends T> boolean canPerform(String permission, Class<S> typeRestriction) {
        if (typeRestriction.isAssignableFrom(sender.getClass())) {
            respond(ResponseLevel.SEVERE, manager.getNoAccessMessage());
            return false;
        }
        if (!sender.hasPermission(permission)) {
            respond(ResponseLevel.SEVERE, manager.getNoPermissionMessage());
            return false;
        }
        return true;
    }

    public void respond(String response) {
        respond(response, manager.getFormatColour(), manager.getHighlightColour());
    }

    public void respond(ResponseLevel level, String response) {
        respond(response, level.getFormatColour(), level.getHighlightColour());
    }

    public void respond(String response, ChatColor formatColour, ChatColor highlightColour) {
        String message = response.replace("{c1}", "" + formatColour).replace("{c2}", "" + highlightColour);

        // Take care of any conversions, special formatting, etc.
        new MarkupBuilder().withText(manager.getResponsePrefix() + " " + ChatColor.RESET + message).build().send(sender());
    }

}