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
import com.dsh105.powermessage.markup.MarkupBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;

public class CommandEvent<T extends CommandSender> {

    private String input;
    private CommandManager manager;
    private String command;
    private T sender;
    private String[] args;

    private VariableMatcher variableMatcher;

    public CommandEvent(CommandManager manager, String args, T sender) {
        this(manager, sender, args.replaceAll("\\s+", "").split("\\s"));
    }

    public CommandEvent(CommandManager manager, T sender, String... args) {
        this(manager, args[0], sender, StringUtil.combineArray(1, " ", args));
    }

    public CommandEvent(CommandManager manager, String command, T sender, String... args) {
        this.manager = manager;
        this.command = command;
        this.sender = sender;
        this.args = args;

        ArrayList<String> argsList = new ArrayList<>();
        argsList.add(command);
        argsList.addAll(Arrays.asList(args));
        input = StringUtil.join(argsList, " ");
    }

    public Plugin getPlugin() {
        return getManager().getPlugin();
    }

    public ICommandManager getManager() {
        return manager;
    }

    protected void setVariableMatcher(VariableMatcher variableMatcher) {
        this.variableMatcher = variableMatcher;
    }

    public VariableMatcher getVariableMatcher() {
        return variableMatcher;
    }

    public String variable(String variable) {
        return variableMatcher.getMatchedArguments().get(variable);
    }

    public String command() {
        return command;
    }

    public T sender() {
        return sender;
    }

    public String input() {
        return input;
    }

    public String[] args() {
        return args;
    }

    public int argsLength() {
        return args().length;
    }

    public String arg(int index) {
        return args[index];
    }

    public boolean canPerform(String permission) {
        return canPerform(permission, (Class<? extends T>) Player.class);
    }

    public <S extends T> boolean canPerform(String permission, Class<S> typeRestriction) {
        if (typeRestriction.isAssignableFrom(sender.getClass())) {
            respond(ResponseLevel.SEVERE, manager.getNoAccessMessage());
            return false;
        }
        if (!sender.hasPermission(permission)) {
            respond(ResponseLevel.SEVERE, manager.getNoPermissionMessage());
            return false;
        }
        return true;
    }

    public void respond(String response) {
        respond(response, manager.getFormatColour(), manager.getHighlightColour());
    }

    public void respond(ResponseLevel level, String response) {
        respond(response, level.getFormatColour(), level.getHighlightColour());
    }

    public void respond(String response, ChatColor formatColour, ChatColor highlightColour) {
        String message = formatColour + response.replace("{c1}", "" + formatColour).replace("{c2}", "" + highlightColour);

        // Take care of any conversions, special formatting, etc.
        new MarkupBuilder().withText((manager.getResponsePrefix() != null && !manager.getResponsePrefix().isEmpty() ? manager.getResponsePrefix() + " " : "") + ChatColor.RESET + message).build().send(sender());
    }

}