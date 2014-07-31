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

import com.dsh105.command.*;
import org.bukkit.entity.Player;
import org.junit.Assert;

@Command(
        command = "parent",
        description = "Test parent command"
)
public class MockCommandListener implements CommandListener {

    @ParentCommand
    public boolean parentCommand(CommandEvent event) {
        event.respond("Parent command successfully fired.");
        return true;
    }

    @NestedCommand
    @Command(
            command = "test",
            description = "Test sub command"
    )
    public boolean testSub(CommandEvent event) {
        System.out.println("Sub command test executed");
        return true;
    }

    @Command(
            command = "something <var>",
            description = "Test command",
            aliases = "v <var>"
    )
    public boolean variableCommand(CommandEvent event) {
        try {
            event.respond("Variable test executed for: " + event.variable("var"));
            Assert.assertEquals(event.variable("var"), "wow");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Command(
            command = "variable",
            description = "Test command",
            aliases = "v"
    )
    public boolean testCommand(CommandEvent<Player> event) {
        event.respond("\"" + event.input() + "\" command fired.");
        return true;
    }

    @Command(
            command = "extra <info...>",
            description = "Test command"
    )
    public boolean infoCommand(CommandEvent event) {
        event.respond("\"" + event.input() + "\" command fired.");
        event.respond("Variable length: " + event.variable("info").split("\\s").length + " (" + event.variable("info") + ")");
        return true;
    }
}