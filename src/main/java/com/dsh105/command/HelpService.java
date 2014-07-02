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
import com.dsh105.commodus.paginator.Paginator;
import com.dsh105.powermessage.core.PowerMessage;
import com.dsh105.powermessage.markup.MarkupBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.ChatPaginator;

import java.util.ArrayList;

public class HelpService {

    private String PAGE_NOT_FOUND = "Page %s does not exist";
    private String PAGE_HEADER;

    private ICommandManager manager;
    private Paginator<PowerMessage> paginator = new Paginator<>();

    public HelpService(ICommandManager manager) {
        this.manager = manager;

        PAGE_HEADER = buildHeader();
    }

    private String buildHeader() {
        StringBuilder header = new StringBuilder();
        header.append(ChatColor.YELLOW);
        header.append("--------- ");
        header.append(ChatColor.WHITE);
        header.append("Help: ");
        header.append(manager.getPlugin().getName());
        header.append(" ");
        header.append("({pages}/{total}");
        header.append(ChatColor.YELLOW);
        for (int i = header.length(); i < ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH; i++) {
            header.append("-");
        }
        return header.toString();
    }

    private void prepare(CommandMethod commandMethod) {
        PowerMessage part = new MarkupBuilder().withText(commandMethod.getCommand().description()).build();
        if (commandMethod.getCommand().help().length <= 0) {
            ArrayList<String> tooltip = new ArrayList<>();
            for (String help : commandMethod.getCommand().help()) {
                tooltip.add(new MarkupBuilder().withText(help).build().getContent());
            }
            part.tooltip(tooltip.toArray(StringUtil.EMPTY_STRING_ARRAY));
        }
        paginator.add(part);
    }

    public ICommandManager getManager() {
        return manager;
    }

    public Paginator<PowerMessage> getPaginator() {
        return paginator;
    }

    public String getPageNotFoundMessage() {
        return PAGE_NOT_FOUND;
    }

    public void setPageNotFoundMessage(String value) {
        this.PAGE_NOT_FOUND = value;
    }

    public void prepare() {
        paginator.clear();
        for (CommandListener commandListener : manager.getRegisteredCommands()) {
            for (CommandMethod commandMethod : manager.getCommandMethods(commandListener)) {
                prepare(commandMethod);
            }
            for (CommandMethod subCommand : manager.getRegisteredSubCommands(commandListener)) {
                prepare(subCommand);
            }
        }
    }

    public void sendPage(CommandSender sender, int pageNumber) {
        if (!paginator.exists(pageNumber)) {
            sender.sendMessage(PAGE_HEADER.replace("{pages}", "" + pageNumber).replace("{total}", "" + paginator.getPages()));
            return;
        }
        sender.sendMessage(PAGE_HEADER.replace("{pages}", "" + pageNumber).replace("{total}", "" + paginator.getPages()));
        paginator.sendPage(sender, pageNumber);
    }
}