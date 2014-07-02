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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableMatcher {

    private static final Pattern SYNTAX_PATTERN = Pattern.compile("(<|\\[)([^>\\]]+)(?:>|\\])", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGEX_SYNTAX_PATTERN = Pattern.compile("(?:<|\\[)(r:([^>\\]]+))(?:>|\\])", Pattern.CASE_INSENSITIVE);

    private Command command;
    private CommandEvent event;

    private String syntaxPattern;
    private List<String> arguments;
    private HashMap<String, Integer> variables;
    private HashMap<String, String> matchedArguments;

    public VariableMatcher(Command command, CommandEvent event) {
        this.command = command;
        this.event = event;
        this.arguments = Arrays.asList(command.command().split("\\s"));
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
        String syntaxPattern = command.command();

        Matcher syntaxMatcher = SYNTAX_PATTERN.matcher(command.command());

        while (syntaxMatcher.find()) {
            // Optional args can match something or nothing - make sure to account for that
            syntaxPattern = syntaxPattern.replace(syntaxMatcher.group(0), "([^\\s]+)" + (syntaxMatcher.group(1).equals("[") ? "?" : ""));
            variables.put(syntaxMatcher.group(2), arguments.indexOf(syntaxMatcher.group(0)));
        }

        this.syntaxPattern = syntaxPattern;
        return syntaxPattern;
    }

    public boolean matches() {
        if (syntaxPattern == null) {
            buildVariableSyntax();
        }
        return Pattern.compile(syntaxPattern).matcher(event.input()).matches();
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
                matchedArguments.put(entry.getKey(), event.input().split("\\s")[entry.getValue()]);
            }
        }
        return matchedArguments;
    }

    public boolean testRegexVariables() {
        Matcher matcher = REGEX_SYNTAX_PATTERN.matcher(command.command());
        while (matcher.find()) {
            if (Pattern.compile(matcher.group(2)).matcher(getMatchedArguments().get(matcher.group(1))).matches()) {
                return true;
            }
        }
        return false;
    }
}