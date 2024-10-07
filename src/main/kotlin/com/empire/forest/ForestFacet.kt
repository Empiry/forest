package com.empire.forest

import com.empire.forest.bossbar.GameBossBar
import com.empire.forest.constants.ForestConstants
import com.empire.forest.gate.EscapeGate
import com.empire.forest.generator.ForestGenerator
import com.empire.forest.generator.GeneratorDescription
import com.empire.forest.kit.HunterKit
import com.empire.forest.kit.SurvivorKit
import com.empire.forest.mechanic.ForestMechanics
import com.empire.forest.scoreboard.SurvivorScoreboard
import com.empire.forest.tablist.ForestTablist
import com.empire.forest.util.ForestMessaging
import com.empire.ignite.Ignite
import com.empire.ignite.game.application.GameFacetV2
import com.empire.ignite.game.facets.DeathEvents
import com.empire.ignite.game.facets.system.PlayerManagementFacetEvents
import com.empire.ignite.game.facets.system.PlayerRemoveReason
import com.empire.ignite.util.*
import com.empire.ignite.util.callback.ForwardingCallback
import com.empire.ignite.util.gui.GuiPrototypes
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.item.PlayerItemModder
import com.empire.ignite.util.item.PlayerItemMods
import com.empire.ignite.util.region.CuboidRegion
import com.empire.ignite.util.region.RegionUtils
import com.empire.ignite.util.timer.Timer
import com.empire.ignite.util.timer.TimerCallback
import com.empire.ignite.util.ui.ManagedBossBar
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*

class ForestFacet(
    plugin: Ignite,
    context: ForestContext
) : GameFacetV2<ForestStaticData, ForestContext>(plugin, context), DeathEvents, PlayerManagementFacetEvents {
    private val dump = GlobalResourceTrackers.createResourceDump()
    private val escapees : MutableSet<Player> = mutableSetOf()
    private val generatorProgressCallback = ForwardingCallback.create<Unit>()

    override fun onLoad() {
//        val hunter = context.playerTracker.players.random()
        context.playerTracker.players.forEach { player ->
//            if (player == hunter) {
//                context.addHunter(player)
//            } else {
//                context.addSurvivor(player)
//            }
//            context.addSurvivor(player)
            context.addHunter(player)
        }


        val success = context.loadSpawns()
        if (!success) {
            this.panic("World was not loaded")
            return
        }
        context.hunterTeam.players.forEach { player -> player.teleport(context.huntersSpawn) }
        context.survivorTeam.players.forEach { player -> player.teleport(context.survivorsSpawn) }
        dump.add(object : UnloadableResource {
            override fun unload(external: Boolean) {
                context.hunterTeam.unload()
                context.survivorTeam.unload()
            }
        })

        val tab = ForestTablist(plugin, context)
        tab.load()
        dump.add(tab)
        context.tablist = tab

        val forestMechanics = ForestMechanics(plugin, context, dump)
        forestMechanics.load()
        dump.add(forestMechanics)
        context.forestMechanics = forestMechanics

        setupHunterKitStage()
        setupSurvivorKitStage()
    }

    private fun setupHunterKitStage() {
        val survivorBlockRegion =
            CuboidRegion(
                Vector(-43, 12, 274),
                Vector(-49, 7, 274)
            )
        RegionUtils.fillRegionReplacing(
            survivorBlockRegion,
            context.world!!,
            Material.BARRIER,
            { it.type == Material.AIR }
        )

        val hunterKitSelectionItem = ItemBuilder(Material.IRON_BARS) {
            name(Component.text("Choose Hunter Kit").color(NamedTextColor.RED))

            lore(listOf(Component.text("Right click to choose your kit!")))
        }

        val hunterTimeDump = GlobalResourceTrackers.createResourceDumpWithParent(dump)
        val defaultHunterKit = HunterKit.HUNTER
        val hunterKitSelection = mutableMapOf<UUID, HunterKit>()
        val itemModderHunter = PlayerItemModder(plugin, hunterKitSelectionItem, PlayerItemMods(
            allowDrop = false,
            rightClickAction = rca@{ player ->
                val (gui, _) = GuiPrototypes.getInteractiveOptionSelector(
                    plugin, player,
                    Component.text("Choose Hunter Kit").color(NamedTextColor.RED), 4,
                    HunterKit.entries.map { v ->
                        v.guiItem to v
                    },
                    defaultHunterKit,
                    true
                ) { hunterKitSelection[player.uniqueId] = it }
                gui.show(player)
            }
        ))
        context.hunterTeam.players.forEach { survivor ->
            survivor.inventory.clear()
            survivor.inventory.setItem(0, itemModderHunter.item)
            survivor.inventory.heldItemSlot = 0
            itemModderHunter.add(survivor)
        }
        itemModderHunter.load()
        hunterTimeDump.add(itemModderHunter)
        val hunterWaitSeconds = 5L
        context.sendToAll(ForestMessaging.withPrefix(
            Component.text("Hunters will be released in ").color(NamedTextColor.GRAY)
                .append(Component.text(hunterWaitSeconds.toString()).color(NamedTextColor.GOLD))
                .append(Component.text(" seconds!").color(NamedTextColor.GRAY))
        ))
        Timer(plugin, 20L * hunterWaitSeconds)
            .attach(object : TimerCallback {
                private val bossBar: ManagedBossBar = ManagedBossBar(
                    plugin.adventure(), BossBar.Color.RED,
                    Component.text("Choose your kits!").color(NamedTextColor.RED)
                )
                init {
                    bossBar.bossbar.progress(1.0f)
                    hunterTimeDump.add(bossBar)
                    bossBar.setupPlayerChangeSet(context.getHunterChangeSet())
                }

                override fun onTick(originalTicks: Long, remainingTicks: Long) {
                    val seconds = remainingTicks / 20
                    if (seconds > 0) {
                        bossBar.bossbar.name(
                            Component.text("You will be released in ").color(NamedTextColor.RED)
                                .append(Component.text("$seconds").color(NamedTextColor.DARK_RED))
                                .append(Component.text(" ${
                                    amountSuffix("second", seconds.toInt())
                                }").color(NamedTextColor.RED))
                        )
                    }
                    bossBar.bossbar.progress(remainingTicks.toFloat() / originalTicks.toFloat())
                }
            }).then {
                hunterTimeDump.destroyAll()
                healAndClearInventoryOfPlayers(context.hunterTeam.players)
                context.hunterTeam.players.forEach { player ->
                    player.closeInventory()
                    val selectedKit = hunterKitSelection[player.uniqueId] ?: defaultHunterKit
                    selectedKit.gameKit.apply(player, context)
                    Bukkit.broadcast(Component.text("Player ${player.name} chose kit ${selectedKit.name}"))
                }
                RegionUtils.fillRegionReplacing(
                    survivorBlockRegion,
                    context.world!!,
                    Material.AIR,
                    { it.type == Material.BARRIER }
                )
            }.start()
    }

    private fun setupSurvivorKitStage() {
        RegionUtils.fillRegionBlockData(
            context.staticData.survivorSpawnBarrierRegion,
            context.world!!,
            HACKS.createIronBarsBlockData(listOf(BlockFace.EAST, BlockFace.WEST))
        )

        val survivorKitSelectionItem = ItemBuilder(Material.CLOCK) {
            name(Component.text("Choose Survivor Kit").color(ForestConstants.SURVIVORS_COLOR))

            lore(listOf(Component.text("Right click to choose your kit!")))
        }

        val survivorTimeDump = GlobalResourceTrackers.createResourceDumpWithParent(dump)
        val defaultSurvivorKit = SurvivorKit.SURVIVALIST
        val survivorKitSelection = mutableMapOf<UUID, SurvivorKit>()
        val itemModderSurvivor = PlayerItemModder(plugin, survivorKitSelectionItem, PlayerItemMods(
            allowDrop = false,
            rightClickAction = rca@{ player ->
                val (gui, _) = GuiPrototypes.getInteractiveOptionSelector(
                    plugin, player,
                    Component.text("Choose Survivor Kit").color(ForestConstants.SURVIVORS_COLOR), 4,
                    SurvivorKit.entries.map { v ->
                        v.guiItem to v
                    },
                    defaultSurvivorKit,
                    true
                ) { survivorKitSelection[player.uniqueId] = it }
                gui.show(player)
            }
        ))
        context.survivorTeam.players.forEach { survivor ->
            survivor.inventory.clear()
            survivor.inventory.setItem(0, itemModderSurvivor.item)
            survivor.inventory.heldItemSlot = 0
            itemModderSurvivor.add(survivor)
        }
        itemModderSurvivor.load()
        survivorTimeDump.add(itemModderSurvivor)
        val survivorWaitSeconds = 7L
        context.sendToAll(ForestMessaging.withPrefix(
            Component.text("Survivors will be released in ").color(NamedTextColor.GRAY)
                .append(Component.text(survivorWaitSeconds.toString()).color(NamedTextColor.GOLD))
                .append(Component.text(" seconds!").color(NamedTextColor.GRAY))
        ))
        Timer(plugin, 20L * survivorWaitSeconds)
            .attach(object : TimerCallback {
                private val bossBar: ManagedBossBar = ManagedBossBar(
                    plugin.adventure(), BossBar.Color.RED,
                    Component.text("Choose your kits!").color(ForestConstants.SURVIVORS_COLOR)
                )
                init {
                    bossBar.bossbar.progress(1.0f)
                    survivorTimeDump.add(bossBar)
                    bossBar.setupPlayerChangeSet(context.getSurvivorChangeSet())
                }

                override fun onTick(originalTicks: Long, remainingTicks: Long) {
                    val seconds = remainingTicks / 20
                    if (seconds > 0) {
                        bossBar.bossbar.name(
                            Component.text("Starting in ").color(NamedTextColor.RED)
                                .append(Component.text("$seconds").color(NamedTextColor.DARK_RED))
                                .append(Component.text(" ${
                                    amountSuffix("second", seconds.toInt())
                                }").color(NamedTextColor.RED))
                        )
                    }
                    bossBar.bossbar.progress(remainingTicks.toFloat() / originalTicks.toFloat())
                }
            })
            .then {
                survivorTimeDump.destroyAll()
                context.survivorTeam.players.forEach { player ->
                    val selectedKit = survivorKitSelection[player.uniqueId] ?: defaultSurvivorKit
                    Bukkit.broadcast(Component.text("Player ${player.name} chose kit ${selectedKit.name}"))
                    player.closeInventory()
                }
                healAndClearInventoryOfPlayers(context.survivorTeam.players)
                RegionUtils.fillRegion(
                    context.staticData.survivorSpawnBarrierRegion,
                    context.world!!,
                    Material.AIR
                )
                context.sendToAll(ForestMessaging.createForestMessage("Survivors have been released!"))

                setupGeneratorTask(context.staticData.generators) {
                    sendMessageToPlayers(
                        context.playerAccess.all,
                        Component.newline()
                            .append(
                                ForestMessaging.withPrefix(
                                    Component.text("All generators have been ").color(NamedTextColor.GRAY)
                                        .append(Component.text("unlocked! ").color(NamedTextColor.GREEN))
                                        .append(Component.text("Survivors can now ").color(NamedTextColor.GRAY))
                                        .append(Component.text("escape!").color(NamedTextColor.YELLOW))
                                )
                            )
                            .append(Component.newline())
                    )
                    playSoundToPlayers(
                        context.playerAccess.all,
                        Sound.BLOCK_NOTE_BLOCK_CHIME,
                        1.0F, 1.0F
                    )
                    context.survivorTeam.players.forEach {
                            player -> player.showTitle(
                        Title.title(
                            Component.text("You can now escape!").color(NamedTextColor.GOLD),
                            Component.text("Run to the gate!").color(NamedTextColor.YELLOW)
                        )
                    )
                    }
                    val gate = EscapeGate(
                        plugin, context,
                        context.staticData.escape.location.toLocation(context.world)!!,
                        context.staticData.escape.radius
                    ) { escapee ->
                        escapees += escapee
                        if (escapees.size >= context.survivorTeam.players.size) {
                            finishGame()
                        }
                    }
                    gate.load()
                    dump.add(gate)
                }
            }
            .start()
    }

    private fun setupGeneratorTask(generators: List<GeneratorDescription>, after: () -> Unit) {
        val generatorCount = generators.size
        var generatorsFinished = 0

        val generatorBossBar = GameBossBar(
            plugin, context, generatorCount,
            ChangingSetUtils.unionContractsToView(
                context.getHunterChangeSet(),
                context.getSurvivorChangeSet()
            )
        )
        generatorBossBar.load()
        dump.add(generatorBossBar)

        val generatorMap = mutableMapOf<GeneratorDescription, ForestGenerator>()

        generators.forEach { desc ->
            val spot = desc.place.toLocation(context.world)!!
            val forwarder = ForwardingCallback.create<Int>()
            forwarder.register { generatorProgressCallback.sink(Unit) }
            val generator = ForestGenerator(plugin, context, spot, desc.unlockSeconds, forwarder.sink) {
                context.playerTracker.players.forEach { survivor ->
                    survivor.sendMessage(
                        ForestMessaging.withPrefix(
                            Component.text("Generator ", NamedTextColor.GRAY)
                                .append(Component.text(desc.name, NamedTextColor.GOLD))
                                .append(Component.text(" was completed!", NamedTextColor.GRAY))
                        )
                    )
                    survivor.playSound(
                        survivor.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0F, 1.0F
                    )
                }
                generatorsFinished++
                generatorBossBar.setGeneratorsComplete(generatorsFinished)
                if (generatorsFinished >= generatorCount) {
                    after()
                }
            }
            dump.add(generator)
            generator.load()
            generatorMap += desc to generator
        }
        val generatorsAssociationList = generatorMap.toList()

        context.tablist.registerGeneratorProgressUpdate(
            generatorsAssociationList, generatorProgressCallback, dump
        )
        SurvivorScoreboard.create(
            plugin, context, generatorsAssociationList,
            context.getSurvivorChangeSet(), generatorProgressCallback, dump
        )
    }

    private var gameOver = false
    private fun finishGame() {
        Bukkit.broadcast(
            Component.text("Match has ended!").color(NamedTextColor.GREEN)
        )
        gameOver = true
        destruct()
    }

    override fun onPlayerRemove(player: Player, reason: PlayerRemoveReason) {
        context.removeFromTeam(player)
//        if (context.playerAccess.all.isEmpty()) {
//            finishGame()
//        }
    }

    override fun onPlayerDeath(player: Player, damageInfo: EntityDamageEvent) {
        if (gameOver) return
        context.playerTracker.removePlayer(player, PlayerRemoveReason.SYSTEM)
        if (gameOver) return
        val addedAsSpectator = context.playerTracker.addSpectator(player)
        if (!addedAsSpectator) {
            IGNITE_LOGGER.warning(
                "Dead survivor ${player.name} could not be added as spectator"
            )
        }
        player.playSound(
            player.location,
            Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.MASTER, 0.4F, 1.0F
        )
        player.showTitle(Title.title(
            Component.text("YOU DIED!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
            Component.text("You are now spectating").color(NamedTextColor.YELLOW),
        ))
        player.sendMessage(
            Component.newline()
                .append(
                    Component.text("You are now spectating, use /leave to leave! " +
                            "You can fly around and watch others")
                        .color(NamedTextColor.RED)
                )
                .append(Component.newline())
        )
        player.addPotionEffect(PotionEffect(
            PotionEffectType.DARKNESS, 80, 1, true, false
        ))
        player.addPotionEffect(PotionEffect(
            PotionEffectType.BLINDNESS, 60, 1, true, false
        ))
    }

    override fun onUnload() {
        dump.destroyAll()
    }
}
