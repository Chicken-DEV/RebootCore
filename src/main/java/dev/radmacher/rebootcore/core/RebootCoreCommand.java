package dev.radmacher.rebootcore.core;

import java.util.List;

import dev.radmacher.rebootcore.RebootCore;
import dev.radmacher.rebootcore.commands.AbstractCommand;
import dev.radmacher.rebootcore.gui.GuiManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RebootCoreCommand extends AbstractCommand {

    protected GuiManager guiManager;

    public RebootCoreCommand() {
        super(false, "rebootcore");
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        if(sender instanceof Player) {
            if(guiManager == null || guiManager.isClosed()) {
                guiManager = new GuiManager(RebootCore.getHijackedPlugin());
            }
            guiManager.showGUI((Player) sender, new RebootCoreOverviewGUI());
        } else {
            sender.sendMessage("/rebootcore diag");
        }
        return ReturnType.SUCCESS;
    }

    @Override
    public String getPermissionNode() {
        return "rebootcore.admin";
    }

    @Override
    public String getSyntax() {
        return "/rebootcore";
    }

    @Override
    public String getDescription() {
        return "Displays this interface.";
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

}
