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
    private boolean showErrorMessage = true;
    private boolean showDefaultUsageAsCommandSyntax;

    /**
     * List of all registered listeners
     */
    private final HashSet<CommandListener> LISTENERS = new HashSet<>();

    /**
     * Maps command listeners to command handlers
     */
    private final HashMap<CommandListener, Set<CommandHandler>> COMMANDS = new HashMap<>();


    private final HashMap<CommandListener, Set<String>> COMMAND_NAMES = new HashMap<>();

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
    public Set<CommandListener> getRegisteredListeners() {
        return Collections.unmodifiableSet(LISTENERS);
    }

    @Override
    public Set<CommandHandler> getAllRegisteredCommands() {
        Set<CommandHandler> commandHandlers = new HashSet<>();
        for (Set<CommandHandler> listenerHandlers : getRegisteredCommands().values()) {
            commandHandlers.addAll(listenerHandlers);
        }
        return Collections.unmodifiableSet(commandHandlers);
    }

    @Override
    public Map<CommandListener, Set<CommandHandler>> getRegisteredCommands() {
        return Collections.unmodifiableMap(COMMANDS);
    }

    @Override
    public Set<CommandHandler> getRegisteredCommands(CommandListener parentListener) {
        Set<CommandHandler> commandHandlers = getRegisteredCommands().get(parentListener);
        if (commandHandlers == null) {
            commandHandlers = new HashSet<>();
        }
        return Collections.unmodifiableSet(commandHandlers);
    }

    @Override
    public Set<String> getAllRegisteredCommandNames() {
        Set<String> commandNames = new HashSet<>();
        for (Set<String> listenerHandlers : getRegisteredCommandNames().values()) {
            commandNames.addAll(listenerHandlers);
        }
        return Collections.unmodifiableSet(commandNames);
    }

    @Override
    public Set<String> getRegisteredCommandNames(CommandListener parentListener) {
        Set<String> commandNames = getRegisteredCommandNames().get(parentListener);
        if (commandNames == null) {
            commandNames = new HashSet<>();
        }
        return Collections.unmodifiableSet(commandNames);
    }

    @Override
    public Map<CommandListener, Set<String>> getRegisteredCommandNames() {
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

    public boolean willShowDefaultUsageAsCommandSyntax() {
        return showDefaultUsageAsCommandSyntax;
    }

    public void setShowDefaultUsageAsCommandSyntax(boolean showDefaultUsageAsCommandSyntax) {
        this.showDefaultUsageAsCommandSyntax = showDefaultUsageAsCommandSyntax;
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
    public void nestCommandsInregister(CommandListener commandListener) {
        if (LISTENERS.contains(commandListener)) {
            throw new CommandRegistrationException("CommandListener already registered!");
        }

        ArrayList<CommandHandler> registrationQueue = new ArrayList<>();

        // Parent command info used in nested commands
        Command commandParent = commandListener.getClass().getAnnotation(Command.class);

        // Search through all methods for traces of command handlers
        for (Method method : commandListener.getClass().getDeclaredMethods()) {
            Command command = method.getAnnotation(Command.class);
            if (command != null) {
                // Make sure this method is fine to register
                ensureValidity(commandListener, method);

                if (method.isAnnotationPresent(NestedCommand.class)) {
                    // These are handled later
                    continue;
                }

                // This command isn't a nested command, so we can treat it like normal
                registrationQueue.add(new CommandHandler(commandListener, commandListener, command, method));
                continue;
            }

            // Check if this method executes the parent command
            if (method.isAnnotationPresent(ParentCommand.class)) {
                registrationQueue.add(new CommandHandler(commandListener, commandListener, commandParent, method));
            }
        }

        // Handle nested commands - only those marked as a nested command
        nestCommandsIn(commandListener, commandListener, false);

        if (!registrationQueue.isEmpty()) {
            // Map commands to their appropriate destinations
            mapCommands(commandListener, registrationQueue);
            refreshHelp();
        }
    }

    @Override
    public void nestCommandsIn(CommandListener origin, CommandListener destination) {
        nestCommandsIn(origin, destination, true);
    }

    @Override
    public void nestCommandsIn(CommandListener origin, CommandListener destination, boolean includeAll) {
        ArrayList<CommandHandler> registrationQueue = new ArrayList<>();

        // Retrieve the prefix for this nested command
        Command commandParent = destination.getClass().getAnnotation(Command.class);
        String prefix = commandParent == null ? "" : commandParent.command() + " ";
        for (Method method : origin.getClass().getDeclaredMethods()) {
            Command command = method.getAnnotation(Command.class);
            if (command != null) {
                ensureValidity(origin, method);

                NestedCommand nestedCommand = method.getAnnotation(NestedCommand.class);
                if (!includeAll && nestedCommand == null) {
                    // Restrict nested commands to only those marked as nested (if this flag is set to true)
                    continue;
                }

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
            mapCommands(destination, registrationQueue);
            refreshHelp();
        }
    }

    @Override
    public void unregister(CommandListener commandListener) {
        // This will also remove any nested commands that have been registered to this listener
        LISTENERS.remove(commandListener);
        COMMANDS.remove(commandListener);
        COMMAND_NAMES.remove(commandListener);
        for (CommandHandler commandHandler : getRegisteredCommands(commandListener)) {
            // Unregister from Bukkit so that this command is no longer fired
            if (getRegistry() != null) {
                getRegistry().unregister(commandHandler.getCommand().command().split("\\s")[0]);
            }
        }

        refreshHelp();
    }

    private void mapCommands(CommandListener commandListener, Collection<CommandHandler> registrationQueue) {
        if (LISTENERS.contains(commandListener)) {
            LISTENERS.add(commandListener);
        }

        if (COMMANDS.get(commandListener) != null) {
            System.out.println(COMMANDS.get(commandListener).size());
        }
        Set<CommandHandler> existing = COMMANDS.get(commandListener);
        if (existing == null) {
            existing = new HashSet<>();
        }
        existing.addAll(registrationQueue);
        COMMANDS.put(commandListener, existing);

        Set<String> existingNames = COMMAND_NAMES.get(commandListener);
        if (existingNames == null) {
            existingNames = new HashSet<>();
        }
        for (CommandHandler commandHandler : registrationQueue) {
            existingNames.add(commandHandler.getCommandName());
            Collections.addAll(existingNames, commandHandler.getCommand().aliases());
        }
        COMMAND_NAMES.put(commandListener, existingNames);

        if (getRegistry() != null) {
            getRegistry().register(registrationQueue);
        }
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
            public String[] usage() {
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
                        if (willShowDefaultUsageAsCommandSyntax()) {
                            // Show the command syntax
                            event.respond(ResponseLevel.SEVERE, event.getVariableMatcher().getHumanReadableSyntax());
                        } else {
                            String[] usage;
                            String[] def = {DEFAULT_USAGE};
                            if (Arrays.equals(command.usage(), def)) {
                                usage = parent == null ? def : parent.usage();
                            } else {
                                usage = command.usage();
                            }
                            for (String part : usage) {
                                event.respond(part);
                            }
                        }
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