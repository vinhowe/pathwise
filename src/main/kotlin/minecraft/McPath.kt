package minecraft

import Path

class McPath(parents: MutableMap<McPathNode, McPathNode>, costs: MutableMap<McPathNode, Double>, var end: McPathNode) :
    Path<McPathNode>(parents, costs) {
    companion object {
        fun empty(end: McPathNode): McPath {
            return McPath(mutableMapOf(), mutableMapOf(), end)
        }
    }
}