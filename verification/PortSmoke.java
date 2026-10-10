package porttest;

import java.util.*;
import java.util.logging.Level;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.coreprotect.bukkit.BukkitAdapter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import io.github.cpvisualizer.*;

/** Runs only in a disposable server: creates test blocks and rolls them back. */
public final class PortSmoke extends JavaPlugin {
    private final String actor = "PortSmoke" + Long.toUnsignedString(System.currentTimeMillis(), 36);
    private CoreProtectAPI api;
    private CoreProtectVisualizerPlugin visualizer;
    private World world;
    private Location center;
    private List<Block> blocks;
    private Map<String, World> worlds;
    private int attempts;

    @Override public void onEnable() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                CoreProtect cp = (CoreProtect) Bukkit.getPluginManager().getPlugin("CoreProtect");
                visualizer = (CoreProtectVisualizerPlugin) Bukkit.getPluginManager().getPlugin("CPVisualizer");
                check(cp != null && cp.isEnabled(), "CoreProtect enabled");
                check(visualizer != null && visualizer.isEnabled(), "CPVisualizer enabled");
                api = cp.getAPI();
                check(api.isEnabled() && api.APIVersion() == 12, "public API 12");
                check(BukkitAdapter.ADAPTER.getClass().getSimpleName().equals("Bukkit_v26_3"), "26.3 adapter selected");
                world = Bukkit.getWorlds().getFirst();
                Map<String, World> loaded = new HashMap<>();
                Bukkit.getWorlds().forEach(value -> loaded.put(value.getName(), value));
                worlds = Map.copyOf(loaded);
                center = new Location(world, 0, 80, 0);
                blocks = new ArrayList<>();
                Material[] materials = {Material.STONE, Material.SULFUR_SPIKE, Material.POTENT_SULFUR, Material.CHEST};
                for (int i = 0; i < materials.length; i++) {
                    Block block = world.getBlockAt(i, 80, 0);
                    world.getBlockAt(i, 79, 0).setType(Material.STONE, false);
                    block.setType(materials[i], false);
                    blocks.add(block);
                    check(api.logPlacement(actor + (i % 2 == 0 ? "A" : "B"), block.getState()), "queued " + materials[i]);
                }
                Block system = world.getBlockAt(5, 80, 0);
                system.setType(Material.DIRT, false);
                check(api.logPlacement("#fire", system.getState()), "system actor fixture queued");
                World secondWorld = Bukkit.getWorlds().get(1);
                Block secondBlock = secondWorld.getBlockAt(0, 80, 0);
                secondBlock.setType(Material.BRICKS, false);
                check(api.logPlacement(actor + "A", secondBlock.getState()), "second-world fixture queued");
                Chest chest = (Chest) blocks.get(3).getState();
                chest.getBlockInventory().clear();
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    try {
                        Chest liveChest = (Chest) blocks.get(3).getState(false);
                        check(api.logContainerTransaction(actor + "B", liveChest.getLocation()), "container snapshot queued");
                        liveChest.getBlockInventory().addItem(new ItemStack(Material.DIAMOND, 3));
                        getLogger().info("Container fixture after insert: " + Arrays.toString(liveChest.getBlockInventory().getContents()));
                    } catch (Throwable failure) { fail(failure); }
                }, 40);
                BlockDisplay display = world.spawn(center.clone().add(0, 3, 0), BlockDisplay.class, entity -> {
                    entity.setVisibleByDefault(false);
                    entity.setPersistent(false);
                    entity.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
                    entity.setGlowColorOverride(Color.LIME);
                    entity.setBrightness(new Display.Brightness(15, 15));
                });
                check(!display.isVisibleByDefault() && !display.isPersistent(), "private display defaults");
                display.remove();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "co status");
                Bukkit.getScheduler().runTaskLaterAsynchronously(this, this::query, 100);
            } catch (Throwable failure) { fail(failure); }
        }, 40);
    }

    private void query() {
        try {
            CoreProtectHook hook = new CoreProtectHook();
            SearchQuery all = queryFor(null, null, SearchQuery.Scope.RADIUS);
            var results = hook.lookup(all, () -> {}).results();
            long found = results.stream().filter(row -> row.player().startsWith(actor)).count();
            if (found < 4 && ++attempts < 12) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(this, this::query, 40);
                return;
            }
            check(found == 4, "SQLite history visible through CPVisualizer, four placements");
            check(results.stream().noneMatch(row -> row.player().startsWith("#")), "all-player filter excludes system actors");
            var stone = hook.lookup(queryFor(actor + "A", Material.STONE, SearchQuery.Scope.RADIUS), () -> {}).results();
            check(stone.size() == 1 && stone.getFirst().block() == Material.STONE, "single player/material filter");
            var global = hook.lookup(queryFor(null, null, SearchQuery.Scope.ALL), () -> {}).results();
            check(global.stream().filter(row -> row.player().startsWith(actor)).count() == 5, "all-world lookup merges two real worlds");
            var containers = api.containerLookup(blocks.get(3).getLocation(), 3600);
            boolean containerFound = containers != null && containers.stream().anyMatch(row -> row.getPlayer().equals(actor + "B")
                    && row.getType() == Material.DIAMOND && row.getAmount() == 3);
            if (!containerFound && ++attempts < 12) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(this, this::query, 40);
                return;
            }
            check(containerFound, "SQLite container item transaction");
            SearchQuery containerQuery = new SearchQuery(null, Material.CHEST, ActionType.CONTAINER_ADD, 3600,
                    SearchQuery.Scope.RADIUS, 16, center, world.getName(), worlds, visualizer.settings());
            var containerResults = hook.lookup(containerQuery, () -> {}).results();
            check(containerResults.stream().anyMatch(row -> row.player().equals(actor + "B") && row.block() == Material.CHEST),
                    "CPVisualizer resolves container material as CHEST, not DIAMOND");
            var rollback = api.performRollback(3600, new ArrayList<>(List.of(actor + "A", actor + "B")), null,
                    null, null, new ArrayList<>(List.of(1)), 16, center);
            check(rollback != null && !rollback.isEmpty(), "rollback submitted");
            Bukkit.getScheduler().runTaskLater(this, this::afterRollback, 60);
        } catch (Throwable failure) { fail(failure); }
    }

    private SearchQuery queryFor(String player, Material material, SearchQuery.Scope scope) {
        // Captured world handles are only passed into CoreProtect lookup options.
        return new SearchQuery(player, material, ActionType.PLACE, 3600, scope, 16,
                center, world.getName(), worlds, visualizer.settings());
    }

    private void afterRollback() {
        try {
            check(blocks.stream().allMatch(block -> block.getType().isAir()), "rollback removed all four blocks");
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    var restored = api.performRestore(3600, new ArrayList<>(List.of(actor + "A", actor + "B")), null,
                            null, null, new ArrayList<>(List.of(1)), 16, center);
                    check(restored != null && !restored.isEmpty(), "restore submitted");
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        try {
                            Material[] expected = {Material.STONE, Material.SULFUR_SPIKE, Material.POTENT_SULFUR, Material.CHEST};
                            for (int i = 0; i < expected.length; i++) check(blocks.get(i).getType() == expected[i], "restored " + expected[i]);
                            getLogger().info("PORT_SMOKE_PASS");
                            Bukkit.shutdown();
                        } catch (Throwable failure) { fail(failure); }
                    }, 60);
                } catch (Throwable failure) { fail(failure); }
            });
        } catch (Throwable failure) { fail(failure); }
    }

    private void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
        getLogger().info("PASS: " + message);
    }

    private void fail(Throwable failure) {
        getLogger().log(Level.SEVERE, "PORT_SMOKE_FAIL", failure);
        Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
    }
}
