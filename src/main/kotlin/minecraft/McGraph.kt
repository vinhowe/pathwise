package minecraft

import Graph
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*

class MinecraftGraph(private val world: World, private val player: Player) : Graph<McPathNode>() {
    fun search(start: McPathNode, end: McPathNode, heuristic: McHeuristic = McHeuristic.EUCLIDEAN): McPath? {
        val frontier: PriorityQueue<Pair<McPathNode, Double>> = PriorityQueue(compareBy { it.second })
        val path = McPath.empty()
        frontier.offer(Pair(start, 0.0))

        path.parents[start] = start
        path.costs[start] = 0.0

        var iterations = 0
        while (frontier.isNotEmpty() && iterations < 100000) {
            iterations++
            val current = frontier.poll().first

            if (current == end) {
                break
            }

            val neighbors = neighbors(current)

            neighbors.forEach {
                val newCost = path.costs[current]!! + cost(current, it)
                if (it !in path.costs || newCost < path.costs[it]!!) {
                    path.costs[it] = newCost
                    val priority = newCost + heuristic(it, end)
                    frontier.offer(Pair(it, priority))
                    path.parents[it] = current;
                }
            }
        }

        var parent: McPathNode? = end
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
        // If you can walk instead falling, it's much better
        val fallingCoeff = if ( to.supported ) 1 else 100
        return 1.0 * fallingCoeff
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