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

import com.dsh105.command.registration.CommandRegistry;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public interface ICommandManager extends CommandExecutor, Iterable<CommandHandler> {

    Plugin getPlugin();

    Set<CommandListener> getRegisteredListeners();

    Set<CommandHandler> getAllRegisteredCommands();

    Map<CommandListener, Set<CommandHandler>> getRegisteredCommands();

    Set<CommandHandler> getRegisteredCommands(CommandListener parentListener);

    Set<String> getAllRegisteredCommandNames();

    Set<String> getRegisteredCommandNames(CommandListener parentListener);

    Map<CommandListener, Set<String>> getRegisteredCommandNames();

    boolean isValid(Method accessor);

    boolean isValid(Method accessor, CommandEvent commandEvent);

    boolean isValid(Method accessor, Class<? extends CommandEvent> type);

    Messenger getMessenger();

    HelpService getHelpService();

    CommandRegistry getRegistry();

    String getResponsePrefix();

    void setResponsePrefix(String responsePrefix);

    void setSuggestCommands(boolean suggestCommands);

    boolean willSuggestCommands();

    void setShowErrorMessage(boolean showErrorMessage);

    boolean shouldShowErrorMessage();

    void refreshHelp();

    void register(CommandListener commandListener);

    void nestCommandsIn(CommandListener origin, CommandListener destination);

    void nestCommandsIn(CommandListener origin, CommandListener destination, boolean includeAll);

    void unregister(CommandListener commandListener);

    <T extends CommandSender> boolean onCommand(CommandEvent<T> event);
}