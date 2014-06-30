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
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandEnvironmentTest {

    private static CommandManager COMMAND_MANAGER = new CommandManager(getMockedPlugin(), mock(CommandRegistry.class), "CommandTest");
    private static Plugin MOCKED_PLUGIN;

    public static Plugin getMockedPlugin() {
        if (MOCKED_PLUGIN == null) {
            MOCKED_PLUGIN = mock(Plugin.class);
            when(MOCKED_PLUGIN.getName()).thenReturn("CommandTest");
        }
        return MOCKED_PLUGIN;
    }

    @Test
    public void testCommands() {
        COMMAND_MANAGER.register(new CommandTest());

        for (String command : new String[]{"parent", "variable wow", "v wow", "variable"}) {
            System.out.println("Testing command: \"/" + command + "\"");
            System.out.println("Result: " + COMMAND_MANAGER.onCommand(new MockCommandEvent(COMMAND_MANAGER, command, mock(CommandSender.class))));
        }
    }
}
