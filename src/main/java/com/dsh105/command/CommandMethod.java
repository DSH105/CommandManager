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

import java.lang.reflect.Method;

public class CommandMethod implements Comparable<CommandMethod> {

    private Command command;
    private Method accessor;
    private CommandListener parent;

    public CommandMethod(CommandListener parent, Command command, Method accessor) {
        this.parent = parent;
        this.command = command;
        this.accessor = accessor;
    }

    public CommandListener getParent() {
        return parent;
    }

    public Command getCommand() {
        return command;
    }

    public Method getAccessor() {
        return accessor;
    }

    @Override
    public int compareTo(CommandMethod commandMethod) {
        String command = getCommand().command();
        String commandToCompare = commandMethod.getCommand().command();

        if (!VariableMatcher.containsVariables(command) && VariableMatcher.containsVariables(commandToCompare)) {
            return 1;
        } else if (VariableMatcher.containsVariables(command) && !VariableMatcher.containsVariables(commandToCompare)) {
            return -1;
        }

        // Useful for comparing certain commands
        // e.g. "/command <hello>" against "/command sub <hello>"
        int variableDiff = command.replaceAll(VariableMatcher.SYNTAX_PATTERN.pattern(), "<>").indexOf("<>") - commandToCompare.replaceAll(VariableMatcher.SYNTAX_PATTERN.pattern(), "<>").indexOf("<>");
        if (variableDiff != 0) {
            return variableDiff;
        }

        // Compare lengths - longer commands get priority as they are harder to find matches for
        return command.length() - commandToCompare.length();
    }
}