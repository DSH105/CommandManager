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
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.ChatPaginator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelpService {

    private String pageNotFoundMessage = "Page %s does not exist";
    private String pageHeader;

    private ICommandManager manager;
    private Paginator<PowerMessage> paginator = new Paginator<>(6);
    private boolean includePermissionTooltip = true;
    private boolean includePermissionListing = true;
    private boolean ignoreCommandAccess = true;

    public HelpService(ICommandManager manager) {
        this.manager = manager;
        this.pageHeader = buildHeader();
    }

    private String buildHeader() {
        StringBuilder header = new StringBuilder();
        header.append(ChatColor.YELLOW)
              .append("------ ")
              .append(ChatColor.WHITE)
              .append("Help: ")
              .append(manager.getPlugin().getName())
              .append(" ")
              .append("Page ")
              .append(ChatColor.RED)
              .append("{pages} ")
              .append(ChatColor.GOLD)
              .append("/ ")
              .append(ChatColor.RED)
              .append("{total} ")
              .append(ChatColor.YELLOW);
        for (int i = header.length(); i < ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH; i++) {
            header.append("-");
        }
        return header.toString();
    }

    private void prepare(CommandHandler commandHandler) {
        String command = commandHandler.getCommandName().replaceAll("(?:r:(?:(?:(?!,n:.+)[^>\\]])+))[^,>|\\]]*", "").replaceAll(",n:", "").replace("<>", "<undefined>");
        String permission = StringUtil.combineArray(", ", commandHandler.getCommand().permission()).trim();

        PowerMessage part = new MarkupBuilder()
                .withText(manager.getMessenger().getHighlightColour().toString())
                .withText("/")
                .withText(command)
                .withText(manager.getMessenger().getFormatColour().toString())
                .withText(" - ")
                .withText(commandHandler.getCommand().description())
                .withText(permission.isEmpty() ? "" : " (" + permission + ")")
                .build();
        part.suggest("/" + command);

        if (commandHandler.getCommand().help().length > 0) {
            ArrayList<String> tooltipLines = new ArrayList<>();
            for (String help : commandHandler.getCommand().help()) {
                tooltipLines.add(new MarkupBuilder().withText(manager.getMessenger().getHighlightColour() + "• " + manager.getMessenger().getFormatColour() + WordUtils.wrap(help, 30, "\n", false)).build().getContent());
            }
            if (!tooltipLines.isEmpty()) {
                part.tooltip(tooltipLines.toArray(StringUtil.EMPTY_STRING_ARRAY));
            }
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
        return pageNotFoundMessage;
    }

    public void setPageNotFoundMessage(String value) {
        this.pageNotFoundMessage = value;
    }

    public boolean willIncludePermissionTooltip() {
        return includePermissionTooltip;
    }

    public void setIncludePermissionTooltip(boolean flag) {
        this.includePermissionTooltip = flag;
    }

    public void setIncludePermissionListing(boolean flag) {
        this.includePermissionListing = flag;
    }

    public boolean willIncludePermissionListing() {
        return includePermissionListing;
    }

    public boolean willIgnoreCommandAccess() {
        return ignoreCommandAccess;
    }

    public void setIgnoreCommandAccess(boolean flag) {
        this.ignoreCommandAccess = flag;
    }

    public void prepare() {
        paginator.clear();
        for (CommandHandler commandHandler : manager.getAllRegisteredCommands()) {
            prepare(commandHandler);
        }
    }

    public void sendPage(CommandSender sender, int pageNumber) {
        Paginator p = this.paginator;
        if (willIncludePermissionTooltip() || !willIncludePermissionListing() || !willIgnoreCommandAccess()) {
            List<PowerMessage> messages = paginator.getRaw();
            Iterator<PowerMessage> iter = messages.iterator();
            while (iter.hasNext()) {
                PowerMessage powerMessage = iter.next();
                Matcher matcher = Pattern.compile("(.+/(.+) - (?:.+))\\(([^\\s]+)\\)?").matcher(powerMessage.getContent());
                if (matcher.find()) {
                    if (!willIncludePermissionListing()) {
                        powerMessage.clear().then(matcher.group(1));
                    }

                    if (willIncludePermissionTooltip() || !willIgnoreCommandAccess()) {
                        String perm = matcher.group(3);
                        if (perm != null && !perm.isEmpty()) {
                            String[] permissions = perm.split(", ");
                            tooltip: {
                                boolean access = true;
                                for (String permission : permissions) {
                                    if (!VariableMatcher.containsVariables(permission) && permissions.length == 1) {
                                        break tooltip;
                                    }
                                    access = sender.hasPermission(permission);
                                }
                                if (!access && !willIgnoreCommandAccess()) {
                                    iter.remove();
                                    continue;
                                }
                                if (willIncludePermissionTooltip()) {
                                    powerMessage.tooltip(ChatColor.ITALIC + (access ? ChatColor.GREEN + "You may use this command" : ChatColor.RED + "You are not allowed to use this command"));
                                }
                            }
                        }
                    }
                }
            }
            p = new Paginator<>(paginator.getPerPage(), messages.toArray(new PowerMessage[0]));
        }

        String pageHeader = this.pageHeader.replace("{pages}", "" + pageNumber).replace("{total}", "" + p.getPages());

        if (!p.exists(pageNumber)) {
            new MarkupBuilder().withText((manager.getResponsePrefix() != null && !manager.getResponsePrefix().isEmpty() ? manager.getResponsePrefix() + " " : "") + ChatColor.RESET + String.format(getPageNotFoundMessage(), "" + pageNumber)).build().send(sender);
            return;
        }
        sender.sendMessage(pageHeader);
        p.sendPage(sender, pageNumber);
    }
}