package minecraft

import PathNode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector
import kotlin.math.pow
import kotlin.math.sqrt

data class McPathNode(val x: Int, val y: Int, val z: Int) : PathNode() {
    // TODO: Consider whether we want to include a reference to the Minecraft world or let the consumer handle dimension
    //  changes
    companion object {
        fun fromBukkitLocation(location: Location): McPathNode {
            return McPathNode(location.blockX, location.blockY, location.blockZ)
        }
    }

    operator fun plus(inc: PathNeighbor): McPathNode {
        return McPathNode(x + inc.x, y + inc.y, z + inc.z)
    }

    fun distanceTo(other: McPathNode): Double {
        val dx = other.x - x.toDouble()
        val dy = other.y - y.toDouble()
        val dz = other.z - z.toDouble()
        return sqrt(
            dx.pow(2) + dy.pow(2) + dz.pow(2)
        );
    }

    fun toBukkitVector(): Vector {
        return Vector(x, y, z)
    }

    fun toBukkitLocation(world: World): Location {
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }
}
