package net.tjalp.peach.apple.green.registry

import net.minestom.server.MinecraftServer
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType

val OVERWORLD: DimensionType = DimensionType.builder(NamespaceID.from("tjalp:overworld"))
    .ultrawarm(false)
    .natural(true)
    .piglinSafe(false)
    .respawnAnchorSafe(false)
    .bedSafe(true)
    .raidCapable(true)
    .skylightEnabled(true) // Ambient light otherwise there's lagspikes when a new chunk loads
    .ceilingEnabled(false)
    .fixedTime(null)
    .ambientLight(0.0f)
    .height(256)
    .logicalHeight(256)
    .infiniburn(NamespaceID.from("minecraft:infiniburn_overworld"))
    .build()

fun registerDimensions() {
    val man = MinecraftServer.getDimensionTypeManager()
    man.removeDimension(DimensionType.OVERWORLD)
    man.addDimension(OVERWORLD)
}