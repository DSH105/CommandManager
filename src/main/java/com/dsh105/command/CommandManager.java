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
import com.dsh105.command.registration.CommandRegistry;
import com.dsh105.command.registration.DynamicPluginCommand;
import com.dsh105.commodus.GeneralUtil;
import com.dsh105.commodus.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;

public class CommandManager implements ICommandManager {

    public static final String DEFAULT_USAGE = "Unknown command. Type \"/help\" for help.";

    private final static Logger LOGGER = Logger.getLogger("CommandManager");

    private final static String INVALID_SUB_COMMAND_WARNING = "%s has attempted to register an invalid command to %s (%s -> %s). %s";
    private final static String INVALID_COMMAND_WARNING = "%s has attempted to register an invalid command (%s -> %s). %s";
    private final static String COMMAND_REQUIREMENTS = "Command method can only have one parameter (" + CommandEvent.class.getCanonicalName() + ") and return a boolean.";

    private final CommandRegistry REGISTRY;
    private final ArrayList<CommandListener> COMMANDS = new ArrayList<>();
    private final HashMap<CommandListener, ArrayList<CommandMethod>> SUB_COMMANDS = new HashMap<>();
    private HelpService HELP_SERVICE;

    /*
     * Messages
     */
    private String NO_PERMISSION = "You are not permitted to do that.";
    private String NO_ACCESS = "You do not have access to this from here.";
    private String ERROR = "Something unexpected happened. Please see the console for any errors and report them immediately.";
    private String COMMAND_NOT_FOUND = "That command does not exist.";

    private Plugin owningPlugin;

    private String responsePrefix;
    private boolean suggestCommands;
    private ChatColor highlightColour = ChatColor.WHITE;
    private ChatColor formatColour = ChatColor.WHITE;

    public CommandManager(Plugin owningPlugin) {
        this(owningPlugin, "");
    }

    public CommandManager(Plugin owningPlugin, String responsePrefix) {
        this(owningPlugin, new CommandRegistry(owningPlugin), true, responsePrefix);
    }

    protected CommandManager(Plugin owningPlugin, CommandRegistry commandRegistry, boolean enableHelpService, String responsePrefix) {
        this.owningPlugin = owningPlugin;
        this.responsePrefix = responsePrefix;
        REGISTRY = commandRegistry;
        if (enableHelpService) {
            this.enableHelpService();
        }
    }

    @Override
    public Plugin getPlugin() {
        return owningPlugin;
    }

    @Override
    public List<CommandListener> getRegisteredCommands() {
        return Collections.unmodifiableList(COMMANDS);
    }

    @Override
    public List<String> getRegisteredCommandNames() {
        ArrayList<String> commands = new ArrayList<>();
        for (CommandListener listener : getRegisteredCommands()) {
            for (CommandMethod method : getCommandMethods(listener)) {
                commands.add(method.getCommand().command());
            }
        }
        return Collections.unmodifiableList(commands);
    }

    @Override
    public Map<CommandListener, ArrayList<CommandMethod>> getRegisteredSubCommands() {
        return Collections.unmodifiableMap(SUB_COMMANDS);
    }

    @Override
    public List<CommandMethod> getRegisteredSubCommands(CommandListener commandListener) {
        ArrayList<CommandMethod> methods = SUB_COMMANDS.get(commandListener);
        if (methods == null) {
            methods = new ArrayList<>();
        }
        return Collections.unmodifiableList(methods);
    }

    @Override
    public HelpService getHelpService() {
        return HELP_SERVICE;
    }

    private void enableHelpService() {
        HELP_SERVICE = new HelpService(this);
    }

    @Override
    public void refreshHelpService() {
        if (HELP_SERVICE != null) {
            HELP_SERVICE.prepare();
        }
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
        return NO_PERMISSION;
    }

    @Override
    public void setNoPermissionMessage(String NO_PERMISSION) {
        this.NO_PERMISSION = NO_PERMISSION;
    }

    @Override
    public String getNoAccessMessage() {
        return NO_ACCESS;
    }

    @Override
    public void setNoAccessMessage(String noAccessMessage) {
        this.NO_ACCESS = noAccessMessage;
    }

    @Override
    public String getErrorMessage() {
        return ERROR;
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        this.ERROR = errorMessage;
    }

    @Override
    public String getCommandNotFoundMessage() {
        return COMMAND_NOT_FOUND;
    }

    @Override
    public void setCommandNotFoundMessage(String commandNotFoundMessage) {
        this.COMMAND_NOT_FOUND = commandNotFoundMessage;
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
        if (COMMANDS.contains(commandListener)) {
            return;
        }

        ArrayList<CommandMethod> bukkitRegistration = new ArrayList<>();
        if (getCommandMethods(commandListener).isEmpty()) {
            LOGGER.warning(owningPlugin.getName() + " has registered a command listener with no valid command methods: " + commandListener.getClass().getCanonicalName() + ".");
        }
        for (CommandMethod commandMethod : getCommandMethods(commandListener)) {
            if (!isValid(commandMethod)) {
                StringBuilder commandRequirements = new StringBuilder();
                if (!commandMethod.getAccessor().getReturnType().equals(boolean.class)) {
                    commandRequirements.append("Command method must return a BOOLEAN");
                }
                if (!(commandMethod.getAccessor().getParameterTypes().length == 1 && CommandEvent.class.isAssignableFrom(commandMethod.getAccessor().getParameterTypes()[0]))) {
                    if (commandRequirements.length() == 0) {
                        commandRequirements.append("Command method ");
                    } else {
                        commandRequirements.append(" and");
                    }
                    commandRequirements.append(" can only have one parameter");
                    commandRequirements.append("(");
                    commandRequirements.append(CommandEvent.class.getCanonicalName());
                    commandRequirements.append(")");
                }
                throw new CommandInvalidException(String.format(INVALID_COMMAND_WARNING, owningPlugin.getName(), commandListener.getClass().getCanonicalName(), commandMethod.getAccessor().getName(), commandRequirements.toString()));
            }
            bukkitRegistration.add(commandMethod);
        }
        COMMANDS.add(commandListener);

        if (REGISTRY != null) {
            // Register all commands to Bukkit
            for (CommandMethod commandMethod : bukkitRegistration) {
                Command command = commandMethod.getCommand();
                List<String> aliases = new ArrayList<>();
                for (String alias : command.aliases()) {
                    aliases.add(alias.split("\\s")[0]);
                }
                REGISTRY.register(new DynamicPluginCommand(command.command().split("\\s")[0], new String[0], command.description().replace("{c1}", getFormatColour() + "").replace("{c2}", getHighlightColour() + ""), command.usage().replace("{c1}", getFormatColour() + "").replace("{c2}", getHighlightColour() + ""), this, owningPlugin));
            }
        }

        refreshHelpService();
    }

    @Override
    public void registerSubCommands(CommandListener registerTo, CommandListener parentListener) {
        for (Method method : parentListener.getClass().getDeclaredMethods()) {
            if (method.getAnnotation(Command.class) != null) {
                registerSubCommand(registerTo, parentListener, method.getName());
            }
        }
    }

    @Override
    public void registerSubCommand(CommandListener registerTo, CommandListener parentListener, String methodName) {
        if (!isParent(registerTo)) {
            throw new CommandInvalidException(String.format(INVALID_SUB_COMMAND_WARNING, owningPlugin.getName(), registerTo.getClass().getCanonicalName(), parentListener.getClass().getCanonicalName(), methodName, ". Class must have a @Command annotation."));
        }

        final Command parent = registerTo.getClass().getAnnotation(Command.class);
        Method method = new Reflection().reflect(parentListener.getClass()).getSafeMethod(methodName).member();
        final Command cmd = method.getAnnotation(Command.class);
        if (cmd == null) {
            throw new CommandInvalidException(String.format(INVALID_SUB_COMMAND_WARNING, owningPlugin.getName(), registerTo.getClass().getCanonicalName(), parentListener.getClass().getCanonicalName(), methodName, ". Method must have a @Command annotation"));
        }

        CommandMethod commandMethod = new CommandMethod(parentListener, new Command() {
            @Override
            public String description() {
                return cmd.description();
            }

            @Override
            public String command() {
                // Combine the two - Sub command :D
                return parent.command() + " " + cmd.command();
            }

            @Override
            public String permission() {
                return cmd.permission();
            }

            @Override
            public String[] aliases() {
                // Keep aliases for sub commands the same
                return cmd.aliases();
            }

            @Override
            public String[] help() {
                return cmd.help();
            }

            @Override
            public String usage() {
                return cmd.usage();
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return cmd.annotationType();
            }
        }, method);

        if (!isValid(commandMethod)) {
            throw new CommandInvalidException(String.format(INVALID_SUB_COMMAND_WARNING, owningPlugin.getName(), registerTo.getClass().getCanonicalName(), parentListener.getClass().getCanonicalName(), methodName, COMMAND_REQUIREMENTS));
        }

        if (SUB_COMMANDS.get(registerTo) != null && SUB_COMMANDS.get(registerTo).contains(commandMethod)) {
            return;
        }

        ArrayList<CommandMethod> existing = SUB_COMMANDS.get(registerTo);
        if (existing == null) {
            existing = new ArrayList<>();
        }
        existing.add(commandMethod);
        SUB_COMMANDS.put(registerTo, existing);

        // No need to register to Bukkit because it's a sub command

        refreshHelpService();
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
        try {
            registerSubCommand(registerTo, parentClass.newInstance(), methodName);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CommandInvalidException(String.format(INVALID_SUB_COMMAND_WARNING, owningPlugin.getName(), registerTo.getClass().getCanonicalName(), parentClass.getCanonicalName(), methodName, "Failed to instantiate parent class. Please use CommandManager#registerSubCommand(CommandListener.class, CommandListener.class, String.class) instead."), e);
        }
    }

    @Override
    public void unregister(CommandListener commandListener) {
        COMMANDS.remove(commandListener);

        if (REGISTRY != null) {
            for (CommandMethod commandMethod : getCommandMethods(commandListener)) {
                REGISTRY.unregister(commandMethod.getCommand().command().split("\\s")[0]);
            }
        }

        refreshHelpService();
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
    public ArrayList<CommandListener> getCommandsFor(String commandArguments) {
        return getCommandsFor(COMMANDS, commandArguments);
    }

    @Override
    public ArrayList<CommandListener> getCommandsFor(String commandArguments, boolean useAliases) {
        return getCommandsFor(COMMANDS, commandArguments, useAliases);
    }

    @Override
    public <T extends CommandListener> ArrayList<T> getCommandsFor(ArrayList<T> commandList, String command) {
        return getCommandsFor(commandList, command, true);
    }

    @Override
    public <T extends CommandListener> ArrayList<T> getCommandsFor(ArrayList<T> commandList, String command, boolean useAliases) {
        return getCommandsFor(commandList, command, useAliases, false);
    }

    @Override
    public <T extends CommandListener> ArrayList<T> getCommandsFor(ArrayList<T> commandList, String command, boolean useAliases, boolean fuzzyMatching) {
        ArrayList<T> matches = new ArrayList<>();
        ArrayList<T> fuzzyMatches = new ArrayList<>();
        for (T commandListener : commandList) {
            Command parent = commandListener.getClass().getAnnotation(Command.class);
            if (parent != null) {
                if (matches(parent.command().split("\\s")[0], command, false)) {
                    matches.add(commandListener);
                    continue;
                }
            }

            for (CommandMethod method : getCommandMethods(commandListener)) {
                Command cmd = method.getCommand();
                if (matches(cmd.command().split("\\s")[0], command, false)) {
                    matches.add(commandListener);
                }

                for (String alias : method.getCommand().aliases()) {
                    if (matches(alias.split("\\s")[0], command, false)) {
                        matches.add(commandListener);
                        break;
                    }
                }

                if (matches(cmd.command().split("\\s")[0], command, true)) {
                    fuzzyMatches.add(commandListener);
                    continue;
                }

                for (String alias : method.getCommand().aliases()) {
                    if (matches(alias.split("\\s")[0], command, true)) {
                        fuzzyMatches.add(commandListener);
                        break;
                    }
                }
            }
        }
        return fuzzyMatching && matches.isEmpty() ? fuzzyMatches : matches;
    }

    @Override
    public boolean matches(String test, String match, boolean fuzzy) {
        return test.equalsIgnoreCase(match) || (fuzzy && match.toLowerCase().startsWith(test.toLowerCase()));
    }

    @Override
    public ArrayList<CommandListener> getCommandMatchesFor(String commandArguments) {
        return getCommandMatchesFor(commandArguments, false);
    }

    @Override
    public ArrayList<CommandListener> getCommandMatchesFor(String commandArguments, boolean useAliases) {
        return getCommandMatchesFor(COMMANDS, commandArguments, useAliases);
    }

    @Override
    public <T extends CommandListener> ArrayList<T> getCommandMatchesFor(ArrayList<T> commandList, String command) {
        return getCommandsFor(commandList, command, true);
    }

    @Override
    public <T extends CommandListener> ArrayList<T> getCommandMatchesFor(ArrayList<T> commandList, String command, boolean useAliases) {
        return getCommandsFor(commandList, command, useAliases, true);
    }

    // TODO: Some caching here?
    @Override
    public ArrayList<CommandMethod> getCommandMethods(CommandListener commandListener) {
        ArrayList<CommandMethod> methods = new ArrayList<>();
        for (Method method : commandListener.getClass().getDeclaredMethods()) {
            Command cmd = method.getAnnotation(Command.class);
            if (cmd != null) {
                methods.add(new CommandMethod(commandListener, cmd, method));
                continue;
            }

            ParentCommand parentCommand = method.getAnnotation(ParentCommand.class);
            if (parentCommand != null) {
                methods.add(new CommandMethod(commandListener, commandListener.getClass().getAnnotation(Command.class), method));
            }
        }

        // Handle any sub-commands registered on-the-fly
        for (CommandMethod subCommand : getRegisteredSubCommands(commandListener)) {
            methods.add(subCommand);
        }

        return methods;
    }

    @Override
    public CommandMethod getCommandMethod(CommandListener commandListener, CommandEvent event) {
        ArrayList<CommandMethod> methods = getCommandMethods(commandListener);
        Collections.sort(methods);
        for (CommandMethod method : getCommandMethods(commandListener)) {
            if (!isValid(method)) {
                continue;
            }

            Command cmd = method.getCommand();

            ArrayList<String[]> commands = new ArrayList<>();
            commands.add(cmd.command().split("\\s"));
            for (String alias : cmd.aliases()) {
                commands.add(alias.split("\\s"));
            }

            for (String[] args : commands) {
                VariableMatcher variableMatcher = new VariableMatcher(StringUtil.combineArray(" ", args), event.input());

                // Test for any variables/regex and check if they meet the requirements
                if (variableMatcher.matches() || variableMatcher.testRegexVariables()) {
                    // We found a match, yay
                    return method;
                }
            }
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
        Method accessor = commandMethod.getAccessor();
        return accessor.getReturnType().equals(boolean.class) && accessor.getParameterTypes().length == 1 && type.isAssignableFrom(accessor.getParameterTypes()[0]);
    }

    @Override
    public boolean isParent(CommandListener commandListener) {
        return commandListener.getClass().isAnnotationPresent(Command.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String commandLabel, String[] args) {
        return onCommand(new CommandEvent<>(this, commandLabel, sender, args));
    }

    @Override
    public <T extends CommandSender> boolean onCommand(T sender, String args) {
        return onCommand(new CommandEvent<>(this, sender, args));
    }

    @Override
    public <T extends CommandSender> boolean onCommand(CommandEvent<T> event) {
        for (CommandListener commandListener : getCommandsFor(event.command())) {
            CommandMethod commandMethod = getCommandMethod(commandListener, event);
            if (commandMethod != null) {

                Command parent = commandListener.getClass().getAnnotation(Command.class);
                Command command = commandMethod.getCommand();

                // Pair up a final matcher that the listener can use
                event.setVariableMatcher(new VariableMatcher(command.command(), event.input()));

                if (parent != null) {
                    if (!parent.permission().isEmpty() && !event.canPerform(parent.permission())) {
                        continue;
                    }
                }

                try {
                    Class<?> paramType = CommandSender.class;
                    Type[] genericParameterTypes = commandMethod.getAccessor().getGenericParameterTypes();
                    for (Type genericType : genericParameterTypes) {
                        if (genericType instanceof ParameterizedType) {
                            ParameterizedType parameterizedType = (ParameterizedType) genericType;
                            Type[] paramArgTypes = parameterizedType.getActualTypeArguments();
                            for (Type paramArgType : paramArgTypes) {
                                if (paramArgType != null) {
                                    paramType = (Class<?>) paramArgType;
                                }
                            }
                        }
                    }

                    String permission = event.getVariableMatcher().replaceVariables(command.permission());

                    while (permission.endsWith(".")) {
                        permission = permission.substring(0, permission.length() - 1);
                    }

                    if (permission.isEmpty() || event.canPerform(permission)) {
                        if (paramType.isAssignableFrom(event.sender().getClass())) {
                            if (!(boolean) commandMethod.getAccessor().invoke(commandMethod.getParent(), event)) {
                                String usage;
                                if (command.usage().equals(DEFAULT_USAGE)) {
                                    usage = parent == null ? DEFAULT_USAGE : parent.usage();
                                } else {
                                    usage = command.usage();
                                }
                                event.respond(usage);
                            }
                        } else {
                            event.respond(ResponseLevel.SEVERE, getNoAccessMessage());
                        }
                    }
                    return true;
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    // Most likely the target type isn't valid
                } catch (Exception e) {
                    event.respond(ResponseLevel.SEVERE, getErrorMessage());
                    e.printStackTrace();
                }

                return true;
            }
        }

        // The command wasn't found :(
        event.respond(ResponseLevel.SEVERE, getCommandNotFoundMessage());

        if (willSuggestCommands()) {
            ArrayList<String> possibleSuggestions = new ArrayList<>();
            for (CommandListener listener : this) {
                if (isParent(listener)) {
                    possibleSuggestions.add(listener.getClass().getAnnotation(Command.class).command());
                }
                for (CommandMethod commandMethod : getCommandMethods(listener)) {
                    if (!commandMethod.getCommand().command().isEmpty()) {
                        possibleSuggestions.add(commandMethod.getCommand().command());
                    }
                }
            }
            Suggestion suggestion = new Suggestion(event.command(), possibleSuggestions.toArray(StringUtil.EMPTY_STRING_ARRAY));
            if (suggestion.getSuggestions().size() > 0) {
                event.respond(ResponseLevel.SEVERE, "Did you mean: " + ChatColor.ITALIC + StringUtil.combine(ChatColor.RESET + "{c1}, " + ChatColor.ITALIC, suggestion.getSuggestions()));
            }
        }
        return true;
    }

    @Override
    public ListIterator<CommandListener> iterator() {
        return getRegisteredCommands().listIterator();
    }
}