import android.graphics.Bitmap

public object CampusMap {
    // ? Types of PoIs
    private enum class NodeType { ROOM, PORT, STAIR, WC, WATER, CAFE, NONE }

    // ? Represents a location, both indoors and outdoors
    private data class MapNode (
        val x: Int,                         // x-coordinate of node on floor plan image
        val y: Int,                         // y-coordinate of node on floor plan image
        val type: NodeType,                 // Type of node
        val id: Int,                        // Node ID, room # if indoors, otherwise -1
        val neighbors: Map<MapNode, Int>    // Neighboring nodes
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
        val ports: Set<MapNode, MapNode>,   // Maps outdoor nodes to corresponding indoor nodes
        val plans: Map<Int, FloorMap>       // Maps floor numbers to floors
    )

    val buildings: Map<Int, BuildingMap>    // Maps building IDs to building objects
    val nodes: Map<Int, MapNode>            // All outdoor nodes and port nodes, currently unused

    init {
        // TODO: Add logic for retrieving and/or loading serialized map data
        // ? Will hardcode points for now
    }

    // ? Returns unmarked floor plan for a given building ID and floor number
    public fun getPlan(buildingId: Int, floor: Int): Bitmap {
        return buildings[buildingId].plans[floor]
    }

    // ? Returns marked floor plan for a given building ID and start/end room IDs
    // ? May return a list in case of routes spanning floors
    public fun getMarkedPlan(buildingId: Int, srcId: Int, destId: Int): List<Bitmap> {
        // Get nodes corresponding to src and dest targets
        val sNode = buildings[buildingId].plans[getFloor(srcId)].nodes[srcId]
        val dNode = buildings[buildingId].plans[getFloor(destId)].nodes[destId]

        return drawPath(buildingId, getPath(sNode, dNode))
    }

    // ? Returns marked floor plan for a given building ID, start ID, and PoI type
    // ? May return a list in case of routes spanning floors
    public fun getMarkedPlan(buildingId: Int, srcId: Int, destType: NodeType): List<BitMap> {
        // Get node corresponding to src
        val sNode = buildings[buildingId].plans[getFloor(srcId)].nodes[srcId]

        return drawPath(buildingId, getPath(sNode, destType))
    }

    // ? Draws a line on floor plan(s) given the path
    private fun drawPath(buildingId: Int, path: List<MapNode>): List<BitMap> {
        // TODO: do the thing.
    }

    // ? For pathing to a specific room
    private fun getPath(src: MapNode, dest: MapNode): List<MapNode> {
        return getPath(src, { n -> n.id == dest.id })
    }

    // ? For pathing to a type of PoI
    private fun getPath(src: MapNode, destType: NodeType): List<MapNode> {
        return getPath(src, { n -> n.type == destType })
    }

    // ? Actual pathing implementation
    private fun getPath(src: MapNode, isTarget: (MapNode) -> Boolean): List<MapNode> {
        // TODO: implement Djikstra's or A* if ROOM->ROOM or ROOM->OTHER, respectively
    }

    // ? Returns the floor number given a room ID
    private fun getFloor(roomId: Int): Int {
        // ! REPLACE THIS IF NOT ALL BUILDINGS FOLLOW THIS PATTERN
        return roomId / 100;
    }
}
