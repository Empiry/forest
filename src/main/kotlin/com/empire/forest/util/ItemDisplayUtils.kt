package com.empire.forest.util

import org.bukkit.entity.Player
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f

object ItemDisplayUtils {
    fun rotateToPlayerFacing(player: Player, pitchOffset: Double = 0.0): Transformation {
        return rotateToYawPitch(player.location.yaw, player.location.pitch, pitchOffset)
    }

    fun rotateToYawPitch(yaw: Float, pitch: Float, pitchOffset: Double = 0.0): Transformation {
        val yawRad = Math.toRadians(-yaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitch.toDouble() + pitchOffset).toFloat()

        val rot = Quaternionf()
            .rotateY(yawRad)
            .rotateX(pitchRad)
            .rotateY(Math.toRadians(90.0).toFloat())

        return Transformation(
            Vector3f(0f, 0f, 0f),   // translation
            rot,                     // leftRotation
            Vector3f(1f, 1f, 1f),   // scale
            Quaternionf()            // rightRotation (identity)
        )
    }
}