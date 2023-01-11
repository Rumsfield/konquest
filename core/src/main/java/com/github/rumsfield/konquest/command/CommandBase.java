package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import org.bukkit.command.CommandSender;

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
}
