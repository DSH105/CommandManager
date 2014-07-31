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

import com.dsh105.command.exception.CommandInvocationException;
import com.dsh105.command.exception.CommandRegistrationException;
import com.dsh105.command.exception.InvalidCommandException;
import com.dsh105.commodus.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public abstract class SimpleCommandManager extends CommandMatcher implements ICommandManager {

    public static final String DEFAULT_USAGE = "Usage: /<command>";
    private final static String INVALID_COMMAND_WARNING = "%s has attempted to register an invalid command (%s -> %s). %s";

    private final Plugin owningPlugin;
    private final Messenger messenger;

    private String responsePrefix;
    private boolean suggestCommands;
    private boolean showErrorMessage;

    /**
     * List of all registered listeners
     */
    private final ArrayList<CommandListener> LISTENERS = new ArrayList<>();

    /**
     * Maps command listeners to command handlers
     */
    private final HashMap<CommandListener, ArrayList<CommandHandler>> COMMANDS = new HashMap<>();


    private final HashMap<CommandListener, ArrayList<String>> COMMAND_NAMES = new HashMap<>();

    public SimpleCommandManager(Plugin owningPlugin) {
        this(owningPlugin, "");
    }

    public SimpleCommandManager(Plugin owningPlugin, String responsePrefix) {
        this.owningPlugin = owningPlugin;
        this.responsePrefix = responsePrefix;
        this.messenger = new Messenger();
    }

    @Override
    public Plugin getPlugin() {
        return owningPlugin;
    }

    @Override
    public Iterator<CommandHandler> iterator() {
        return getAllRegisteredCommands().iterator();
    }

    @Override
    public List<CommandListener> getRegisteredListeners() {
        return Collections.unmodifiableList(LISTENERS);
    }

    @Override
    public List<CommandHandler> getAllRegisteredCommands() {
        ArrayList<CommandHandler> commandHandlers = new ArrayList<>();
        for (ArrayList<CommandHandler> listenerHandlers : getRegisteredCommands().values()) {
            commandHandlers.addAll(listenerHandlers);
        }
        return Collections.unmodifiableList(commandHandlers);
    }

    @Override
    public Map<CommandListener, ArrayList<CommandHandler>> getRegisteredCommands() {
        return Collections.unmodifiableMap(COMMANDS);
    }

    @Override
    public List<CommandHandler> getRegisteredCommands(CommandListener parentListener) {
        ArrayList<CommandHandler> commandHandlers = getRegisteredCommands().get(parentListener);
        if (commandHandlers == null) {
            commandHandlers = new ArrayList<>();
        }
        return Collections.unmodifiableList(commandHandlers);
    }

    @Override
    public List<String> getAllRegisteredCommandNames() {
        ArrayList<String> commandNames = new ArrayList<>();
        for (ArrayList<String> listenerHandlers : getRegisteredCommandNames().values()) {
            commandNames.addAll(listenerHandlers);
        }
        return Collections.unmodifiableList(commandNames);
    }

    @Override
    public List<String> getRegisteredCommandNames(CommandListener parentListener) {
        ArrayList<String> commandNames = getRegisteredCommandNames().get(parentListener);
        if (commandNames == null) {
            commandNames = new ArrayList<>();
        }
        return Collections.unmodifiableList(commandNames);
    }

    @Override
    public Map<CommandListener, ArrayList<String>> getRegisteredCommandNames() {
        return Collections.unmodifiableMap(COMMAND_NAMES);
    }

    @Override
    public boolean isValid(Method accessor) {
        return isValid(accessor, CommandEvent.class);
    }

    @Override
    public boolean isValid(Method accessor, CommandEvent commandEvent) {
        return isValid(accessor, commandEvent.getClass());
    }

    @Override
    public boolean isValid(Method accessor, Class<? extends CommandEvent> type) {
        return accessor.getReturnType().equals(boolean.class) && accessor.getParameterTypes().length == 1 && type.isAssignableFrom(accessor.getParameterTypes()[0]);
    }

    @Override
    public Messenger getMessenger() {
        return messenger;
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
    public void setSuggestCommands(boolean suggestCommands) {
        this.suggestCommands = suggestCommands;
    }

    @Override
    public boolean willSuggestCommands() {
        return suggestCommands;
    }

    @Override
    public void setShowErrorMessage(boolean showErrorMessage) {
        this.showErrorMessage = showErrorMessage;
    }

    @Override
    public boolean shouldShowErrorMessage() {
        return showErrorMessage;
    }

    @Override
    public void refreshHelp() {
        if (getHelpService() != null) {
            getHelpService().prepare();
        }
    }

    /*
     * Begin registration and execution of commands
     */

    @Override
    public void register(CommandListener commandListener) {
        if (LISTENERS.contains(commandListener)) {
            throw new CommandRegistrationException("CommandListener already registered!");
        }

        ArrayList<CommandHandler> registrationQueue = new ArrayList<>();

        // Parent command info used in nested commands
        Command commandParent = commandListener.getClass().getAnnotation(Command.class);
        String prefix = commandParent == null ? "" : commandParent.command() + " ";

        // Search through all methods for traces of command handlers
        for (Method method : commandListener.getClass().getDeclaredMethods()) {
            Command command = method.getAnnotation(Command.class);
            if (command != null) {
                // Make sure this method is fine to register
                ensureValidity(commandListener, method);

                NestedCommand nestedCommand = method.getAnnotation(NestedCommand.class);
                if (nestedCommand != null) {
                    // This can't be a 'nested' command if there is no provided parent prefix
                    if (prefix.isEmpty() && nestedCommand.parentCommand().isEmpty()) {
                        throw new InvalidCommandException(String.format(INVALID_COMMAND_WARNING, getPlugin().getName(), commandListener.getClass().getCanonicalName(), method.getName(), "Either: Provide a value for \"parentCommand\" in the annotation, OR add the @Command annotation to the CommandListener class."));
                    }

                    registrationQueue.add(buildNestedCommand(commandListener, commandListener, nestedCommand.parentCommand().isEmpty() ? prefix : nestedCommand.parentCommand(), method, command, commandParent == null ? new String[0] : commandParent.aliases()));
                    continue;
                }

                // This command isn't a nested command, so we can treat it like normal
                registrationQueue.add(new CommandHandler(commandListener, commandListener, command, method));
            }
        }

        if (!registrationQueue.isEmpty()) {
            // Map commands to their appropriate desinations
            LISTENERS.add(commandListener);
            COMMANDS.put(commandListener, registrationQueue);

            // Finally, register everything with Bukkit
            if (getRegistry() != null) {
                getRegistry().register(registrationQueue);
            }
            this.updateNeeded = true;
            refreshHelp();
        }
    }

    @Override
    public void nestCommandsIn(CommandListener origin, CommandListener destination) {
        ArrayList<CommandHandler> registrationQueue = new ArrayList<>();

        // Retrieve the prefix for this nested command
        Command commandParent = destination.getClass().getAnnotation(Command.class);
        String prefix = commandParent == null ? "" : commandParent.command() + " ";
        for (Method method : origin.getClass().getDeclaredMethods()) {
            Command command = method.getAnnotation(Command.class);
            if (command != null) {
                ensureValidity(origin, method);

                NestedCommand nestedCommand = method.getAnnotation(NestedCommand.class);
                if (nestedCommand != null) {
                    // This can't be a 'nested' command if there is no provided parent prefix
                    if (!nestedCommand.parentCommand().isEmpty()) {
                        prefix = nestedCommand.parentCommand();
                    }
                }

                if (prefix.isEmpty()) {
                    throw new InvalidCommandException(String.format(INVALID_COMMAND_WARNING, getPlugin().getName(), origin.getClass().getCanonicalName(), method.getName(), "Either: Provide a value for \"parentCommand\" in the annotation, OR add the @Command annotation to the CommandListener class."));
                }

                registrationQueue.add(buildNestedCommand(origin, destination, prefix, method, command, commandParent == null ? new String[0] : commandParent.aliases()));
            }
        }

        if (!registrationQueue.isEmpty()) {
            if (!LISTENERS.contains(destination)) {
                LISTENERS.add(destination);
            }
            COMMANDS.put(destination, registrationQueue);
            if (getRegistry() != null) {
                getRegistry().register(registrationQueue);
            }
            this.updateNeeded = true;
            refreshHelp();
        }
    }

    @Override
    public void unregister(CommandListener commandListener) {
        // This will also remove any nested commands that have been registered to this listener
        LISTENERS.remove(commandListener);
        COMMANDS.remove(commandListener);
        for (CommandHandler commandHandler : getRegisteredCommands(commandListener)) {
            // Unregister from Bukkit so that this command is no longer fired
            if (getRegistry() != null) {
                getRegistry().unregister(commandHandler.getCommand().command().split("\\s")[0]);
            }
        }

        this.updateNeeded = true;
        refreshHelp();
    }

    private CommandHandler buildNestedCommand(CommandListener handlerOrigin, CommandListener registerTo, final String parentPrefix, Method methodOrigin, final Command command, final String... parentAliases) {
        return new CommandHandler(handlerOrigin, registerTo, new Command() {
            @Override
            public String command() {
                return parentPrefix + command.command();
            }

            @Override
            public String description() {
                return command.description();
            }

            @Override
            public String[] permission() {
                return command.permission();
            }

            @Override
            public String[] aliases() {
                ArrayList<String> aliases = new ArrayList<>();
                aliases.add(parentPrefix + command.command());

                for (String alias : command.aliases()) {
                    // Append the parent prefix to all aliases
                    aliases.add(parentPrefix + alias);

                    // Append any existing parent aliases to this command as well
                    for (String parentAlias : parentAliases) {
                        aliases.add(parentAlias + " " + alias);
                    }
                }
                return aliases.toArray(StringUtil.EMPTY_STRING_ARRAY);
            }

            @Override
            public String[] help() {
                return command.help();
            }

            @Override
            public String usage() {
                return command.usage();
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return command.annotationType();
            }
        }, methodOrigin);
    }

    private void ensureValidity(CommandListener commandListener, Method method) {
        if (!isValid(method)) {
            StringBuilder requirements = new StringBuilder("Command handler ");
            if (!method.getReturnType().equals(boolean.class)) {
                requirements.append("must return a BOOLEAN");
            }

            if (!(method.getParameterTypes().length == 1 && CommandEvent.class.isAssignableFrom(method.getParameterTypes()[0]))) {
                if (requirements.length() > 0) {
                    requirements.append(" and ");
                }
                requirements.append("can only have one parameter (")
                            .append(CommandEvent.class.getCanonicalName())
                            .append(")");
            }

            throw new InvalidCommandException(String.format(INVALID_COMMAND_WARNING, owningPlugin.getName(), commandListener.getClass().getCanonicalName(), method.getName(), requirements.toString()));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        return onCommand(new CommandEvent<>(this, label, sender, args));
    }

    @Override
    public <T extends CommandSender> boolean onCommand(CommandEvent<T> event) {
        // Match a command for this event
        MatchedCommand matchedCommand = matchCommand(event.input());
        if (matchedCommand != null) {
            CommandHandler handler = matchedCommand.getCommandHandler();
            Command parent = handler.getParentCommand();
            Command command = handler.getCommand();

            // Pair up a variable matcher that utilises the executed command and syntax pattern
            event.setVariableMatcher(new VariableMatcher(matchedCommand.getMatchedLabel(), event.input()));

            // Ensure that the command handler accepts this type of CommandSender
            if (!handler.isSenderAccepted(event.sender())) {
                event.respond(ResponseLevel.SEVERE, getMessenger().getNoAccessMessage());
                return true;
            }

            // Build a permission list, inclusive of all parent permissions
            List<String> permissions = buildPermissionList(command, event.getVariableMatcher());
            if (parent != null) {
                permissions.addAll(buildPermissionList(parent, event.getVariableMatcher()));
            }

            if (permissions.size() <= 0 || event.canPerform(permissions.toArray(StringUtil.EMPTY_STRING_ARRAY))) {
                try {
                    // Execute the command handler
                    boolean executionResult = (boolean) handler.getAccessor().invoke(handler.getParent(), event);
                    if (!executionResult) {
                        // The handler didn't accept this command - deal with sending usage info
                        String usage;
                        if (command.usage().equals(DEFAULT_USAGE)) {
                            usage = parent == null ? DEFAULT_USAGE : parent.usage();
                        } else {
                            usage = command.usage();
                        }
                        event.respond(usage);
                    }
                } catch (Exception e) {
                    if (shouldShowErrorMessage()) {
                        event.respond(ResponseLevel.SEVERE, getMessenger().getErrorMessage());
                    }
                    throw new CommandInvocationException("Unhandled exception executing \"" + event.input() + "\" in " + owningPlugin.getName(), e);
                }
            }
            return true;
        }

        // Command wasn't found :(
        event.respond(ResponseLevel.SEVERE, getMessenger().getCommandNotFoundMessage());

        if (willSuggestCommands()) {
            // Find any suggestions from registered commands
            ArrayList<String> possibleSuggestions = new ArrayList<>();
            for (CommandHandler handler : matchCommands(event.command(), true, true)) {
                possibleSuggestions.add(handler.getCommandName());
            }

            Suggestion suggestion = new Suggestion(event.command(), possibleSuggestions);
            if (suggestion.getSuggestions().size() > 0) {
                event.respond(ResponseLevel.SEVERE, "Did you mean: " + ChatColor.ITALIC + StringUtil.combine(ChatColor.RESET + "{c1}, " + ChatColor.ITALIC, suggestion.getSuggestions()));
            }
        }
        return true;
    }

    private ArrayList<String> buildPermissionList(Command command, VariableMatcher variableMatcher) {
        ArrayList<String> permissions = new ArrayList<>();

        for (String permission : command.permission()) {
            // Replace any variables from the command event
            String matchedPermission = variableMatcher.replaceVariables(permission);

            // Remove all unwanted periods at the end of permission nodes
            while (permission.endsWith(".")) {
                permission = permission.substring(0, permission.length() - 1);
            }
            if (matchedPermission != null) {
                permissions.add(matchedPermission);
            }
        }

        return permissions;
    }
}