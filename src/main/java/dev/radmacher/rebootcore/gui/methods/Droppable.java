package dev.radmacher.rebootcore.gui.methods;

import dev.radmacher.rebootcore.gui.events.GuiDropItemEvent;

public interface Droppable {

    boolean onDrop(GuiDropItemEvent event);
}
