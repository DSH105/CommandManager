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

public abstract class CommandMatcher {

    public abstract List<CommandHandler> getAllRegisteredCommands();

    public List<CommandHandler> matchCommands(String commandArguments) {
        return matchCommands(getAllRegisteredCommands(), commandArguments);
    }

    public List<CommandHandler> matchCommands(String commandArguments, boolean matchAliases) {
        return matchCommands(getAllRegisteredCommands(), commandArguments, matchAliases);
    }

    public List<CommandHandler> matchCommands(String commandArguments, boolean matchAliases, boolean enableFuzzyMatching) {
        return matchCommands(getAllRegisteredCommands(), commandArguments, matchAliases, enableFuzzyMatching);
    }

    public List<CommandHandler> matchCommands(List<CommandHandler> commandHandlers, String commandArguments) {
        return matchCommands(commandHandlers, commandArguments, true);
    }

    public List<CommandHandler> matchCommands(List<CommandHandler> commandHandlers, String commandArguments, boolean matchAliases) {
        return matchCommands(commandHandlers, commandArguments, matchAliases, false);
    }

    public List<CommandHandler> matchCommands(List<CommandHandler> commandHandlers, String commandArguments, boolean matchAliases, boolean enableFuzzyMatching) {
        // TODO: Do something with this:
        MatchCondition condition = new MatchCondition(commandArguments, matchAliases, enableFuzzyMatching);

        ArrayList<CommandHandler> matches = new ArrayList<>();
        ArrayList<CommandHandler> fuzzyMatches = new ArrayList<>();

        for (CommandHandler handler : commandHandlers) {
            if (matches(handler, commandArguments, matchAliases, false)) {
                matches.add(handler);
            }

            if (enableFuzzyMatching) {
                if (matches(handler, commandArguments, matchAliases, true)) {
                    fuzzyMatches.add(handler);
                }
            }
        }

        if (!fuzzyMatches.isEmpty()) {
            Collections.sort(fuzzyMatches);

            if (enableFuzzyMatching) {
                matches.addAll(fuzzyMatches);
            }
        }

        if (!matches.isEmpty()) {
            Collections.sort(matches);
        }
        return matches;
    }

    public MatchedCommand matchCommand(String fullCommand) {
        for (CommandHandler handler : matchCommands(fullCommand)) {
            List<String> commandLabels = new ArrayList<>();
            commandLabels.add(handler.getCommandName());
            Collections.addAll(commandLabels, handler.getCommand().aliases());

            for (String label : commandLabels) {
                VariableMatcher variableMatcher = new VariableMatcher(label, fullCommand);
                if (variableMatcher.matches() || variableMatcher.testRegexVariables()) {
                    return new MatchedCommand(label, handler);
                }
            }
        }
        return null;
    }

    public boolean matches(CommandHandler commandHandler, String commandArguments, boolean matchAliases, boolean enableFuzzyMatching) {
        if (matches(commandHandler.getCommandName().split("\\s")[0], commandArguments, enableFuzzyMatching)) {
            return true;
        }

        if (matchAliases) {
            for (String alias : commandHandler.getCommand().aliases()) {
                if (matches(alias.split("\\s")[0], commandArguments, enableFuzzyMatching)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean matches(String test, String match, boolean enableFuzzyMatching) {
        return test.equalsIgnoreCase(match) || (enableFuzzyMatching && match.toLowerCase().startsWith(test.toLowerCase()));
    }

    class MatchCondition {

        private String commandArguments;
        private boolean matchedAliases;
        private boolean fuzzyMatchingEnabled;

        protected MatchCondition(String commandArguments, boolean matchedAliases, boolean fuzzyMatchingEnabled) {
            this.commandArguments = commandArguments;
            this.matchedAliases = matchedAliases;
            this.fuzzyMatchingEnabled = fuzzyMatchingEnabled;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MatchCondition that = (MatchCondition) o;

            return fuzzyMatchingEnabled == that.fuzzyMatchingEnabled && matchedAliases == that.matchedAliases && commandArguments.equals(that.commandArguments);
        }
    }
}