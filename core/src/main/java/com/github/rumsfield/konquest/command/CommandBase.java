package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class CommandBase {

    private final Konquest konquest;
    private final CommandSender sender;
    private String[] args;

    public CommandBase(Konquest konquest, CommandSender sender, String[] args) {
        this.konquest = konquest;
        this.sender = sender;
        this.args = args;
    }

    public CommandBase(Konquest konquest, CommandSender sender) {
        this.konquest = konquest;
        this.sender = sender;
    }

    public Konquest getKonquest() {
        return konquest;
    }

    public CommandSender getSender() {
        return sender;
    }

    public String[] getArgs() {
        return args;
    }

    public abstract void execute();
    
    public abstract List<String> tabComplete();

    public void sendInvalidArgMessage(Player bukkitPlayer, CommandType command) {
        ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        ChatUtil.sendMessage(bukkitPlayer, Labeler.format(command));
    }

    public void sendInvalidArgMessage(Player bukkitPlayer, AdminCommandType command) {
        ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS_ADMIN.getMessage());
        ChatUtil.sendMessage(bukkitPlayer, Labeler.format(command));
    }
}
