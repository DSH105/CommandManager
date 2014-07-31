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
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandManagerTest {

    private static MockCommandManager COMMAND_MANAGER;
    private static Plugin MOCKED_PLUGIN;

    public static MockCommandManager getCommandManager() {
        if (COMMAND_MANAGER == null) {
            COMMAND_MANAGER = new MockCommandManager(getMockedPlugin());
        }
        return COMMAND_MANAGER;
    }

    public static Plugin getMockedPlugin() {
        if (MOCKED_PLUGIN == null) {
            MOCKED_PLUGIN = mock(Plugin.class);
            when(MOCKED_PLUGIN.getName()).thenReturn("CommandTest");
        }
        return MOCKED_PLUGIN;
    }

    @Test
    public void testCommands() {
        CommandListener parent = new MockCommandListener();
        getCommandManager().register(parent);

        System.out.println("Registered commands: " + StringUtil.combineArray(0, ", ", getCommandManager().getAllRegisteredCommandNames().toArray(StringUtil.EMPTY_STRING_ARRAY)) + " (" + getCommandManager().getAllRegisteredCommands().size() + ")");

        for (String command : new String[]{"parent", "parent test", "something wow", "v wow", "variable", "extra command length woo", "parent sub"}) {
            System.out.println("Testing command: \"" + command + "\"");
            getCommandManager().onCommand(new MockCommandEvent<>(getCommandManager(), command, mock(CommandSender.class)));

            System.out.println("Testing command as Player: \"" + command + "\"");
            getCommandManager().onCommand(new MockCommandEvent<>(getCommandManager(), command, mock(Player.class)));
        }
    }
}
