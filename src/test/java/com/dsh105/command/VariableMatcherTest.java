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

    @Test
    public void testVariables() {
        String[] commandSyntax = {"wow", "<much>", "doge", "<r:such|match>", "[command]"};
        String[] command = {"wow", "variable", "doge", "such", "nothing"};
        List<String> commandSyntaxArgs = Arrays.asList(commandSyntax);
        List<String> commandArgs = Arrays.asList(command);

        VariableMatcher variableMatcher = new VariableMatcher(StringUtil.combineArray(" ", commandSyntax), StringUtil.combineArray(" ", command));

        Assert.assertTrue(variableMatcher.matches());
        Assert.assertTrue(variableMatcher.testRegexVariables());

        for (Variable variable : variableMatcher.getVariables()) {
            Assert.assertEquals(variable.getName(), commandSyntax[variable.getRange().getStartIndex()].replaceAll("r:|<|>|\\[|\\]", ""));
        }

        for (Map.Entry<Variable, String> entry : variableMatcher.getMatchedArguments().entrySet()) {
            Assert.assertEquals(commandArgs.indexOf(entry.getKey().getName()), commandSyntaxArgs.indexOf(entry.getValue()));
        }
    }

    @Test
    public void testMatcher() {
        VariableMatcher falseVariableMatcher = new VariableMatcher("match <r:nope,n:boolean>", "match yer");
        Assert.assertEquals("yer", falseVariableMatcher.getMatchedArgumentByVariableName("boolean"));
        Assert.assertEquals("yer", falseVariableMatcher.getMatchedArgumentByVariableRegex("nope"));
        Assert.assertFalse(falseVariableMatcher.testRegexVariables());

        VariableMatcher trueVariableMatcher = new VariableMatcher("match <r:yer>", "match yer");
        Assert.assertEquals("yer", trueVariableMatcher.getMatchedArgumentByVariableName("yer"));
        Assert.assertEquals("yer", trueVariableMatcher.getMatchedArgumentByVariableRegex("yer"));
        Assert.assertTrue(trueVariableMatcher.testRegexVariables());
    }
}