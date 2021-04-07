package minecraft

import PathNode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock
import org.bukkit.util.Vector
import kotlin.math.pow
import kotlin.math.sqrt

data class McPathNode(val world: World, val x: Int, val y: Int, val z: Int) : PathNode() {
    // TODO: Consider whether we want to include a reference to the Minecraft world or let the consumer handle dimension
    //  changes
    companion object {
        fun fromBukkitLocation(location: Location): McPathNode {
            return McPathNode(location.world!!, location.blockX, location.blockY, location.blockZ)
        }

        fun fromBukkitVector(vector: Vector, world: World): McPathNode {
            return McPathNode(world, vector.blockX, vector.blockY, vector.blockZ)
        }
    }

    private var _safe: Boolean? = null
    val safe: Boolean
        get() {
            if (_safe == null) {
                _safe = toBukkitLocation().block.type !in listOf(
                    Material.LAVA,
                    Material.MAGMA_BLOCK,
                    Material.SWEET_BERRY_BUSH,
                    Material.CACTUS
                )
            }
            return _safe!!
        }

    private var _passable: Boolean? = null
    val passable: Boolean
        get() {
            if (_passable == null) {
                _passable = toBukkitLocation().block.isPassable
            }
            return _passable!!
        }

    private var _canJumpOver: Boolean? = null
    val canJumpOver: Boolean
        get() {
            if (_canJumpOver == null) {
                val block = toBukkitLocation().block
                _canJumpOver = (block as CraftBlock).nms.getCollisionShape(
                    (block.world as CraftWorld).handle,
                    block.position
                ).boundingBox.maxY <= 1.25
            }
            return _canJumpOver!!
        }

    private var _climbable: Boolean? = null
    val climbable: Boolean
        get() {
            if (_climbable == null) {
                val location = toBukkitLocation()
                _climbable = Tag.CLIMBABLE.isTagged(location.block.type) || location.block.type == Material.WATER
            }
            return _climbable!!
        }

    private var _canFit: Boolean? = null
    val canFit: Boolean
        get() {
            if (_canFit == null) {
                val passableAndSafe = safe && (passable || climbable)
                val up = plus(Neighbor.U)
                _canFit = passableAndSafe && (up.passable || up.climbable)
            }
            return _canFit!!
        }

    private var _supported: Boolean? = null
    val supported: Boolean
        get() {
            if (_supported == null) {
                val down = plus(Neighbor.D)
                _supported = climbable || down.climbable || (!down.passable && down.safe)
            }
            return _supported!!
        }

    operator fun plus(inc: Triple<Int, Int, Int>): McPathNode {
        return McPathNode(world, x + inc.first, y + inc.second, z + inc.third)
    }

    operator fun plus(inc: Neighbor): McPathNode {
        return McPathNode(world, x + inc.x, y + inc.y, z + inc.z)
    }

    operator fun plus(inc: McPathNode): Vector {
        return Vector(x + inc.x, y + inc.y, z + inc.z)
    }

    operator fun minus(dec: McPathNode): Vector {
        return Vector(x - dec.x, y - dec.y, z - dec.z)
    }

    fun distanceTo(other: McPathNode): Double {
        val dx = other.x - x.toDouble()
        val dy = other.y - y.toDouble()
        val dz = other.z - z.toDouble()
        return sqrt(
            dx.pow(2) + dy.pow(2) + dz.pow(2)
        )
    }

    fun toBukkitVector(): Vector {
        return Vector(x, y, z)
    }

    fun toBukkitLocation(): Location {
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }

}
