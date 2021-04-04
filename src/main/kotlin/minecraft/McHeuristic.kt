package minecraft

enum class McHeuristic(val fn: (a: McPathNode, b: McPathNode) -> Double) {
    // TODO: Add other heuristics
    EUCLIDEAN({ a, b ->
        a.distanceTo(b)
    });

    operator fun invoke(a: McPathNode, b: McPathNode): Double {
        return fn(a, b)
    }
}