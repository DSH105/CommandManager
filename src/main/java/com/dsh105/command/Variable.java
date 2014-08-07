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

import com.dsh105.command.exception.InvalidCommandException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Variable implements Comparable<Variable> {

    private Pattern pattern;

    private String fullName;
    private String regex;
    private String name;
    private Range range;
    private boolean continuous;
    private boolean optional;

    public Variable(String fullName, String name, Range range, boolean optional, boolean continuous) {
        this(fullName, "", name, range, optional, continuous);
    }

    public Variable(String fullName, String regex, String name, Range range, boolean optional, boolean continuous) {
        this.fullName = fullName;
        this.regex = regex;
        this.name = name;
        this.range = range;
        this.optional = optional;
        this.continuous = continuous;

        if (!this.regex.isEmpty()) {
            try {
                pattern = Pattern.compile(this.regex);
            } catch (PatternSyntaxException e) {
                throw new InvalidCommandException("Invalid pattern syntax for command variable (\"" + this.fullName + "\"): \"" + this.regex + "\"", e);
            }
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

    public boolean isContinuous() {
        return continuous;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getOpeningTag() {
        return isOptional() ? "[" : "<";
    }

    public String getClosingTag() {
        return isOptional() ? "]" : ">";
    }

    @Override
    public int compareTo(Variable variable) {
        return variable.getRange().getStartIndex() - this.getRange().getStartIndex();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Variable variable = (Variable) o;

        return range.equals(variable.range);

    }
}