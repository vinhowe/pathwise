package minecraft

import Path

class McPath(parents: MutableMap<McPathNode, McPathNode>, costs: MutableMap<McPathNode, Double>) :
    Path<McPathNode>(parents, costs) {
    companion object {
        fun empty(): McPath {
            return McPath(mutableMapOf(), mutableMapOf())
        }
    }
}