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
import java.util.LinkedList
import java.util.Objects
import java.util.Queue
import java.util.TreeSet
import kotlin.math.abs
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

        paint.strokeWidth = 2f
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
                2f,
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

        val f1 = buildings[-1]!!.plans[1]!!

        // ROOMS HAVE NUMBERS -- 1324, 1326
        // PORTS HAVE NEGATIVE 10XX
        // WC HAVE NEGATIVE 11XX
        // STAIRS HAVE NEGATIVE 12XX
        // WATER HAVE NEGATIVE 13XX
        // NONE IS ALL ELSE 1XXX

        // TODO: SERIALIZE THIS DATA SOMEHOW TO STORE IN DB

        // F1 INIT
        run {
            // COLUMN 1
            f1.nodes[-1000] = MapNode(222, 90, NodeType.PORT, -1000, HashMap(), f1)
            f1.nodes[-1200] = MapNode(222, 102, NodeType.STAIR, -1200, HashMap(), f1)
            f1.nodes[1324] = MapNode(222, 185, NodeType.ROOM, 1324, HashMap(), f1)
            f1.nodes[1326] = MapNode(222, 240, NodeType.ROOM, 1326, HashMap(), f1)
            f1.nodes[1327] = MapNode(222, 240, NodeType.ROOM, 1327, HashMap(), f1)
            f1.nodes[-1500] = MapNode(222, 316, NodeType.NONE, -1500, HashMap(), f1)
            f1.nodes[-1501] = MapNode(222, 343, NodeType.PORT, -1501, HashMap(), f1)
            addEdge(f1, -1000, -1200)
            addEdge(f1, -1200, 1324)
            addEdge(f1, 1324, 1326)
            addEdge(f1, 1326, 1327)
            addEdge(f1, 1327, -1500)
            addEdge(f1, -1500, -1501)

            // COLUMN 2
            f1.nodes[1302] = MapNode(327, 185, NodeType.ROOM, 1302, HashMap(), f1)
            f1.nodes[1327] = MapNode(327, 204, NodeType.ROOM, 1327, HashMap(), f1)
            f1.nodes[1343] = MapNode(327, 211, NodeType.ROOM, 1343, HashMap(), f1)
            f1.nodes[1339] = MapNode(327, 236, NodeType.ROOM, 1339, HashMap(), f1)
            f1.nodes[1331] = MapNode(327, 297, NodeType.ROOM, 1331, HashMap(), f1)
            f1.nodes[-1502] = MapNode(327, 316, NodeType.NONE, -1502, HashMap(), f1)
            f1.nodes[-1503] = MapNode(327, 343, NodeType.NONE, -1503, HashMap(), f1)
            f1.nodes[-1504] = MapNode(327, 373, NodeType.NONE, -1504, HashMap(), f1)
            addEdge(f1, 1302, 1327)
            addEdge(f1, 1327, 1343)
            addEdge(f1, 1343, 1339)
            addEdge(f1, 1339, 1331)
            addEdge(f1, 1331, -1502)
            addEdge(f1, -1502, -1503)
            addEdge(f1, -1503, -1504)

            // COLUMN 1 X 2
            addEdge(f1, 1324, 1302)
            addEdge(f1, -1500, -1502)
            addEdge(f1, -1501, -1503)

            // COLUMN 3
            f1.nodes[-1201] = MapNode(345, 373, NodeType.STAIR, -1201, HashMap(), f1)

            // COLUMN 2 X 3
            addEdge(f1, -1504, -1201)

            // COLUMN 4
            f1.nodes[-1505] = MapNode(447, 185, NodeType.NONE, -1505, HashMap(), f1)
            f1.nodes[-1506] = MapNode(447, 316, NodeType.NONE, -1506, HashMap(), f1)
            f1.nodes[-1002] = MapNode(447, 445, NodeType.PORT, -1002, HashMap(), f1)
            addEdge(f1, -1505, -1506)
            addEdge(f1, -1506, -1002)

            // COLUMN 3 X 4
            addEdge(f1, 1302, -1505)
            addEdge(f1, -1502, -1506)

            // COLUMN 5
            f1.nodes[-1003] = MapNode(472, 154, NodeType.PORT, -1003, HashMap(), f1)
            f1.nodes[-1507] = MapNode(472, 185, NodeType.NONE, -1507, HashMap(), f1)
            addEdge(f1, -1003, -1507)

            // COLUMN 4 X 5
            addEdge(f1, -1505, -1507)

            // COLUMN 6
            f1.nodes[-1100] = MapNode(502, 185, NodeType.WC, -1100, HashMap(), f1)
            f1.nodes[-1300] = MapNode(502, 281, NodeType.WATER, -1300, HashMap(), f1)
            f1.nodes[-1508] = MapNode(502, 316, NodeType.NONE, -1508, HashMap(), f1)
            addEdge(f1, -1100, -1300)
            addEdge(f1, -1300, -1508)

            // COLUMN 5 X 6
            addEdge(f1, -1507, -1100)
            addEdge(f1, -1506, -1508)

            // COLUMN 7
            f1.nodes[1419] = MapNode(574, 316, NodeType.ROOM, 1419, HashMap(), f1)
            f1.nodes[1416] = MapNode(574, 316, NodeType.ROOM, 1416, HashMap(), f1)
            addEdge(f1, 1419, 1416)

            // COLUMN 6 X 7
            addEdge(f1, -1508, 1419)

            // COLUMN 8
            f1.nodes[1427] = MapNode(607, 316, NodeType.ROOM, 1427, HashMap(), f1)

            // COLUMN 7 X 8
            addEdge(f1, 1416, 1427)

            // COLUMN 9
            f1.nodes[-1004] = MapNode(656, 316, NodeType.PORT, -1004, HashMap(), f1)

            // COLUMN 8 X 9
            addEdge(f1, 1427, -1004)
        }

        // F2 INIT
    }

    fun getNorthHeading(buildingId: Int): Int {
        return buildings[buildingId]!!.northDeg
    }

    // ? Returns marked floor plan for a given building ID and start/end room IDs
    // ? May return a list in case of routes spanning floors
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getMarkedPlan(buildingId: Int, srcId: Int, destId: Int): List<Bitmap> {
        if(srcId == destId)
            return drawPath(buildingId, listOf())

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

        if(path.isEmpty())
            return listOf(copy);

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
        val queue: TreeSet<MapNode> = TreeSet(                  // Expand search based on distance
            compareBy { n -> srcDist.getOrDefault(n, .0) }
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
