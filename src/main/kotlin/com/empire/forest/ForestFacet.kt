package com.empire.forest

import com.empire.forest.bossbar.GameBossBar
import com.empire.forest.constants.ForestConstants
import com.empire.forest.data.ForestUserData
import com.empire.forest.data.ForestUserDatabase
import com.empire.forest.gate.EscapeGate
import com.empire.forest.generator.ForestGenerator
import com.empire.forest.generator.ForestGeneratorDiscovery
import com.empire.forest.generator.ForestGeneratorNameplate
import com.empire.forest.generator.GeneratorDescription
import com.empire.forest.generator.NameplateProximityTask
import com.empire.forest.hud.HunterHUD
import com.empire.forest.hud.SlasherHUD
import com.empire.forest.kit.ForestKitKey
import com.empire.forest.kit.ForestKitProvider
import com.empire.forest.kit.HunterKit
import com.empire.forest.kit.SurvivorKit
import com.empire.forest.mechanic.SurvivorHealth
import com.empire.forest.mechanic.downed.DownedMechanic
import com.empire.forest.mechanic.phantom.CloakingPerk
import com.empire.forest.mechanic.stamina.SurvivorStamina
import com.empire.forest.mechanic.werewolf.WerewolfSpeedPerk
import com.empire.forest.perk.BearTrapPerk
import com.empire.forest.perk.BloodhoundPerk
import com.empire.forest.perk.BloodlustPerk
import com.empire.forest.perk.BuckshotPerk
import com.empire.forest.perk.FearPerk
import com.empire.forest.perk.HeartbeatPerk
import com.empire.forest.perk.JammerPerk
import com.empire.forest.perk.MachetePerk
import com.empire.forest.perk.MedicPerk
import com.empire.forest.perk.NewMotionSensorPerk
import com.empire.forest.perk.RoarPerk
import com.empire.forest.perk.ShadowPerk
import com.empire.forest.perk.StaredownPerk
import com.empire.forest.scoreboard.SurvivorScoreboard
import com.empire.forest.spectator.SpectatorTracker
import com.empire.forest.util.ForestMessaging
import com.empire.forest.util.ForestUtils
import com.empire.ignite.Ignite
import com.empire.ignite.display.Displays
import com.empire.ignite.game.application.GameFacetV2
import com.empire.ignite.game.facets.DeathEvents
import com.empire.ignite.game.facets.system.PlayerManagementFacetEvents
import com.empire.ignite.game.facets.system.PlayerRemoveReason
import com.empire.ignite.game.kit.KitTracker
import com.empire.ignite.util.*
import com.empire.ignite.util.callback.ForwardingCallback
import com.empire.ignite.util.gui.GuiPrototypes
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.item.PlayerItemModder
import com.empire.ignite.util.item.PlayerItemMods
import com.empire.ignite.util.region.RegionUtils
import com.empire.ignite.util.text.TextUtils.flattenComponents
import com.empire.ignite.util.timer.Timer
import com.empire.ignite.util.timer.TimerCallback
import com.empire.ignite.util.ui.ManagedBossBar
import com.empire.ignite.util.worldborder.WorldBorderTint
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

class ForestFacet(
    plugin: Ignite,
    context: ForestContext,
    private val userDatabase: ForestUserDatabase,
    private val dump: ManagedResourceDump
) : GameFacetV2<ForestStaticData, ForestContext>(plugin, context), DeathEvents, PlayerManagementFacetEvents, Listener {
    private val escapees : MutableSet<UUID> = mutableSetOf()
    private val generatorProgressCallback = ForwardingCallback.create<Unit>()
    private val kitTracker: KitTracker<ForestKitKey, ForestContext> = KitTracker(plugin, ForestKitProvider)
    private val debug = (context.interactiveData?.isDebug ?: false)
    private var hunterHUD : HunterHUD? = null
    private var slasherHUD : SlasherHUD? = null

    override fun onLoad() {
        setupPerks()
        val hunter = context.playerTracker.players.random()
        healStabilizePlayers(context.playerTracker.players)
        context.playerTracker.players.forEach { player ->
            player.gameMode = GameMode.ADVENTURE
            if (debug) {
//                context.addSurvivor(player)
                if ("ThatOneTqnk".equals(player.name, ignoreCase = true)) {
                    context.addHunter(player)
//                    context.addSurvivor(player)
                } else {
                    context.addSurvivor(player)
                }
            } else {
                if (player == hunter) {
                    context.addHunter(player)
                } else {
                    context.addSurvivor(player)
                }
            }
            Displays.getActionBar(player).displayQueueManager.removeAll()
        }

        val success = context.loadSpawns()
        if (!success) {
            this.panic("World was not loaded")
            return
        }
        context.world!!.time = 19000
        context.world!!.setGameRule(GameRules.ADVANCE_TIME, false)
        context.world!!.setGameRule(GameRules.LOCATOR_BAR, false)
        context.world!!.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false)
        context.hunterTeam.players.forEach { player -> player.teleport(context.huntersSpawn) }
        context.survivorTeam.players.forEach { player ->
            player.teleport(
                context.survivorsRegion?.random(0.4)?.toLocation(context.world!!) ?:
                context.survivorsSpawn
            )
        }
        dump.add(object : UnloadableResource {
            override fun unload(external: Boolean) {
                context.hunterTeam.unload()
                context.survivorTeam.unload()
            }
        })

        dump.add(SpectatorTracker(plugin, context))

//        val tab = ForestTablist(plugin, context)
//        tab.load()
//        dump.add(tab)
//        context.tablist = tab

        this.hunterHUD = HunterHUD(plugin, context, kitTracker)
        this.hunterHUD!!.load()
        dump.add(hunterHUD!!)
        this.slasherHUD = SlasherHUD(plugin, context, kitTracker)
        this.slasherHUD!!.load()
        dump.add(slasherHUD!!)

        val stamina = SurvivorStamina(plugin, context, drainSeconds = 20, regainSeconds = 10)
        stamina.load()
        dump.add(stamina)
        context.stamina = stamina

        val health = SurvivorHealth(plugin, context)
        health.load()
        dump.add(health)
        context.survivorHealth = health

        val downed = DownedMechanic(plugin, context)
        context.messageBus.add(downed)
        dump.add(downed)

        dump.add(object : UnloadableResource {
            override fun unload(external: Boolean) {
                context.forestPlayerContextData.values.forEach { it.unload(external) }
                context.forestPlayerContextData.unload(external)
            }
        })

        setupHunterKitStage()
        setupSurvivorKitStage()
    }

    private fun setupHunterKitStage() {
        RegionUtils.fillRegionReplacing(
            context.staticData.hunterSpawnBarrierRegion,
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
                    hunterKitSelection[player.uniqueId] ?: getHunterKitPref(player) ?: defaultHunterKit,
                    true
                ) { hunterKitSelection[player.uniqueId] = it }
                gui.show(player)
            }
        ))
        context.hunterTeam.players.forEach { hunter ->
            hunter.inventory.clear()
            hunter.inventory.setItem(0, itemModderHunter.item)
            hunter.inventory.heldItemSlot = 0
            itemModderHunter.add(hunter)
            hunter.showTitle(Title.title(
                Component.text("Choose your kit!").color(NamedTextColor.DARK_RED),
                Component.text("Right-click the kit selector item to choose").color(NamedTextColor.RED)
            ))
        }
        itemModderHunter.load()
        hunterTimeDump.add(itemModderHunter)
        val hunterWaitSeconds = context.staticData.hunterReleaseSeconds
        context.sendToAll(ForestMessaging.withPrefix(
            Component.text("Hunters will be released in ").color(NamedTextColor.GRAY)
                .append(Component.text(hunterWaitSeconds.toString()).color(NamedTextColor.GOLD))
                .append(Component.text(" seconds!").color(NamedTextColor.GRAY))
        ))
        val chooseTimer = Timer(plugin, 20L * hunterWaitSeconds)
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
                    val selectedKit = hunterKitSelection[player.uniqueId] ?:
                        getHunterKitPref(player) ?: defaultHunterKit
//                    selectedKit.gameKit.apply(player, context)
                    kitTracker.addKitToPlayer(player, ForestKitKey(selectedKit), context)
                }
                userDatabase.setDataBatch(
                    context.hunterTeam.players.mapNotNull { player ->
                        val kitSel = hunterKitSelection[player.uniqueId] ?: return@mapNotNull null
                        val userData = getUserData(player) ?: return@mapNotNull null
                        userData.lastSelectedHunterKit = kitSel.name
                        player to userData
                    }.associate { (p, u) -> p.uniqueId to u }.map { it.toPair() }
                )
                hunterHUD!!.onKitSelection()
                slasherHUD!!.onKitSelection()
                if (context.staticData.mapName == "farm") {
                    RegionUtils.fillRegion(
                        context.staticData.hunterSpawnBarrierRegion,
                        context.world!!,
                        Material.AIR
                    )
                } else {
                    RegionUtils.fillRegionReplacing(
                        context.staticData.hunterSpawnBarrierRegion,
                        context.world!!,
                        Material.AIR,
                        { it.type == Material.BARRIER }
                    )
                }
//                setupHunterPerks()
            }
        dump.add(chooseTimer)
        chooseTimer.start()
    }

    private fun setupSurvivorKitStage() {
        if (context.staticData.mapName == "farm") {
        } else {
            RegionUtils.fillRegionBlockData(
                context.staticData.survivorSpawnBarrierRegion,
                context.world!!,
                HACKS.createIronBarsBlockData(listOf(BlockFace.EAST, BlockFace.WEST))
            )
        }

        val survivorKitSelectionItem = ItemBuilder(Material.CLOCK) {
            name(Component.text("Choose Survivor Kit").color(ForestConstants.SURVIVORS_COLOR))

            lore(listOf(Component.text("Right click to choose your kit!")))
        }

        val survivorTimeDump = GlobalResourceTrackers.createResourceDumpWithParent(dump)
        val defaultSurvivorKit = SurvivorKit.NAVIGATOR
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
                    survivorKitSelection[player.uniqueId] ?: getSurvivorKitPref(player)
                    ?: defaultSurvivorKit,
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
            survivor.showTitle(Title.title(
                Component.text("Choose your kit!").color(ForestConstants.SURVIVORS_COLOR),
                Component.text("Right-click the kit selector item to choose").color(NamedTextColor.YELLOW)
            ))
            ForestUtils.freezePlayer(survivor)
        }
        itemModderSurvivor.load()
        survivorTimeDump.add(itemModderSurvivor)
        val survivorWaitSeconds = context.staticData.survivorReleaseSeconds
        context.sendToAll(ForestMessaging.withPrefix(
            Component.text("Survivors will be released in ").color(NamedTextColor.GRAY)
                .append(Component.text(survivorWaitSeconds.toString()).color(NamedTextColor.GOLD))
                .append(Component.text(" seconds!").color(NamedTextColor.GRAY))
        ))
        val chooseTimer = Timer(plugin, 20L * survivorWaitSeconds)
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
                healAndClearInventoryOfPlayers(context.survivorTeam.players)
                context.survivorTeam.players.forEach { player ->
                    val selectedKit = survivorKitSelection[player.uniqueId] ?:
                        getSurvivorKitPref(player) ?: defaultSurvivorKit
                    player.closeInventory()
                    kitTracker.addKitToPlayer(player, ForestKitKey(selectedKit), context)
                }

                userDatabase.setDataBatch(
                    context.survivorTeam.players.mapNotNull { player ->
                        val kitSel = survivorKitSelection[player.uniqueId] ?: return@mapNotNull null
                        val userData = getUserData(player) ?: return@mapNotNull null
                        userData.lastSelectedSurvivorKit = kitSel.name
                        player to userData
                    }.associate { (p, u) -> p.uniqueId to u }.map { it.toPair() }
                )
                RegionUtils.fillRegion(
                    context.staticData.survivorSpawnBarrierRegion,
                    context.world!!,
                    Material.AIR
                )
                for (overwriteRegion in context.staticData.overwriteRegions) {
                    RegionUtils.fillRegion(
                        overwriteRegion,
                        context.world!!,
                        Material.BARRIER
                    )
                }
                context.sendToAll(ForestMessaging.createForestMessage("Survivors have been released!"))

                val selectedGenerators = if (context.staticData.generatorSelection == null)
                    context.staticData.generators
                else
                    context.staticData.generatorSelection!!.invoke(context.staticData.generators)
                setupGeneratorTask(selectedGenerators) {
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
                        context.staticData.escape.region
                    ) { escapee ->
                        val survivorCount = context.survivorTeam.players.size
                        escapees += escapee.uniqueId
                        context.sendToAll(
                ForestMessaging.withPrefix(
                    escapee.displayName()
                                    .append(Component.text(" has ", NamedTextColor.GRAY))
                                    .append(Component.text("escaped!", NamedTextColor.YELLOW))
                            )
                        )
                        playSoundToPlayers(
                            context.playerAccess.all,
                            Sound.UI_BUTTON_CLICK,
                            1.0F, 1.0F
                        )
                        skipGameOverCheck = true
                        context.playerTracker.removePlayer(escapee, PlayerRemoveReason.SYSTEM)
                        context.addSpectator(escapee)
                        skipGameOverCheck = false
                        if (survivorCount <= 1) {
                            finishGame(Winner.SURVIVOR)
                        }
                    }
                    gate.load()
                    dump.add(gate)
                }
//                setupSurvivorPerks()
            }
        dump.add(chooseTimer)
        chooseTimer.start()
    }

    private fun setupGeneratorTask(generators: List<GeneratorDescription>, after: () -> Unit) {
        val minRequired = context.staticData.minRequiredGeneratorsFn(context)
        val generatorCount =
            if (minRequired == -1) generators.size
            else minRequired
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
        context.gameBossBar = generatorBossBar

        val generatorMap = mutableMapOf<GeneratorDescription, ForestGenerator>()

        generators.forEach { desc ->
            val spot = desc.place.toLocation(context.world)!!
            val forwarder = ForwardingCallback.create<Int>()
            forwarder.register { generatorProgressCallback.sink(Unit) }
            val generator = ForestGenerator(
                plugin, context, desc,  spot, desc.unlockSeconds,
                context.staticData.contributionMultiplier, forwarder.sink,
            ) {
                context.survivorTeam.players.forEach { survivor ->
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
            ForestUtils.replacePlaceholderBlocks(spot)
            generatorMap += desc to generator
        }
        val generatorsAssociationList = generatorMap.toList()

        val task = NameplateProximityTask(plugin, context, generatorsAssociationList.map { it.second })
        val action = { task.run() }
        dump.add(task)
        dump.add(GlobalResourceTrackers.scheduler.repeatTask(5L, action))
        context.proximityTask = task

        val discovery = ForestGeneratorDiscovery(context, generatorsAssociationList)
        dump.add(discovery)

        context.tablist?.registerGeneratorProgressUpdate(
            generatorsAssociationList, discovery, generatorProgressCallback, dump
        )
        SurvivorScoreboard.create(
            plugin, context, generatorsAssociationList,
            discovery, context.getSurvivorChangeSet(), generatorProgressCallback, dump
        )

        generatorsAssociationList.forEach { (desc, gen) ->
            val nameplate =
                ForestGeneratorNameplate(
                    plugin, discovery,  context, desc, gen
                )
            context.nameplates += nameplate
            dump.add(nameplate)
        }
    }

    private var skipGameOverCheck = false
    private var gameOver = false
    enum class Winner {
        SURVIVOR, HUNTER
    }
    private fun finishGame(override: Winner? = null) {
        if (skipGameOverCheck || gameOver) return
        var winner : Winner? = null
        if (override != null) winner = override
        else if (context.survivorTeam.players.isEmpty()) winner = Winner.HUNTER
        else winner = Winner.SURVIVOR

        val winnerMessage = if (winner == Winner.HUNTER)
            Component.text("HUNTERS WIN!").color(NamedTextColor.RED)
        else
            Component.text("SURVIVORS WIN!").color(NamedTextColor.GREEN)
        val dash =
            Component.text(" ".repeat(20)).color(NamedTextColor.DARK_GRAY)
                .decorate(TextDecoration.STRIKETHROUGH)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            Bukkit.broadcast(
                flattenComponents(
                    dash.appendNewline(),
                    winnerMessage.appendNewline(),
                    dash
                )
            )
            playSoundToPlayers(
                Bukkit.getOnlinePlayers(),
                Sound.UI_TOAST_CHALLENGE_COMPLETE,
                1f, 1f
            )
        }, 1L)
        gameOver = true
        context.playerAccess.all.forEach { context.playerTracker.removePlayer(it) }
        destruct()
    }

    override fun onPlayerRemove(player: Player, reason: PlayerRemoveReason) {
        context.removeFromTeam(player)
        if ((context.hunterTeam.players.isEmpty() || context.survivorTeam.players.isEmpty())) {
//        if ((context.hunterTeam.players.isEmpty())) {
            finishGame()
        }
    }

    override fun onPlayerDeath(player: Player, damageInfo: EntityDamageEvent) {
        val currentLoc = player.location
        if (gameOver) return
        kitTracker.removeKitsFromPlayer(player)
        context.playerTracker.removePlayer(player, PlayerRemoveReason.SYSTEM)
        if (gameOver) return
        val addedAsSpectator = context.addSpectator(player)
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
        player.teleport(currentLoc)
//        player.addPotionEffect(PotionEffect(
//            PotionEffectType.DARKNESS, 80, 1, true, false
//        ))
//        player.addPotionEffect(
//            PotionEffect(
//                PotionEffectType.BLINDNESS, 60, 1, true, false
//            )
//        )
        WorldBorderTint.add(player)
        val timer = Timer(plugin, 100L)
            .then { WorldBorderTint.remove(player) }
            .whenCancelled { WorldBorderTint.remove(player) }
        timer.start()
        dump.add(timer)
    }

    @EventHandler
    private fun onHunger(event: FoodLevelChangeEvent) {
        val e = event.entity
        if (e !is Player || !context.shouldReplenishHunger(e)) return
        event.isCancelled = true
        event.entity.foodLevel = 20
    }

    @EventHandler
    private fun onBlockFade(event: BlockFadeEvent) {
        if (event.block.world != context.world) return
        if (event.block.type.name.contains("CORAL_FAN")) {
            event.isCancelled = true
        }
    }

    @EventHandler
    private fun onMove(event: PlayerMoveEvent) {
        if (event.from.world != context.world) return
        if (event.player !in context.playerAccess.alive || event.player !in context.survivorTeam.players) return
        if (!getBodyBlocks(event.from).any { it.block.type.name.contains("CORAL_FAN") }) return
        if (event.player.activePotionEffects.any { it.type == PotionEffectType.SLOWNESS }) return
        event.player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOWNESS, 10, 0, true, false
            )
        )
    }

    private fun setupPerks() {
        val machetePerk = MachetePerk(plugin, context)
        context.messageBus.add(machetePerk)
        context.perks.addAll(
            listOf(
                BearTrapPerk(plugin, context),
                HeartbeatPerk(plugin, context),
                CloakingPerk(plugin),
                WerewolfSpeedPerk(plugin),
                RoarPerk(plugin, context),
                BuckshotPerk(plugin, context),
                NewMotionSensorPerk(plugin, context),
                BloodhoundPerk(plugin, context),
                JammerPerk(plugin, context),
                machetePerk,
                StaredownPerk(plugin, context),
                ShadowPerk(plugin, context),
                BloodlustPerk(plugin, context),
                FearPerk(plugin, context),
                MedicPerk(plugin, context)
            )
        )
        dump.add(object : UnloadableResource {
            override fun unload(external: Boolean) {
                context.perks.forEach { it.unload(external) }
            }
        })
    }

    private fun getBodyBlocks(source: Location) : List<Location> =
        listOf(
            source,
            source.clone().add(0.0, 1.0, 0.0),
            source.clone().add(0.0, 2.0, 0.0)
        )

    private fun getSurvivorKitPref(player: Player) : SurvivorKit? {
        val userDataMap = context.playerDataFetchFuture?.get() ?: return null
        val forestPlayerData = userDataMap.get(player.uniqueId) ?: return null
        val survivorKitName = forestPlayerData.lastSelectedSurvivorKit ?: return null
        return SurvivorKit.valueOf(survivorKitName)
    }

    private fun getHunterKitPref(player: Player) : HunterKit? {
        val userDataMap = context.playerDataFetchFuture?.get() ?: return null
        val forestPlayerData = userDataMap.get(player.uniqueId) ?: return null
        val survivorKitName = forestPlayerData.lastSelectedHunterKit ?: return null
        return HunterKit.valueOf(survivorKitName)
    }

    private fun getUserData(player: Player) : ForestUserData? {
        val userDataMap = context.playerDataFetchFuture?.get() ?: return null
        val forestPlayerData = userDataMap.get(player.uniqueId) ?: return null
        return forestPlayerData
    }

    override fun onUnload() {
        dump.destroyAll()
        kitTracker.unload()
    }
}
