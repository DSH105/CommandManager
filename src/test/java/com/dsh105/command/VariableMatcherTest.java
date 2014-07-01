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
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VariableMatcherTest {

    private static VariableMatcher mockMatcher(String commandSyntax, String command) {
        Command mockedCommand = mock(Command.class);
        when(mockedCommand.command()).thenReturn(StringUtil.combineArray(" ", commandSyntax));

        String[] commandParts = command.split("\\s");
        CommandEvent mockedEvent = mock(CommandEvent.class);
        when(mockedEvent.input()).thenReturn(StringUtil.combineArray(" ", commandParts));
        for (int i = 0; i < commandParts.length; i++) {
            when(mockedEvent.arg(i)).thenReturn(commandParts[i]);
        }
        return new VariableMatcher(mockedCommand, mockedEvent);
    }

    @Test
    public void testVariables() {
        String[] commandSyntax = {"wow", "<much>", "doge", "<r:such|match>", "[command]"};
        String[] command = {"wow", "variable", "doge", "such", "nothing"};
        List<String> commandSyntaxArgs = Arrays.asList(commandSyntax);
        List<String> commandArgs = Arrays.asList(command);

        VariableMatcher variableMatcher = mockMatcher(StringUtil.combineArray(" ", commandSyntax), StringUtil.combineArray(" ", command));

        Assert.assertTrue(variableMatcher.matches());
        Assert.assertTrue(variableMatcher.testRegexVariables());

        for (Map.Entry<String, Integer> entry : variableMatcher.getVariables().entrySet()) {
            Assert.assertEquals(entry.getKey(), commandSyntax[entry.getValue()].replaceAll("<|>|\\[|\\]", ""));
        }

        for (Map.Entry<String, String> entry : variableMatcher.getMatchedArguments().entrySet()) {
            Assert.assertEquals(commandArgs.indexOf(entry.getKey()), commandSyntaxArgs.indexOf(entry.getValue()));
        }
    }

    @Test
    public void testMatcher() {
        VariableMatcher falseVariableMatcher = mockMatcher("match <r:nope>", "match yer");
        Assert.assertFalse(falseVariableMatcher.testRegexVariables());

        VariableMatcher trueVariableMatcher = mockMatcher("match <r:yer>", "match yer");
        Assert.assertTrue(trueVariableMatcher.testRegexVariables());
    }
}