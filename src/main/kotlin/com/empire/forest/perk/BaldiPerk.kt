package com.empire.forest.perk

import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.util.applySkin
import com.empire.ignite.util.setCustomSkin
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class BaldiPerk(
    val plugin: JavaPlugin
) : ForestPerk<Unit> {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()

    override fun apply(player: Player) {
        setCustomSkin(
            player,
            "https://textures.minecraft.net/texture/3dc092cb71215a68c6ac9bc4e9e454a4410ef45e9a759c527e84557da102b02f"
        )
//        applySkin(
//            plugin, player,
//            "3dc092cb71215a68c6ac9bc4e9e454a4410ef45e9a759c527e84557da102b02f"
//        )
//        fetchAndApplySkin(plugin, player, "kennyswagger29")
    }

    override fun remove(player: Player) {
    }

    override fun unload(external: Boolean) {
    }
}