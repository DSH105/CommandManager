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

public class Suggestion {

    private String message;
    private String suggestions;

    public Suggestion(String message, String... possibleSuggestions) {
        this.message = message;
        StringBuilder builder = new StringBuilder();
        for (String suggestion : possibleSuggestions) {
            if (message.startsWith(suggestion)) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(suggestion);
            }
        }
        suggestions = builder.toString();
    }

    public String getMessage() {
        return message;
    }

    public String getSuggestions() {
        return suggestions;
    }
}