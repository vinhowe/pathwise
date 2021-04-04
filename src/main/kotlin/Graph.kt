abstract class Graph<N : PathNode> {
    abstract fun neighbors(node: N): List<N>
    abstract fun cost(from: N, to: N): Double
}