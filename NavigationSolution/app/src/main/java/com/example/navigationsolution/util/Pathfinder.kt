import android.graphics.Bitmap

public object CampusMap {
    private enum class NodeType { ROOM, PORT, STAIR, WC, WATER, CAFE, NONE }

    // ? Represents a location, both indoors and outdoors
    private data class MapNode (
        const val x: Int,                   // x-coordinate of node on floor plan image
        const val y: Int,                   // y-coordinate of node on floor plan image
        const val type: NodeType,           // Type of node
        const val id: Int,                  // Node ID, room # if indoors, otherwise -1
        val neighbors: Map<MapNode, Int>    // Neighboring nodes
    )

    // ? ALl relevant data about a floor
    private data class FloorMap (
        const val level: Int,               // Which floor the map corresponds to
        val nodes: Map<Int, MapNode>,       // Maps IDs to nodes within floor
        const val plan: Bitmap              // Raster floor plan
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

    // Returns unmarked floor plan for a given building ID and floor number
    fun getPlan(buildingId: Int, floor: Int): Bitmap {
        return buildings[buildingId].plans[floor]
    }

    // Returns marked floor plan for a given building ID and start/end room IDs
    // May return a list in case of routes spanning floors
    fun getMarkedPlan(buildingId: Int, srcId: Int, destId: Int): ListOf<Bitmap> {
        // Get nodes corresponding to src and dest targets
        const val sNode = buildings[buildingId].plans[getFloor(srcId)].nodes[srcId]
        const val dNode = buildings[buildingId].plans[getFloor(destId)].nodes[destId]

        // TODO: Pathfind and draw
    }

    private fun getFloor(roomId: Int): Int {
        // ! REPLACE THIS IF NOT ALL BUILDINGS FOLLOW THIS PATTERN
        return roomId / 100;
    }
}
