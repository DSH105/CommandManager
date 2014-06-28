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

import com.dsh105.command.exceptions.CommandInvalidException;
import com.dsh105.commodus.StringUtil;
import com.dsh105.commodus.paginator.Paginator;
import com.dsh105.powermessage.core.PowerMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class CommandManager implements CommandExecutor {

    private final static Logger LOGGER = Logger.getLogger("UnitCommand");

    private final ArrayList<CommandListener> COMMANDS = new ArrayList<>();

    private Plugin plugin;
    private Paginator<PowerMessage> paginator = new Paginator<>();

    /*
     * Messages
     */
    private String noPermissionMessage = "{c1}You are not permitted to do that.";
    private String noAccessMessage = "{c1}You do not have access to this from here.";
    private String errorMessage = "{c1}Something unexpected happened. Please see the console for any errors and report them immediately.";
    private String commandNotFoundMessage = "{c1}That command does not exist.";

    private String responsePrefix;
    private boolean suggestCommands;
    private ChatColor highlightColour = ChatColor.WHITE;
    private ChatColor formatColour = ChatColor.WHITE;

    public CommandManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public CommandManager(Plugin plugin, String responsePrefix) {
        this(plugin);
        this.responsePrefix = responsePrefix;
    }

    public List<CommandListener> getRegisteredCommands() {
        return Collections.unmodifiableList(COMMANDS);
    }

    public Paginator<PowerMessage> getPaginator() {
        return paginator;
    }

    public String getResponsePrefix() {
        return responsePrefix;
    }

    public void setResponsePrefix(String responsePrefix) {
        this.responsePrefix = responsePrefix;
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

    public boolean willSuggestCommands() {
        return suggestCommands;
    }

    public void setSuggestCommands(boolean suggestCommands) {
        this.suggestCommands = suggestCommands;
    }

    public void register(CommandListener commandListener) {
        for (CommandMethod commandMethod : getCommandMethods(commandListener)) {
            if (!isValid(commandMethod)) {
                throw new CommandInvalidException(plugin.getName() + " has registered an invalid command: " + commandListener.getClass().getCanonicalName() + " -> " + commandMethod.getAccessor().getName() + ". Command method can only have one parameter that MUST extend " + CommandEvent.class.getCanonicalName() + " and return a BOOLEAN.");
            }
        }
        if (getCommandMethods(commandListener).isEmpty()) {
            LOGGER.warning(plugin.getName() + " has registered a command listener with no valid command methods: " + commandListener.getClass().getCanonicalName() + ".");
        }
        COMMANDS.add(commandListener);
    }

    public void unregister(CommandListener commandListener) {
        COMMANDS.remove(commandListener);
    }

    public <T extends CommandListener> ArrayList<T> getCommandsOfType(Class<T> type) {
        ArrayList<T> commands = new ArrayList<>();
        for (CommandListener commandListener : COMMANDS) {
            if (type.getClass().isAssignableFrom(commandListener.getClass())) {
                commands.add((T) commandListener);
            }
        }
        return commands;
    }

    public <T extends CommandListener> T getCommandOfType(Class<T> type) {
        ArrayList<T> commands = getCommandsOfType(type);
        if (commands.isEmpty()) {
            return null;
        }
        return commands.get(0);
    }

    public CommandListener getCommandFor(String commandArguments) {
        return getCommandFor(COMMANDS, commandArguments);
    }

    public CommandListener getCommandFor(String commandArguments, boolean useAliases) {
        return getCommandFor(COMMANDS, commandArguments, useAliases);
    }

    public <T extends CommandListener> T getCommandFor(ArrayList<T> commandList, String command) {
        return getCommandFor(commandList, command, true);
    }

    public <T extends CommandListener> T getCommandFor(ArrayList<T> commandList, String command, boolean useAliases) {
        return getCommandFor(commandList, command, useAliases, false);
    }

    public <T extends CommandListener> T getCommandFor(ArrayList<T> commandList, String command, boolean useAliases, boolean fuzzyMatching) {
        T fuzzyMatch = null;
        for (T commandListener : commandList) {
            Command parent = commandListener.getClass().getAnnotation(Command.class);
            for (CommandMethod method : getCommandMethods(commandListener)) {
                Command cmd = method.getCommand();
                if (parent != null) {
                    if (matches(parent.command(), command, false)) {
                        return commandListener;
                    }
                }
                if (matches(cmd.command(), command, false)) {
                    return commandListener;
                }

                for (String alias : method.getCommand().aliases()) {
                    if (matches(alias, command, false)) {
                        return commandListener;
                    }
                }

                if (matches(cmd.command(), command, true)) {
                    fuzzyMatch = commandListener;
                    continue;
                }

                for (String alias : method.getCommand().aliases()) {
                    if (matches(alias, command, true)) {
                        fuzzyMatch = commandListener;
                        break;
                    }
                }
            }
        }
        return fuzzyMatching ? fuzzyMatch : null;
    }

    private boolean matches(String test, String match, boolean fuzzy) {
        return fuzzy ? match.toLowerCase().startsWith(test.toLowerCase()) : test.equalsIgnoreCase(match);
    }

    public CommandListener matchCommand(String commandArguments) {
        return matchCommand(commandArguments, false);
    }

    public CommandListener matchCommand(String commandArguments, boolean useAliases) {
        return matchCommand(COMMANDS, commandArguments, useAliases);
    }

    public <T extends CommandListener> T matchCommand(ArrayList<T> commandList, String command) {
        return getCommandFor(commandList, command, true);
    }

    public <T extends CommandListener> T matchCommand(ArrayList<T> commandList, String command, boolean useAliases) {
        return getCommandFor(commandList, command, useAliases, true);
    }

    public ArrayList<CommandMethod> getCommandMethods(CommandListener commandListener) {
        ArrayList<CommandMethod> methods = new ArrayList<>();
        for (Method method : commandListener.getClass().getDeclaredMethods()) {
            Command cmd = method.getAnnotation(Command.class);
            if (cmd != null) {
                methods.add(new CommandMethod(cmd, method));
            }
        }
        return methods;
    }

    public CommandMethod getCommandMethod(CommandListener commandListener, CommandEvent commandEvent) {
        for (CommandMethod method : getCommandMethods(commandListener)) {
            if (!isValid(method, commandEvent)) {
                continue;
            }

            Command parent = commandListener.getClass().getAnnotation(Command.class);
            Command cmd = method.getCommand();

            if (parent != null) {
                if (commandEvent.argsLength() <= 0) {
                    if (!cmd.command().equalsIgnoreCase("")) {
                        return null;
                    }
                } else if (!matches(commandEvent.arg(0), commandEvent.command(), false)) {
                    return null;
                }
            }

            Target target = method.getAccessor().getAnnotation(Target.class);
            if (target != null) {
                if (!target.target().isAssignableFrom(commandEvent.sender().getClass())) {
                    if (!target.accessMessage().isEmpty()) {
                        commandEvent.respond(ResponseLevel.SEVERE, target.accessMessage());
                        return null;
                    }
                    continue;
                }
            }
            return method;
        }
        return null;
    }

    public boolean isValid(CommandMethod commandMethod) {
        return isValid(commandMethod, CommandEvent.class);
    }

    public boolean isValid(CommandMethod commandMethod, CommandEvent commandEvent) {
        return isValid(commandMethod, commandEvent.getClass());
    }

    public boolean isValid(CommandMethod commandMethod, Class<? extends CommandEvent> type) {
        return commandMethod.getAccessor().getReturnType().equals(Boolean.class) && commandMethod.getAccessor().getParameterTypes().length == 1 && type.isAssignableFrom(commandMethod.getAccessor().getParameterTypes()[0]);
    }

    public boolean isParent(CommandListener commandListener) {
        return commandListener.getClass().getAnnotation(Command.class) != null;
    }

    public <T extends CommandSender> boolean onCommand(T sender, String args) {
        return onCommand(new CommandEvent<>(this, sender, args));
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String commandLabel, String[] args) {
        return onCommand(new CommandEvent<>(this, sender, args));
    }

    public <T extends CommandSender> boolean onCommand(CommandEvent<T> commandEvent) {
        try {
            CommandListener commandListener = getCommandFor(commandEvent.command());
            CommandMethod commandMethod = getCommandMethod(commandListener, commandEvent);
            if (commandMethod != null) {
                Command cmd = commandMethod.getCommand();
                if (!cmd.permission().isEmpty()) {
                    if (!commandEvent.canPerform(cmd.permission())) {
                        return true;
                    }
                }
                if (!(boolean) commandMethod.getAccessor().invoke(commandEvent)) {
                    commandEvent.respond(cmd.usage());
                }

                commandEvent.respond(ResponseLevel.SEVERE, getCommandNotFoundMessage());

                if (willSuggestCommands()) {
                    ArrayList<String> possibleSuggestions = new ArrayList<>();
                    if (isParent(commandListener)) {
                        for (CommandMethod method : getCommandMethods(commandListener)) {
                            if (!method.getCommand().command().isEmpty()) {
                                possibleSuggestions.add(method.getCommand().command());
                            }
                        }
                    } else {
                        for (CommandListener listener : getRegisteredCommands()) {
                            if (isParent(listener)) {
                                possibleSuggestions.add(listener.getClass().getAnnotation(Command.class).command());
                            }
                            for (CommandMethod method : getCommandMethods(listener)) {
                                if (!method.getCommand().command().isEmpty()) {
                                    possibleSuggestions.add(method.getCommand().command());
                                }
                            }
                        }
                    }
                    Suggestion suggestion = new Suggestion(cmd.command(), possibleSuggestions.toArray(new String[0]));
                    commandEvent.respond(ResponseLevel.SEVERE, "{c1}Did you mean: " + ChatColor.ITALIC + StringUtil.combineArray(0, suggestion.getSuggestions(), ChatColor.RESET + "{c1}, " + ChatColor.ITALIC));
                }
                return true;
            }


        } catch (Exception e) {
            commandEvent.respond(ResponseLevel.SEVERE, getErrorMessage());
            return true;
        }
        return false;
    }
}