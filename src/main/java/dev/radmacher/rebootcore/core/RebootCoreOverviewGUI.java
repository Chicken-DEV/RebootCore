package dev.radmacher.rebootcore.core;

import dev.radmacher.rebootcore.RebootCore;
import dev.radmacher.rebootcore.configuration.editor.PluginConfigGui;
import dev.radmacher.rebootcore.gui.Gui;
import dev.radmacher.rebootcore.gui.GuiUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

final class RebootCoreOverviewGUI extends Gui {

    protected RebootCoreOverviewGUI() {
        List<PluginInfo> plugins = RebootCore.getPlugins();
        // could do pages, too, but don't think we'll have that many at a time for a while
        int max = (int) Math.ceil(plugins.size() / 9.);
        setRows(max);
        setTitle("Reboot Plugins");

        // TODO: this could use some decorating

        for (int i = 0; i < plugins.size(); i++) {
            final PluginInfo plugin = plugins.get(i);
            if (plugin.hasUpdate()) {
                setButton(i, GuiUtils.createButtonItem(plugin.icon != null ? plugin.icon : Material.STONE,
                        ChatColor.GOLD + plugin.getJavaPlugin().getName(),
                        ChatColor.GRAY + "Latest Version: " + plugin.getLatestVersion(),
                        ChatColor.GRAY + "Installed Version: " + plugin.getJavaPlugin().getDescription().getVersion(),
                        "",
                        "Change log:",
                        plugin.getChangeLog(),
                        "",
                        ChatColor.GOLD + "Click for the marketplace page link.",
                        ChatColor.GOLD + "Right Click to edit plugin settings."
                        ),
                        ClickType.LEFT, (event) -> event.player.sendMessage(plugin.getMarketplaceLink()));
                setAction(i, ClickType.RIGHT, (event) -> event.manager.showGUI(event.player, new PluginConfigGui(plugin.getJavaPlugin(), event.gui)));
                highlightItem(i);
            } else {
                setButton(i, GuiUtils.createButtonItem(plugin.icon != null ? plugin.icon : Material.STONE,
                        ChatColor.GOLD + plugin.getJavaPlugin().getName(),
                        ChatColor.GRAY + "Installed Version: " + plugin.getJavaPlugin().getDescription().getVersion(),
                        "",
                        ChatColor.GOLD + "Click for the marketplace page link.",
                        ChatColor.GOLD + "Right Click to edit plugin settings."
                        ),
                        ClickType.LEFT, (event) -> event.player.sendMessage(plugin.getMarketplaceLink()));
                setAction(i, ClickType.RIGHT, (event) -> event.manager.showGUI(event.player, new PluginConfigGui(plugin.getJavaPlugin(), event.gui)));
            }
        }
    }
}
