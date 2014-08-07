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

import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class CommandHandler implements Comparable<CommandHandler> {

    private CommandListener parent;
    private CommandListener registeredTo;
    private Command command;
    private Method accessor;
    private Class<?> acceptedSenderType;

    public CommandHandler(CommandListener parentListener, CommandListener registeredTo, Command command, Method accessor) {
        this.parent = parentListener;
        this.registeredTo = registeredTo;
        this.command = command;
        this.accessor = accessor;
        this.acceptedSenderType = getSenderType();
    }

    public CommandListener getParent() {
        return parent;
    }

    public CommandListener getRegisteredTo() {
        return registeredTo;
    }

    public Command getCommand() {
        return command;
    }

    public Method getAccessor() {
        return accessor;
    }

    public Class<?> getAcceptedSenderType() {
        return acceptedSenderType;
    }

    public boolean isSenderAccepted(CommandSender sender) {
        return getAcceptedSenderType().isAssignableFrom(sender.getClass());
    }

    public Command getParentCommand() {
        return getRegisteredTo().getClass().getAnnotation(Command.class);
    }

    public String getCommandName() {
        return getCommand().command();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommandHandler that = (CommandHandler) o;

        if (!acceptedSenderType.equals(that.acceptedSenderType)) return false;
        if (!accessor.equals(that.accessor)) return false;
        if (!command.equals(that.command)) return false;
        if (!parent.equals(that.parent)) return false;

        return registeredTo.equals(that.registeredTo);
    }

    @Override
    public int hashCode() {
        int result = parent.hashCode();
        result = 31 * result + registeredTo.hashCode();
        result = 31 * result + command.hashCode();
        result = 31 * result + accessor.hashCode();
        result = 31 * result + acceptedSenderType.hashCode();
        return result;
    }

    @Override
    public int compareTo(CommandHandler handler) {
        String command = getCommandName();
        String commandToCompare = handler.getCommandName();

        if (!VariableMatcher.containsRegexVariables(command) && VariableMatcher.containsRegexVariables(commandToCompare)) {
            return -1;
        } else if (VariableMatcher.containsRegexVariables(command) && !VariableMatcher.containsRegexVariables(commandToCompare)) {
            return 1;
        }

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

    private Class<?> getSenderType() {
        Type[] genericParameterTypes = getAccessor().getGenericParameterTypes();
        for (Type genericType : genericParameterTypes) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Type[] paramArgTypes = parameterizedType.getActualTypeArguments();
                for (Type paramArgType : paramArgTypes) {
                    if (paramArgType != null) {
                        return (Class<?>) paramArgType;
                    }
                }
            }
        }
        return CommandSender.class;
    }
}
