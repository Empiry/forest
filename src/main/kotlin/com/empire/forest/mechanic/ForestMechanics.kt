package com.empire.forest.mechanic

import com.empire.forest.ForestContext
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.ManagedResourceDump
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class ForestMechanics(
    private val plugin: JavaPlugin,
    private val context: ForestContext,
    private val dump: ManagedResourceDump
) : IgniteResource {
    private lateinit var bearTrapListener: BearTrapListener

    override fun load() {
        bearTrapListener = BearTrapListener(plugin, context, dump)
        bearTrapListener.load()
    }

    fun giveBearTrapItem(player: Player, amount: Int) {
        val itemBuilderFn = bearTrapListener.bearTrapItemFn
        val itemBuilder = itemBuilderFn(player)
        itemBuilder.amount(amount)
        val shears = itemBuilder.build()
        player.inventory.addItem(shears)
    }

    fun getBearTrapItem(player: Player) : ItemStack {
        val itemBuilderFn = bearTrapListener.bearTrapItemFn
        val itemBuilder = itemBuilderFn(player)
        val shears = itemBuilder.build()
        return shears
    }

    override fun unload(external: Boolean) {
        bearTrapListener.unload()
    }
}