package com.empire.forest.kit

import com.empire.forest.ForestContext
import com.empire.ignite.game.kit.GameKit
import com.empire.ignite.game.kit.IgniteKit
import com.empire.ignite.game.kit.KitProvider

object ForestKitProvider : KitProvider<ForestKitKey, ForestContext> {
    override fun getKitByID(id: ForestKitKey): IgniteKit<ForestContext> {
        return id.gameKit
    }
}

class ForestKitKey {
    val gameKit: GameKit<ForestContext>
    constructor(kit: SurvivorKit) {
        this.gameKit = kit.gameKit
    }
    constructor(kit: HunterKit) {
        this.gameKit = kit.gameKit
    }
}