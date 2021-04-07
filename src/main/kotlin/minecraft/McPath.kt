package minecraft

import Path
import java.util.*

class McPath(parents: MutableMap<McPathNode, McPathNode>, costs: MutableMap<McPathNode, Double>, var end: McPathNode) :
    Path<McPathNode>(parents, costs) {
    companion object {
        fun empty(end: McPathNode): McPath {
            return McPath(mutableMapOf(), mutableMapOf(), end)
        }
    }

    fun reconstruct(node: McPathNode = end): List<McPathNode> {
        val path = Stack<McPathNode>()
        path.push(node)
        var current = node
        while (current in parents) {
            current = parents[current]!!
            path.push(current)
        }
        return path.toList()
    }
}