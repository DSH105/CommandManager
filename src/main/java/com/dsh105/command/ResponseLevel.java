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

public enum ResponseLevel {

    WARNING(ChatColor.GOLD, ChatColor.YELLOW),
    SEVERE(ChatColor.DARK_RED, ChatColor.RED);

    private ChatColor formatColour;
    private ChatColor highlightColour;

    ResponseLevel(ChatColor formatColour, ChatColor highlightColour) {
        this.formatColour = formatColour;
        this.highlightColour = highlightColour;
    }

    public ChatColor getFormatColour() {
        return formatColour;
    }

    public ChatColor getHighlightColour() {
        return highlightColour;
    }
}