import android.graphics.Bitmap
import java.util.PriorityQueue
import kotlin.math.pow
import kotlin.math.sqrt

public object CampusMap {
    // ? Types of PoIs
    enum class NodeType { ROOM, PORT, STAIR, WC, WATER, CAFE, NONE }

    // ? Represents a location, both indoors and outdoors
    private data class MapNode (
        val x: Int,                         // x-coordinate of node on floor plan image
        val y: Int,                         // y-coordinate of node on floor plan image
        val type: NodeType,                 // Type of node
        val id: Int,                        // Node ID, room # if indoors, otherwise -1
        val neighbors: Map<MapNode, Int>,   // Neighboring nodes
        val floor: FloorMap?                // FloorMap object the node belongs to; null if outdoors
    )

    // ? ALl relevant data about a floor
    private data class FloorMap (
        val level: Int,                     // Which floor the map corresponds to
        val nodes: Map<Int, MapNode>,       // Maps IDs to nodes within floor
        val plan: Bitmap                    // Raster floor plan
    )

    // ? All relevant data about a building
    private data class BuildingMap (
        val id: Int,                        // Building ID
        val ports: Map<MapNode, MapNode>,   // Maps outdoor nodes to corresponding indoor nodes
        val plans: Map<Int, FloorMap>       // Maps floor numbers to floors
    )

    private val buildings: Map<Int, BuildingMap> = HashMap()    // Maps building IDs to building objects
    private val nodes: Map<Int, MapNode> = HashMap()            // All outdoor nodes and port nodes, currently unused

    init {
        // TODO: Add logic for retrieving and/or loading serialized map data
        // ? Will hardcode points for now
    }

    // ? Returns unmarked floor plan for a given building ID and floor number
    public fun getPlan(buildingId: Int, floor: Int): Bitmap {
        return buildings[buildingId]!!.plans[floor]!!.plan
    }

    // ? Returns marked floor plan for a given building ID and start/end room IDs
    // ? May return a list in case of routes spanning floors
    public fun getMarkedPlan(buildingId: Int, srcId: Int, destId: Int): List<Bitmap> {
        // Get nodes corresponding to src and dest targets
        val sNode = buildings[buildingId]!!.plans[getFloor(srcId)]!!.nodes[srcId]
        val dNode = buildings[buildingId]!!.plans[getFloor(destId)]!!.nodes[destId]

        return drawPath(buildingId, getPath(sNode!!, dNode!!))
    }

    // ? Returns marked floor plan for a given building ID, start ID, and PoI type
    // ? May return a list in case of routes spanning floors
    public fun getMarkedPlan(buildingId: Int, srcId: Int, destType: NodeType): List<Bitmap> {
        // Get node corresponding to src
        val sNode = buildings[buildingId]!!.plans[getFloor(srcId)]!!.nodes[srcId]

        return drawPath(buildingId, getPath(sNode!!, destType))
    }

    // ? Draws a line on floor plan(s) given the path
    private fun drawPath(buildingId: Int, path: List<MapNode>): List<Bitmap> {
        // TODO: do the thing.
        return listOf()
    }

    // ? For pathing to a specific room
    private fun getPath(src: MapNode, dest: MapNode): List<MapNode> {
        return getPath(src, { it.id == dest.id }, dest)
    }

    // ? For pathing to a type of PoI
    private fun getPath(src: MapNode, destType: NodeType): List<MapNode> {
        return getPath(src, { it.type == destType })
    }

    // ? Generates path as list given parentage map
    private fun generatePath(target: MapNode, parent: Map<MapNode, MapNode>): List<MapNode> {
        val ret = mutableListOf<MapNode>()
        var cur: MapNode? = target

        while(cur != null) {
            ret.add(cur)
            cur = parent[cur]
        }

        // For graphical purposes, doesn't actually need to be but follows logic
        return ret.reversed()
    }

    // ? Actual pathing implementation
    private fun getPath(src: MapNode, isTarget: (MapNode) -> Boolean, dest: MapNode? = null): List<MapNode> {
        val parent: MutableMap<MapNode, MapNode> = HashMap()    // Track parents to generate path
        val srcDist: MutableMap<MapNode, Int> = HashMap()       // Track shortest known distance to src
        val queue: PriorityQueue<MapNode> = PriorityQueue(      // Expand search based on distance and/or heuristic
            compareBy { n ->
                // Estimated distance heuristic, used for A* but defaults to 0 if Djikstra's
                val estDist = dest?.let {
                    sqrt(
                        (dest.x - n.x).toDouble().pow(2.0) + (dest.y - n.y).toDouble().pow(2.0)
                    )
                } ?: 0.0

                srcDist[n]!! + estDist
            }
        )

        // Search root
        queue.offer(src)
        srcDist[src] = 0

        while(queue.isNotEmpty()) {
            val cur = queue.poll()!!
            val curDist = srcDist[cur]!!

            if(isTarget(cur))
                return generatePath(cur, parent)

            // Enqueue neighbors
            for (n in cur.neighbors.keys) {
                val newDist = curDist + cur.neighbors[n]!!

                // Skip if not a relaxation
                if(srcDist.getOrDefault(n, Int.MAX_VALUE) <= newDist)
                    continue

                srcDist[n] = newDist
                parent[n] = cur
                queue.offer(n)
            }
        }

        // ! Error state; should never occur
        return mutableListOf()
    }

    // ? Returns the floor number given a room ID
    private fun getFloor(roomId: Int): Int {
        // ! REPLACE THIS IF NOT ALL BUILDINGS FOLLOW THIS PATTERN
        return roomId / 100;
    }
}
