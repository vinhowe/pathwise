package minecraft

import Graph
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.util.*

class MinecraftGraph(val world: World, val player: Player) : Graph<McPathNode>() {
    fun search(start: McPathNode, end: McPathNode, heuristic: McHeuristic = McHeuristic.EUCLIDEAN): McPath? {
        val frontier: PriorityQueue<Pair<McPathNode, Double>> = PriorityQueue(compareBy { it.second })
        val path = McPath.empty()
        frontier.offer(Pair(start, 0.0))

        path.parents[start] = start
        path.costs[start] = 0.0

        while (frontier.isNotEmpty()) {
            val current = frontier.poll().first
            player.sendBlockChange(current.toBukkitLocation(world), Material.WHITE_CONCRETE.createBlockData())

            if (current == end) {
                break
            }

            val neighbors = neighbors(current);

            neighbors.forEach {
                val newCost = path.costs[current]!! + cost(current, it)
                if (it !in path.costs || newCost < path.costs[it]!!) {
                    path.costs[it] = newCost;
                    val priority = newCost + heuristic(it, end);
                    frontier.offer(Pair(it, priority))
                    path.parents[it] = current;
                }
            }
        }

        var parent: McPathNode? = end
        while (true) {
            player.sendBlockChange(parent!!.toBukkitLocation(world), Material.RED_CONCRETE.createBlockData())
            parent = path.parents[parent];

            if (parent == null || parent == start) {
                break
            }
        }

        return path
    }

    override fun neighbors(node: McPathNode): List<McPathNode> {
        // TODO: Make this context-dependent on whether the player can fly
        return PathNeighbor.values().map { (node + it).toBukkitLocation(world) }
            .filter {
                it.block.isPassable
                        && it.clone().add(0.0, 1.0, 0.0).block.isPassable
                        && !it.clone().add(1.0, -1.0, 0.0).block.isPassable
            }
//            .filter { it.block.isPassable }
            .map { McPathNode.fromBukkitLocation(it) }
    }

    override fun cost(from: McPathNode, to: McPathNode): Double {
        // TODO: Come up with some common sense rules for this
        return 1.0
    }
}

enum class PathNeighbor(val x: Int, val y: Int, val z: Int) {
    UP(0, 1, 0),
    DOWN(0, -1, 0),
    EAST(1, 0, 0),
    WEST(-1, 0, 0),
    SOUTH(0, 0, 1),
    NORTH(0, 0, -1),
    NORTHEAST(1, 0, -1),
    NORTHWEST(-1, 0, -1),
    SOUTHEAST(1, 0, 1),
    SOUTHWEST(-1, 0, 1),
    UPEAST(1, 1, -1),
    UPWEST(-1, 1, -1),
    UPSOUTH(0, 1, 1),
    UPNORTH(0, 1, -1),
    UPNORTHEAST(1, 1, -1),
    UPNORTHWEST(-1, 1, -1),
    UPSOUTHEAST(1, 1, 1),
    UPSOUTHWEST(-1, 1, 1),
    DOWNEAST(1, -1, -1),
    DOWNWEST(-1, -1, -1),
    DOWNSOUTH(0, -1, 1),
    DOWNNORTH(0, -1, -1),
    DOWNNORTHEAST(1, -1, -1),
    DOWNNORTHWEST(-1, -1, -1),
    DOWNSOUTHEAST(1, -1, 1),
    DOWNSOUTHWEST(-1, -1, 1),
}