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

import com.dsh105.commodus.StringUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandEnvironmentTest {

    private static CommandManager COMMAND_MANAGER = new CommandManager(getMockedPlugin(), null, false, "CommandTest");
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

        System.out.println("Registered commands: " + StringUtil.combineArray(0, ", ", COMMAND_MANAGER.getRegisteredCommandNames().toArray(StringUtil.EMPTY_STRING_ARRAY)));

        for (String command : new String[]{"parent", "variable wow", "v wow", "variable"}) {
            System.out.println("Testing command: \"" + command + "\"");
            COMMAND_MANAGER.onCommand(new MockCommandEvent<>(COMMAND_MANAGER, command, mock(CommandSender.class)));

            System.out.println("Testing command as Player: \"" + command + "\"");
            COMMAND_MANAGER.onCommand(new MockCommandEvent<>(COMMAND_MANAGER, command, mock(Player.class)));
        }
    }

    @Test
    public void testVariableMatcher() {
        Command mockedCommand = mock(Command.class);
        when(mockedCommand.command()).thenReturn("wow <such> doge <much> [command]");

        CommandEvent mockedEvent = mock(CommandEvent.class);
        when(mockedEvent.input()).thenReturn(mockedCommand.command());
        when(mockedEvent.arg(anyInt())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return ((CommandEvent) invocation.getMock()).command().split("\\s")[(Integer) invocation.getArguments()[0]];
            }
        });

        VariableMatcher variableMatcher = new VariableMatcher(mockedCommand, mockedEvent);
        System.out.println(variableMatcher.buildVariableSyntax());
    }
}
