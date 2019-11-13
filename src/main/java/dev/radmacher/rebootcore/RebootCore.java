package dev.radmacher.rebootcore;

import dev.radmacher.rebootcore.commands.CommandManager;
import dev.radmacher.rebootcore.compatibility.ClientVersion;
import dev.radmacher.rebootcore.core.LocaleModule;
import dev.radmacher.rebootcore.core.PluginInfo;
import dev.radmacher.rebootcore.core.RebootCoreCommand;
import dev.radmacher.rebootcore.core.RebootCoreDiagCommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;


public final class RebootCore {

    private final static String prefix = "Rebootcore >";

    /**
     * Whenever we make a major change to the core GUI, updater,
     * or other function used by the core, increment this number
     */
    private final static int coreRevision = 6;

    /**
     * This has been added as of Rev 6 <br>
     * This value is automatically filled in by gitlab-ci
     */
    private final static String coreVersion = "maven-version-number";

    /**
     * This is specific to the website api
     */
    private final static int updaterVersion = 1;

    private final static Set<PluginInfo> registeredPlugins = new HashSet<>();

    private static RebootCore INSTANCE = null;
    private JavaPlugin piggybackedPlugin;
    private CommandManager commandManager;
    private EventListener loginListener;
    private ShadedEventListener shadingListener;

    public static boolean hasShading() {
        // sneaky hack to check the package name since maven tries to re-shade all references to the package string
        return !RebootCore.class.getPackage().getName().equals(new String(new char[]{'d','e','v','.','r','a','d','m','a','c','h', 'e', 'r', '.','c','o','r','e'}));
    }

    public static void registerPlugin(JavaPlugin plugin, int pluginID, Material icon) {
        registerPlugin(plugin, pluginID, icon, "?");
    }

    public static void registerPlugin(JavaPlugin plugin, int pluginID, Material icon, String coreVersion) {
        if(INSTANCE == null) {
            // First: are there any other instances of SongodaCore active?
            for (Class<?> clazz : Bukkit.getServicesManager().getKnownServices()) {
                if(clazz.getSimpleName().equals("RebootCore")) {
                    try {
                        // test to see if we're up to date
                        int otherVersion;
                        try {
                            otherVersion = (int) clazz.getMethod("getCoreVersion").invoke(null);
                        } catch (Exception ignore) {
                            otherVersion = -1;
                        }
                        if(otherVersion >= getCoreVersion()) {
                            // use the active service
                            // assuming that the other is greater than R6 if we get here ;)
                            clazz.getMethod("registerPlugin", JavaPlugin.class, int.class, String.class, String.class).invoke(null, plugin, pluginID, icon, coreVersion);

                            if(hasShading()) {
                                (INSTANCE = new RebootCore()).piggybackedPlugin = plugin;
                                INSTANCE.shadingListener = new ShadedEventListener();
                                Bukkit.getPluginManager().registerEvents(INSTANCE.shadingListener, plugin);
                            }
                        } else {
                            // we are newer than the registered service: steal all of its registrations
                            // grab the old core's registrations
                            List otherPlugins = (List) clazz.getMethod("getPlugins").invoke(null);
                            // destroy the old core
                            Object oldCore = clazz.getMethod("getInstance").invoke(null);
                            Method destruct = clazz.getDeclaredMethod("destroy");
                            destruct.setAccessible(true);
                            destruct.invoke(oldCore);
                            // register ourselves as the SongodaCore service!
                            INSTANCE = new RebootCore(plugin);
                            INSTANCE.init();
                            INSTANCE.register(plugin, pluginID, icon, coreVersion);
                            Bukkit.getServicesManager().register(RebootCore.class, INSTANCE, plugin, ServicePriority.Normal);
                            // we need (JavaPlugin plugin, int pluginID, String icon) for our object
                            if(!otherPlugins.isEmpty()) {
                                Object testSubject = otherPlugins.get(0);
                                Class otherPluginInfo = testSubject.getClass();
                                Method otherPluginInfo_getJavaPlugin = otherPluginInfo.getMethod("getJavaPlugin");
                                Method otherPluginInfo_getRebootID = otherPluginInfo.getMethod("getRebootID");
                                Method otherPluginInfo_getCoreIcon = otherPluginInfo.getMethod("getCoreIcon");
                                Method otherPluginInfo_getCoreLibraryVersion = otherVersion >= 6 ? otherPluginInfo.getMethod("getCoreLibraryVersion") : null;
                                for(Object other : otherPlugins) {
                                    INSTANCE.register(
                                            (JavaPlugin) otherPluginInfo_getJavaPlugin.invoke(other),
                                            (int) otherPluginInfo_getRebootID.invoke(other),
                                            (Material) otherPluginInfo_getCoreIcon.invoke(other),
                                            otherPluginInfo_getCoreLibraryVersion != null ? (String) otherPluginInfo_getCoreLibraryVersion.invoke(other) : "?");
                                }
                            }
                        }
                        return;
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
                        plugin.getLogger().log(Level.WARNING, "Error registering core service", ignored);
                    }
                }
            }
            // register ourselves as the SongodaCore service!
            INSTANCE = new RebootCore(plugin);
            INSTANCE.init();
            Bukkit.getServicesManager().register(RebootCore.class, INSTANCE, plugin, ServicePriority.Normal);
        }
        INSTANCE.register(plugin, pluginID, icon, coreVersion);
    }

    RebootCore() {
        commandManager = null;
    }

    RebootCore(JavaPlugin javaPlugin) {
        piggybackedPlugin = javaPlugin;
        commandManager = new CommandManager(piggybackedPlugin);
        loginListener = new EventListener();
    }

    private void init() {
        shadingListener = new ShadedEventListener();
        commandManager.registerCommandDynamically(new RebootCoreCommand())
                .addSubCommand(new RebootCoreDiagCommand());
        Bukkit.getPluginManager().registerEvents(loginListener, piggybackedPlugin);
        Bukkit.getPluginManager().registerEvents(shadingListener, piggybackedPlugin);
        // we aggressevely want to own this command
        tasks.add(Bukkit.getScheduler().runTaskLaterAsynchronously(piggybackedPlugin, ()->{CommandManager.registerCommandDynamically(piggybackedPlugin, "reboot", commandManager, commandManager);}, 10 * 60 * 1));
        tasks.add(Bukkit.getScheduler().runTaskLaterAsynchronously(piggybackedPlugin, ()->{CommandManager.registerCommandDynamically(piggybackedPlugin, "reboot", commandManager, commandManager);}, 20 * 60 * 1));
        tasks.add(Bukkit.getScheduler().runTaskLaterAsynchronously(piggybackedPlugin, ()->{CommandManager.registerCommandDynamically(piggybackedPlugin, "reboot", commandManager, commandManager);}, 20 * 60 * 2));
        tasks.add(Bukkit.getScheduler().runTaskLaterAsynchronously(piggybackedPlugin, ()->registerAllPlugins(), 20 * 60 * 2));
    }

    /**
     * Used to yield this core to a newer core
     */
    private void destroy() {
        Bukkit.getServicesManager().unregister(RebootCore.class, INSTANCE);
        tasks.stream().filter(Objects::nonNull)
                .forEach(task -> Bukkit.getScheduler().cancelTask(task.getTaskId()));
        HandlerList.unregisterAll(loginListener);
        if (!hasShading()) {
            HandlerList.unregisterAll(shadingListener);
        }
        registeredPlugins.clear();
        commandManager = null;
        loginListener = null;
    }
    private ArrayList<BukkitTask> tasks = new ArrayList();

    /**
     * Register plugins that may not have been updated yet
     */
    private void registerAllPlugins() {
        PluginManager pm = Bukkit.getPluginManager();
        String p;
        if (!isRegistered(p = "EpicAnchors") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 31, Material.END_PORTAL_FRAME);
        }
        if (!isRegistered(p = "EpicBosses") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 19, Material.ZOMBIE_SPAWN_EGG);
        }
        if (!isRegistered(p = "EpicEnchants") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 67, Material.DIAMOND_SWORD);
        }
        if (!isRegistered(p = "EpicFarming") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 21, Material.WHEAT);
        }
        if (!isRegistered(p = "EpicFurnaces") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 22, Material.FURNACE);
        }
        if (!isRegistered(p = "EpicHeads") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 26, Material.PLAYER_HEAD);
        }
        if (!isRegistered(p = "EpicHoppers") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 15, Material.HOPPER);
        }
        if (!isRegistered(p = "EpicLevels") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 44, Material.NETHER_STAR);
        }
        if (!isRegistered(p = "EpicSpawners") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 13, Material.SPAWNER);
        }
        if (!isRegistered(p = "EpicVouchers") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 25, Material.EMERALD);
        }
        if (!isRegistered(p = "FabledSkyBlock") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 17, Material.GRASS_BLOCK);
        }
        if (!isRegistered(p = "UltimateCatcher") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 51, Material.EGG);
        }
        if (!isRegistered(p = "UltimateClaims") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 65, Material.CHEST);
        }
        if (!isRegistered(p = "UltimateFishing") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 59, Material.COD);
        }
        if (!isRegistered(p = "UltimateKits") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 14, Material.BEACON);
        }
        if (!isRegistered(p = "UltimateModeration") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 29, Material.DIAMOND_CHESTPLATE);
        }
        if (!isRegistered(p = "UltimateRepairing") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 20, Material.ANVIL);
        }
        if (!isRegistered(p = "UltimateStacker") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 16, Material.IRON_INGOT);
        }
        if (!isRegistered(p = "UltimateTimber") && pm.isPluginEnabled(p)) {
            register((JavaPlugin) pm.getPlugin(p), 18, Material.IRON_AXE);
        }
    }

    private void register(JavaPlugin plugin, int pluginID, Material icon) {
        register(plugin, pluginID, icon, "?");
    }

    private void register(JavaPlugin plugin, int pluginID, Material icon, String libraryVersion) {
        System.out.println(getPrefix() + "Hooked " + plugin.getName() + ".");
        PluginInfo info = new PluginInfo(plugin, pluginID, icon, libraryVersion);
        // don't forget to check for language pack updates ;)
        info.addModule(new LocaleModule());
        registeredPlugins.add(info);
        tasks.add(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> update(info), 60L));
    }

    private void update(PluginInfo plugin) {
        System.out.println("Updating " + plugin.getRebootID());
    }

    public static List<PluginInfo> getPlugins() {
        return new ArrayList<>(registeredPlugins);
    }

    public static int getCoreVersion() {
        return coreRevision;
    }

    public static String getCoreLibraryVersion() {
        return coreVersion;
    }

    public static int getUpdaterVersion() {
        return updaterVersion;
    }

    public static String getPrefix() {
        return prefix + " ";
    }

    public static boolean isRegistered(String plugin) {
        return registeredPlugins.stream().anyMatch(p -> p.getJavaPlugin().getName().equalsIgnoreCase(plugin));
    }

    public static JavaPlugin getHijackedPlugin() {
        return INSTANCE == null ? null : INSTANCE.piggybackedPlugin;
    }

    public static RebootCore getInstance() {
        return INSTANCE;
    }

    private static class ShadedEventListener implements Listener {
        boolean via = false;
        boolean proto = false;

        ShadedEventListener() {
            if ((via = Bukkit.getPluginManager().isPluginEnabled("ViaVersion"))) {
                Bukkit.getOnlinePlayers().forEach(p -> ClientVersion.onLoginVia(p));
            } else if ((proto = Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport"))) {
                Bukkit.getOnlinePlayers().forEach(p -> ClientVersion.onLoginProtocol(p));
            }
        }

        @EventHandler
        void onLogin(PlayerLoginEvent event) {
            if (via) {
                ClientVersion.onLoginVia(event.getPlayer());
            } else if (proto) {
                ClientVersion.onLoginProtocol(event.getPlayer());
            }
        }

        @EventHandler
        void onLogout(PlayerQuitEvent event) {
            if (via) {
                ClientVersion.onLogout(event.getPlayer());
            }
        }

        @EventHandler
        void onEnable(PluginEnableEvent event) {
            // technically shouldn't have online players here, but idk
            if (!via && (via = event.getPlugin().getName().equals("ViaVersion"))) {
                Bukkit.getOnlinePlayers().forEach(p -> ClientVersion.onLoginVia(p));
            } else if (!proto && (proto = event.getPlugin().getName().equals("ProtocolSupport"))) {
                Bukkit.getOnlinePlayers().forEach(p -> ClientVersion.onLoginProtocol(p));
            }
        }
    }

    private class EventListener implements Listener {
        final HashMap<UUID, Long> lastCheck = new HashMap();

        @EventHandler
        void onLogin(PlayerLoginEvent event) {
            final Player player = event.getPlayer();
            // don't spam players with update checks
            long now = System.currentTimeMillis();
            Long last = lastCheck.get(player.getUniqueId());
            if(last != null && now - 10000 < last) return;
            lastCheck.put(player.getUniqueId(), now);
            // is this player good to revieve update notices?
            if (!event.getPlayer().isOp() && !player.hasPermission("reboot.updatecheck")) return;
            // check for updates! ;)
            for (PluginInfo plugin : getPlugins()) {
                if (plugin.getNotification() != null && plugin.getJavaPlugin().isEnabled())
                    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin.getJavaPlugin(), () ->
                            player.sendMessage("[" + plugin.getJavaPlugin().getName() + "] " + plugin.getNotification()), 10L);
            }
        }

        @EventHandler
        void onDisable(PluginDisableEvent event) {
            // don't track disabled plugins
            PluginInfo pi = registeredPlugins.stream().filter(p -> event.getPlugin() == p.getJavaPlugin()).findFirst().orElse(null);
            if (pi != null) {
                registeredPlugins.remove(pi);
            }
            if (event.getPlugin() == piggybackedPlugin) {
                // uh-oh! Abandon ship!!
                Bukkit.getServicesManager().unregisterAll(piggybackedPlugin);
                // can we move somewhere else?
                if ((pi = registeredPlugins.stream().findFirst().orElse(null)) != null) {
                    // move ourselves to this plugin
                    piggybackedPlugin = pi.getJavaPlugin();
                    Bukkit.getServicesManager().register(RebootCore.class, INSTANCE, piggybackedPlugin, ServicePriority.Normal);
                    Bukkit.getPluginManager().registerEvents(loginListener, piggybackedPlugin);
                    Bukkit.getPluginManager().registerEvents(shadingListener, piggybackedPlugin);
                    CommandManager.registerCommandDynamically(piggybackedPlugin, "reboot", commandManager, commandManager);
                }
            }
        }
    }

}
