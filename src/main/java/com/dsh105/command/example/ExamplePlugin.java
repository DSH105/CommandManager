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

package com.dsh105.command.example;

import com.dsh105.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class ExamplePlugin extends JavaPlugin {

    protected ICommandManager PRETEND_MANAGER;

    @Override
    public void onEnable() {
        PRETEND_MANAGER = new CommandManager(this, "Example");
        new ExampleCommand(this);
    }

    // This is only needed if you want a parent command
    @Command(
            command = "example",
            description = "An example command, which does absolutely nothing!",
            aliases = {"eg", "expl"},
            permission = "example.command",
            help = "Type \"/example help\" for help!",
            usage = "Unknown command. Type \"/example help\" for help!"
    )
    public class ExampleCommand implements CommandListener {

        public ExampleCommand(ExamplePlugin plugin) {
            // We'll just pretend we're doing something useful here...
            plugin.PRETEND_MANAGER.register(this);

            new ExampleTwoCommand(this);
            new ExampleThreeCommand();
        }

        @ParentCommand
        public boolean command(CommandEvent event) {
            event.respond("This is an example command. It does nothing. Hover for some text.[txt:Hello world!]");
            event.respond("Try \"/example help\" for help!");
            return true;
        }

        @Command(
                command = "help",
                description = "View help information",
                permission = "example.command.help",
                aliases = "h"
        )
        public boolean helpCommand(CommandEvent event) {
            event.respond("You performed " + event.command() + event.arg(0) + "!");
            event.respond("Sadly, there's nothing here either.");
            return true;
        }

        @Command(
                command = "online",
                description = "Are you logged in?",
                permission = "example.command.online",
                aliases = "o"
        )
        public boolean playerOnlyCommand(CommandEvent<Player> event) {
            event.respond("Looks like you're logged in correctly :)");
            return true;
        }

        @Command(
                command = "variable <var>",
                description = "Test variables",
                permission = "example.command.variable",
                aliases = "v <var>"
        )
        public boolean variableCommand(CommandEvent event) {
            event.respond("You entered a variable: " + event.variable("var"));
            return true;
        }

        public class ExampleTwoCommand implements CommandListener {

            public ExampleTwoCommand(ExampleCommand exampleCommand) {
                // Register all valid sub commands in this class to the parent
                PRETEND_MANAGER.registerSubCommands(exampleCommand, this);
            }

            // This command is registered as "example two" - the power of sub commands!
            @Command(
                    command = "two",
                    description = "Number the second one",
                    permission = "example.command.two",
                    aliases = "t"
            )
            public boolean twoCommand(CommandEvent event) {
                event.respond("Such two. Much command. Wow");
                return true;
            }
        }

        public class ExampleThreeCommand implements CommandListener {

            public ExampleThreeCommand() {
                // Register all valid sub commands in this class to the parent
                PRETEND_MANAGER.register(this);
            }

            // This listener *should* take precedence over the one above IF (and only if) "example something" is fired (not just "example")
            // It becomes interesting here when making decisions on whether to use the multi-argument command listener
            @Command(
                    command = "example something",
                    description = "Number the second one",
                    permission = "example.command.two",
                    aliases = "three" // So either "example something" or "three" can be entered to fire this off. This should work. I hope
            )
            public boolean twoCommand(CommandEvent event) {
                event.respond("THREES!");
                return true;
            }
        }
    }
}