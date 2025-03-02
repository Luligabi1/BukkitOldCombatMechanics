/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kernitus.plugin.OldCombatMechanics.utilities.storage

import kernitus.plugin.OldCombatMechanics.OCMMain
import kernitus.plugin.OldCombatMechanics.module.OCMModule
import kernitus.plugin.OldCombatMechanics.utilities.Config
import kernitus.plugin.OldCombatMechanics.utilities.Messenger
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import java.util.*

/**
 * Listens to players changing world / spawning etc.
 * and updates modeset accordingly
 */
class ModesetListener(plugin: OCMMain) : OCMModule(plugin, "modeset-listener") {

    override fun isEnabled() = true

    override fun isEnabled(world: World) = true

    @EventHandler(ignoreCancelled = false)
    fun onWorldUnload(event: WorldUnloadEvent) {
        ModesetStorage.removeModesetForWorld(event.world)
    }

}