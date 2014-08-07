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

public class MatchedCommand {

    private String matchedLabel;
    private CommandHandler commandHandler;

    protected MatchedCommand(String matchedLabel, CommandHandler commandHandler) {
        this.matchedLabel = matchedLabel;
        this.commandHandler = commandHandler;
    }

    public String getMatchedLabel() {
        return matchedLabel;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }
}