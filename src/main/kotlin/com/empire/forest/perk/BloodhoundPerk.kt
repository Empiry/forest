package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.mechanic.CooldownRightClickAbility
import com.empire.forest.mechanic.CooldownRightClickAbilityOptions
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.entity.EntityModelBuilder
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.scoreboard.ScoreboardAllocator
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.entity.Wolf
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class BloodhoundPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit> {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()
    companion object {
        private val WOLF_EMB : EntityModelBuilder<Wolf> = EntityModelBuilder(Wolf::class.java) {
            onSpawn {
                isAngry = true
                getAttribute(Attribute.MOVEMENT_SPEED)?.let {
                    it.baseValue = 0.6
                }
                health = 1.0
            }
        }
    }

    private val itemBuilder = ItemBuilder(Material.BONE) {
        itemModel(NamespacedKey("horror", "ability/bloodhound"))
        name(Component.text("Bloodhound Ability").color(NamedTextColor.RED))
        unbreakable()
    }
    val cooldownAbility : CooldownRightClickAbility
    private val item : ItemStack
    private val dump = GlobalResourceTrackers.createResourceDump()

    init {
        cooldownAbility = CooldownRightClickAbility(
            plugin, 20L * 75L,
            CooldownRightClickAbilityOptions(consume = false), itemBuilder,
            this::use
        )
        item = itemBuilder.build()
        createFakeRedTeam()
    }

    override fun apply(player: Player) {
        cooldownAbility.add(player)
        player.give(item)
    }

    private fun use(player: Player) {
        spawnWolvesForTargets(
            player.location, context.survivorTeam.players.shuffled().take(2)
        )
    }

    private fun spawnWolvesForTargets(
        sourceLocation: Location, players: List<Player>
    ) {
        val wolves = players.map {
            val wolf = WOLF_EMB.spawn(sourceLocation)
            ScoreboardAllocator.addEntityToTeam("wolf_reddish", wolf.uniqueId)
            wolf.target = it
            wolf.addPotionEffect(
                PotionEffect(
                    PotionEffectType.GLOWING, 20 * 50, 0,
                    true, false
                )
            )
            wolf
        }
        dump.addSelfTracking(GlobalResourceTrackers.scheduler.afterUntracked(20L * 20L) {
            wolves.forEach { it.remove() }
        })
    }

    override fun remove(player: Player) {
        cooldownAbility.remove(player)
    }

    override fun unload(external: Boolean) {
        cooldownAbility.unload(external)
        dump.destroyAll()
    }

    private fun createFakeRedTeam() {
        val sbTeam = ScoreboardAllocator.registerMainScoreboardTeam("wolf_reddish")
        sbTeam.color(NamedTextColor.RED)
        ScoreboardAllocator.propagateUpdatedTeamProperties(sbTeam)
    }
}