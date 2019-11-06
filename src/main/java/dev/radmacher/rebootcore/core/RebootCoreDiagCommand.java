package dev.radmacher.rebootcore.core;

import dev.radmacher.rebootcore.RebootCore;
import dev.radmacher.rebootcore.commands.AbstractCommand;

import dev.radmacher.rebootcore.compatibility.ServerProject;
import dev.radmacher.rebootcore.compatibility.ServerVersion;
import dev.radmacher.rebootcore.utils.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.List;


public class RebootCoreDiagCommand extends AbstractCommand {
    private final DecimalFormat format = new DecimalFormat("##.##");

    private Object serverInstance;
    private Field tpsField;

    public RebootCoreDiagCommand() {
        super(false, "diag");

        try {
            serverInstance = NMSUtils.getNMSClass("MinecraftServer").getMethod("getServer").invoke(null);
            tpsField = serverInstance.getClass().getField("recentTps");
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {

        sender.sendMessage("");
        sender.sendMessage("Reboot Diagnostics Information");
        sender.sendMessage("");
        sender.sendMessage("Plugins:");
        for (PluginInfo plugin : RebootCore.getPlugins()) {
            sender.sendMessage(plugin.getJavaPlugin().getName()
                    + " (" + plugin.getJavaPlugin().getDescription().getVersion() + " Core " + plugin.getCoreLibraryVersion() + ")");
        }
        sender.sendMessage("");
        sender.sendMessage("Server Version: " + Bukkit.getVersion());
        sender.sendMessage("NMS: " + ServerProject.getServerVersion() + " " + ServerVersion.getServerVersionString());
        sender.sendMessage("Operating System: " + System.getProperty("os.name"));
        sender.sendMessage("Allocated Memory: " + format.format(Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "Mb");
        sender.sendMessage("Online Players: " +  Bukkit.getOnlinePlayers().size());
        if(tpsField != null) {
            try {
                double[] tps = ((double[]) tpsField.get(serverInstance));

                sender.sendMessage("TPS from last 1m, 5m, 15m: " + format.format(tps[0]) + ", "
                        + format.format(tps[1]) + ", " + format.format(tps[2]));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "rebootcore.admin";
    }

    @Override
    public String getSyntax() {
        return "/rebootcore diag";
    }

    @Override
    public String getDescription() {
        return "Display diagnostics information.";
    }
}
