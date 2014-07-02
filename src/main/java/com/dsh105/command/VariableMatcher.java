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

    private String command;
    private String eventInput;

    private String syntaxPattern;
    private List<String> arguments;
    private HashMap<String, Range> variables;
    private HashMap<String, String> matchedArguments;

    public VariableMatcher(String command, String eventInput) {
        this.command = command;
        this.eventInput = eventInput;
        this.arguments = Arrays.asList(command.split("\\s"));
    }

    protected String buildVariableSyntax() {
        if (variables == null) {
            variables = new HashMap<>();
        }
        String syntaxPattern = command;

        Matcher syntaxMatcher = SYNTAX_PATTERN.matcher(command);

        while (syntaxMatcher.find()) {
            // Optional args can match something or nothing - make sure to account for that
            syntaxPattern = syntaxPattern.replace(syntaxMatcher.group(0), (syntaxMatcher.group(2).endsWith("...") ? "(.+)" : "([^\\s]+)") + (syntaxMatcher.group(1).equals("[") ? "?" : ""));
            int startIndex = arguments.indexOf(syntaxMatcher.group(0));
            variables.put(syntaxMatcher.group(2).replace("...", ""), new Range(startIndex, syntaxMatcher.group(2).endsWith("...") ? eventInput.length() : startIndex));
        }

        this.syntaxPattern = syntaxPattern;
        return syntaxPattern;
    }

    public boolean matches() {
        if (syntaxPattern == null) {
            buildVariableSyntax();
        }
        return Pattern.compile("\\b" + syntaxPattern + "\\b").matcher(eventInput).matches();
    }

    public HashMap<String, Range> getVariables() {
        if (variables == null) {
            buildVariableSyntax();
        }
        return variables;
    }

    public Map<String, String> getMatchedArguments() {
        if (matchedArguments == null) {
            matchedArguments = new HashMap<>();

            HashMap<String, Range> variables = getVariables();

            for (Map.Entry<String, Range> entry : variables.entrySet()) {
                String[] input = eventInput.split("\\s");
                if (entry.getValue().getEndIndex() <= entry.getValue().getStartIndex()) {
                    matchedArguments.put(entry.getKey(), input[entry.getValue().getStartIndex()]);
                } else {
                    matchedArguments.put(entry.getKey(), StringUtil.combineArray(entry.getValue().getStartIndex(), " ", input));
                }
            }
        }
        return matchedArguments;
    }

    public boolean testRegexVariables() {
        Matcher matcher = REGEX_SYNTAX_PATTERN.matcher(command);
        while (matcher.find()) {
            if (Pattern.compile(matcher.group(2)).matcher(getMatchedArguments().get(matcher.group(1))).matches()) {
                return true;
            }
        }
        return false;
    }
}