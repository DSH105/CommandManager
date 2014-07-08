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

import java.util.regex.Pattern;

public class Variable {

    private Pattern pattern;

    private String fullName;
    private String regex;
    private String name;
    private Range range;

    public Variable(String fullName, String name, Range range) {
        this(fullName, "", name, range);
    }

    public Variable(String fullName, String regex, String name, Range range) {
        this.regex = regex;
        this.name = name;
        this.range = range;

        if (!this.regex.isEmpty()) {
            pattern = Pattern.compile(this.regex);
        }
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRegex() {
        return regex;
    }

    public String getName() {
        return name;
    }

    public Range getRange() {
        return range;
    }
}