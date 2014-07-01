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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableMatcher {

    private static final Pattern SYNTAX_PATTERN = Pattern.compile("(<|\\[)([^>\\]]+)(?:>|\\])", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGEX_SYNTAX_PATTERN = Pattern.compile("(<|\\[)(r:.+)(?:>|\\])", Pattern.CASE_INSENSITIVE);
    private static final char VARIABLE_BOUNDARY = '\'';

    private Command command;
    private CommandEvent event;

    private HashMap<String, Integer> variables;
    private HashMap<String, String> matchedArguments;

    public VariableMatcher(Command command, CommandEvent event) {
        this.command = command;
        this.event = event;
    }

    public Command getCommand() {
        return command;
    }

    public CommandEvent getEvent() {
        return event;
    }

    protected String buildVariableSyntax() {
        if (variables == null) {
            variables = new HashMap<>();
        }
        List<String> arguments = Arrays.asList(command.command().split("\\s"));
        String syntaxPattern = command.command();

        Matcher argMatcher = SYNTAX_PATTERN.matcher(command.command());

        while (argMatcher.find()) {
            if (argMatcher.group(1).equals("[")) {
                // Optional args can match something or nothing
                syntaxPattern = syntaxPattern.replace(argMatcher.group(0), "(?:(" + VARIABLE_BOUNDARY + "[^" + VARIABLE_BOUNDARY + "]+" + VARIABLE_BOUNDARY + "|[^\\s]+))?");
            } else {
                syntaxPattern = syntaxPattern.replace(argMatcher.group(1), "(" + VARIABLE_BOUNDARY + "[^" + VARIABLE_BOUNDARY + "]+" + VARIABLE_BOUNDARY + ")");
            }

            String variable = argMatcher.group(2);
            variables.put(variable, arguments.indexOf(variable));
        }

        return syntaxPattern;
    }


    public HashMap<String, Integer> getVariables() {
        if (variables == null) {
            buildVariableSyntax();
        }
        return variables;
    }

    public Map<String, String> getMatchedArguments() {
        if (matchedArguments == null) {
            matchedArguments = new HashMap<>();

            HashMap<String, Integer> variables = getVariables();

            for (Map.Entry<String, Integer> entry : variables.entrySet()) {
                matchedArguments.put(entry.getKey(), event.arg(entry.getValue()));
            }
        }
        return matchedArguments;
    }

    public boolean testRegexVariables() {
        Matcher matcher = REGEX_SYNTAX_PATTERN.matcher(event.input());
        while (matcher.find()) {
            String variableRegex = matcher.group(matcher.group(3) != null ? 3 : 6);
            if (!Pattern.compile(variableRegex).matcher(getMatchedArguments().get(variableRegex)).matches()) {
                return false;
            }
        }
        return true;
    }
}