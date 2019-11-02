package dev.radmacher.rebootcore.gui.events;

import dev.radmacher.rebootcore.gui.Gui;
import dev.radmacher.rebootcore.gui.GuiManager;
import org.bukkit.entity.Player;

public class GuiCloseEvent extends GuiEvent {

    public GuiCloseEvent(GuiManager manager, Gui gui, Player player) {
        super(manager, gui, player);
    }

}
