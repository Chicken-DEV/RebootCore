package dev.radmacher.rebootcore.gui.methods;

import dev.radmacher.rebootcore.gui.events.GuiCloseEvent;

public interface Closable {

    void onClose(GuiCloseEvent event);
}
