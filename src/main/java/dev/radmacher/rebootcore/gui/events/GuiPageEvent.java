package dev.radmacher.rebootcore.gui.events;

import dev.radmacher.rebootcore.gui.Gui;
import dev.radmacher.rebootcore.gui.GuiManager;

public class GuiPageEvent {

    final Gui gui;
    final GuiManager manager;
    final int lastPage;
    final int page;

    public GuiPageEvent(Gui gui, GuiManager manager, int lastPage, int page) {
        this.gui = gui;
        this.manager = manager;
        this.lastPage = lastPage;
        this.page = page;
    }

}
