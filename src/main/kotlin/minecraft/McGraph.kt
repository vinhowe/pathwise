package minecraft

import Graph
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.abs
import kotlin.math.sign

class MinecraftGraph(private val world: World, private val player: Player) : Graph<McPathNode>() {
    fun search(
        start: McPathNode,
        end: McPathNode,
        maxDistance: Int,
        heuristic: McHeuristic = McHeuristic.EUCLIDEAN
    ): McPath? {
        val frontier: PriorityQueue<Pair<McPathNode, Double>> = PriorityQueue(compareBy { it.second })
        val path = McPath.empty(end)
        frontier.offer(Pair(start, 0.0))

        path.parents[start] = start
        path.costs[start] = 0.0

        var iterations = 0
        while (frontier.isNotEmpty() && iterations < 100000) {
            iterations++
            val current = frontier.poll().first

            if (current.distanceTo(start) >= maxDistance) {
                path.end = current
                break
            }

            if (current == end) {
                break
            }

            val neighbors = neighbors(current)

            neighbors.forEach {
                // TODO: Pull this logic out into updateVertex or similar
                val parent = path.parents[current]!!
                // Theta* algorithm
                val targetNode = if (lineOfSight(parent, it)) parent else current
                val newCost = path.costs[targetNode]!! + cost(targetNode, it)

                if (it !in path.costs || newCost < path.costs[it]!!) {
                    path.costs[it] = newCost
                    val priority = newCost + heuristic(it, end)
                    frontier.offer(Pair(it, priority))
                    path.parents[it] = targetNode
                }
            }
        }

        player.sendBlockChange(start.toBukkitLocation(), Material.RED_CONCRETE.createBlockData())
        var parent: McPathNode? = path.end
        while (true) {
            player.sendBlockChange(parent!!.toBukkitLocation(), Material.RED_CONCRETE.createBlockData())
            parent = path.parents[parent]

            if (parent == null || parent == start) {
                break
            }
        }

        return path
    }


    override fun neighbors(node: McPathNode): List<McPathNode> {
        val possibleNeighbors = Neighbor.values()
            .map {
                it.toTriple() to node + it
            }.toMap().toMutableMap()

        val open = mutableMapOf<Triple<Int, Int, Int>, McPathNode>()

        val headHorizontalNeighbors = Neighbor.HORIZONTALS.map {
            Pair(it.x, it.z) to node + (it + Neighbor.U)
        }.toMap()

        // Limit diagonal movement
        Neighbor.BASE_DIAGONALS.forEach { it ->
            val triple = it.toTriple()
            val diagonalNode = possibleNeighbors.remove(triple)!!
            if (node.supported && diagonalNode.passable && it.directions.all {
                    possibleNeighbors[it.toTriple()]!!.passable
                            && headHorizontalNeighbors[Pair(it.x, it.z)]!!.passable
                }) {
                open += triple to diagonalNode
            }
        }

        Neighbor.UP_DIAGONALS.forEach {
            val triple = it.toTriple()
            val diagonalNode = possibleNeighbors.remove(triple)!!
            if (node.supported && diagonalNode.passable && it.directions.slice(1..2)
                    .all { direction ->
                        possibleNeighbors[Triple(
                            direction.x,
                            it.directions[0].y,
                            direction.z
                        )]!!.passable
                                // Check that nothing is obstructing player's head
                                && headHorizontalNeighbors[Pair(direction.x, direction.z)]!!.passable
                    }
            ) {
                open += triple to diagonalNode
            }
        }

        Neighbor.DOWN_DIAGONALS.zip(Neighbor.BASE_DIAGONALS).forEach {
            val triple = it.first.toTriple()
            val directions = it.first.directions
            val diagonalNode = possibleNeighbors.remove(it.first.toTriple())!!
            if (node.supported && it.second.toTriple() in open && diagonalNode.passable && directions.slice(
                    1..2
                )
                    .all { direction ->
                        possibleNeighbors[Triple(
                            direction.x,
                            directions[0].y,
                            direction.z
                        )]!!.passable
                    }
            ) {
                open += triple to diagonalNode
            }
        }

        open += possibleNeighbors

        return open.filter { it.value.canFit && (node.supported && (it.value.supported || it.key.second == 0) || it.key == Neighbor.D.toTriple()) }.values.toList()
    }

    override fun cost(from: McPathNode, to: McPathNode): Double {
        // If you can walk instead falling, it's much better. In every scenario I could come up with, this cost is so
        // high that a path with falling won't be picked unless there is no other path.
        val fallingCoeff = if (to.supported) 1 else 100
        return 1.0 * fallingCoeff
    }

    private fun lineOfSight(from: McPathNode, to: McPathNode): Boolean {
        if (from == to) {
            return true
        }
        // Rule of thumb: Is the graph disconnected? The fact that there's line of sight from one side of a chasm to
        // another doesn't mean that there is a reasonable path between them
        //
        // Rules (short circuit if any one scanned node doesn't match this):
        // - all blocks in direct line meet criteria:
        //   - climbable OR supported
        //   - headroom (block above)

        val difference = to - from
        val isStraightDown = sign(difference.y) == -1.0 && difference.x == 0.0 && difference.z == 0.0
        if (!isStraightDown) {
            val supportedLine = linePoints(from.x, from.y - 1, from.z, to.x, to.y - 1, to.z)
            val notSupported =
                supportedLine.map { McPathNode.fromBukkitVector(it, world) }
                    .any { it.passable && !it.climbable || !it.safe }
            // Debugging
//            supportedLine.map { it.toLocation(world).block }.filter { !it.type.isAir }
//                .forEach { it.type = Material.RED_CONCRETE }
            if (notSupported) {
                return false
            }
        }

        val bottomHitResult = world.rayTraceBlocks(
            from.toBukkitLocation().add(0.5, 0.51, 0.5),
            difference.clone().normalize(),
            difference.length()
        )
        if (bottomHitResult != null) {
            // Useful for debugging hits
//            bottomHitResult.hitBlock!!.type = Material.GREEN_CONCRETE
            return false
        }

        val topHitResult = world.rayTraceBlocks(
            from.toBukkitLocation().add(0.5, 1.51, 0.5),
            difference.clone().normalize(),
            difference.length()
        )
        if (topHitResult != null) {
//            topHitResult.hitBlock!!.type = Material.BLUE_CONCRETE
            return false
        }

        return true
    }

    // https://www.geeksforgeeks.org/bresenhams-algorithm-for-3-d-line-drawing/
    fun linePoints(x0: Int, y0: Int, z0: Int, x1: Int, y1: Int, z1: Int): List<Vector> {
        // TODO: Make this more specialized to lineOfSight so that it can short circuit when finding a block that
        //  doesn't satisfy the rules

        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val dz = abs(z1 - z0)
        var x = x0
        var y = y0
        var z = z0
        val sx = sign((x1 - x0).toDouble()).toInt()
        val sy = sign((y1 - y0).toDouble()).toInt()
        val sz = sign((z1 - z0).toDouble()).toInt()

        val points = mutableListOf<Vector>()
        points += Vector(x, y, z)

        if (dx >= dy && dx >= dz) {
            // X axis drives
            var p1 = 2.0 * dy - dx
            var p2 = 2.0 * dz - dx
            while (x != x1) {
                x += sx
                if (p1 >= 0.0) {
                    y += sy
                    p1 -= 2.0 * dx
                }
                if (p2 >= 0.0) {
                    z += sz
                    p2 -= 2.0 * dx
                }
                p1 += 2 * dy
                p2 += 2 * dz
                points += Vector(x, y, z)
            }
        } else if (dy >= dx && dy >= dz) {
            // Y axis drives
            var p1 = 2.0 * dx - dy
            var p2 = 2.0 * dz - dy
            while (y != y1) {
                y += sy
                if (p1 >= 0) {
                    x += sx
                    p1 -= 2 * dy
                }
                if (p2 >= 0) {
                    z += sz
                    p2 -= 2 * dy
                }
                p1 += 2 * dx
                p2 += 2 * dz
                points += Vector(x, y, z)
            }
        } else {
            // Z axis drives
            var p1 = 2.0 * dy - dz
            var p2 = 2.0 * dx - dz
            while (z != z1) {
                z += sz
                if (p1 >= 0.0) {
                    y += sy
                    p1 -= 2.0 * dz
                }
                if (p2 >= 0.0) {
                    x += sx
                    p2 -= 2.0 * dz
                }
                p1 += 2.0 * dy
                p2 += 2.0 * dx
                points += Vector(x, y, z)
            }
        }
        return points
    }
}

enum class Direction(val x: Int, val y: Int, val z: Int) {
    U(0, 1, 0),
    D(0, -1, 0),
    E(1, 0, 0),
    W(-1, 0, 0),
    N(0, 0, -1),
    S(0, 0, 1);

    fun toTriple(): Triple<Int, Int, Int> {
        return Triple(x, y, z)
    }
}

enum class Neighbor(vararg val directions: Direction) {
    U(Direction.U),
    D(Direction.D),
    E(Direction.E),
    W(Direction.W),
    N(Direction.N),
    S(Direction.S),
    NE(Direction.N, Direction.E),
    NW(Direction.N, Direction.W),
    SE(Direction.S, Direction.E),
    SW(Direction.S, Direction.W),
    UE(Direction.U, Direction.E),
    UW(Direction.U, Direction.W),
    US(Direction.U, Direction.S),
    UN(Direction.U, Direction.N),
    UNE(Direction.U, Direction.N, Direction.E),
    UNW(Direction.U, Direction.N, Direction.W),
    USE(Direction.U, Direction.S, Direction.E),
    USW(Direction.U, Direction.S, Direction.W),
    DE(Direction.D, Direction.E),
    DW(Direction.D, Direction.W),
    DN(Direction.D, Direction.N),
    DS(Direction.D, Direction.S),
    DNE(Direction.D, Direction.N, Direction.E),
    DNW(Direction.D, Direction.N, Direction.W),
    DSE(Direction.D, Direction.S, Direction.E),
    DSW(Direction.D, Direction.S, Direction.W);

    companion object {
        val BASE_DIAGONALS = listOf(NE, NW, SE, SW)
        val UP_DIAGONALS = listOf(UNE, UNW, USE, USW)
        val DOWN_DIAGONALS = listOf(DNE, DNW, DSE, DSW)
        val HORIZONTALS = listOf(N, E, S, W)
    }

    val x: Int
        get() = this.directions.sumBy { it.x }

    val y: Int
        get() = this.directions.sumBy { it.y }

    val z: Int
        get() = this.directions.sumBy { it.z }

    operator fun plus(inc: Triple<Int, Int, Int>): Triple<Int, Int, Int> {
        return Triple(x + inc.first, y + inc.second, z + inc.third)
    }

    operator fun plus(inc: Neighbor): Triple<Int, Int, Int> {
        return plus(inc.toTriple())
    }

    fun toTriple(): Triple<Int, Int, Int> {
        return Triple(x, y, z)
    }
}