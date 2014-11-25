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

public class Messenger {

    private ChatColor highlightColour = ChatColor.WHITE;
    private ChatColor formatColour = ChatColor.WHITE;

    private String noPermissionMessage = "You are not permitted to do that.";
    private String noAccessMessage = "You do not have access to this from here.";
    private String errorMessage = "Something unexpected happened. Please see the console for any errors and report them immediately.";
    private String commandNotFoundMessage = "That command does not exist.";

    public String format(String input) {
        return format(input, formatColour, highlightColour);
    }

    public String format(String input, ChatColor formatColour, ChatColor highlightColour) {
        return input.replace("{c1}", "" + formatColour).replace("{c2}", "" + highlightColour);
    }

    public ChatColor getHighlightColour() {
        return highlightColour;
    }

    public void setHighlightColour(ChatColor highlightColour) {
        this.highlightColour = highlightColour;
    }

    public ChatColor getFormatColour() {
        return formatColour;
    }

    public void setFormatColour(ChatColor formatColour) {
        this.formatColour = formatColour;
    }

    public String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    public void setNoPermissionMessage(String noPermissionMessage) {
        this.noPermissionMessage = noPermissionMessage;
    }

    public String getNoAccessMessage() {
        return noAccessMessage;
    }

    public void setNoAccessMessage(String noAccessMessage) {
        this.noAccessMessage = noAccessMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCommandNotFoundMessage() {
        return commandNotFoundMessage;
    }

    public void setCommandNotFoundMessage(String commandNotFoundMessage) {
        this.commandNotFoundMessage = commandNotFoundMessage;
    }
}