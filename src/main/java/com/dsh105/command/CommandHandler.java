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

    protected static int compare(String command, String commandToCompare) {
        /*
         * Conditions:
         * Commands with a great number of arguments get priority over those with less
         * Commands with no variables get priority over those without
         * Commands with variables later on get priority over commands with variables placed earlier in the syntax
         * Commands with regex variables are filtered last, as sometimes the regex may override commands above, which *should* get precedence
         * Finally, longer commands get priority over shorter commands
         */
        int firstArgsLength = command.split("\\s").length;
        int secondArgsLength = commandToCompare.split("\\s").length;
        boolean firstContainsVars = VariableMatcher.containsVariables(command);
        boolean secondContainsVars = VariableMatcher.containsVariables(commandToCompare);
        boolean firstContainsRegexVars = VariableMatcher.containsRegexVariables(command);
        boolean secondContainsRegexVars = VariableMatcher.containsRegexVariables(commandToCompare);
        int variableDiff = commandToCompare.replaceAll(VariableMatcher.SYNTAX_PATTERN.pattern(), "<>").indexOf("<>") - command.replaceAll(VariableMatcher.SYNTAX_PATTERN.pattern(), "<>").indexOf("<>");

        if (firstContainsRegexVars != secondContainsRegexVars) {
            if (firstContainsRegexVars && secondContainsVars) {
                return 1;
            }
        }
        if (firstContainsVars != secondContainsVars) {
            return firstContainsVars ? -1 : 1;
        }

        // Compare difference in where first variables are placed - gives commands with other words before variables precedence
        // e.g. "/command <hello>" against "/command sub <hello>"
        if (variableDiff != 0) {
            return variableDiff;
        }

        // Compare lengths - longer commands get priority as they are harder to find matches for
        // Commands with more args also get a higher priority
        return firstArgsLength == secondArgsLength ? secondArgsLength - firstArgsLength : commandToCompare.length() - command.length();
    }

    @Override
    public int compareTo(CommandHandler handler) {
        return compare(getCommandName(), handler.getCommandName());
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
