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

import com.captainbern.reflection.Reflection;
import com.dsh105.command.exceptions.CommandInvalidException;
import com.dsh105.commodus.StringUtil;
import com.dsh105.commodus.paginator.Paginator;
import com.dsh105.powermessage.core.PowerMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

public class CommandManager implements ICommandManager {

    private final static Logger LOGGER = Logger.getLogger("CommandManager");

    private final ArrayList<CommandListener> COMMANDS = new ArrayList<>();
    private final HashMap<CommandListener, CommandMethod> SUB_COMMANDS = new HashMap<>();

    /*
     * Messages
     */
    private String noPermissionMessage = "You are not permitted to do that.";
    private String noAccessMessage = "You do not have access to this from here.";
    private String errorMessage = "Something unexpected happened. Please see the console for any errors and report them immediately.";
    private String commandNotFoundMessage = "That command does not exist.";

    private Plugin plugin;
    private Paginator<PowerMessage> paginator = new Paginator<>();

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

    @Override
    public List<CommandListener> getRegisteredCommands() {
        return Collections.unmodifiableList(COMMANDS);
    }

    @Override
    public Map<CommandListener, CommandMethod> getRegisteredSubCommands() {
        return Collections.unmodifiableMap(SUB_COMMANDS);
    }

    @Override
    public List<CommandMethod> getRegisteredSubCommands(CommandListener commandListener) {
        ArrayList<CommandMethod> subCommands = new ArrayList<>();
        for (Map.Entry<CommandListener, CommandMethod> entry : getRegisteredSubCommands().entrySet()) {
            if (entry.getKey().equals(commandListener)) {
                subCommands.add(entry.getValue());
            }
        }
        return Collections.unmodifiableList(subCommands);
    }

    @Override
    public Paginator<PowerMessage> getPaginator() {
        return paginator;
    }

    @Override
    public String getResponsePrefix() {
        return responsePrefix;
    }

    @Override
    public void setResponsePrefix(String responsePrefix) {
        this.responsePrefix = responsePrefix;
    }

    @Override
    public ChatColor getHighlightColour() {
        return highlightColour;
    }

    @Override
    public void setHighlightColour(ChatColor highlightColour) {
        this.highlightColour = highlightColour;
    }

    @Override
    public ChatColor getFormatColour() {
        return formatColour;
    }

    @Override
    public void setFormatColour(ChatColor formatColour) {
        this.formatColour = formatColour;
    }

    @Override
    public String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    @Override
    public void setNoPermissionMessage(String noPermissionMessage) {
        this.noPermissionMessage = noPermissionMessage;
    }

    @Override
    public String getNoAccessMessage() {
        return noAccessMessage;
    }

    @Override
    public void setNoAccessMessage(String noAccessMessage) {
        this.noAccessMessage = noAccessMessage;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String getCommandNotFoundMessage() {
        return commandNotFoundMessage;
    }

    @Override
    public void setCommandNotFoundMessage(String commandNotFoundMessage) {
        this.commandNotFoundMessage = commandNotFoundMessage;
    }

    @Override
    public boolean willSuggestCommands() {
        return suggestCommands;
    }

    @Override
    public void setSuggestCommands(boolean suggestCommands) {
        this.suggestCommands = suggestCommands;
    }

    @Override
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

    @Override
    public void registerSubCommands(CommandListener registerTo, CommandListener parent) {
        registerSubCommands(registerTo, parent.getClass());
    }

    @Override
    public void registerSubCommand(CommandListener registerTo, CommandListener parent, String methodName) {
        registerSubCommand(registerTo, parent.getClass(), methodName);
    }

    @Override
    public void registerSubCommands(CommandListener registerTo, Class<? extends CommandListener> parentClass) {
        for (Method method : parentClass.getDeclaredMethods()) {
            if (method.getAnnotation(Command.class) != null) {
                registerSubCommand(registerTo, parentClass, method.getName());
            }
        }
    }

    @Override
    public void registerSubCommand(CommandListener registerTo, Class<? extends CommandListener> parentClass, String methodName) {
        Method method = new Reflection().reflect(parentClass).getSafeMethod(methodName).member();
        Command cmd = method.getAnnotation(Command.class);
        if (cmd == null) {
            throw new CommandInvalidException(plugin.getName() + " has attempted to register an invalid command to " + registerTo.getClass().getCanonicalName() + ": " + parentClass.getCanonicalName() + "#" + methodName + ". Method must have a @Command annotation");
        }

        CommandMethod commandMethod = new CommandMethod(cmd, method);
        if (!isValid(commandMethod)) {
            throw new CommandInvalidException(plugin.getName() + " has attempted to register an invalid command to " + registerTo.getClass().getCanonicalName() + ": " + parentClass.getCanonicalName() + "#" + methodName + ". Command method can only have one parameter that MUST extend " + CommandEvent.class.getCanonicalName() + " and return a BOOLEAN.");
        }
        SUB_COMMANDS.put(registerTo, commandMethod);
    }

    @Override
    public void unregister(CommandListener commandListener) {
        COMMANDS.remove(commandListener);
    }

    @Override
    public <T extends CommandListener> ArrayList<T> getCommandsOfType(Class<T> type) {
        ArrayList<T> commands = new ArrayList<>();
        for (CommandListener commandListener : COMMANDS) {
            if (type.getClass().isAssignableFrom(commandListener.getClass())) {
                commands.add((T) commandListener);
            }
        }
        return commands;
    }

    @Override
    public <T extends CommandListener> T getCommandOfType(Class<T> type) {
        ArrayList<T> commands = getCommandsOfType(type);
        if (commands.isEmpty()) {
            return null;
        }
        return commands.get(0);
    }

    @Override
    public CommandListener getCommandFor(String commandArguments) {
        return getCommandFor(COMMANDS, commandArguments);
    }

    @Override
    public CommandListener getCommandFor(String commandArguments, boolean useAliases) {
        return getCommandFor(COMMANDS, commandArguments, useAliases);
    }

    @Override
    public <T extends CommandListener> T getCommandFor(ArrayList<T> commandList, String command) {
        return getCommandFor(commandList, command, true);
    }

    @Override
    public <T extends CommandListener> T getCommandFor(ArrayList<T> commandList, String command, boolean useAliases) {
        return getCommandFor(commandList, command, useAliases, false);
    }

    // TODO: Handle multiple methods having the same command name
    @Override
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

    @Override
    public boolean matches(String test, String match, boolean fuzzy) {
        return fuzzy ? match.toLowerCase().startsWith(test.toLowerCase()) : test.equalsIgnoreCase(match);
    }

    @Override
    public CommandListener matchCommand(String commandArguments) {
        return matchCommand(commandArguments, false);
    }

    @Override
    public CommandListener matchCommand(String commandArguments, boolean useAliases) {
        return matchCommand(COMMANDS, commandArguments, useAliases);
    }

    @Override
    public <T extends CommandListener> T matchCommand(ArrayList<T> commandList, String command) {
        return getCommandFor(commandList, command, true);
    }

    @Override
    public <T extends CommandListener> T matchCommand(ArrayList<T> commandList, String command, boolean useAliases) {
        return getCommandFor(commandList, command, useAliases, true);
    }

    @Override
    public ArrayList<CommandMethod> getCommandMethods(CommandListener commandListener) {
        ArrayList<CommandMethod> methods = new ArrayList<>();
        for (Method method : commandListener.getClass().getDeclaredMethods()) {
            Command cmd = method.getAnnotation(Command.class);
            if (cmd != null) {
                methods.add(new CommandMethod(cmd, method));
            }
        }

        // Handle any sub-commands registered on-the-fly
        for (CommandMethod subCommand : getRegisteredSubCommands().values()) {
            methods.add(subCommand);
        }

        return methods;
    }

    @Override
    public CommandMethod getCommandMethod(CommandListener commandListener, CommandEvent commandEvent) {
        for (CommandMethod method : getCommandMethods(commandListener)) {
            if (!isValid(method, commandEvent)) {
                return null;
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
            return method;
        }
        return null;
    }

    @Override
    public Method getParentCommandMethod(CommandListener commandListener) {
        for (Method method : commandListener.getClass().getDeclaredMethods()) {
            if (method.getAnnotation(ParentCommand.class) != null) {
                return method;
            }
        }
        return null;
    }

    @Override
    public boolean isValid(CommandMethod commandMethod) {
        return isValid(commandMethod, CommandEvent.class);
    }

    @Override
    public boolean isValid(CommandMethod commandMethod, CommandEvent commandEvent) {
        return isValid(commandMethod, commandEvent.getClass());
    }

    @Override
    public boolean isValid(CommandMethod commandMethod, Class<? extends CommandEvent> type) {
        return commandMethod.getAccessor().getReturnType().equals(Boolean.class) && commandMethod.getAccessor().getParameterTypes().length == 1 && type.isAssignableFrom(commandMethod.getAccessor().getParameterTypes()[0]);
    }

    @Override
    public boolean isParent(CommandListener commandListener) {
        return commandListener.getClass().getAnnotation(Command.class) != null;
    }

    @Override
    public <T extends CommandSender> boolean onCommand(T sender, String args) {
        return onCommand(new CommandEvent<>(this, sender, args));
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String commandLabel, String[] args) {
        return onCommand(new CommandEvent<>(this, sender, args));
    }

    @Override
    public <T extends CommandSender> boolean onCommand(CommandEvent<T> commandEvent) {
        Method method = null;
        Command cmd = null;
        try {
            CommandListener commandListener = getCommandFor(commandEvent.command());
            if (commandEvent.argsLength() == 0) {
                Method parentMethod = getParentCommandMethod(commandListener);
                if (getParentCommandMethod(commandListener) != null) {
                    method = parentMethod;
                    cmd = parentMethod.getAnnotation(Command.class);
                }
            } else {
                CommandMethod command = getCommandMethod(commandListener, commandEvent);
                if (command == null) {
                    return false;
                }
                cmd = command.getCommand();
                method = command.getAccessor();
            }

            if (cmd != null && method != null) {
                if (!cmd.permission().isEmpty()) {
                    if (!commandEvent.canPerform(cmd.permission())) {
                        return true;
                    }
                }
                if (!(boolean) method.invoke(commandEvent)) {
                    commandEvent.respond(cmd.usage());
                }

                commandEvent.respond(ResponseLevel.SEVERE, getCommandNotFoundMessage());

                if (willSuggestCommands()) {
                    ArrayList<String> possibleSuggestions = new ArrayList<>();
                    if (isParent(commandListener)) {
                        for (CommandMethod commandMethod : getCommandMethods(commandListener)) {
                            if (!commandMethod.getCommand().command().isEmpty()) {
                                possibleSuggestions.add(commandMethod.getCommand().command());
                            }
                        }
                    } else {
                        for (CommandListener listener : getRegisteredCommands()) {
                            if (isParent(listener)) {
                                possibleSuggestions.add(listener.getClass().getAnnotation(Command.class).command());
                            }
                            for (CommandMethod commandMethod : getCommandMethods(listener)) {
                                if (!commandMethod.getCommand().command().isEmpty()) {
                                    possibleSuggestions.add(commandMethod.getCommand().command());
                                }
                            }
                        }
                    }
                    Suggestion suggestion = new Suggestion(cmd.command(), possibleSuggestions.toArray(new String[0]));
                    commandEvent.respond(ResponseLevel.SEVERE, "{c1}Did you mean: " + ChatColor.ITALIC + StringUtil.combineArray(0, suggestion.getSuggestions(), ChatColor.RESET + "{c1}, " + ChatColor.ITALIC));
                }
            }
        } catch (Exception e) {
            commandEvent.respond(ResponseLevel.SEVERE, getErrorMessage());
            return true;
        }
        return false;
    }
}