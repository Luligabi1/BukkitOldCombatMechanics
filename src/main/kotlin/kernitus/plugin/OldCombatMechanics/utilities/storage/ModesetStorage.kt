/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kernitus.plugin.OldCombatMechanics.utilities.storage

import kernitus.plugin.OldCombatMechanics.utilities.Config.modesets
import org.bukkit.World
import java.util.*

object ModesetStorage {

    private val modesetByWorld: MutableMap<UUID, Pair<String, Set<String>>> = HashMap()

    fun getModesetNameForWorld(world: World): String? {
        return getModesetNameForWorld(world.uid)
    }

    fun getModesetForWorld(world: World): Set<String>? {
        return getModesetForWorld(world.uid)
    }

    fun setModesetForWorld(world: World, modesetId: String) {
        setModesetForWorld(world.uid, modesetId)
    }

    fun removeModesetForWorld(world: World) = removeModesetForWorld(world.uid)

    fun getModesetNameForWorld(worldId: UUID): String? {
        return modesetByWorld[worldId]?.first
    }

    fun getModesetForWorld(worldId: UUID): Set<String>? {
        return modesetByWorld[worldId]?.second
    }

    fun setModesetForWorld(worldId: UUID, modesetId: String) {
        val modeset = modesets[modesetId] ?: throw IllegalStateException("Modeset $modesetId is missing from config.yml")
        modesetByWorld[worldId] = Pair(modesetId, modeset)
    }

    fun removeModesetForWorld(worldId: UUID) = modesetByWorld.remove(worldId)

}