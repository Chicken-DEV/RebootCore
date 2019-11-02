package dev.radmacher.rebootcore.anvil;

import net.minecraft.server.v1_14_R1.EntityPlayer;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;


public class NMS {

    public CustomAnvil createAnvil(Player player) {
        EntityPlayer p = ((CraftPlayer) player).getHandle();
        return new AnvilView(p.nextContainerCounter(), p, null);
    }

    public CustomAnvil createAnvil(Player player, InventoryHolder holder) {
        EntityPlayer p = ((CraftPlayer) player).getHandle();
        return new AnvilView(p.nextContainerCounter(), p, holder);
    }

}
