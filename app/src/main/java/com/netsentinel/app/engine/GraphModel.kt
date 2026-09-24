package com.netsentinel.app.engine

data class NetworkNode(
    val id: String,
    val label: String,
    val isGateway: Boolean = false,
    val isThreatNode: Boolean = false
)

data class NetworkEdge(
    val sourceId: String,
    val targetId: String,
    val latencyMs: Long
)

/**
 * Access point topology and client connection graph representation.
 */
class GraphModel {
    private val nodes = mutableListOf<NetworkNode>()
    private val edges = mutableListOf<NetworkEdge>()

    fun addNode(node: NetworkNode) = nodes.add(node)
    fun addEdge(edge: NetworkEdge) = edges.add(edge)

    fun getNodes(): List<NetworkNode> = nodes.toList()
    fun getEdges(): List<NetworkEdge> = edges.toList()
}
