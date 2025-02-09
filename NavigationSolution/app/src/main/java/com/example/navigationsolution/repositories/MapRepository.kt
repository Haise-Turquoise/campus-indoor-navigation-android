import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.TreeSet
import kotlin.math.pow
import kotlin.math.sqrt

// We're using the application context; memory leak is not possible
// Might be replaced by getting data from API later on, but context is required for accessing resources
@Suppress("StaticFieldLeak")
object MapRepository{
    const val LINE_COLOR = Color.RED

    private lateinit var applicationContext: Context

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

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
    fun getPlan(buildingId: Int, floor: Int): Bitmap {
        return buildings[buildingId]!!.plans[floor]!!.plan
    }

    // ? Returns marked floor plan for a given building ID and start/end room IDs
    // ? May return a list in case of routes spanning floors
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getMarkedPlan(buildingId: Int, srcId: Int, destId: Int): List<Bitmap> {
        // Get nodes corresponding to src and dest targets
        val sNode = buildings[buildingId]!!.plans[getFloor(srcId)]!!.nodes[srcId]
        val dNode = buildings[buildingId]!!.plans[getFloor(destId)]!!.nodes[destId]

        return drawPath(buildingId, getPath(sNode!!, dNode!!))
    }

    // ? Returns marked floor plan for a given building ID, start ID, and PoI type
    // ? May return a list in case of routes spanning floors
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getMarkedPlan(buildingId: Int, srcId: Int, destType: NodeType): List<Bitmap> {
        // Get node corresponding to src
        val sNode = buildings[buildingId]!!.plans[getFloor(srcId)]!!.nodes[srcId]

        return drawPath(buildingId, getPath(sNode!!, destType))
    }

    // ? Draws a line on floor plan(s) given the path
    private fun drawPath(buildingId: Int, path: List<MapNode>): List<Bitmap> {
        // TODO: add support for cross-floor routes

        // Load floor plan copy
        val copy = BitmapFactory.decodeResource(applicationContext.resources, 1)
            .copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(copy)
        val paint = Paint()
        paint.color = LINE_COLOR
        paint.strokeWidth = 10f

        // Draw path
        for(i in 1..path.size)
            canvas.drawLine(
                path[i - 1].x.toFloat(),
                path[i - 1].y.toFloat(),
                path[i].x.toFloat(),
                path[i].y.toFloat(),
                paint
            )

        // TODO: remove the following section; used only for testing
        val file = File(applicationContext.cacheDir, "temp.png")
        val outStream = FileOutputStream(file)
        copy.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.flush()
        outStream.close()
        // TODO: end of section

        return listOf(copy)
    }

    // ? For pathing to a specific room
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun getPath(src: MapNode, dest: MapNode): List<MapNode> {
        return getPath(src, { it.id == dest.id }, dest)
    }

    // ? For pathing to a type of PoI
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun getPath(src: MapNode, destType: NodeType): List<MapNode> {
        return getPath(src, { it.type == destType })
    }

    // ? Generates path as list given parentage map
    private fun generatePath(target: MapNode, parent: Map<MapNode, MapNode>): List<MapNode> {
        val ret = mutableListOf<MapNode>()
        var cur: MapNode? = target

        // While root (i.e. src) has not been reached
        while(cur != null) {
            ret.add(cur)
            cur = parent[cur]
        }

        // For graphical purposes, doesn't actually need to be but follows logic
        return ret.reversed()
    }

    // ? Actual pathing implementation
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun getPath(src: MapNode, isTarget: (MapNode) -> Boolean, dest: MapNode? = null): List<MapNode> {
        val parent: MutableMap<MapNode, MapNode> = HashMap()    // Track parents to generate path
        val srcDist: MutableMap<MapNode, Int> = HashMap()       // Track shortest known distance to src
        val queue: TreeSet<MapNode> = TreeSet(                  // Expand search based on distance and/or heuristic
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
        queue.add(src)
        srcDist[src] = 0

        while(queue.isNotEmpty()) {
            val cur = queue.removeFirst()!!
            val curDist = srcDist[cur]!!

            if(isTarget(cur))
                return generatePath(cur, parent)

            // Enqueue neighbors
            for (n in cur.neighbors.keys) {
                val newDist = curDist + cur.neighbors[n]!!

                // Skip if not a relaxation
                if(srcDist.getOrDefault(n, Int.MAX_VALUE) <= newDist)
                    continue

                // Remove if already exists
                if(queue.contains(n))
                    queue.remove(n)

                srcDist[n] = newDist
                parent[n] = cur
                queue.add(n)
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
