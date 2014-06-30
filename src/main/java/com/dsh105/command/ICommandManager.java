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

import com.dsh105.commodus.paginator.Paginator;
import com.dsh105.powermessage.core.PowerMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO: JavaDocs
public interface ICommandManager extends CommandExecutor, Iterable<CommandListener> {

    Plugin getPlugin();

    public List<CommandListener> getRegisteredCommands();

    Map<CommandListener, CommandMethod> getRegisteredSubCommands();

    List<CommandMethod> getRegisteredSubCommands(CommandListener commandListener);

    HelpService getHelpService();

    void refreshHelpService();

    String getResponsePrefix();

    void setResponsePrefix(String responsePrefix);

    ChatColor getHighlightColour();

    void setHighlightColour(ChatColor highlightColour);

    ChatColor getFormatColour();

    void setFormatColour(ChatColor formatColour);

    String getNoPermissionMessage();

    void setNoPermissionMessage(String noPermissionMessage);

    String getNoAccessMessage();

    void setNoAccessMessage(String noAccessMessage);

    String getErrorMessage();

    void setErrorMessage(String errorMessage);

    String getCommandNotFoundMessage();

    void setCommandNotFoundMessage(String commandNotFoundMessage);

    boolean willSuggestCommands();

    void setSuggestCommands(boolean suggestCommands);

    void register(CommandListener commandListener);

    void registerSubCommands(CommandListener registerTo, CommandListener parent);

    void registerSubCommand(CommandListener registerTo, CommandListener parent, String methodName);

    void registerSubCommands(CommandListener registerTo, Class<? extends CommandListener> parentClass);

    void registerSubCommand(CommandListener registerTo, Class<? extends CommandListener> parentClass, String methodName);

    void unregister(CommandListener commandListener);

    <T extends CommandListener> ArrayList<T> getCommandsOfType(Class<T> type);

    <T extends CommandListener> T getCommandOfType(Class<T> type);

    ArrayList<CommandListener> getCommandsFor(String commandArguments);

    ArrayList<CommandListener> getCommandsFor(String commandArguments, boolean useAliases);

    <T extends CommandListener> ArrayList<T> getCommandsFor(ArrayList<T> commandList, String command);

    <T extends CommandListener> ArrayList<T> getCommandsFor(ArrayList<T> commandList, String command, boolean useAliases);

    <T extends CommandListener> ArrayList<T> getCommandsFor(ArrayList<T> commandList, String command, boolean useAliases, boolean fuzzyMatching);

    boolean matches(String test, String match, boolean fuzzy);

    ArrayList<CommandListener> getCommandMatchesFor(String commandArguments);

    ArrayList<CommandListener> getCommandMatchesFor(String commandArguments, boolean useAliases);

    <T extends CommandListener> ArrayList<T> getCommandMatchesFor(ArrayList<T> commandList, String command);

    <T extends CommandListener> ArrayList<T> getCommandMatchesFor(ArrayList<T> commandList, String command, boolean useAliases);

    ArrayList<CommandMethod> getCommandMethods(CommandListener commandListener);

    CommandMethod getCommandMethod(CommandListener commandListener, CommandEvent commandEvent);

    Method getParentCommandMethod(CommandListener commandListener);

    boolean isValid(CommandMethod commandMethod);

    boolean isValid(CommandMethod commandMethod, CommandEvent commandEvent);

    boolean isValid(CommandMethod commandMethod, Class<? extends CommandEvent> type);

    boolean isParent(CommandListener commandListener);

    <T extends CommandSender> boolean onCommand(T sender, String args);

    @Override
    boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String commandLabel, String[] args);

    <T extends CommandSender> boolean onCommand(CommandEvent<T> event);
}