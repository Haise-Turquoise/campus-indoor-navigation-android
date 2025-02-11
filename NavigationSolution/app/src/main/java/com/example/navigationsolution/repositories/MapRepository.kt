import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.navigationsolution.R
import java.io.File
import java.io.FileOutputStream
import java.util.Objects
import java.util.TreeSet
import kotlin.math.pow
import kotlin.math.sqrt

// We're using the application context; memory leak is not possible
// Might be replaced by getting data from API later on, but context is required for accessing resources
@Suppress("StaticFieldLeak")
object MapRepository{
    private const val LINE_COLOR = Color.RED
    private const val NODE_COLOR = Color.GREEN

    private lateinit var applicationContext: Context

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun initialize(applicationContext: Context) {
        Log.i("i", "INITIALIZED")
        this.applicationContext = applicationContext

        drawGraph()

        val file = File(applicationContext.cacheDir, "temp_path.png")
        val outStream = FileOutputStream(file)
        getMarkedPlan(-1, 1326, 1427)[0].compress(Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.flush()
        outStream.close()
    }

    // ? Types of PoIs
    enum class NodeType { ROOM, PORT, STAIR, WC, WATER, CAFE, NONE }

    // ? Represents a location, both indoors and outdoors
    private data class MapNode (
        val x: Int,                                 // x-coordinate of node on floor plan image
        val y: Int,                                 // y-coordinate of node on floor plan image
        val type: NodeType,                         // Type of node
        val id: Int,                                // Node ID, room # if indoors, otherwise -1
        val adj: MutableMap<MapNode, Double>,       // Neighboring nodes
        val floor: FloorMap?                        // FloorMap object the node belongs to; null if outdoors
    ) {
        // Avoids circular references when hashing
        // ! DO NOT REMOVE
        override fun hashCode(): Int {
            return Objects.hash(x, y, type, id)
        }

        override fun equals(other: Any?): Boolean {
            return this === other
        }
    }

    // ? ALl relevant data about a floor
    private data class FloorMap (
        val level: Int,                             // Which floor the map corresponds to
        val nodes: MutableMap<Int, MapNode>,        // Maps IDs to nodes within floor
        val plan: Int                               // Resource ID of raster floor plan
    )

    // ? All relevant data about a building
    private data class BuildingMap (
        val id: Int,                               // Building ID
        val ports: MutableMap<MapNode, MapNode>,   // Maps outdoor nodes to corresponding indoor nodes
        val plans: MutableMap<Int, FloorMap>,      // Maps floor numbers to floors
        val northDeg: Int = 0                      // Orientation of the North direction, 0 is straight up (i.e. negative-y)
    )

    private val buildings: MutableMap<Int, BuildingMap> = HashMap()    // Maps building IDs to building objects
    private val nodes: Map<Int, MapNode> = HashMap()                   // All outdoor nodes and port nodes, currently unused

    // TODO: Delete this function, it's used only for concise generation of edges
    private fun addEdge(f: FloorMap, r1: Int, r2: Int) {
        val n1 = f.nodes[r1]!!
        val n2 = f.nodes[r2]!!

        val dist = sqrt((n1.x - n2.x).toDouble().pow(2.0) + (n1.y - n2.y).toDouble().pow(2.0))

        n1.adj[n2] = dist;
        n2.adj[n1] = dist;
    }

    // TODO: Delete this function, it's used only for verification of graph models
    private fun drawGraph() {
        // Load floor plan copy
        val img = (applicationContext.resources.getDrawable(R.drawable.e7f1, null) as BitmapDrawable).bitmap
        val copy = img.copy(img.config ?: Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(copy)
        val paint = Paint()

        paint.strokeWidth = 10f
        paint.color = LINE_COLOR

        for(n in buildings[-1]!!.plans[1]!!.nodes.values)
            for(nn in n.adj.keys)
                canvas.drawLine(
                    n.x.toFloat(),
                    n.y.toFloat(),
                    nn.x.toFloat(),
                    nn.y.toFloat(),
                    paint
                )

        paint.color = NODE_COLOR

        for(n in buildings[-1]!!.plans[1]!!.nodes.values) {
            if(n.type == NodeType.NONE)
                paint.color = LINE_COLOR

            canvas.drawCircle(
                n.x.toFloat(),
                n.y.toFloat(),
                15f,
                paint
            )

            paint.color = NODE_COLOR
        }

        val file = File(applicationContext.cacheDir, "temp_full_graph.png")
        val outStream = FileOutputStream(file)
        copy.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.flush()
        outStream.close()
    }

    init {
        // TODO: Add logic for retrieving and/or loading serialized map data
        // ? Will hardcode points for now

        buildings[-1] = BuildingMap(
            -1,
            HashMap(),
            HashMap(),
            305
        )

        buildings[-1]!!.plans[1] = FloorMap(
            1,
            HashMap(),
            R.drawable.e7f1
        )

        val f = buildings[-1]!!.plans[1]!!

        f.nodes[1331] = MapNode(1114,   1016,   NodeType.ROOM, 1331,    HashMap(), f)
        f.nodes[1339] = MapNode(1114,   813,    NodeType.ROOM, 1339,    HashMap(), f)
        f.nodes[1327] = MapNode(1114,   694,    NodeType.ROOM, 1327,    HashMap(), f)
        f.nodes[1343] = MapNode(1114,   710,    NodeType.ROOM, 1343,    HashMap(), f)
        f.nodes[1302] = MapNode(1114,   638,    NodeType.ROOM, 1302,    HashMap(), f)
        f.nodes[1324] = MapNode(750,    638,    NodeType.ROOM, 1324,    HashMap(), f)
        f.nodes[1326] = MapNode(750,    820,    NodeType.ROOM, 1326,    HashMap(), f)
        f.nodes[-1] =   MapNode(750,    1085,   NodeType.NONE, -1,      HashMap(), f)
        f.nodes[-2] =   MapNode(1114,   1085,   NodeType.NONE, -2,      HashMap(), f)
        f.nodes[-3] =   MapNode(1520,   1085,   NodeType.NONE, -3,      HashMap(), f)
        f.nodes[-4] =   MapNode(1520,   638,    NodeType.NONE, -4,      HashMap(), f)

        f.nodes[1419] = MapNode(1960,   1085,   NodeType.ROOM,  1419,   HashMap(), f)
        f.nodes[1416] = MapNode(1960,   1085,   NodeType.ROOM,  1416,   HashMap(), f)
        f.nodes[1427] = MapNode(2060,   1085,   NodeType.ROOM,  1427,   HashMap(), f)

        addEdge(f, 1331, 1339)
        addEdge(f, 1339, 1327)
        addEdge(f, 1327, 1343)
        addEdge(f, 1343, 1302)
        addEdge(f, 1302, 1324)
        addEdge(f, 1324, 1326)
        addEdge(f, 1326, -1)
        addEdge(f, -1, -2)
        addEdge(f, -2, -3)
        addEdge(f, -3, -4)

        addEdge(f, 1419, 1416)
        addEdge(f, 1416, 1427)

        addEdge(f, 1331, -2)
        addEdge(f, 1302, -4)
        addEdge(f, 1419, -3)
    }

    fun getNorthHeading(buildingId: Int): Int {
        return buildings[buildingId]!!.northDeg
    }

    // ? Returns unmarked floor plan for a given building ID and floor number
    fun getPlan(buildingId: Int, floor: Int): Bitmap {
        return BitmapFactory.decodeResource(applicationContext.resources, buildings[buildingId]!!.plans[floor]!!.plan)
    }

    // ? Returns marked floor plan for a given building ID and start/end room IDs
    // ? May return a list in case of routes spanning floors
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getMarkedPlan(buildingId: Int, srcId: Int, destId: Int): List<Bitmap> {
        // Get nodes corresponding to src and dest targets
        val sNode = buildings[buildingId]!!.plans[getFloor(srcId)]!!.nodes[srcId]!!
        val dNode = buildings[buildingId]!!.plans[getFloor(destId)]!!.nodes[destId]!!

        return drawPath(buildingId, getPath(sNode, dNode))
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
        val img = (applicationContext.resources.getDrawable(R.drawable.e7f1, null) as BitmapDrawable).bitmap
        val copy = img.copy(img.config ?: Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(copy)
        val paint = Paint()
        paint.color = LINE_COLOR
        paint.strokeWidth = 10f

        // Draw path
        for(i in 1..<path.size)
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
        return getPath(src, { n -> n.id == dest.id }, dest)
    }

    // ? For pathing to a type of PoI
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun getPath(src: MapNode, destType: NodeType): List<MapNode> {
        return getPath(src, { n -> n.type == destType })
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
        val srcDist: MutableMap<MapNode, Double> = HashMap()    // Track shortest known distance to src
        val queue: TreeSet<MapNode> = TreeSet(                  // Expand search based on distance and/or heuristic
            compareBy { n ->
                // Estimated distance heuristic, used for A* but defaults to 0 if Djikstra's
                val estDist = dest?.let {
                    sqrt(
                        (dest.x - n.x).toDouble().pow(2.0) + (dest.y - n.y).toDouble().pow(2.0)
                    )
                } ?: 0.0

                srcDist.getOrDefault(n, .0) + estDist
            }
        )

        // Search root
        queue.add(src)
        srcDist[src] = .0

        while(queue.isNotEmpty()) {
            val cur = queue.removeFirst()!!
            val curDist = srcDist[cur]!!

            if(isTarget(cur))
                return generatePath(cur, parent)

            // Enqueue neighbors
            for (n in cur.adj.keys) {
                val newDist = curDist + cur.adj[n]!!

                // Skip if not a relaxation
                if(srcDist.getOrDefault(n, Double.MAX_VALUE) <= newDist)
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
        return roomId / 1000;
    }
}
