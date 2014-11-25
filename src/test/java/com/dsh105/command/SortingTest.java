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

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortingTest {

    @Test
    public void testCommandSorting() {
        List<String> commands = Arrays.asList("test", "test <var>", "test nothing", "test nothing <var>", "test <such> <command> <very> <wow>", "such command <wow>", "test <r:\"(.+)\",n:var>", "testing", "testing nothing <var>", "testing <r:\"(.+)\",n:var>", "yay command");

        System.out.println("Sorting the following:");
        for (String command : commands) {
            System.out.println("- " + command);
        }

        Collections.sort(commands, new Comparator<String>() {
            @Override
            public int compare(String first, String second) {
                int result = CommandHandler.compare(first, second);
                System.out.println("Comparing: \"" + first + "\" and \"" + second + "\" -> " + result);
                return result;
            }
        });

        System.out.println("Sorting results:");
        for (String command : commands) {
            System.out.println("- (" + commands.indexOf(command) + ") " + command);
        }
    }
}