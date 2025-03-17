import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
    private const val COLOR_LINE = Color.CYAN
    private const val COLOR_START = COLOR_LINE
    private const val COLOR_END = Color.RED
    private const val COLOR_STAIRS_UP = Color.MAGENTA
    private const val COLOR_STAIRS_DOWN = Color.MAGENTA

    private const val RADIUS_NODE = 3f
    private const val WIDTH_PATH = 2f

    private lateinit var applicationContext: Context

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun initialize(applicationContext: Context) {
        this.applicationContext = applicationContext

        drawGraph()

//        val file = File(applicationContext.cacheDir, "temp_path.png")
//        val outStream = FileOutputStream(file)
//        getMarkedPlan(-1, 1326, 1427)[2]!!.compress(Bitmap.CompressFormat.PNG, 100, outStream)
//        outStream.flush()
//        outStream.close()
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
        val nodes: MutableMap<Double, MapNode>,     // Maps IDs to nodes within floor; fractional for multiple entrances
        val plan: Int,                              // Resource ID of raster floor plan
        val building: BuildingMap                   // BuildingMap the floor belongs to
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
        addEdge(f, r1.toDouble(), r2.toDouble())
    }

    private fun addEdge(f: FloorMap, r1: Int, r2: Double) {
        addEdge(f, r1.toDouble(), r2)
    }

    private fun addEdge(f: FloorMap, r1: Double, r2: Int) {
        addEdge(f, r1, r2.toDouble())
    }

    private fun addEdge(f: FloorMap, r1: Double, r2: Double) {
        val n1 = f.nodes[r1]!!
        val n2 = f.nodes[r2]!!

        val dist = sqrt((n1.x - n2.x).toDouble().pow(2.0) + (n1.y - n2.y).toDouble().pow(2.0))

        n1.adj[n2] = dist;
        n2.adj[n1] = dist;
    }

    private fun addEdge(buildingId: Int, r1: Int, r2: Int) {
        addEdge(buildingId, r1.toDouble(), r2.toDouble())
    }

    private fun addEdge(buildingId: Int, r1: Double, r2: Double) {
        val n1 = buildings[buildingId]!!.plans[getFloor(r1)]!!.nodes[r1]!!
        val n2 = buildings[buildingId]!!.plans[getFloor(r2)]!!.nodes[r2]!!

        if(getFloor(r1) == getFloor(r2)) {
            Log.w("MapRepository", "Attempted to add invalid stair edge $r1 -- $r2")
            return
        }

        n1.adj[n2] = 50.0;
        n2.adj[n1] = 50.0;
    }

    // TODO: Delete this function, it's used only for verification of graph models
    private fun drawSearchPath(map: Map<MapNode, MapNode>) {
        for(f in buildings[1]!!.plans.values) {
            // Load floor plan copy
            val img = (applicationContext.resources.getDrawable(
                f.plan,
                null
            ) as BitmapDrawable).bitmap
            val copy = img.copy(img.config ?: Bitmap.Config.ARGB_8888, true)

            val canvas = Canvas(copy)
            val paint = Paint()

            paint.strokeWidth = WIDTH_PATH
            paint.color = COLOR_LINE

            for (e in map)
                if(getFloor(e.key.id) == getFloor(e.value.id) && getFloor(e.key.id) == f.level)
                    canvas.drawLine(
                        e.key.x.toFloat(),
                        e.key.y.toFloat(),
                        e.value.x.toFloat(),
                        e.value.y.toFloat(),
                        paint
                    )
                else if(getFloor(e.key.id) == f.level)
                    canvas.drawCircle(
                        e.key.x.toFloat(),
                        e.key.y.toFloat(),
                        RADIUS_NODE,
                        paint
                    )
                else if(getFloor(e.value.id) == f.level)
                    canvas.drawCircle(
                        e.value.x.toFloat(),
                        e.value.y.toFloat(),
                        RADIUS_NODE,
                        paint
                    )

            val file = File(applicationContext.cacheDir, "temp_path_${f.level}.png")
            val outStream = FileOutputStream(file)
            copy.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
            outStream.close()
        }
    }

    private fun drawGraph() {
        for(f in buildings[1]!!.plans.values) {
            // Load floor plan copy
            val img = (applicationContext.resources.getDrawable(
                f.plan,
                null
            ) as BitmapDrawable).bitmap
            val copy = img.copy(img.config ?: Bitmap.Config.ARGB_8888, true)

            val canvas = Canvas(copy)
            val paint = Paint()

            paint.strokeWidth = WIDTH_PATH
            paint.color = COLOR_LINE

            for (n in f.nodes.values)
                for (nn in n.adj.keys)
                    canvas.drawLine(
                        n.x.toFloat(),
                        n.y.toFloat(),
                        nn.x.toFloat(),
                        nn.y.toFloat(),
                        paint
                    )

            for (n in f.nodes.values) {
                // TODO: this is kind of gross, maybe add more const colors with names that make sense

                paint.color = when (n.type) {
                    NodeType.NONE -> COLOR_LINE
                    NodeType.STAIR -> COLOR_START
                    NodeType.PORT -> COLOR_END
                    NodeType.ROOM -> COLOR_STAIRS_UP
                    else -> COLOR_STAIRS_DOWN
                }

                canvas.drawCircle(
                    n.x.toFloat(),
                    n.y.toFloat(),
                    RADIUS_NODE,
                    paint
                )
            }

            val file = File(applicationContext.cacheDir, "temp_full_graph_${f.level}.png")
            val outStream = FileOutputStream(file)
            copy.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
            outStream.close()
        }
    }

    init {
        // TODO: Add logic for retrieving and/or loading serialized map data
        // ? Will hardcode points for now

        // Fallback
        buildings[-1] = BuildingMap(
            1,
            HashMap(),
            HashMap(),
            0
        )

        buildings[-1]!!.plans[1] = FloorMap(
            1,
            HashMap(),
            R.drawable.uwlogo,
            buildings[-1]!!
        )

        // E7
        buildings[1] = BuildingMap(
            1,
            HashMap(),
            HashMap(),
            305
        )

        buildings[1]!!.plans[1] = FloorMap(
            1,
            HashMap(),
            R.drawable.e7f1,
            buildings[1]!!
        )

        val f1 = buildings[1]!!.plans[1]!!

        buildings[1]!!.plans[2] = FloorMap(
            2,
            HashMap(),
            R.drawable.e7f2,
            buildings[1]!!
        )

        val f2 = buildings[1]!!.plans[2]!!

        buildings[1]!!.plans[3] = FloorMap(
            3,
            HashMap(),
            R.drawable.e7f3,
            buildings[1]!!
        )

        val f3 = buildings[1]!!.plans[3]!!

        buildings[1]!!.plans[4] = FloorMap(
            4,
            HashMap(),
            R.drawable.e7f4hi,
            buildings[1]!!
        )

        val f4 = buildings[1]!!.plans[4]!!

        buildings[1]!!.plans[5] = FloorMap(
            5,
            HashMap(),
            R.drawable.e7f5hi,
            buildings[1]!!
        )

        val f5 = buildings[1]!!.plans[5]!!

        buildings[1]!!.plans[6] = FloorMap(
            6,
            HashMap(),
            R.drawable.e7f6hi,
            buildings[1]!!
        )

        val f6 = buildings[1]!!.plans[6]!!

        buildings[1]!!.plans[7] = FloorMap(
            7,
            HashMap(),
            R.drawable.e7f7hi,
            buildings[1]!!
        )

        val f7 = buildings[1]!!.plans[7]!!

        // TODO: SERIALIZE THIS DATA SOMEHOW TO STORE IN DB

        // F1 INIT

        // ROOMS HAVE NUMBERS -- 1324, 1326
        // PORTS HAVE NEGATIVE 10XX
        // WC HAVE NEGATIVE 11XX
        // STAIRS HAVE NEGATIVE 12XX
        // WATER HAVE NEGATIVE 13XX
        // NONE IS ALL ELSE 1XXX
        run {
            // COLUMN 1
            f1.nodes[-1000.0] = MapNode(222, 90, NodeType.PORT, -1000, HashMap(), f1)
            f1.nodes[-1200.0] = MapNode(222, 102, NodeType.STAIR, -1200, HashMap(), f1)
            f1.nodes[1324.0] = MapNode(222, 185, NodeType.ROOM, 1324, HashMap(), f1)
            f1.nodes[1326.0] = MapNode(222, 240, NodeType.ROOM, 1326, HashMap(), f1)
            f1.nodes[1327.0] = MapNode(222, 240, NodeType.ROOM, 1327, HashMap(), f1)
            f1.nodes[-1201.0] = MapNode(222, 284, NodeType.STAIR, -1201, HashMap(), f1)
            f1.nodes[-1500.0] = MapNode(222, 316, NodeType.NONE, -1500, HashMap(), f1)
            f1.nodes[-1501.0] = MapNode(222, 343, NodeType.PORT, -1501, HashMap(), f1)
            addEdge(f1, -1000, -1200)
            addEdge(f1, -1200, 1324)
            addEdge(f1, 1324, 1326)
            addEdge(f1, 1326, 1327)
            addEdge(f1, 1327, -1201)
            addEdge(f1, -1201, -1500)
            addEdge(f1, -1500, -1501)

            // COLUMN 2
            f1.nodes[1302.0] = MapNode(327, 185, NodeType.ROOM, 1302, HashMap(), f1)
            f1.nodes[1327.1] = MapNode(327, 204, NodeType.ROOM, 1327, HashMap(), f1)
            f1.nodes[1343.0] = MapNode(327, 211, NodeType.ROOM, 1343, HashMap(), f1)
            f1.nodes[1339.0] = MapNode(327, 236, NodeType.ROOM, 1339, HashMap(), f1)
            f1.nodes[1331.0] = MapNode(327, 297, NodeType.ROOM, 1331, HashMap(), f1)
            f1.nodes[-1502.0] = MapNode(327, 316, NodeType.NONE, -1502, HashMap(), f1)
            f1.nodes[-1503.0] = MapNode(327, 343, NodeType.NONE, -1503, HashMap(), f1)
            f1.nodes[-1504.0] = MapNode(327, 373, NodeType.NONE, -1504, HashMap(), f1)
            addEdge(f1, 1302, 1327.1)
            addEdge(f1, 1327.1, 1343)
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
            f1.nodes[-1202.0] = MapNode(345, 373, NodeType.STAIR, -1202, HashMap(), f1)

            // COLUMN 2 X 3
            addEdge(f1, -1504, -1202)

            // COLUMN 4
            f1.nodes[-1505.0] = MapNode(447, 185, NodeType.NONE, -1505, HashMap(), f1)
            f1.nodes[-1506.0] = MapNode(447, 316, NodeType.NONE, -1506, HashMap(), f1)
            f1.nodes[-1002.0] = MapNode(447, 445, NodeType.PORT, -1002, HashMap(), f1)
            addEdge(f1, -1505, -1506)
            addEdge(f1, -1506, -1002)

            // COLUMN 3 X 4
            addEdge(f1, 1302, -1505)
            addEdge(f1, -1502, -1506)

            // COLUMN 5
            f1.nodes[-1003.0] = MapNode(472, 154, NodeType.PORT, -1003, HashMap(), f1)
            f1.nodes[-1507.0] = MapNode(472, 185, NodeType.NONE, -1507, HashMap(), f1)
            addEdge(f1, -1003, -1507)

            // COLUMN 4 X 5
            addEdge(f1, -1505, -1507)

            // COLUMN 6
            f1.nodes[-1100.0] = MapNode(502, 185, NodeType.WC, -1100, HashMap(), f1)
            f1.nodes[-1300.0] = MapNode(502, 281, NodeType.WATER, -1300, HashMap(), f1)
            f1.nodes[-1508.0] = MapNode(502, 316, NodeType.NONE, -1508, HashMap(), f1)
            addEdge(f1, -1100, -1300)
            addEdge(f1, -1300, -1508)

            // COLUMN 5 X 6
            addEdge(f1, -1507, -1100)
            addEdge(f1, -1506, -1508)

            // COLUMN 7
            f1.nodes[1419.0] = MapNode(574, 316, NodeType.ROOM, 1419, HashMap(), f1)
            f1.nodes[1416.0] = MapNode(574, 316, NodeType.ROOM, 1416, HashMap(), f1)
            addEdge(f1, 1419, 1416)

            // COLUMN 6 X 7
            addEdge(f1, -1508, 1419)

            // COLUMN 8
            f1.nodes[1427.0] = MapNode(607, 316, NodeType.ROOM, 1427, HashMap(), f1)

            // COLUMN 7 X 8
            addEdge(f1, 1416, 1427)

            // COLUMN 9
            f1.nodes[-1004.0] = MapNode(680, 316, NodeType.PORT, -1004, HashMap(), f1)
            f1.nodes[-1203.0] = MapNode(680, 282, NodeType.STAIR, -1202, HashMap(), f1)
            addEdge(f1, -1004, -1203)

            // COLUMN 8 X 9
            addEdge(f1, 1427, -1004)
        }

        // F2 INIT

        // ROOMS HAVE NUMBERS -- 2324, 2328
        // PORTS HAVE NEGATIVE 20XX
        // WC HAVE NEGATIVE 21XX
        // STAIRS HAVE NEGATIVE 22XX
        // NONE IS ALL ELSE 2XXX
        run {
            // COLUMN 1 (LEFTMOST)
            f2.nodes[-2200.0] = MapNode(222, 102, NodeType.STAIR, -2200, HashMap(), f2)
            f2.nodes[2324.0] = MapNode(222, 128, NodeType.ROOM, 2324, HashMap(), f2)
            f2.nodes[2328.0] = MapNode(222, 259, NodeType.ROOM, 2328, HashMap(), f2)
            f2.nodes[-2201.0] = MapNode(222, 284, NodeType.STAIR, -2201, HashMap(), f2)
            f2.nodes[2334.0] = MapNode(222, 352, NodeType.ROOM, 2334, HashMap(), f2)
            f2.nodes[-2000.0] = MapNode(222, 393, NodeType.PORT, -2000, HashMap(), f2)
            addEdge(f2, -2200, 2324)
            addEdge(f2, 2324, 2328)
            addEdge(f2, 2328, -2201)
            addEdge(f2, -2201, 2334)
            addEdge(f2, 2334, -2000)

            // ROW 1 (TOPMOST)
            f2.nodes[2322.0] = MapNode(250, 128, NodeType.ROOM, 2322, HashMap(), f2)
            f2.nodes[2318.0] = MapNode(271, 128, NodeType.ROOM, 2318, HashMap(), f2)
            f2.nodes[2316.0] = MapNode(294, 128, NodeType.ROOM, 2316, HashMap(), f2)
            f2.nodes[2317.0] = MapNode(310, 128, NodeType.ROOM, 2317, HashMap(), f2)
            f2.nodes[2314.0] = MapNode(316, 128, NodeType.ROOM, 2314, HashMap(), f2)
            f2.nodes[2312.0] = MapNode(339, 128, NodeType.ROOM, 2312, HashMap(), f2)
            f2.nodes[2357.0] = MapNode(356, 128, NodeType.ROOM, 2357, HashMap(), f2)
            f2.nodes[2308.0] = MapNode(360, 128, NodeType.ROOM, 2308, HashMap(), f2)
            f2.nodes[2306.0] = MapNode(384, 128, NodeType.ROOM, 2306, HashMap(), f2)
            f2.nodes[2304.0] = MapNode(395, 128, NodeType.ROOM, 2304, HashMap(), f2)
            f2.nodes[-2500.0] = MapNode(446, 128, NodeType.NONE, -2500, HashMap(), f2)
            f2.nodes[2301.0] = MapNode(473, 128, NodeType.ROOM, 2301, HashMap(), f2)
            f2.nodes[-2501.0] = MapNode(500, 128, NodeType.NONE, -2501, HashMap(), f2)
            f2.nodes[2402.0] = MapNode(529, 128, NodeType.ROOM, 2402, HashMap(), f2)
            f2.nodes[2404.0] = MapNode(551, 128, NodeType.ROOM, 2404, HashMap(), f2)
            f2.nodes[2911.0] = MapNode(564, 128, NodeType.ROOM, 2911, HashMap(), f2)
            f2.nodes[2406.0] = MapNode(574, 128, NodeType.ROOM, 2406, HashMap(), f2)
            f2.nodes[2408.0] = MapNode(596, 128, NodeType.ROOM, 2408, HashMap(), f2)
            f2.nodes[2409.0] = MapNode(596, 128, NodeType.ROOM, 2409, HashMap(), f2)
            f2.nodes[2412.0] = MapNode(619, 128, NodeType.ROOM, 2412, HashMap(), f2)
            f2.nodes[2414.0] = MapNode(641, 128, NodeType.ROOM, 2414, HashMap(), f2)
            f2.nodes[2416.0] = MapNode(664, 128, NodeType.ROOM, 2416, HashMap(), f2)
            f2.nodes[2418.0] = MapNode(686, 128, NodeType.ROOM, 2418, HashMap(), f2)
            f2.nodes[2422.0] = MapNode(708, 128, NodeType.ROOM, 2422, HashMap(), f2)
            f2.nodes[2423.0] = MapNode(725, 128, NodeType.ROOM, 2423, HashMap(), f2)
            f2.nodes[2424.0] = MapNode(730, 128, NodeType.ROOM, 2424, HashMap(), f2)
            f2.nodes[2426.0] = MapNode(753, 128, NodeType.ROOM, 2426, HashMap(), f2)
            f2.nodes[2428.0] = MapNode(775, 128, NodeType.ROOM, 2428, HashMap(), f2)
            f2.nodes[2432.0] = MapNode(786, 128, NodeType.ROOM, 2432, HashMap(), f2)

            addEdge(f2, 2322, 2318)
            addEdge(f2, 2318, 2316)
            addEdge(f2, 2316, 2317)
            addEdge(f2, 2317, 2314)
            addEdge(f2, 2314, 2312)
            addEdge(f2, 2312, 2357)
            addEdge(f2, 2357, 2308)
            addEdge(f2, 2308, 2306)
            addEdge(f2, 2306, 2304)
            addEdge(f2, 2304, -2500)
            addEdge(f2, -2500, 2301)
            addEdge(f2, 2301, -2501)
            addEdge(f2, -2501, 2402)
            addEdge(f2, 2402, 2404)
            addEdge(f2, 2404, 2911)
            addEdge(f2, 2911, 2406)
            addEdge(f2, 2406, 2408)
            addEdge(f2, 2408, 2409)
            addEdge(f2, 2409, 2412)
            addEdge(f2, 2412, 2414)
            addEdge(f2, 2414, 2416)
            addEdge(f2, 2416, 2418)
            addEdge(f2, 2418, 2422)
            addEdge(f2, 2422, 2423)
            addEdge(f2, 2423, 2424)
            addEdge(f2, 2424, 2426)
            addEdge(f2, 2426, 2428)
            addEdge(f2, 2428, 2432)

            // ROW 1 X 2302
            f2.nodes[2302.0] = MapNode(395, 105, NodeType.ROOM, 2302, HashMap(), f2)
            addEdge(f2, 2304, 2302)

            // COLUMN 1 X ROW 1
            addEdge(f2, 2324, 2322)

            // INNER 1
            f2.nodes[2342.0] = MapNode(249, 259, NodeType.ROOM, 2342, HashMap(), f2)
            f2.nodes[2344.0] = MapNode(271, 259, NodeType.ROOM, 2344, HashMap(), f2)
            f2.nodes[2343.0] = MapNode(279, 259, NodeType.ROOM, 2343, HashMap(), f2)
            f2.nodes[2346.0] = MapNode(294, 259, NodeType.ROOM, 2346, HashMap(), f2)
            f2.nodes[2348.0] = MapNode(319, 259, NodeType.ROOM, 2348, HashMap(), f2)
            f2.nodes[2349.0] = MapNode(324, 259, NodeType.ROOM, 2349, HashMap(), f2)
            f2.nodes[2352.0] = MapNode(334, 259, NodeType.ROOM, 2352, HashMap(), f2)
            f2.nodes[2317.1] = MapNode(334, 218, NodeType.ROOM, 2317, HashMap(), f2)
            f2.nodes[2357.1] = MapNode(346, 218, NodeType.ROOM, 2357, HashMap(), f2)
            addEdge(f2, 2342, 2344)
            addEdge(f2, 2344, 2343)
            addEdge(f2, 2343, 2346)
            addEdge(f2, 2346, 2348)
            addEdge(f2, 2348, 2349)
            addEdge(f2, 2349, 2352)
            addEdge(f2, 2352, 2317.1)
            addEdge(f2, 2317.1, 2357.1)

            // COLUMN 1 X INNER 1
            addEdge(f2, 2328, 2342)

            // COLUMN 2
            f2.nodes[-2502.0] = MapNode(446, 218, NodeType.NONE, -2502, HashMap(), f2)
            f2.nodes[-2202.0] = MapNode(446, 373, NodeType.STAIR, -2202, HashMap(), f2)
            f2.nodes[-2503.0] = MapNode(446, 393, NodeType.NONE, -2503, HashMap(), f2)
            addEdge(f2, -2502, -2202)
            addEdge(f2, -2202, -2503)

            // ROW 1 X COLUMN 2
            addEdge(f2, -2500, -2502)
            // INNER 1 X COLUMN 2
            addEdge(f2, 2357.1, -2502)
            // COLUMN 1 X COLUMN 2
            addEdge(f2, -2000, -2503)

            // COLUMN 3
            f2.nodes[-2203.0] = MapNode(500, 150, NodeType.STAIR, -2203, HashMap(), f2)
            f2.nodes[-2100.0] = MapNode(500, 185, NodeType.WC, -2100, HashMap(), f2)
            f2.nodes[-2101.0] = MapNode(500, 195, NodeType.WC, -2101, HashMap(), f2)
            f2.nodes[-2504.0] = MapNode(500, 260, NodeType.NONE, -2504, HashMap(), f2)
            f2.nodes[-2505.0] = MapNode(500, 314, NodeType.NONE, -2505, HashMap(), f2)
            f2.nodes[-2506.0] = MapNode(500, 393, NodeType.NONE, -2506, HashMap(), f2)
            addEdge(f2, -2203, -2100)
            addEdge(f2, -2100, -2101)
            addEdge(f2, -2101, -2504)
            addEdge(f2, -2504, -2505)
            addEdge(f2, -2505, -2506)

            // ROW 1 X COLUMN 3
            addEdge(f2, -2501, -2203)

            // ROW 2
            f2.nodes[2466.0] = MapNode(530, 260, NodeType.ROOM, 2466, HashMap(), f2)
            f2.nodes[-2102.0] = MapNode(538, 260, NodeType.WC, -2102, HashMap(), f2)
            f2.nodes[2464.0] = MapNode(551, 260, NodeType.ROOM, 2464, HashMap(), f2)
            f2.nodes[2466.0] = MapNode(530, 260, NodeType.ROOM, 2466, HashMap(), f2)
            f2.nodes[2462.0] = MapNode(563, 260, NodeType.ROOM, 2462, HashMap(), f2)
            f2.nodes[2916.0] = MapNode(572, 260, NodeType.ROOM, 2916, HashMap(), f2)
            f2.nodes[2458.0] = MapNode(597, 260, NodeType.ROOM, 2458, HashMap(), f2)
            f2.nodes[2456.0] = MapNode(641, 260, NodeType.ROOM, 2456, HashMap(), f2)
            f2.nodes[2454.0] = MapNode(663, 260, NodeType.ROOM, 2454, HashMap(), f2)
            f2.nodes[2409.1] = MapNode(679, 260, NodeType.ROOM, 2409, HashMap(), f2)
            f2.nodes[2453.0] = MapNode(736, 260, NodeType.ROOM, 2453, HashMap(), f2)
            f2.nodes[2452.0] = MapNode(767, 260, NodeType.ROOM, 2452, HashMap(), f2)
            f2.nodes[2448.0] = MapNode(786, 260, NodeType.ROOM, 2448, HashMap(), f2)
            f2.nodes[2917.0] = MapNode(803, 260, NodeType.ROOM, 2917, HashMap(), f2)
            f2.nodes[2446.0] = MapNode(814, 260, NodeType.ROOM, 2446, HashMap(), f2)
            addEdge(f2, 2466, -2102)
            addEdge(f2, -2102, 2464)
            addEdge(f2, 2464, 2466)
            addEdge(f2, 2466, 2462)
            addEdge(f2, 2462, 2916)
            addEdge(f2, 2916, 2458)
            addEdge(f2, 2458, 2456)
            addEdge(f2, 2456, 2454)
            addEdge(f2, 2454, 2409.1)
            addEdge(f2, 2409.1, 2453)
            addEdge(f2, 2453, 2452)
            addEdge(f2, 2452, 2448)
            addEdge(f2, 2448, 2917)
            addEdge(f2, 2917, 2446)

            // COLUMN 3 X ROW 2
            addEdge(f2, -2504, 2466)

            // COLUMN 4 (TOP RIGHT)
            f2.nodes[2429.0] = MapNode(786, 141, NodeType.ROOM, 2429, HashMap(), f2)
            f2.nodes[2434.0] = MapNode(786, 141, NodeType.ROOM, 2434, HashMap(), f2)
            f2.nodes[2433.0] = MapNode(786, 164, NodeType.ROOM, 2433, HashMap(), f2)
            f2.nodes[2436.0] = MapNode(786, 164, NodeType.ROOM, 2436, HashMap(), f2)
            f2.nodes[2437.0] = MapNode(786, 188, NodeType.ROOM, 2437, HashMap(), f2)
            f2.nodes[2438.0] = MapNode(786, 188, NodeType.ROOM, 2438, HashMap(), f2)
            f2.nodes[2439.0] = MapNode(786, 211, NodeType.ROOM, 2439, HashMap(), f2)
            f2.nodes[2442.0] = MapNode(786, 211, NodeType.ROOM, 2442, HashMap(), f2)
            f2.nodes[2441.0] = MapNode(786, 234, NodeType.ROOM, 2441, HashMap(), f2)
            f2.nodes[2444.0] = MapNode(786, 234, NodeType.ROOM, 2444, HashMap(), f2)
            addEdge(f2, 2429, 2434)
            addEdge(f2, 2434, 2433)
            addEdge(f2, 2433, 2436)
            addEdge(f2, 2436, 2437)
            addEdge(f2, 2437, 2438)
            addEdge(f2, 2438, 2439)
            addEdge(f2, 2439, 2442)
            addEdge(f2, 2442, 2441)
            addEdge(f2, 2441, 2444)

            // ROW 1 X COLUMN 4
            addEdge(f2, 2432, 2429)
            // COLUMN 4 X ROW 2
            addEdge(f2, 2444, 2448)

            // ROW 3
            f2.nodes[2472.0] = MapNode(595, 314, NodeType.ROOM, 2472, HashMap(), f2)
            f2.nodes[2456.1] = MapNode(641, 314, NodeType.ROOM, 2456, HashMap(), f2)
            f2.nodes[2454.1] = MapNode(652, 314, NodeType.ROOM, 2454, HashMap(), f2)
            addEdge(f2, 2472, 2456.1)
            addEdge(f2, 2456, 2454.1)

            // COLUMN 3 X ROW 3
            addEdge(f2, -2505, 2472)

            // ROW 4
            f2.nodes[-2001.0] = MapNode(473, 393, NodeType.PORT, -2001, HashMap(), f2)
            // COLUMN 2 X ROW 4
            addEdge(f2, -2503, -2001)
            // ROW 4 X COLUMN 3
            addEdge(f2, -2001, -2506)

            // COLUMN 5
            f2.nodes[-2204.0] = MapNode(679, 260, NodeType.STAIR, -2204, HashMap(), f2)
            f2.nodes[-2002.0] = MapNode(679, 393, NodeType.PORT, -2002, HashMap(), f2)
            addEdge(f2, -2204, -2002)
            // ROW 2 X COLUMN 5
            addEdge(f2, 2409.1, -2204)
            // COLUMN 3 X COLUMN 5
            addEdge(f2, -2506, -2002)

        }

        // F1 X F2
        run {
            addEdge(1, -1200, -2200)
            addEdge(1, -1201, -2201)
            addEdge(1, -1202, -2202)
            addEdge(1, -1203, -2204)
        }

        // F3 INIT

        // ROOMS HAVE NUMBERS -- 3324, 3328
        // PORTS HAVE NEGATIVE 30XX
        // WC HAVE NEGATIVE 31XX
        // STAIRS HAVE NEGATIVE 32XX
        // NONE IS ALL ELSE 3XXX

        run {
            // COLUMN 1 (LEFTMOST)
            f3.nodes[3324.0] = MapNode(175, 128, NodeType.ROOM, 3324, HashMap(), f3)
            f3.nodes[3326.0] = MapNode(175, 141, NodeType.ROOM, 3326, HashMap(), f3)
            f3.nodes[3327.0] = MapNode(175, 145, NodeType.ROOM, 3327, HashMap(), f3)
            f3.nodes[3328.0] = MapNode(175, 163, NodeType.ROOM, 3328, HashMap(), f3)
            f3.nodes[3332.0] = MapNode(175, 187, NodeType.ROOM, 3332, HashMap(), f3)
            f3.nodes[3343.0] = MapNode(175, 187, NodeType.ROOM, 3343, HashMap(), f3)
            f3.nodes[3334.0] = MapNode(175, 210, NodeType.ROOM, 3334, HashMap(), f3)
            f3.nodes[3336.0] = MapNode(175, 233, NodeType.ROOM, 3336, HashMap(), f3)
            f3.nodes[3338.0] = MapNode(175, 256, NodeType.ROOM, 3338, HashMap(), f3)
            addEdge(f3, 3324, 3326)
            addEdge(f3, 3326, 3327)
            addEdge(f3, 3327, 3328)
            addEdge(f3, 3328, 3332)
            addEdge(f3, 3332, 3343)
            addEdge(f3, 3343, 3334)
            addEdge(f3, 3334, 3336)
            addEdge(f3, 3336, 3338)

            // ROW 1 (TOPMOST)
            f3.nodes[3921.0] = MapNode(175, 128, NodeType.ROOM, 3921, HashMap(), f3)
            f3.nodes[-3500.0] = MapNode(221, 128, NodeType.NONE, -3500, HashMap(), f3)
            f3.nodes[3322.0] = MapNode(250, 128, NodeType.ROOM, 3322, HashMap(), f3)
            f3.nodes[3318.0] = MapNode(272, 128, NodeType.ROOM, 3318, HashMap(), f3)
            f3.nodes[3316.0] = MapNode(295, 128, NodeType.ROOM, 3316, HashMap(), f3)
            f3.nodes[3314.0] = MapNode(318, 128, NodeType.ROOM, 3314, HashMap(), f3)
            f3.nodes[3312.0] = MapNode(340, 128, NodeType.ROOM, 3312, HashMap(), f3)
            f3.nodes[3308.0] = MapNode(361, 128, NodeType.ROOM, 3308, HashMap(), f3)
            f3.nodes[3306.0] = MapNode(384, 128, NodeType.ROOM, 3306, HashMap(), f3)
            f3.nodes[3353.0] = MapNode(384, 128, NodeType.ROOM, 3353, HashMap(), f3)
            f3.nodes[3304.0] = MapNode(406, 128, NodeType.ROOM, 3304, HashMap(), f3)
            f3.nodes[3302.0] = MapNode(418, 128, NodeType.ROOM, 3302, HashMap(), f3)
            f3.nodes[3303.0] = MapNode(421, 128, NodeType.ROOM, 3303, HashMap(), f3)
            f3.nodes[-3501.0] = MapNode(491, 128, NodeType.NONE, -3501, HashMap(), f3)
            f3.nodes[-3502.0] = MapNode(501, 128, NodeType.NONE, -3502, HashMap(), f3)
            f3.nodes[3402.0] = MapNode(528, 128, NodeType.ROOM, 3402, HashMap(), f3)
            f3.nodes[3404.0] = MapNode(552, 128, NodeType.ROOM, 3404, HashMap(), f3)
            f3.nodes[3911.0] = MapNode(564, 128, NodeType.ROOM, 3911, HashMap(), f3)
            f3.nodes[3406.0] = MapNode(574, 128, NodeType.ROOM, 3406, HashMap(), f3)
            f3.nodes[3408.0] = MapNode(597, 128, NodeType.ROOM, 3408, HashMap(), f3)
            f3.nodes[3412.0] = MapNode(618, 128, NodeType.ROOM, 3412, HashMap(), f3)
            f3.nodes[3414.0] = MapNode(641, 128, NodeType.ROOM, 3414, HashMap(), f3)
            f3.nodes[3417.0] = MapNode(645, 128, NodeType.ROOM, 3417, HashMap(), f3)
            f3.nodes[3416.0] = MapNode(664, 128, NodeType.ROOM, 3416, HashMap(), f3)
            f3.nodes[3419.0] = MapNode(680, 128, NodeType.ROOM, 3419, HashMap(), f3)
            f3.nodes[3418.0] = MapNode(686, 128, NodeType.ROOM, 3418, HashMap(), f3)
            f3.nodes[3423.0] = MapNode(700, 128, NodeType.ROOM, 3423, HashMap(), f3)
            f3.nodes[3422.0] = MapNode(708, 128, NodeType.ROOM, 3422, HashMap(), f3)
            f3.nodes[3424.0] = MapNode(731, 128, NodeType.ROOM, 3424, HashMap(), f3)
            f3.nodes[3427.0] = MapNode(731, 128, NodeType.ROOM, 3427, HashMap(), f3)
            f3.nodes[3426.0] = MapNode(753, 128, NodeType.ROOM, 3426, HashMap(), f3)
            f3.nodes[3428.0] = MapNode(775, 128, NodeType.ROOM, 3428, HashMap(), f3)
            f3.nodes[3431.0] = MapNode(775, 128, NodeType.ROOM, 3431, HashMap(), f3)
            f3.nodes[3432.0] = MapNode(796, 128, NodeType.ROOM, 3432, HashMap(), f3)
            f3.nodes[3433.0] = MapNode(804, 128, NodeType.ROOM, 3433, HashMap(), f3)
            f3.nodes[3434.0] = MapNode(816, 128, NodeType.ROOM, 3434, HashMap(), f3)
            addEdge(f3, 3921, -3500)
            addEdge(f3, -3500, 3322)
            addEdge(f3, 3322, 3318)
            addEdge(f3, 3318, 3316)
            addEdge(f3, 3316, 3314)
            addEdge(f3, 3314, 3312)
            addEdge(f3, 3312, 3308)
            addEdge(f3, 3308, 3306)
            addEdge(f3, 3306, 3353)
            addEdge(f3, 3353, 3304)
            addEdge(f3, 3304, 3302)
            addEdge(f3, 3302, 3303)
            addEdge(f3, 3303, -3501)
            addEdge(f3, -3501, -3502)
            addEdge(f3, -3502, 3402)
            addEdge(f3, 3402, 3404)
            addEdge(f3, 3404, 3911)
            addEdge(f3, 3911, 3406)
            addEdge(f3, 3406, 3408)
            addEdge(f3, 3408, 3412)
            addEdge(f3, 3412, 3414)
            addEdge(f3, 3414, 3417)
            addEdge(f3, 3417, 3416)
            addEdge(f3, 3416, 3419)
            addEdge(f3, 3419, 3418)
            addEdge(f3, 3418, 3423)
            addEdge(f3, 3423, 3422)
            addEdge(f3, 3422, 3424)
            addEdge(f3, 3424, 3427)
            addEdge(f3, 3427, 3426)
            addEdge(f3, 3426, 3428)
            addEdge(f3, 3428, 3431)
            addEdge(f3, 3431, 3432)
            addEdge(f3, 3432, 3433)
            addEdge(f3, 3433, 3434)

            // COLUMN 1 X ROW 1
            addEdge(f3, 3324, 3921)

            // Offshoots from Row 1
            f3.nodes[3921.0] = MapNode(221, 104, NodeType.ROOM, 3921, HashMap(), f3)
            addEdge(f3, -3500, 3921)
            f3.nodes[-3200.0] = MapNode(221, 104, NodeType.STAIR, -3200, HashMap(), f3)
            addEdge(f3, 3921, -3200)

            f3.nodes[-3000.0] = MapNode(491, 100, NodeType.PORT, -3000, HashMap(), f3)
            addEdge(f3, -3501, -3000)

            // Bottom left
            f3.nodes[-3503.0] = MapNode(222, 256, NodeType.NONE, -3503, HashMap(), f3)
            f3.nodes[-3504.0] = MapNode(222, 272, NodeType.NONE, -3504, HashMap(), f3)
            f3.nodes[-3201.0] = MapNode(222, 283, NodeType.STAIR, -3201, HashMap(), f3)
            f3.nodes[-3001.0] = MapNode(222, 295, NodeType.PORT, -3001, HashMap(), f3)
            addEdge(f3, -3503, -3504)
            addEdge(f3, -3504, -3201)
            addEdge(f3, -3201, -3001)
            f3.nodes[3342.0] = MapNode(241, 272, NodeType.ROOM, 3342, HashMap(), f3)
            addEdge(f3, -3504, 3342)
            f3.nodes[3344.0] = MapNode(293, 272, NodeType.ROOM, 3344, HashMap(), f3)
            f3.nodes[-3505.0] = MapNode(300, 272, NodeType.NONE, -3505, HashMap(), f3)
            f3.nodes[3346.0] = MapNode(309, 272, NodeType.ROOM, 3346, HashMap(), f3)
            f3.nodes[-3506.0] = MapNode(320, 272, NodeType.NONE, -3506, HashMap(), f3)
            f3.nodes[3348.0] = MapNode(359, 272, NodeType.ROOM, 3348, HashMap(), f3)
            f3.nodes[3352.0] = MapNode(375, 272, NodeType.ROOM, 3352, HashMap(), f3)
            f3.nodes[3354.0] = MapNode(426, 272, NodeType.ROOM, 3354, HashMap(), f3)
            addEdge(f3, 3342, 3344)
            addEdge(f3, 3344, -3505)
            addEdge(f3, -3505, 3346)
            addEdge(f3, 3346, -3506)
            addEdge(f3, -3506, 3348)
            addEdge(f3, 3348, 3352)
            addEdge(f3, 3352, 3354)
            f3.nodes[3343.1] = MapNode(300, 228, NodeType.ROOM, 3343, HashMap(), f3)
            f3.nodes[3353.1] = MapNode(320, 228, NodeType.ROOM, 3353, HashMap(), f3)
            addEdge(f3, -3505, 3343.1)
            addEdge(f3, -3506, 3353.1)
            addEdge(f3, 3343.1, 3353.1)

            // Bottom left x Column 1
            addEdge(f3, -3503, 3338)

            // COLUMN 2
            f3.nodes[-3203.0] = MapNode(501, 151, NodeType.STAIR, -3203, HashMap(), f3)
            f3.nodes[-3100.0] = MapNode(501, 190, NodeType.WC, -3100, HashMap(), f3) // combined both "rooms" into one node
            f3.nodes[-3507.0] = MapNode(501, 259, NodeType.NONE, -3507, HashMap(), f3)
            f3.nodes[-3508.0] = MapNode(501, 272, NodeType.NONE, -3508, HashMap(), f3)
            f3.nodes[-3204.0] = MapNode(501, 372, NodeType.STAIR, -3204, HashMap(), f3)
            f3.nodes[-3002.0] = MapNode(501, 392, NodeType.PORT, -3002, HashMap(), f3)
            addEdge(f3, -3203, -3100)
            addEdge(f3, -3100, -3507)
            addEdge(f3, -3507, -3508)
            addEdge(f3, -3508, -3204)
            addEdge(f3, -3204, -3002)

            // COLUMN 2 X ROW 1
            addEdge(f3, -3203, -3502)

            // COLUMN 2 X Bottom Left
            addEdge(f3, -3508, 3354)

            // ROW 2 (Bottom Right)
            f3.nodes[3458.0] = MapNode(529, 259, NodeType.ROOM, 3458, HashMap(), f3)
            f3.nodes[3914.0] = MapNode(539, 259, NodeType.ROOM, 3914, HashMap(), f3)
            f3.nodes[3456.0] = MapNode(550, 259, NodeType.ROOM, 3456, HashMap(), f3)
            f3.nodes[3916.0] = MapNode(571, 259, NodeType.ROOM, 3916, HashMap(), f3)
            f3.nodes[3454.0] = MapNode(573, 259, NodeType.ROOM, 3454, HashMap(), f3)
            f3.nodes[3452.0] = MapNode(597, 259, NodeType.ROOM, 3452, HashMap(), f3)
            f3.nodes[3448.0] = MapNode(618, 259, NodeType.ROOM, 3448, HashMap(), f3)
            f3.nodes[3449.0] = MapNode(637, 259, NodeType.ROOM, 3449, HashMap(), f3)
            f3.nodes[3446.0] = MapNode(641, 259, NodeType.ROOM, 3446, HashMap(), f3)
            f3.nodes[3444.0] = MapNode(664, 259, NodeType.ROOM, 3444, HashMap(), f3)
            f3.nodes[-3509.0] = MapNode(680, 259, NodeType.NONE, -3509, HashMap(), f3)
            f3.nodes[-3206.0] = MapNode(680, 284, NodeType.STAIR, -3206, HashMap(), f3)
            f3.nodes[-3003.0] = MapNode(680, 396, NodeType.PORT, -3003, HashMap(), f3)
            f3.nodes[3447.0] = MapNode(700, 259, NodeType.ROOM, 3447, HashMap(), f3)
            f3.nodes[3443.0] = MapNode(732, 259, NodeType.ROOM, 3443, HashMap(), f3)
            f3.nodes[3442.0] = MapNode(767, 259, NodeType.ROOM, 3442, HashMap(), f3)
            f3.nodes[3439.0] = MapNode(774, 259, NodeType.ROOM, 3439, HashMap(), f3)
            f3.nodes[3438.0] = MapNode(790, 259, NodeType.ROOM, 3438, HashMap(), f3)
            f3.nodes[3437.0] = MapNode(804, 259, NodeType.ROOM, 3437, HashMap(), f3)
            f3.nodes[3917.0] = MapNode(804, 259, NodeType.ROOM, 3917, HashMap(), f3)
            f3.nodes[3436.0] = MapNode(816, 259, NodeType.ROOM, 3436, HashMap(), f3)
            addEdge(f3, 3458, 3914)
            addEdge(f3, 3914, 3456)
            addEdge(f3, 3456, 3916)
            addEdge(f3, 3916, 3454)
            addEdge(f3, 3454, 3452)
            addEdge(f3, 3452, 3448)
            addEdge(f3, 3448, 3449)
            addEdge(f3, 3449, 3446)
            addEdge(f3, 3446, 3444)
            addEdge(f3, 3444, -3509)
            addEdge(f3, -3509, 3447)
            addEdge(f3, -3509, -3206)
            addEdge(f3, -3206, -3003)
            addEdge(f3, 3447, 3443)
            addEdge(f3, 3443, 3442)
            addEdge(f3, 3442, 3439)
            addEdge(f3, 3439, 3438)
            addEdge(f3, 3438, 3437)
            addEdge(f3, 3437, 3917)

            // ROW 2 X ROW 1
            addEdge(f3, 3436, 3434)

            // ROW 2 X COLUMN 2
            addEdge(f3, 3458, -3507)

        }

        // F2 X F3
        run {
            addEdge(1, -3200, -2200) // J
            addEdge(1, -3201, -2201) // H
            addEdge(1, -3203, -2203) // F
            addEdge(1, -3204, -2202) // Bottom centre
            addEdge(1, -3206, -2204) // G
        }

        // F4 INIT

        // ROOMS HAVE NUMBERS -- 4322, 4318
        // PORTS HAVE NEGATIVE 40XX
        // WC HAVE NEGATIVE 41XX
        // STAIRS HAVE NEGATIVE 42XX
        // NONE IS ALL ELSE 4XXX (USUALLY 45XX)

        run {
            // ROW 1 (TOPMOST)
            f4.nodes[4324.0] = MapNode(507, 433, NodeType.ROOM, 4324, HashMap(), f4)
            f4.nodes[4043.0] = MapNode(616, 433, NodeType.ROOM, 4043, HashMap(), f4)
            f4.nodes[4921.0] = MapNode(616, 433, NodeType.ROOM, 4921, HashMap(), f4)
            f4.nodes[-4500.0] = MapNode(755, 433, NodeType.NONE, -4500, HashMap(), f4)
            f4.nodes[4322.0] = MapNode(849, 433, NodeType.ROOM, 4322, HashMap(), f4)
            f4.nodes[4318.0] = MapNode(928, 433, NodeType.ROOM, 4318, HashMap(), f4)
            f4.nodes[4316.0] = MapNode(1007, 433, NodeType.ROOM, 4316, HashMap(), f4)
            f4.nodes[4314.0] = MapNode(1078, 433, NodeType.ROOM, 4314, HashMap(), f4)
            f4.nodes[4312.0] = MapNode(1155, 433, NodeType.ROOM, 4312, HashMap(), f4)
            f4.nodes[4308.0] = MapNode(1231, 433, NodeType.ROOM, 4308, HashMap(), f4)
            f4.nodes[4306.0] = MapNode(1307, 433, NodeType.ROOM, 4306, HashMap(), f4)
            f4.nodes[4304.0] = MapNode(1382, 433, NodeType.ROOM, 4304, HashMap(), f4)
            f4.nodes[4302.0] = MapNode(1420, 433, NodeType.ROOM, 4302, HashMap(), f4)
            f4.nodes[4053.0] = MapNode(1441, 433, NodeType.ROOM, 4053, HashMap(), f4)
            f4.nodes[4301.0] = MapNode(1703, 433, NodeType.ROOM, 4301, HashMap(), f4)
            f4.nodes[4402.0] = MapNode(1800, 433, NodeType.ROOM, 4402, HashMap(), f4)
            f4.nodes[4404.0] = MapNode(1875, 433, NodeType.ROOM, 4404, HashMap(), f4)
            f4.nodes[4911.0] = MapNode(1920, 433, NodeType.ROOM, 4911, HashMap(), f4)
            f4.nodes[4406.0] = MapNode(1950, 433, NodeType.ROOM, 4406, HashMap(), f4)
            f4.nodes[4408.0] = MapNode(2028, 433, NodeType.ROOM, 4408, HashMap(), f4)
            f4.nodes[4417.0] = MapNode(2028, 433, NodeType.ROOM, 4417, HashMap(), f4)
            f4.nodes[4412.0] = MapNode(2104, 433, NodeType.ROOM, 4412, HashMap(), f4)
            f4.nodes[4414.0] = MapNode(2180, 433, NodeType.ROOM, 4414, HashMap(), f4)
            f4.nodes[4416.0] = MapNode(2256, 433, NodeType.ROOM, 4416, HashMap(), f4)
            f4.nodes[4418.0] = MapNode(2335, 433, NodeType.ROOM, 4418, HashMap(), f4)
            f4.nodes[4422.0] = MapNode(2410, 433, NodeType.ROOM, 4422, HashMap(), f4)
            f4.nodes[4433.0] = MapNode(2460, 433, NodeType.ROOM, 4433, HashMap(), f4)
            f4.nodes[4424.0] = MapNode(2484, 433, NodeType.ROOM, 4424, HashMap(), f4)
            f4.nodes[4426.0] = MapNode(2561, 433, NodeType.ROOM, 4426, HashMap(), f4)
            f4.nodes[4428.0] = MapNode(2636, 433, NodeType.ROOM, 4428, HashMap(), f4)
            f4.nodes[4432.0] = MapNode(2713, 433, NodeType.ROOM, 4432, HashMap(), f4)
            f4.nodes[4434.0] = MapNode(2787, 433, NodeType.ROOM, 4434, HashMap(), f4)
            addEdge(f4, 4324, 4043)
            addEdge(f4, 4043, 4921)
            addEdge(f4, 4921, -4500)
            addEdge(f4, -4500, 4322)
            addEdge(f4, 4322, 4318)
            addEdge(f4, 4318, 4316)
            addEdge(f4, 4316, 4314)
            addEdge(f4, 4314, 4312)
            addEdge(f4, 4312, 4308)
            addEdge(f4, 4308, 4306)
            addEdge(f4, 4306, 4304)
            addEdge(f4, 4304, 4302)
            addEdge(f4, 4302, 4053)
            addEdge(f4, 4053, 4301)
            addEdge(f4, 4301, 4402)
            addEdge(f4, 4402, 4404)
            addEdge(f4, 4404, 4911)
            addEdge(f4, 4911, 4406)
            addEdge(f4, 4406, 4408)
            addEdge(f4, 4408, 4417)
            addEdge(f4, 4417, 4412)
            addEdge(f4, 4412, 4414)
            addEdge(f4, 4414, 4416)
            addEdge(f4, 4416, 4418)
            addEdge(f4, 4418, 4422)
            addEdge(f4, 4422, 4433)
            addEdge(f4, 4433, 4424)
            addEdge(f4, 4424, 4426)
            addEdge(f4, 4426, 4428)
            addEdge(f4, 4428, 4432)
            addEdge(f4, 4432, 4434)
            f4.nodes[4919.0] = MapNode(755, 370, NodeType.ROOM, 4919, HashMap(), f4)
            f4.nodes[-4200.0] = MapNode(755, 350, NodeType.STAIR, -4200, HashMap(), f4)
            addEdge(f4, -4500, 4919)
            addEdge(f4, 4919, -4200)

            // ROW 2
            f4.nodes[4338.0] = MapNode(507, 901, NodeType.ROOM, 4338, HashMap(), f4)
            f4.nodes[4043.1] = MapNode(755, 901, NodeType.ROOM, 4043, HashMap(), f4)
            f4.nodes[4342.0] = MapNode(907, 901, NodeType.ROOM, 4342, HashMap(), f4)
            f4.nodes[4346.0] = MapNode(1137, 901, NodeType.ROOM, 4346, HashMap(), f4)
            f4.nodes[4053.1] = MapNode(1261, 901, NodeType.ROOM, 4053, HashMap(), f4)
            f4.nodes[4352.0] = MapNode(1365, 901, NodeType.ROOM, 4352, HashMap(), f4)
            f4.nodes[4356.0] = MapNode(1539, 901, NodeType.ROOM, 4356, HashMap(), f4)
            f4.nodes[-4501.0] = MapNode(1703, 901, NodeType.NONE, -4501, HashMap(), f4)
            f4.nodes[4458.0] = MapNode(1835, 901, NodeType.ROOM, 4458, HashMap(), f4)
            f4.nodes[-4100.0] = MapNode(1835, 901, NodeType.WC, -4100, HashMap(), f4)
            f4.nodes[4916.0] = MapNode(1944, 901, NodeType.ROOM, 4916, HashMap(), f4)
            f4.nodes[4446.0] = MapNode(2011, 901, NodeType.ROOM, 4446, HashMap(), f4)
            f4.nodes[4444.0] = MapNode(2151, 901, NodeType.ROOM, 4444, HashMap(), f4)
            f4.nodes[4417.1] = MapNode(2312, 901, NodeType.ROOM, 4417, HashMap(), f4)
            f4.nodes[4437.0] = MapNode(2460, 901, NodeType.ROOM, 4437, HashMap(), f4)
            f4.nodes[4442.0] = MapNode(2610, 901, NodeType.ROOM, 4442, HashMap(), f4)
            f4.nodes[4438.0] = MapNode(2687, 901, NodeType.ROOM, 4438, HashMap(), f4)
            f4.nodes[4917.2] = MapNode(2729, 901, NodeType.ROOM, 4917, HashMap(), f4)
            f4.nodes[4436.0] = MapNode(2765, 901, NodeType.ROOM, 4436, HashMap(), f4)
            addEdge(f4, 4338, 4043.1)
            addEdge(f4, 4043.1, 4342)
            addEdge(f4, 4342, 4346)
            addEdge(f4, 4346, 4053.1)
            addEdge(f4, 4053.1, 4352)
            addEdge(f4, 4352, 4356)
            addEdge(f4, 4356, -4501)
            addEdge(f4, -4501, 4458)
            addEdge(f4, 4458, -4100)
            addEdge(f4, -4100, 4916)
            addEdge(f4, 4916, 4446)
            addEdge(f4, 4446, 4444)
            addEdge(f4, 4444, 4417.1)
            addEdge(f4, 4417.1, 4437)
            addEdge(f4, 4437, 4442)
            addEdge(f4, 4442, 4438)
            addEdge(f4, 4438, 4917.2)
            addEdge(f4, 4917.2, 4436)

            // ROW 1 X ROW 2
            addEdge(f4, 4324, 4338)
            f4.nodes[4433.1] = MapNode(2787, 433, NodeType.ROOM, 4434, HashMap(), f4)
            f4.nodes[4437.1] = MapNode(2787, 433, NodeType.ROOM, 4434, HashMap(), f4)
            addEdge(f4, 4434, 4433.1)
            addEdge(f4, 4433.1, 4437.1)
            addEdge(f4, 4437.1, 4436)

            // Left and right offshoots from Row 2
            f4.nodes[-4201.0] = MapNode(755, 967, NodeType.STAIR, -4201, HashMap(), f4)
            f4.nodes[-4000.0] = MapNode(755, 1347, NodeType.PORT, -4000, HashMap(), f4)
            addEdge(f4, 4043, -4201)
            addEdge(f4, -4000, -4201)

            f4.nodes[-4202.0] = MapNode(2312, 967, NodeType.STAIR, -4202, HashMap(), f4)
            f4.nodes[-4001.0] = MapNode(2312, 1347, NodeType.PORT, -4001, HashMap(), f4)
            addEdge(f4, 4417, -4202)
            addEdge(f4, -4001, -4202)

            // Middle Column
            f4.nodes[-4002.0] = MapNode(1703, 365, NodeType.PORT, -4002, HashMap(), f4)
            f4.nodes[-4203.0] = MapNode(1703, 515, NodeType.STAIR, -4203, HashMap(), f4)
            f4.nodes[-4101.0] = MapNode(1703, 651, NodeType.WC, -4101, HashMap(), f4)
            f4.nodes[-4204.0] = MapNode(1703, 1267, NodeType.STAIR, -4204, HashMap(), f4)
            f4.nodes[-4003.0] = MapNode(1703, 1337, NodeType.PORT, -4003, HashMap(), f4)
            addEdge(f4, -4002, 4301)
            addEdge(f4, 4301, -4203)
            addEdge(f4, -4203, -4101)
            addEdge(f4, -4101, -4501)
            addEdge(f4, -4501, -4204)
            addEdge(f4, -4204, -4003)


        }

        // F3 X F4
        run {
            addEdge(1, -3200, -4200) // J
            addEdge(1, -3201, -4201) // H
            addEdge(1, -3203, -4203) // F
            addEdge(1, -3204, -4204) // Bottom centre
            addEdge(1, -3206, -4202) // G
        }

        // F5 INIT

        // ROOMS HAVE NUMBERS -- 5322, 5318
        // PORTS HAVE NEGATIVE 50XX
        // WC HAVE NEGATIVE 51XX
        // STAIRS HAVE NEGATIVE 52XX
        // NONE IS ALL ELSE 5XXX (USUALLY 55XX)

        run {
            // ROW 1 (TOPMOST)
            f5.nodes[5324.0] = MapNode(507, 433, NodeType.ROOM, 5324, HashMap(), f5)
            f5.nodes[5343.0] = MapNode(616, 433, NodeType.ROOM, 5343, HashMap(), f5)
            f5.nodes[5921.0] = MapNode(616, 433, NodeType.ROOM, 5921, HashMap(), f5)
            f5.nodes[-5500.0] = MapNode(755, 433, NodeType.NONE, -5500, HashMap(), f5)
            f5.nodes[5322.0] = MapNode(849, 433, NodeType.ROOM, 5322, HashMap(), f5)
            f5.nodes[5318.0] = MapNode(928, 433, NodeType.ROOM, 5318, HashMap(), f5)
            f5.nodes[5316.0] = MapNode(1007, 433, NodeType.ROOM, 5316, HashMap(), f5)
            f5.nodes[5314.0] = MapNode(1078, 433, NodeType.ROOM, 5314, HashMap(), f5)
            f5.nodes[5312.0] = MapNode(1155, 433, NodeType.ROOM, 5312, HashMap(), f5)
            f5.nodes[5308.0] = MapNode(1231, 433, NodeType.ROOM, 5308, HashMap(), f5)
            f5.nodes[5306.0] = MapNode(1307, 433, NodeType.ROOM, 5306, HashMap(), f5)
            f5.nodes[5304.0] = MapNode(1382, 433, NodeType.ROOM, 5304, HashMap(), f5)
            f5.nodes[5302.0] = MapNode(1420, 433, NodeType.ROOM, 5302, HashMap(), f5)
            f5.nodes[5353.0] = MapNode(1441, 433, NodeType.ROOM, 5353, HashMap(), f5)
            f5.nodes[5301.0] = MapNode(1703, 433, NodeType.ROOM, 5301, HashMap(), f5)
            f5.nodes[5402.0] = MapNode(1800, 433, NodeType.ROOM, 5402, HashMap(), f5)
            f5.nodes[5404.0] = MapNode(1875, 433, NodeType.ROOM, 5404, HashMap(), f5)
            f5.nodes[5911.0] = MapNode(1920, 433, NodeType.ROOM, 5911, HashMap(), f5)
            f5.nodes[5406.0] = MapNode(1950, 433, NodeType.ROOM, 5406, HashMap(), f5)
            f5.nodes[5408.0] = MapNode(2028, 433, NodeType.ROOM, 5408, HashMap(), f5)
            f5.nodes[5411.0] = MapNode(2088, 433, NodeType.ROOM, 5411, HashMap(), f5)
            f5.nodes[5412.0] = MapNode(2104, 433, NodeType.ROOM, 5412, HashMap(), f5)
            f5.nodes[5413.0] = MapNode(2165, 433, NodeType.ROOM, 5413, HashMap(), f5)
            f5.nodes[5414.0] = MapNode(2180, 433, NodeType.ROOM, 5414, HashMap(), f5)
            f5.nodes[5416.0] = MapNode(2256, 433, NodeType.ROOM, 5416, HashMap(), f5)
            f5.nodes[5417.0] = MapNode(2300, 433, NodeType.ROOM, 5417, HashMap(), f5)
            f5.nodes[5418.0] = MapNode(2335, 433, NodeType.ROOM, 5418, HashMap(), f5)
            f5.nodes[5419.0] = MapNode(2410, 433, NodeType.ROOM, 5419, HashMap(), f5)
            f5.nodes[5422.0] = MapNode(2410, 433, NodeType.ROOM, 5422, HashMap(), f5)
            f5.nodes[5421.0] = MapNode(2460, 433, NodeType.ROOM, 5421, HashMap(), f5)
            f5.nodes[5424.0] = MapNode(2484, 433, NodeType.ROOM, 5424, HashMap(), f5)
            f5.nodes[5426.0] = MapNode(2561, 433, NodeType.ROOM, 5426, HashMap(), f5)
            f5.nodes[5423.0] = MapNode(2620, 433, NodeType.ROOM, 5423, HashMap(), f5)
            f5.nodes[5428.0] = MapNode(2636, 433, NodeType.ROOM, 5428, HashMap(), f5)
            f5.nodes[5427.0] = MapNode(2675, 433, NodeType.ROOM, 5427, HashMap(), f5)
            f5.nodes[5432.0] = MapNode(2713, 433, NodeType.ROOM, 5432, HashMap(), f5)
            f5.nodes[5434.0] = MapNode(2775, 433, NodeType.ROOM, 5434, HashMap(), f5)
            addEdge(f5, 5324, 5343)
            addEdge(f5, 5343, 5921)
            addEdge(f5, 5921, -5500)
            addEdge(f5, -5500, 5322)
            addEdge(f5, 5322, 5318)
            addEdge(f5, 5318, 5316)
            addEdge(f5, 5316, 5314)
            addEdge(f5, 5314, 5312)
            addEdge(f5, 5312, 5308)
            addEdge(f5, 5308, 5306)
            addEdge(f5, 5306, 5304)
            addEdge(f5, 5304, 5302)
            addEdge(f5, 5302, 5353)
            addEdge(f5, 5353, 5301)
            addEdge(f5, 5301, 5402)
            addEdge(f5, 5402, 5404)
            addEdge(f5, 5404, 5911)
            addEdge(f5, 5911, 5406)
            addEdge(f5, 5406, 5408)
            addEdge(f5, 5408, 5411)
            addEdge(f5, 5411, 5412)
            addEdge(f5, 5412, 5413)
            addEdge(f5, 5413, 5414)
            addEdge(f5, 5414, 5416)
            addEdge(f5, 5416, 5417)
            addEdge(f5, 5417, 5418)
            addEdge(f5, 5418, 5419)
            addEdge(f5, 5419, 5422)
            addEdge(f5, 5422, 5421)
            addEdge(f5, 5421, 5424)
            addEdge(f5, 5424, 5426)
            addEdge(f5, 5426, 5423)
            addEdge(f5, 5423, 5428)
            addEdge(f5, 5428, 5427)
            addEdge(f5, 5427, 5434)
            addEdge(f5, 5432, 5434)
            f5.nodes[5919.0] = MapNode(755, 370, NodeType.ROOM, 5919, HashMap(), f5)
            f5.nodes[-5200.0] = MapNode(755, 350, NodeType.STAIR, -5200, HashMap(), f5)
            addEdge(f5, -5500, 5919)
            addEdge(f5, 5919, -5200)

            // ROW 2
            f5.nodes[5338.0] = MapNode(507, 885, NodeType.ROOM, 5338, HashMap(), f5)
            f5.nodes[5343.1] = MapNode(752, 885, NodeType.ROOM, 5343, HashMap(), f5)
            f5.nodes[5342.0] = MapNode(913, 885, NodeType.ROOM, 5342, HashMap(), f5)
            f5.nodes[5344.0] = MapNode(1139, 885, NodeType.ROOM, 5344, HashMap(), f5)
            f5.nodes[5353.1] = MapNode(1255, 885, NodeType.ROOM, 5353, HashMap(), f5)
            f5.nodes[5352.0] = MapNode(1368, 885, NodeType.ROOM, 5352, HashMap(), f5)
            f5.nodes[-5501.0] = MapNode(1703, 885, NodeType.NONE, -5501, HashMap(), f5)
            f5.nodes[5458.0] = MapNode(1796, 885, NodeType.ROOM, 5458, HashMap(), f5)
            f5.nodes[5914.0] = MapNode(1834, 885, NodeType.ROOM, 5914, HashMap(), f5)
            f5.nodes[5456.0] = MapNode(1875, 885, NodeType.ROOM, 5456, HashMap(), f5)
            f5.nodes[5916.0] = MapNode(1942, 885, NodeType.ROOM, 5916, HashMap(), f5)
            f5.nodes[5454.0] = MapNode(1943, 885, NodeType.ROOM, 5454, HashMap(), f5)
            f5.nodes[5452.0] = MapNode(2025, 885, NodeType.ROOM, 5452, HashMap(), f5)
            f5.nodes[5451.0] = MapNode(2088, 885, NodeType.ROOM, 5451, HashMap(), f5)
            f5.nodes[5448.0] = MapNode(2103, 885, NodeType.ROOM, 5448, HashMap(), f5)
            f5.nodes[5449.0] = MapNode(2170, 885, NodeType.ROOM, 5449, HashMap(), f5)
            f5.nodes[5446.0] = MapNode(2179, 885, NodeType.ROOM, 5446, HashMap(), f5)
            f5.nodes[5444.0] = MapNode(2258, 885, NodeType.ROOM, 5444, HashMap(), f5)
            f5.nodes[5447.0] = MapNode(2312, 885, NodeType.ROOM, 5447, HashMap(), f5)
            f5.nodes[5443.0] = MapNode(2412, 885, NodeType.ROOM, 5443, HashMap(), f5)
            f5.nodes[5441.0] = MapNode(2468, 885, NodeType.ROOM, 5441, HashMap(), f5)
            f5.nodes[5442.0] = MapNode(2612, 885, NodeType.ROOM, 5442, HashMap(), f5)
            f5.nodes[5439.0] = MapNode(2615, 885, NodeType.ROOM, 5439, HashMap(), f5)
            f5.nodes[5437.0] = MapNode(2674, 885, NodeType.ROOM, 5437, HashMap(), f5)
            f5.nodes[5438.0] = MapNode(2688, 885, NodeType.ROOM, 5438, HashMap(), f5)
            f5.nodes[5436.0] = MapNode(2775, 885, NodeType.ROOM, 5436, HashMap(), f5)
            addEdge(f5, 5338, 5343.1)
            addEdge(f5, 5343.1, 5342)
            addEdge(f5, 5342, 5344)
            addEdge(f5, 5344, 5353.1)
            addEdge(f5, 5353.1, 5352)
            addEdge(f5, 5352, -5501)
            addEdge(f5, -5501, 5458)
            addEdge(f5, 5458, 5914)
            addEdge(f5, 5914, 5456)
            addEdge(f5, 5456, 5916)
            addEdge(f5, 5916, 5454)
            addEdge(f5, 5454, 5452)
            addEdge(f5, 5452, 5451)
            addEdge(f5, 5451, 5448)
            addEdge(f5, 5448, 5449)
            addEdge(f5, 5449, 5446)
            addEdge(f5, 5446, 5444)
            addEdge(f5, 5444, 5447)
            addEdge(f5, 5447, 5443)
            addEdge(f5, 5443, 5441)
            addEdge(f5, 5441, 5442)
            addEdge(f5, 5442, 5439)
            addEdge(f5, 5439, 5437)
            addEdge(f5, 5437, 5438)
            addEdge(f5, 5438, 5436)

            // ROW 1 X ROW 2
            addEdge(f5, 5324, 5338)
            addEdge(f5, 5434, 5436)

            // Left and right offshoots from Row 2
            f5.nodes[-5201.0] = MapNode(755, 967, NodeType.STAIR, -5201, HashMap(), f5)
            f5.nodes[-5000.0] = MapNode(755, 1347, NodeType.PORT, -5000, HashMap(), f5)
            addEdge(f5, 5343, -5201)
            addEdge(f5, -5000, -5201)

            f5.nodes[-5202.0] = MapNode(2312, 967, NodeType.STAIR, -5202, HashMap(), f5)
            f5.nodes[-5001.0] = MapNode(2312, 1347, NodeType.PORT, -5001, HashMap(), f5)
            addEdge(f5, 5447, -5202)
            addEdge(f5, -5001, -5202)

            // Middle Column
            f5.nodes[-5203.0] = MapNode(1703, 515, NodeType.STAIR, -5203, HashMap(), f5)
            f5.nodes[-5101.0] = MapNode(1703, 651, NodeType.WC, -5101, HashMap(), f5)
            f5.nodes[-5204.0] = MapNode(1703, 1267, NodeType.STAIR, -5204, HashMap(), f5)
            f5.nodes[-5003.0] = MapNode(1703, 1337, NodeType.PORT, -5003, HashMap(), f5)
            addEdge(f5, 5301, -5203)
            addEdge(f5, -5203, -5101)
            addEdge(f5, -5101, -5501)
            addEdge(f5, -5501, -5204)
            addEdge(f5, -5204, -5003)

        }

        // F5 X F4
        run {
            addEdge(1, -5200, -4200) // J
            addEdge(1, -5201, -4201) // H
            addEdge(1, -5203, -4203) // F
            addEdge(1, -5204, -4204) // Bottom centre
            addEdge(1, -5202, -4202) // G
        }

        // F6 INIT

        // ROOMS HAVE NUMBERS -- 6322, 6318
        // PORTS HAVE NEGATIVE 60XX
        // WC HAVE NEGATIVE 61XX
        // STAIRS HAVE NEGATIVE 62XX
        // NONE IS ALL ELSE 6XXX (USUALLY 65XX)

        run {
            // ROW 1

            f6.nodes[6324.0] = MapNode(595, 433, NodeType.ROOM, 6324, HashMap(), f6)
            f6.nodes[6921.0] = MapNode(693, 433, NodeType.ROOM, 6921, HashMap(), f6)
            f6.nodes[6323.0] = MapNode(719, 433, NodeType.ROOM, 6323, HashMap(), f6)
            f6.nodes[-6500.0] = MapNode(756, 433, NodeType.NONE, -6500, HashMap(), f6)
            f6.nodes[6321.0] = MapNode(826, 433, NodeType.ROOM, 6321, HashMap(), f6)
            f6.nodes[6322.0] = MapNode(848, 433, NodeType.ROOM, 6322, HashMap(), f6)
            f6.nodes[6318.0] = MapNode(927, 433, NodeType.ROOM, 6318, HashMap(), f6)
            f6.nodes[6317.0] = MapNode(927, 433, NodeType.ROOM, 6317, HashMap(), f6)
            f6.nodes[6316.0] = MapNode(1002, 433, NodeType.ROOM, 6316, HashMap(), f6)
            f6.nodes[6314.0] = MapNode(1081, 433, NodeType.ROOM, 6314, HashMap(), f6)
            f6.nodes[6349.0] = MapNode(1091, 433, NodeType.ROOM, 6349, HashMap(), f6)
            f6.nodes[6312.0] = MapNode(1156, 433, NodeType.ROOM, 6312, HashMap(), f6)
            f6.nodes[6309.0] = MapNode(1220, 433, NodeType.ROOM, 6309, HashMap(), f6)
            f6.nodes[6308.0] = MapNode(1231, 433, NodeType.ROOM, 6308, HashMap(), f6)
            f6.nodes[6306.0] = MapNode(1305, 433, NodeType.ROOM, 6306, HashMap(), f6)
            f6.nodes[6304.0] = MapNode(1381, 433, NodeType.ROOM, 6304, HashMap(), f6)
            f6.nodes[6302.0] = MapNode(1422, 433, NodeType.ROOM, 6302, HashMap(), f6)
            f6.nodes[6301.0] = MapNode(1703, 433, NodeType.ROOM, 6301, HashMap(), f6)
            f6.nodes[6402.0] = MapNode(1800, 433, NodeType.ROOM, 6402, HashMap(), f6)
            f6.nodes[6404.0] = MapNode(1877, 433, NodeType.ROOM, 6404, HashMap(), f6)
            f6.nodes[6911.0] = MapNode(1919, 433, NodeType.ROOM, 6911, HashMap(), f6)
            f6.nodes[6406.0] = MapNode(1951, 433, NodeType.ROOM, 6406, HashMap(), f6)
            f6.nodes[6408.0] = MapNode(2027, 433, NodeType.ROOM, 6408, HashMap(), f6)
            f6.nodes[6411.0] = MapNode(2043, 433, NodeType.ROOM, 6411, HashMap(), f6)
            f6.nodes[6412.0] = MapNode(2105, 433, NodeType.ROOM, 6412, HashMap(), f6)
            f6.nodes[6417.0] = MapNode(2173, 433, NodeType.ROOM, 6417, HashMap(), f6)
            f6.nodes[6414.0] = MapNode(2180, 433, NodeType.ROOM, 6414, HashMap(), f6)
            f6.nodes[6416.0] = MapNode(2257, 433, NodeType.ROOM, 6416, HashMap(), f6)
            f6.nodes[6418.0] = MapNode(2326, 433, NodeType.ROOM, 6418, HashMap(), f6)
            f6.nodes[6422.0] = MapNode(2408, 433, NodeType.ROOM, 6422, HashMap(), f6)
            f6.nodes[6421.0] = MapNode(2429, 433, NodeType.ROOM, 6421, HashMap(), f6)
            f6.nodes[6424.0] = MapNode(2484, 433, NodeType.ROOM, 6424, HashMap(), f6)
            f6.nodes[6423.0] = MapNode(2528, 433, NodeType.ROOM, 6423, HashMap(), f6)
            f6.nodes[6426.0] = MapNode(2559, 433, NodeType.ROOM, 6426, HashMap(), f6)
            f6.nodes[6427.0] = MapNode(2627, 433, NodeType.ROOM, 6427, HashMap(), f6)
            f6.nodes[6428.0] = MapNode(2637, 433, NodeType.ROOM, 6428, HashMap(), f6)
            f6.nodes[6432.0] = MapNode(2675, 433, NodeType.ROOM, 6432, HashMap(), f6)
            f6.nodes[6434.0] = MapNode(2746, 433, NodeType.ROOM, 6434, HashMap(), f6)
            addEdge(f6, 6324, 6921)
            addEdge(f6, 6921, 6323)
            addEdge(f6, 6323, -6500)
            addEdge(f6, -6500, 6321)
            addEdge(f6, 6321, 6322)
            addEdge(f6, 6322, 6318)
            addEdge(f6, 6318, 6317)
            addEdge(f6, 6317, 6316)
            addEdge(f6, 6316, 6314)
            addEdge(f6, 6314, 6349)
            addEdge(f6, 6349, 6312)
            addEdge(f6, 6312, 6309)
            addEdge(f6, 6309, 6308)
            addEdge(f6, 6308, 6306)
            addEdge(f6, 6306, 6304)
            addEdge(f6, 6304, 6302)
            addEdge(f6, 6302, 6301)
            addEdge(f6, 6301, 6402)
            addEdge(f6, 6402, 6404)
            addEdge(f6, 6404, 6911)
            addEdge(f6, 6911, 6406)
            addEdge(f6, 6406, 6408)
            addEdge(f6, 6408, 6411)
            addEdge(f6, 6411, 6412)
            addEdge(f6, 6412, 6417)
            addEdge(f6, 6417, 6414)
            addEdge(f6, 6414, 6416)
            addEdge(f6, 6416, 6418)
            addEdge(f6, 6418, 6422)
            addEdge(f6, 6422, 6421)
            addEdge(f6, 6421, 6424)
            addEdge(f6, 6424, 6423)
            addEdge(f6, 6423, 6426)
            addEdge(f6, 6426, 6427)
            addEdge(f6, 6427, 6428)
            addEdge(f6, 6428, 6432)
            addEdge(f6, 6432, 6434)
            f6.nodes[6919.0] = MapNode(756, 370, NodeType.ROOM, 6919, HashMap(), f6)
            f6.nodes[-6200.0] = MapNode(756, 354, NodeType.STAIR, -6200, HashMap(), f6)
            addEdge(f6, -6500, 6919)
            addEdge(f6, 6919, -6200)

            // COLUMN 1

            f6.nodes[6326.0] = MapNode(595, 433, NodeType.ROOM, 6326, HashMap(), f6)
            f6.nodes[6328.0] = MapNode(595, 521, NodeType.ROOM, 6328, HashMap(), f6)
            f6.nodes[6332.0] = MapNode(595, 680, NodeType.ROOM, 6332, HashMap(), f6)
            f6.nodes[6333.0] = MapNode(595, 742, NodeType.ROOM, 6333, HashMap(), f6)
            f6.nodes[6334.0] = MapNode(595, 759, NodeType.ROOM, 6334, HashMap(), f6)
            f6.nodes[6336.0] = MapNode(595, 840, NodeType.ROOM, 6336, HashMap(), f6)
            f6.nodes[6338.0] = MapNode(595, 876, NodeType.ROOM, 6338, HashMap(), f6)
            addEdge(f6, 6326, 6328)
            addEdge(f6, 6328, 6332)
            addEdge(f6, 6332, 6333)
            addEdge(f6, 6333, 6334)
            addEdge(f6, 6334, 6336)
            addEdge(f6, 6336, 6338)

            // ROW 1 X COLUMN 1
            addEdge(f6, 6324, 6326)

            // ROW 2
            f6.nodes[6339.0] = MapNode(757, 876, NodeType.ROOM, 6339, HashMap(), f6)
            f6.nodes[6342.0] = MapNode(851, 876, NodeType.ROOM, 6342, HashMap(), f6)
            f6.nodes[6343.0] = MapNode(862, 876, NodeType.ROOM, 6343, HashMap(), f6)
            f6.nodes[6344.0] = MapNode(927, 876, NodeType.ROOM, 6344, HashMap(), f6)
            f6.nodes[6346.0] = MapNode(1002, 876, NodeType.ROOM, 6346, HashMap(), f6)
            f6.nodes[6349.1] = MapNode(1067, 876, NodeType.ROOM, 6349, HashMap(), f6)
            f6.nodes[6348.0] = MapNode(1080, 876, NodeType.ROOM, 6348, HashMap(), f6)
            f6.nodes[6352.0] = MapNode(1154, 876, NodeType.ROOM, 6352, HashMap(), f6)
            f6.nodes[6353.0] = MapNode(1172, 876, NodeType.ROOM, 6353, HashMap(), f6)
            f6.nodes[6354.0] = MapNode(1232, 876, NodeType.ROOM, 6354, HashMap(), f6)
            f6.nodes[6356.0] = MapNode(1307, 876, NodeType.ROOM, 6356, HashMap(), f6)
            f6.nodes[6358.0] = MapNode(1381, 876, NodeType.ROOM, 6358, HashMap(), f6)
            f6.nodes[6362.0] = MapNode(1422, 876, NodeType.ROOM, 6362, HashMap(), f6)
            f6.nodes[6364.0] = MapNode(1542, 876, NodeType.ROOM, 6364, HashMap(), f6)
            f6.nodes[-6501.0] = MapNode(1703, 876, NodeType.NONE, -6501, HashMap(), f6)
            f6.nodes[6472.0] = MapNode(1801, 876, NodeType.ROOM, 6472, HashMap(), f6)
            f6.nodes[6914.0] = MapNode(1835, 876, NodeType.ROOM, 6914, HashMap(), f6)
            f6.nodes[6468.0] = MapNode(1876, 876, NodeType.ROOM, 6468, HashMap(), f6)
            f6.nodes[6916.0] = MapNode(1944, 876, NodeType.ROOM, 6916, HashMap(), f6)
            f6.nodes[6466.0] = MapNode(1954, 876, NodeType.ROOM, 6466, HashMap(), f6)
            f6.nodes[6464.0] = MapNode(2029, 876, NodeType.ROOM, 6464, HashMap(), f6)
            f6.nodes[6459.0] = MapNode(2041, 876, NodeType.ROOM, 6459, HashMap(), f6)
            f6.nodes[6462.0] = MapNode(2104, 876, NodeType.ROOM, 6462, HashMap(), f6)
            f6.nodes[6457.0] = MapNode(2173, 876, NodeType.ROOM, 6457, HashMap(), f6)
            f6.nodes[6458.0] = MapNode(2375, 876, NodeType.ROOM, 6458, HashMap(), f6)
            f6.nodes[6456.0] = MapNode(2260, 876, NodeType.ROOM, 6456, HashMap(), f6)
            f6.nodes[6453.0] = MapNode(2313, 876, NodeType.ROOM, 6453, HashMap(), f6)
            f6.nodes[6447.0] = MapNode(2527, 876, NodeType.ROOM, 6447, HashMap(), f6)
            f6.nodes[6454.0] = MapNode(2613, 876, NodeType.ROOM, 6454, HashMap(), f6)
            f6.nodes[6452.0] = MapNode(2675, 876, NodeType.ROOM, 6452, HashMap(), f6)
            f6.nodes[6448.0] = MapNode(2734, 876, NodeType.ROOM, 6448, HashMap(), f6)
            addEdge(f6, 6339, 6342)
            addEdge(f6, 6342, 6343)
            addEdge(f6, 6343, 6344)
            addEdge(f6, 6344, 6346)
            addEdge(f6, 6346, 6349.1)
            addEdge(f6, 6349.1, 6348)
            addEdge(f6, 6348, 6352)
            addEdge(f6, 6352, 6353)
            addEdge(f6, 6353, 6354)
            addEdge(f6, 6354, 6356)
            addEdge(f6, 6356, 6358)
            addEdge(f6, 6358, 6362)
            addEdge(f6, 6362, 6364)
            addEdge(f6, 6364, -6501)
            addEdge(f6, -6501, 6472)
            addEdge(f6, 6472, 6914)
            addEdge(f6, 6914, 6468)
            addEdge(f6, 6468, 6916)
            addEdge(f6, 6916, 6466)
            addEdge(f6, 6466, 6464)
            addEdge(f6, 6464, 6459)
            addEdge(f6, 6459, 6462)
            addEdge(f6, 6462, 6457)
            addEdge(f6, 6457, 6458)
            addEdge(f6, 6458, 6456)
            addEdge(f6, 6456, 6453)
            addEdge(f6, 6453, 6447)
            addEdge(f6, 6447, 6454)
            addEdge(f6, 6454, 6452)
            addEdge(f6, 6452, 6448)

            // COLUMN 1 X ROW 2
            addEdge(f6, 6338, 6339)

            // Left and right offshoots from Row 2
            f6.nodes[-6201.0] = MapNode(757, 967, NodeType.STAIR, -6201, HashMap(), f6)
            f6.nodes[-6000.0] = MapNode(757, 1347, NodeType.PORT, -6000, HashMap(), f6)
            addEdge(f6, 6339, -6201)
            addEdge(f6, -6000, -6201)

            f6.nodes[-6202.0] = MapNode(2313, 967, NodeType.STAIR, -6202, HashMap(), f6)
            f6.nodes[-6001.0] = MapNode(2313, 1347, NodeType.PORT, -6001, HashMap(), f6)
            addEdge(f6, 6453, -6202)
            addEdge(f6, -6001, -6202)

            // COLUMN 2
            f6.nodes[-6203.0] = MapNode(1703, 515, NodeType.STAIR, -6203, HashMap(), f6)
            f6.nodes[-6101.0] = MapNode(1703, 651, NodeType.WC, -6101, HashMap(), f6)
            f6.nodes[6303.0] = MapNode(1482, 651, NodeType.ROOM, 6303, HashMap(), f6)
            f6.nodes[-6204.0] = MapNode(1703, 1267, NodeType.STAIR, -6204, HashMap(), f6)
            f6.nodes[-6003.0] = MapNode(1703, 1337, NodeType.PORT, -6003, HashMap(), f6)
            addEdge(f6, 6301, -6203)
            addEdge(f6, -6203, -6101)
            addEdge(f6, -6101, -6501)
            addEdge(f6, -6101, 6303)
            addEdge(f6, -6501, -6204)
            addEdge(f6, -6204, -6003)

            // COLUMN 3

            f6.nodes[6436.0] = MapNode(2675, 433, NodeType.ROOM, 6436, HashMap(), f6)
            f6.nodes[6438.0] = MapNode(2675, 521, NodeType.ROOM, 6438, HashMap(), f6)
            f6.nodes[6442.0] = MapNode(2675, 680, NodeType.ROOM, 6442, HashMap(), f6)
            f6.nodes[6443.0] = MapNode(2675, 742, NodeType.ROOM, 6443, HashMap(), f6)
            f6.nodes[6444.0] = MapNode(2675, 759, NodeType.ROOM, 6444, HashMap(), f6)
            f6.nodes[6446.0] = MapNode(2675, 840, NodeType.ROOM, 6446, HashMap(), f6)
            addEdge(f6, 6436, 6438)
            addEdge(f6, 6438, 6442)
            addEdge(f6, 6442, 6443)
            addEdge(f6, 6443, 6444)
            addEdge(f6, 6444, 6446)

            // Join with rows
            addEdge(f6, 6432, 6436)
            addEdge(f6, 6446, 6452)

        }

        // F5 X F6
        run {
            addEdge(1, -5200, -6200) // J
            addEdge(1, -5201, -6201) // H
            addEdge(1, -5203, -6203) // F
            addEdge(1, -5204, -6204) // Bottom centre
            addEdge(1, -5202, -6202) // G
        }

        // F7 INIT

        // ROOMS HAVE NUMBERS -- 7316, 7318
        // PORTS HAVE NEGATIVE 70XX
        // WC HAVE NEGATIVE 71XX
        // STAIRS HAVE NEGATIVE 72XX
        // NONE IS ALL ELSE 7XXX (USUALLY 75XX)

        run {
            // ROW 1

            f7.nodes[7921.0] = MapNode(602, 433, NodeType.ROOM, 7921, HashMap(), f7)
            f7.nodes[7318.0] = MapNode(766, 433, NodeType.ROOM, 7318, HashMap(), f7)
            f7.nodes[7321.0] = MapNode(766, 433, NodeType.ROOM, 7321, HashMap(), f7)
            f7.nodes[7316.0] = MapNode(851, 433, NodeType.ROOM, 7316, HashMap(), f7)
            f7.nodes[7319.0] = MapNode(857, 433, NodeType.ROOM, 7319, HashMap(), f7)
            f7.nodes[7314.0] = MapNode(927, 433, NodeType.ROOM, 7314, HashMap(), f7)
            f7.nodes[7317.0] = MapNode(940, 433, NodeType.ROOM, 7317, HashMap(), f7)
            f7.nodes[7312.0] = MapNode(990, 433, NodeType.ROOM, 7312, HashMap(), f7)
            f7.nodes[7308.0] = MapNode(1041, 433, NodeType.ROOM, 7308, HashMap(), f7)
            f7.nodes[7306.0] = MapNode(1246, 433, NodeType.ROOM, 7306, HashMap(), f7)
            f7.nodes[7919.0] = MapNode(1246, 433, NodeType.ROOM, 7919, HashMap(), f7)
            f7.nodes[7304.0] = MapNode(1357, 433, NodeType.ROOM, 7304, HashMap(), f7)
            f7.nodes[7302.0] = MapNode(1394, 433, NodeType.ROOM, 7302, HashMap(), f7)
            f7.nodes[7301.0] = MapNode(1630, 433, NodeType.ROOM, 7301, HashMap(), f7)
            f7.nodes[7402.0] = MapNode(1800, 433, NodeType.ROOM, 7402, HashMap(), f7)
            f7.nodes[7404.0] = MapNode(1874, 433, NodeType.ROOM, 7404, HashMap(), f7)
            f7.nodes[7911.0] = MapNode(1918, 433, NodeType.ROOM, 7911, HashMap(), f7)
            f7.nodes[7406.0] = MapNode(1953, 433, NodeType.ROOM, 7406, HashMap(), f7)
            f7.nodes[7408.0] = MapNode(2027, 433, NodeType.ROOM, 7408, HashMap(), f7)
            f7.nodes[7409.0] = MapNode(2058, 433, NodeType.ROOM, 7409, HashMap(), f7)
            f7.nodes[7414.0] = MapNode(2143, 433, NodeType.ROOM, 7414, HashMap(), f7)
            f7.nodes[-7500.0] = MapNode(2317, 433, NodeType.NONE, -7500, HashMap(), f7)
            f7.nodes[7418.0] = MapNode(2408, 433, NodeType.ROOM, 7418, HashMap(), f7)
            f7.nodes[7419.0] = MapNode(2478, 433, NodeType.ROOM, 7419, HashMap(), f7)
            f7.nodes[7421.0] = MapNode(2553, 433, NodeType.ROOM, 7421, HashMap(), f7)
            f7.nodes[7422.0] = MapNode(2561, 433, NodeType.ROOM, 7422, HashMap(), f7)
            f7.nodes[7424.0] = MapNode(2598, 433, NodeType.ROOM, 7424, HashMap(), f7)
            f7.nodes[7423.0] = MapNode(2629, 433, NodeType.ROOM, 7423, HashMap(), f7)
            f7.nodes[7426.0] = MapNode(2675, 433, NodeType.ROOM, 7426, HashMap(), f7)
            addEdge(f7, 7921, 7318)
            addEdge(f7, 7318, 7321)
            addEdge(f7, 7321, 7316)
            addEdge(f7, 7316, 7319)
            addEdge(f7, 7319, 7314)
            addEdge(f7, 7314, 7317)
            addEdge(f7, 7317, 7312)
            addEdge(f7, 7312, 7308)
            addEdge(f7, 7308, 7306)
            addEdge(f7, 7306, 7919)
            addEdge(f7, 7919, 7304)
            addEdge(f7, 7304, 7302)
            addEdge(f7, 7302, 7301)
            addEdge(f7, 7301, 7402)
            addEdge(f7, 7402, 7404)
            addEdge(f7, 7404, 7911)
            addEdge(f7, 7911, 7406)
            addEdge(f7, 7406, 7408)
            addEdge(f7, 7408, 7409)
            addEdge(f7, 7409, 7414)
            addEdge(f7, 7414, -7500)
            addEdge(f7, -7500, 7418)
            addEdge(f7, 7418, 7419)
            addEdge(f7, 7419, 7421)
            addEdge(f7, 7421, 7422)
            addEdge(f7, 7422, 7424)
            addEdge(f7, 7424, 7423)
            addEdge(f7, 7423, 7426)
            f7.nodes[7922.0] = MapNode(600, 304, NodeType.ROOM, 7922, HashMap(), f7)
            f7.nodes[7324.0] = MapNode(564, 304, NodeType.ROOM, 7324, HashMap(), f7)
            f7.nodes[-7503.0] = MapNode(766, 304, NodeType.NONE, -7503, HashMap(), f7)
            addEdge(f7, 7324, 7922)
            addEdge(f7, 7922, -7503)
            addEdge(f7, -7503, 7318)
            f7.nodes[7412.0] = MapNode(2143, 386, NodeType.ROOM, 7412, HashMap(), f7)
            f7.nodes[7416.0] = MapNode(2143, 386, NodeType.ROOM, 7416, HashMap(), f7)
            addEdge(f7, 7414, 7412)
            addEdge(f7, 7412, 7416)

            // ROW 2

            f7.nodes[7338.0] = MapNode(602, 880, NodeType.ROOM, 7338, HashMap(), f7)
            f7.nodes[7337.0] = MapNode(691, 880, NodeType.ROOM, 7337, HashMap(), f7)
            f7.nodes[7339.0] = MapNode(752, 880, NodeType.ROOM, 7339, HashMap(), f7)
            f7.nodes[7341.0] = MapNode(857, 880, NodeType.ROOM, 7341, HashMap(), f7)
            f7.nodes[7342.0] = MapNode(898, 880, NodeType.ROOM, 7342, HashMap(), f7)
            f7.nodes[7343.0] = MapNode(938, 880, NodeType.ROOM, 7343, HashMap(), f7)
            f7.nodes[7344.0] = MapNode(990, 880, NodeType.ROOM, 7344, HashMap(), f7)
            f7.nodes[7346.0] = MapNode(1077, 880, NodeType.ROOM, 7346, HashMap(), f7)
            f7.nodes[7349.0] = MapNode(1144, 880, NodeType.ROOM, 7349, HashMap(), f7)
            f7.nodes[7348.0] = MapNode(1154, 880, NodeType.ROOM, 7348, HashMap(), f7)
            f7.nodes[7363.0] = MapNode(1223, 880, NodeType.ROOM, 7363, HashMap(), f7)
            f7.nodes[7352.0] = MapNode(1225, 880, NodeType.ROOM, 7352, HashMap(), f7)
            f7.nodes[7354.0] = MapNode(1305, 880, NodeType.ROOM, 7354, HashMap(), f7)
            f7.nodes[7356.0] = MapNode(1382, 880, NodeType.ROOM, 7356, HashMap(), f7)
            f7.nodes[7358.0] = MapNode(1458, 880, NodeType.ROOM, 7358, HashMap(), f7)
            f7.nodes[7362.0] = MapNode(1498, 880, NodeType.ROOM, 7362, HashMap(), f7)
            f7.nodes[-7501.0] = MapNode(1630, 880, NodeType.NONE, -7501, HashMap(), f7)
            f7.nodes[7462.0] = MapNode(1799, 880, NodeType.ROOM, 7462, HashMap(), f7)
            f7.nodes[7914.0] = MapNode(1832, 880, NodeType.ROOM, 7914, HashMap(), f7)
            f7.nodes[7458.0] = MapNode(1877, 880, NodeType.ROOM, 7458, HashMap(), f7)
            f7.nodes[7916.0] = MapNode(1942, 880, NodeType.ROOM, 7916, HashMap(), f7)
            f7.nodes[7456.0] = MapNode(1950, 880, NodeType.ROOM, 7456, HashMap(), f7)
            f7.nodes[7454.0] = MapNode(2028, 880, NodeType.ROOM, 7454, HashMap(), f7)
            f7.nodes[7453.0] = MapNode(2080, 880, NodeType.ROOM, 7453, HashMap(), f7)
            f7.nodes[7452.0] = MapNode(2104, 880, NodeType.ROOM, 7452, HashMap(), f7)
            f7.nodes[7451.0] = MapNode(2176, 880, NodeType.ROOM, 7451, HashMap(), f7)
            f7.nodes[7448.0] = MapNode(2188, 880, NodeType.ROOM, 7448, HashMap(), f7)
            f7.nodes[7449.0] = MapNode(2214, 880, NodeType.ROOM, 7449, HashMap(), f7)
            f7.nodes[7446.0] = MapNode(2268, 880, NodeType.ROOM, 7446, HashMap(), f7)
            f7.nodes[-7502.0] = MapNode(2317, 880, NodeType.NONE, -7502, HashMap(), f7)
            f7.nodes[7447.0] = MapNode(2402, 880, NodeType.ROOM, 7447, HashMap(), f7)
            f7.nodes[7443.0] = MapNode(2476, 880, NodeType.ROOM, 7443, HashMap(), f7)
            f7.nodes[7441.0] = MapNode(2553, 880, NodeType.ROOM, 7441, HashMap(), f7)
            f7.nodes[7444.0] = MapNode(2611, 880, NodeType.ROOM, 7444, HashMap(), f7)
            f7.nodes[7442.0] = MapNode(2675, 880, NodeType.ROOM, 7442, HashMap(), f7)
            f7.nodes[7917.0] = MapNode(2732, 880, NodeType.ROOM, 7917, HashMap(), f7)
            f7.nodes[7438.0] = MapNode(2764, 880, NodeType.ROOM, 7438, HashMap(), f7)
            addEdge(f7, 7338, 7337)
            addEdge(f7, 7337, 7339)
            addEdge(f7, 7339, 7341)
            addEdge(f7, 7341, 7342)
            addEdge(f7, 7342, 7343)
            addEdge(f7, 7343, 7344)
            addEdge(f7, 7344, 7346)
            addEdge(f7, 7346, 7349)
            addEdge(f7, 7349, 7348)
            addEdge(f7, 7348, 7363)
            addEdge(f7, 7363, 7352)
            addEdge(f7, 7352, 7354)
            addEdge(f7, 7354, 7356)
            addEdge(f7, 7356, 7358)
            addEdge(f7, 7358, 7362)
            addEdge(f7, 7362, -7501)
            addEdge(f7, -7501, 7462)
            addEdge(f7, 7462, 7914)
            addEdge(f7, 7914, 7458)
            addEdge(f7, 7458, 7916)
            addEdge(f7, 7916, 7456)
            addEdge(f7, 7456, 7454)
            addEdge(f7, 7454, 7453)
            addEdge(f7, 7453, 7452)
            addEdge(f7, 7452, 7451)
            addEdge(f7, 7451, 7448)
            addEdge(f7, 7448, 7449)
            addEdge(f7, 7449, 7446)
            addEdge(f7, 7446, -7502)
            addEdge(f7, -7502, 7447)
            addEdge(f7, 7447, 7443)
            addEdge(f7, 7443, 7441)
            addEdge(f7, 7441, 7444)
            addEdge(f7, 7444, 7442)
            addEdge(f7, 7442, 7917)
            addEdge(f7, 7917, 7438)
            f7.nodes[-7200.0] = MapNode(752, 965, NodeType.STAIR, -7200, HashMap(), f7)
            addEdge(f7, 7339, -7200)

            // COLUMNS

            f7.nodes[7326.0] = MapNode(602, 537, NodeType.ROOM, 7326, HashMap(), f7)
            f7.nodes[7328.0] = MapNode(602, 572, NodeType.ROOM, 7328, HashMap(), f7)
            f7.nodes[7331.0] = MapNode(602, 656, NodeType.ROOM, 7331, HashMap(), f7)
            f7.nodes[7334.0] = MapNode(602, 755, NodeType.ROOM, 7334, HashMap(), f7)
            f7.nodes[7336.0] = MapNode(602, 834, NodeType.ROOM, 7336, HashMap(), f7)
            addEdge(f7, 7921, 7326)
            addEdge(f7, 7326, 7328)
            addEdge(f7, 7328, 7331)
            addEdge(f7, 7331, 7334)
            addEdge(f7, 7334, 7336)
            addEdge(f7, 7336, 7338)

            f7.nodes[7353.0] = MapNode(990, 558, NodeType.ROOM, 7353, HashMap(), f7)
            f7.nodes[7347.0] = MapNode(990, 618, NodeType.ROOM, 7347, HashMap(), f7)
            f7.nodes[7351.0] = MapNode(990, 618, NodeType.ROOM, 7351, HashMap(), f7)
            addEdge(f7, 7312, 7353)
            addEdge(f7, 7353, 7347)
            addEdge(f7, 7347, 7351)
            addEdge(f7, 7351, 7344)

            f7.nodes[-7201.0] = MapNode(1630, 513, NodeType.STAIR, -7201, HashMap(), f7)
            f7.nodes[7303.0] = MapNode(1630, 626, NodeType.ROOM, 7303, HashMap(), f7)
            f7.nodes[-7100.0] = MapNode(1630, 649, NodeType.WC, -7100, HashMap(), f7)
            f7.nodes[7363.1] = MapNode(1630, 719, NodeType.ROOM, 7363, HashMap(), f7)
            f7.nodes[-7202.0] = MapNode(1630, 1017, NodeType.STAIR, -7201, HashMap(), f7)
            addEdge(f7, 7301, -7201)
            addEdge(f7, -7201, 7303)
            addEdge(f7, 7303, -7100)
            addEdge(f7, -7100, 7363.1)
            addEdge(f7, 7363.1, -7501)
            addEdge(f7, -7501, -7202)

            f7.nodes[7417.0] = MapNode(2317, 529, NodeType.ROOM, 7417, HashMap(), f7)
            f7.nodes[7411.0] = MapNode(2317, 655, NodeType.ROOM, 7411, HashMap(), f7)
            f7.nodes[7413.0] = MapNode(2317, 655, NodeType.ROOM, 7413, HashMap(), f7)
            f7.nodes[-7203.0] = MapNode(2317, 963, NodeType.STAIR, -7203, HashMap(), f7)
            addEdge(f7, -7500, 7417)
            addEdge(f7, 7417, 7411)
            addEdge(f7, 7411, 7413)
            addEdge(f7, 7413, -7502)
            addEdge(f7, -7502, -7203)

            f7.nodes[7428.0] = MapNode(2675, 600, NodeType.ROOM, 7428, HashMap(), f7)
            f7.nodes[7431.0] = MapNode(2675, 605, NodeType.ROOM, 7431, HashMap(), f7)
            f7.nodes[7432.0] = MapNode(2675, 637, NodeType.ROOM, 7332, HashMap(), f7)
            f7.nodes[7434.0] = MapNode(2675, 716, NodeType.ROOM, 7434, HashMap(), f7)
            f7.nodes[7433.0] = MapNode(2675, 748, NodeType.ROOM, 7433, HashMap(), f7)
            f7.nodes[7436.0] = MapNode(2675, 794, NodeType.ROOM, 7436, HashMap(), f7)
            addEdge(f7, 7426, 7428)
            addEdge(f7, 7428, 7431)
            addEdge(f7, 7431, 7432)
            addEdge(f7, 7432, 7434)
            addEdge(f7, 7434, 7433)
            addEdge(f7, 7433, 7436)
            addEdge(f7, 7436, 7442)

        }

        // F7 X F6
        run {
            addEdge(1, -7200, -6201) // H
            addEdge(1, -7201, -6203) // F
            addEdge(1, -7202, -6204) // Bottom centre
            addEdge(1, -7203, -6202) // G
        }

        // E6

        buildings[2] = BuildingMap(
            2,
            HashMap(),
            HashMap(),
            305 // TODO: Update this
        )

        buildings[2]!!.plans[1] = FloorMap(
            1,
            HashMap(),
            R.drawable.e6f1,
            buildings[2]!!
        )

        val e6f1 = buildings[2]!!.plans[1]!!

        buildings[2]!!.plans[2] = FloorMap(
            2,
            HashMap(),
            R.drawable.e6f2,
            buildings[2]!!
        )

        val e6f2 = buildings[2]!!.plans[2]!!

        buildings[2]!!.plans[3] = FloorMap(
            3,
            HashMap(),
            R.drawable.e6f3,
            buildings[2]!!
        )

        val e6f3 = buildings[2]!!.plans[3]!!

        buildings[2]!!.plans[4] = FloorMap(
            4,
            HashMap(),
            R.drawable.e6f4,
            buildings[2]!!
        )

        val e6f4 = buildings[2]!!.plans[4]!!

        buildings[2]!!.plans[5] = FloorMap(
            5,
            HashMap(),
            R.drawable.e6f5,
            buildings[2]!!
        )

        val e6f5 = buildings[2]!!.plans[5]!!

        // F1 INIT

        // ROOMS HAVE NUMBERS -- 1002, 1004
        // PORTS HAVE NEGATIVE 10XX
        // WC HAVE NEGATIVE 11XX
        // STAIRS HAVE NEGATIVE 12XX
        // NONE IS ALL ELSE 1XXX (USUALLY 15XX)

        run {
            // ROW 1 (TOPMOST)

            e6f1.nodes[-1000.0] = MapNode(907, 389, NodeType.PORT, -1000, HashMap(), e6f1)
            e6f1.nodes[-1200.0] = MapNode(1034, 389, NodeType.STAIR, -1200, HashMap(), e6f1)
            e6f1.nodes[1907.0] = MapNode(1045, 389, NodeType.ROOM, 1907, HashMap(), e6f1)
            e6f1.nodes[-1100.0] = MapNode(1115, 389, NodeType.ROOM, -1100, HashMap(), e6f1)
            e6f1.nodes[1102.0] = MapNode(1244, 389, NodeType.ROOM, 1102, HashMap(), e6f1)
            e6f1.nodes[1103.0] = MapNode(1318, 389, NodeType.ROOM, 1103, HashMap(), e6f1)
            e6f1.nodes[1104.0] = MapNode(1355, 389, NodeType.ROOM, 1104, HashMap(), e6f1)
            e6f1.nodes[1106.0] = MapNode(1462, 389, NodeType.ROOM, 1106, HashMap(), e6f1)
            e6f1.nodes[1108.0] = MapNode(1570, 389, NodeType.ROOM, 1108, HashMap(), e6f1)
            e6f1.nodes[1110.0] = MapNode(1675, 389, NodeType.ROOM, 1110, HashMap(), e6f1)
            e6f1.nodes[1112.0] = MapNode(1718, 389, NodeType.ROOM, 1112, HashMap(), e6f1)
            e6f1.nodes[1109.0] = MapNode(1748, 389, NodeType.ROOM, 1109, HashMap(), e6f1)
            e6f1.nodes[1114.0] = MapNode(1826, 389, NodeType.ROOM, 1114, HashMap(), e6f1)
            e6f1.nodes[1116.0] = MapNode(1936, 389, NodeType.ROOM, 1116, HashMap(), e6f1)
            e6f1.nodes[1113.0] = MapNode(1968, 389, NodeType.ROOM, 1113, HashMap(), e6f1)
            e6f1.nodes[1118.0] = MapNode(2043, 389, NodeType.ROOM, 1118, HashMap(), e6f1)
            e6f1.nodes[1119.0] = MapNode(2075, 389, NodeType.ROOM, 1119, HashMap(), e6f1)
            e6f1.nodes[1120.0] = MapNode(2152, 389, NodeType.ROOM, 1120, HashMap(), e6f1)
            e6f1.nodes[-1201.0] = MapNode(2272, 389, NodeType.STAIR, -1201, HashMap(), e6f1)
            addEdge(e6f1, -1000, -1200)
            addEdge(e6f1, -1200, 1907)
            addEdge(e6f1, 1907, -1100)
            addEdge(e6f1, -1100, 1102)
            addEdge(e6f1, 1102, 1103)
            addEdge(e6f1, 1103, 1104)
            addEdge(e6f1, 1104, 1106)
            addEdge(e6f1, 1106, 1108)
            addEdge(e6f1, 1108, 1110)
            addEdge(e6f1, 1110, 1112)
            addEdge(e6f1, 1112, 1109)
            addEdge(e6f1, 1109, 1114)
            addEdge(e6f1, 1114, 1116)
            addEdge(e6f1, 1116, 1113)
            addEdge(e6f1, 1113, 1118)
            addEdge(e6f1, 1118, 1119)
            addEdge(e6f1, 1119, 1120)
            addEdge(e6f1, 1120, -1201)
            e6f1.nodes[-1001.0] = MapNode(1034, 243, NodeType.PORT, -1001, HashMap(), e6f1)
            addEdge(e6f1, -1001, -1200)
            e6f1.nodes[-1002.0] = MapNode(2272, 243, NodeType.PORT, -1002, HashMap(), e6f1)
            addEdge(e6f1, -1002, -1201)

            // ROW 2
            e6f1.nodes[-1003.0] = MapNode(907, 1213, NodeType.PORT, -1003, HashMap(), e6f1)
            e6f1.nodes[-1202.0] = MapNode(1034, 1213, NodeType.STAIR, -1202, HashMap(), e6f1)
            e6f1.nodes[1901.0] = MapNode(1045, 1213, NodeType.ROOM, 1901, HashMap(), e6f1)
            e6f1.nodes[-1101.0] = MapNode(1115, 1213, NodeType.ROOM, -1101, HashMap(), e6f1)
            e6f1.nodes[1903.0] = MapNode(1244, 1213, NodeType.ROOM, 1903, HashMap(), e6f1)
            e6f1.nodes[1003.0] = MapNode(1318, 1213, NodeType.ROOM, 1003, HashMap(), e6f1)
            e6f1.nodes[1002.0] = MapNode(1355, 1213, NodeType.ROOM, 1002, HashMap(), e6f1)
            e6f1.nodes[1004.0] = MapNode(1462, 1213, NodeType.ROOM, 1004, HashMap(), e6f1)
            e6f1.nodes[1006.0] = MapNode(1570, 1213, NodeType.ROOM, 1006, HashMap(), e6f1)
            e6f1.nodes[1008.0] = MapNode(1675, 1213, NodeType.ROOM, 1008, HashMap(), e6f1)
            e6f1.nodes[1009.0] = MapNode(1748, 1213, NodeType.ROOM, 1009, HashMap(), e6f1)
            e6f1.nodes[1010.0] = MapNode(1786, 1213, NodeType.ROOM, 1010, HashMap(), e6f1)
            e6f1.nodes[1012.0] = MapNode(1894, 1213, NodeType.ROOM, 1012, HashMap(), e6f1)
            e6f1.nodes[1013.0] = MapNode(1973, 1213, NodeType.ROOM, 1013, HashMap(), e6f1)
            e6f1.nodes[1909.0] = MapNode(2154, 1213, NodeType.ROOM, 1909, HashMap(), e6f1)
            e6f1.nodes[1017.0] = MapNode(2182, 1213, NodeType.ROOM, 1017, HashMap(), e6f1)
            e6f1.nodes[-1203.0] = MapNode(2272, 1213, NodeType.STAIR, -1203, HashMap(), e6f1)
            addEdge(e6f1, -1003, -1202)
            addEdge(e6f1, -1202, 1901)
            addEdge(e6f1, 1901, -1101)
            addEdge(e6f1, -1101, 1903)
            addEdge(e6f1, 1903, 1003)
            addEdge(e6f1, 1003, 1002)
            addEdge(e6f1, 1002, 1004)
            addEdge(e6f1, 1004, 1008)
            addEdge(e6f1, 1008, 1009)
            addEdge(e6f1, 1009, 1010)
            addEdge(e6f1, 1010, 1012)
            addEdge(e6f1, 1012, 1013)
            addEdge(e6f1, 1013, 1909)
            addEdge(e6f1, 1909, 1017)
            addEdge(e6f1, 1017, -1203)
            e6f1.nodes[-1004.0] = MapNode(1034, 1361, NodeType.PORT, -1004, HashMap(), e6f1)
            addEdge(e6f1, -1004, -1202)
            e6f1.nodes[-1005.0] = MapNode(2272, 1361, NodeType.PORT, -1005, HashMap(), e6f1)
            addEdge(e6f1, -1005, -1203)

            // Rightmost section

            e6f1.nodes[1122.0] = MapNode(2272, 531, NodeType.ROOM, 1122, HashMap(), e6f1)
            e6f1.nodes[-1500.0] = MapNode(2272, 911, NodeType.NONE, -1500, HashMap(), e6f1)
            e6f1.nodes[1021.0] = MapNode(2388, 911, NodeType.ROOM, 1021, HashMap(), e6f1)
            addEdge(e6f1, 1122, -1500)
            addEdge(e6f1, -1500, 1021)

            e6f1.nodes[-1006.0] = MapNode(2460, 816, NodeType.PORT, -1006, HashMap(), e6f1)
            e6f1.nodes[1022.0] = MapNode(2460, 816, NodeType.ROOM, 1022, HashMap(), e6f1)
            e6f1.nodes[1911.0] = MapNode(2460, 911, NodeType.ROOM, 1911, HashMap(), e6f1)
            e6f1.nodes[1024.0] = MapNode(2460, 1060, NodeType.ROOM, 1024, HashMap(), e6f1)
            e6f1.nodes[1026.0] = MapNode(2460, 1155, NodeType.ROOM, 1026, HashMap(), e6f1)
            e6f1.nodes[-1007.0] = MapNode(2460, 1155, NodeType.PORT, -1006, HashMap(), e6f1)
            addEdge(e6f1, -1006, 1022)
            addEdge(e6f1, 1022, 1911)
            addEdge(e6f1, 1911, 1024)
            addEdge(e6f1, 1024, 1026)
            addEdge(e6f1, 1026, -1007)

            addEdge(e6f1, 1021, 1911)

            // Connect components

            addEdge(e6f1, -1000, -1003)
            addEdge(e6f1, -1002, 1122)
            addEdge(e6f1, -1005, -1500)

        }

        // F2 INIT

        // ROOMS HAVE NUMBERS -- 2002, 2004
        // PORTS HAVE NEGATIVE 20XX
        // WC HAVE NEGATIVE 21XX
        // STAIRS HAVE NEGATIVE 22XX
        // NONE IS ALL ELSE 1XXX (USUALLY 25XX)

        run {
            // ROW 1 (TOPMOST)

            e6f2.nodes[-2500.0] = MapNode(965, 494, NodeType.NONE, -2500, HashMap(), e6f2)
            e6f2.nodes[-2200.0] = MapNode(1040, 494, NodeType.STAIR, -2200, HashMap(), e6f2)
            e6f2.nodes[2907.0] = MapNode(1046, 494, NodeType.ROOM, 2907, HashMap(), e6f2)
            e6f2.nodes[-2100.0] = MapNode(1118, 494, NodeType.WC, -2100, HashMap(), e6f2)
            e6f2.nodes[2909.0] = MapNode(1196, 494, NodeType.ROOM, 2909, HashMap(), e6f2)
            e6f2.nodes[2102.0] = MapNode(1249, 494, NodeType.ROOM, 2102, HashMap(), e6f2)
            e6f2.nodes[2104.0] = MapNode(1353, 494, NodeType.ROOM, 2104, HashMap(), e6f2)
            e6f2.nodes[2107.0] = MapNode(1435, 494, NodeType.ROOM, 2107, HashMap(), e6f2)
            e6f2.nodes[2106.0] = MapNode(1459, 494, NodeType.ROOM, 2106, HashMap(), e6f2)
            e6f2.nodes[2108.0] = MapNode(1566, 494, NodeType.ROOM, 2108, HashMap(), e6f2)
            e6f2.nodes[2109.0] = MapNode(1640, 494, NodeType.ROOM, 2109, HashMap(), e6f2)
            e6f2.nodes[2110.0] = MapNode(1671, 494, NodeType.ROOM, 2110, HashMap(), e6f2)
            e6f2.nodes[2112.0] = MapNode(1711, 494, NodeType.ROOM, 2112, HashMap(), e6f2)
            e6f2.nodes[2113.0] = MapNode(1736, 494, NodeType.ROOM, 2113, HashMap(), e6f2)
            e6f2.nodes[2114.0] = MapNode(1817, 494, NodeType.ROOM, 2114, HashMap(), e6f2)
            e6f2.nodes[2116.0] = MapNode(1922, 494, NodeType.ROOM, 2116, HashMap(), e6f2)
            e6f2.nodes[2118.0] = MapNode(2030, 494, NodeType.ROOM, 2118, HashMap(), e6f2)
            e6f2.nodes[2119.0] = MapNode(2060, 494, NodeType.ROOM, 2119, HashMap(), e6f2)
            e6f2.nodes[2120.0] = MapNode(2134, 494, NodeType.ROOM, 2120, HashMap(), e6f2)
            e6f2.nodes[-2201.0] = MapNode(2249, 494, NodeType.STAIR, -2201, HashMap(), e6f2)
            addEdge(e6f2, -2500, -2200)
            addEdge(e6f2, -2200, 2907)
            addEdge(e6f2, 2907, -2100)
            addEdge(e6f2, -2100, 2909)
            addEdge(e6f2, 2909, 2102)
            addEdge(e6f2, 2102, 2104)
            addEdge(e6f2, 2104, 2107)
            addEdge(e6f2, 2107, 2106)
            addEdge(e6f2, 2106, 2108)
            addEdge(e6f2, 2108, 2109)
            addEdge(e6f2, 2109, 2110)
            addEdge(e6f2, 2110, 2112)
            addEdge(e6f2, 2112, 2113)
            addEdge(e6f2, 2113, 2114)
            addEdge(e6f2, 2114, 2116)
            addEdge(e6f2, 2116, 2118)
            addEdge(e6f2, 2118, 2119)
            addEdge(e6f2, 2119, 2120)
            addEdge(e6f2, 2120, -2201)

            // ROW 2

            e6f2.nodes[-2501.0] = MapNode(965, 1302, NodeType.NONE, -2501, HashMap(), e6f2)
            e6f2.nodes[-2202.0] = MapNode(1040, 1302, NodeType.STAIR, -2202, HashMap(), e6f2)
            e6f2.nodes[2901.0] = MapNode(1046, 1302, NodeType.ROOM, 2901, HashMap(), e6f2)
            e6f2.nodes[-2101.0] = MapNode(1118, 1302, NodeType.WC, -2101, HashMap(), e6f2)
            e6f2.nodes[2002.0] = MapNode(1249, 1302, NodeType.ROOM, 2002, HashMap(), e6f2)
            e6f2.nodes[2003.0] = MapNode(1320, 1302, NodeType.ROOM, 2003, HashMap(), e6f2)
            e6f2.nodes[2004.0] = MapNode(1353, 1302, NodeType.ROOM, 2004, HashMap(), e6f2)
            e6f2.nodes[2006.0] = MapNode(1459, 1302, NodeType.ROOM, 2006, HashMap(), e6f2)
            e6f2.nodes[2008.0] = MapNode(1566, 1302, NodeType.ROOM, 2008, HashMap(), e6f2)
            e6f2.nodes[2009.0] = MapNode(1638, 1302, NodeType.ROOM, 2009, HashMap(), e6f2)
            e6f2.nodes[2010.0] = MapNode(1671, 1302, NodeType.ROOM, 2010, HashMap(), e6f2)
            e6f2.nodes[2012.0] = MapNode(1711, 1302, NodeType.ROOM, 2012, HashMap(), e6f2)
            e6f2.nodes[2014.0] = MapNode(1817, 1302, NodeType.ROOM, 2014, HashMap(), e6f2)
            e6f2.nodes[2016.0] = MapNode(1922, 1302, NodeType.ROOM, 2016, HashMap(), e6f2)
            e6f2.nodes[2018.0] = MapNode(2030, 1302, NodeType.ROOM, 2018, HashMap(), e6f2)
            e6f2.nodes[2020.0] = MapNode(2134, 1302, NodeType.ROOM, 2020, HashMap(), e6f2)
            e6f2.nodes[-2203.0] = MapNode(2249, 1302, NodeType.STAIR, -2203, HashMap(), e6f2)
            addEdge(e6f2, -2501, -2202)
            addEdge(e6f2, -2202, 2901)
            addEdge(e6f2, 2901, -2101)
            addEdge(e6f2, -2101, 2002)
            addEdge(e6f2, 2002, 2003)
            addEdge(e6f2, 2003, 2004)
            addEdge(e6f2, 2004, 2006)
            addEdge(e6f2, 2006, 2008)
            addEdge(e6f2, 2008, 2009)
            addEdge(e6f2, 2009, 2010)
            addEdge(e6f2, 2010, 2012)
            addEdge(e6f2, 2012, 2014)
            addEdge(e6f2, 2014, 2016)
            addEdge(e6f2, 2016, 2018)
            addEdge(e6f2, 2018, 2020)
            addEdge(e6f2, 2020, -2203)

            // Right column

            e6f2.nodes[2024.0] = MapNode(2249, 797, NodeType.ROOM, 2024, HashMap(), e6f2)
            e6f2.nodes[2023.0] = MapNode(2249, 1068, NodeType.ROOM, 2023, HashMap(), e6f2)
            e6f2.nodes[2021.0] = MapNode(2249, 1154, NodeType.ROOM, 2021, HashMap(), e6f2)
            e6f2.nodes[2022.0] = MapNode(2249, 1165, NodeType.ROOM, 2022, HashMap(), e6f2)
            addEdge(e6f2, 2024, 2023)
            addEdge(e6f2, 2023, 2021)
            addEdge(e6f2, 2021, 2022)

            // Join components
            addEdge(e6f2, -2500, -2501)
            addEdge(e6f2, -2201, 2024)
            addEdge(e6f2, -2203, 2022)

        }

        // F3 INIT

        // ROOMS HAVE NUMBERS -- 3002, 3004
        // PORTS HAVE NEGATIVE 30XX
        // WC HAVE NEGATIVE 31XX
        // STAIRS HAVE NEGATIVE 32XX
        // NONE IS ALL ELSE 3XXX (USUALLY 35XX)

        run {

            // ROW 1 (TOPMOST)

            val e6F2F3RowXDiff = 100

            e6f3.nodes[-3500.0] = MapNode(980, 494, NodeType.NONE, -3500, HashMap(), e6f3)
            e6f3.nodes[-3200.0] = MapNode(1040 + e6F2F3RowXDiff, 494, NodeType.STAIR, -3200, HashMap(), e6f3)
            e6f3.nodes[3907.0] = MapNode(1046 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3907, HashMap(), e6f3)
            e6f3.nodes[-3100.0] = MapNode(1118 + e6F2F3RowXDiff, 494, NodeType.WC, -3100, HashMap(), e6f3)
            e6f3.nodes[3102.0] = MapNode(1249 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3102, HashMap(), e6f3)
            e6f3.nodes[3104.0] = MapNode(1353 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3104, HashMap(), e6f3)
            e6f3.nodes[3103.0] = MapNode(1353 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3103, HashMap(), e6f3)
            e6f3.nodes[3107.0] = MapNode(1527, 494, NodeType.ROOM, 3107, HashMap(), e6f3)
            e6f3.nodes[3106.0] = MapNode(1459 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3106, HashMap(), e6f3)
            e6f3.nodes[3109.0] = MapNode(1632, 494, NodeType.ROOM, 3109, HashMap(), e6f3)
            e6f3.nodes[3108.0] = MapNode(1566 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3108, HashMap(), e6f3)
            e6f3.nodes[3110.0] = MapNode(1671 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3110, HashMap(), e6f3)
            e6f3.nodes[3112.0] = MapNode(1711 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3112, HashMap(), e6f3)
            e6f3.nodes[3113.0] = MapNode(1736 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3113, HashMap(), e6f3)
            e6f3.nodes[3114.0] = MapNode(1817 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3114, HashMap(), e6f3)
            e6f3.nodes[3116.0] = MapNode(1922 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3116, HashMap(), e6f3)
            e6f3.nodes[3117.0] = MapNode(2050, 494, NodeType.ROOM, 3117, HashMap(), e6f3)
            e6f3.nodes[3118.0] = MapNode(2030 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3118, HashMap(), e6f3)
            e6f3.nodes[3120.0] = MapNode(2134 + e6F2F3RowXDiff, 494, NodeType.ROOM, 3120, HashMap(), e6f3)
            e6f3.nodes[3121.0] = MapNode(2267, 494, NodeType.ROOM, 3121, HashMap(), e6f3)
            e6f3.nodes[-3201.0] = MapNode(2249 + e6F2F3RowXDiff, 494, NodeType.STAIR, -3201, HashMap(), e6f3)
            addEdge(e6f3, -3500, -3200)
            addEdge(e6f3, -3200, 3907)
            addEdge(e6f3, 3907, -3100)
            addEdge(e6f3, -3100, 3102)
            addEdge(e6f3, 3102, 3104)
            addEdge(e6f3, 3104, 3103)
            addEdge(e6f3, 3103, 3107)
            addEdge(e6f3, 3107, 3106)
            addEdge(e6f3, 3106, 3109)
            addEdge(e6f3, 3109, 3108)
            addEdge(e6f3, 3108, 3110)
            addEdge(e6f3, 3110, 3112)
            addEdge(e6f3, 3112, 3113)
            addEdge(e6f3, 3113, 3114)
            addEdge(e6f3, 3114, 3116)
            addEdge(e6f3, 3116, 3117)
            addEdge(e6f3, 3117, 3118)
            addEdge(e6f3, 3118, 3120)
            addEdge(e6f3, 3120, 3121)
            addEdge(e6f3, 3121, -3201)

            // ROW 2

            e6f3.nodes[-3000.0] = MapNode(980, 1302, NodeType.PORT, -3000, HashMap(), e6f3)
            e6f3.nodes[-3202.0] = MapNode(1040 + e6F2F3RowXDiff, 1302, NodeType.STAIR, -3202, HashMap(), e6f3)
            e6f3.nodes[3901.0] = MapNode(1046 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3901, HashMap(), e6f3)
            e6f3.nodes[-3101.0] = MapNode(1118 + e6F2F3RowXDiff, 1302, NodeType.WC, -3101, HashMap(), e6f3)
            e6f3.nodes[3002.0] = MapNode(1249 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3002, HashMap(), e6f3)
            e6f3.nodes[3003.0] = MapNode(1320 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3003, HashMap(), e6f3)
            e6f3.nodes[3004.0] = MapNode(1353 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3004, HashMap(), e6f3)
            e6f3.nodes[3006.0] = MapNode(1459 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3006, HashMap(), e6f3)
            e6f3.nodes[3008.0] = MapNode(1566 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3008, HashMap(), e6f3)
            e6f3.nodes[3009.0] = MapNode(1638 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3009, HashMap(), e6f3)
            e6f3.nodes[3010.0] = MapNode(1671 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3010, HashMap(), e6f3)
            e6f3.nodes[3012.0] = MapNode(1711 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3012, HashMap(), e6f3)
            e6f3.nodes[3013.0] = MapNode(1952, 1302, NodeType.ROOM, 3013, HashMap(), e6f3)
            e6f3.nodes[3014.0] = MapNode(1817 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3014, HashMap(), e6f3)
            e6f3.nodes[3016.0] = MapNode(1922 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3016, HashMap(), e6f3)
            e6f3.nodes[3018.0] = MapNode(2030 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3018, HashMap(), e6f3)
            e6f3.nodes[3020.0] = MapNode(2134 + e6F2F3RowXDiff, 1302, NodeType.ROOM, 3020, HashMap(), e6f3)
            e6f3.nodes[-3203.0] = MapNode(2249 + e6F2F3RowXDiff, 1302, NodeType.STAIR, -2203, HashMap(), e6f3)
            addEdge(e6f3, -3000, -3202)
            addEdge(e6f3, -3202, 3901)
            addEdge(e6f3, 3901, -3101)
            addEdge(e6f3, -3101, 3002)
            addEdge(e6f3, 3002, 3003)
            addEdge(e6f3, 3003, 3004)
            addEdge(e6f3, 3004, 3006)
            addEdge(e6f3, 3006, 3008)
            addEdge(e6f3, 3008, 3009)
            addEdge(e6f3, 3009, 3010)
            addEdge(e6f3, 3010, 3012)
            addEdge(e6f3, 3012, 3013)
            addEdge(e6f3, 3013, 3014)
            addEdge(e6f3, 3014, 3016)
            addEdge(e6f3, 3016, 3018)
            addEdge(e6f3, 3018, 3020)
            addEdge(e6f3, 3020, -3203)

            // Right column

            e6f3.nodes[3038.0] = MapNode(2465, 692, NodeType.ROOM, 3038, HashMap(), e6f3)
            e6f3.nodes[3036.0] = MapNode(2523, 692, NodeType.ROOM, 3036, HashMap(), e6f3)
            e6f3.nodes[3034.0] = MapNode(2523, 710, NodeType.ROOM, 3034, HashMap(), e6f3)
            e6f3.nodes[3032.0] = MapNode(2523, 814, NodeType.ROOM, 3032, HashMap(), e6f3)
            e6f3.nodes[3029.0] = MapNode(2523, 834, NodeType.ROOM, 3029, HashMap(), e6f3)
            e6f3.nodes[3028.0] = MapNode(2523, 857, NodeType.ROOM, 3028, HashMap(), e6f3)
            e6f3.nodes[3026.0] = MapNode(2523, 1024, NodeType.ROOM, 3026, HashMap(), e6f3)
            e6f3.nodes[3024.0] = MapNode(2523, 1057, NodeType.ROOM, 3024, HashMap(), e6f3)
            e6f3.nodes[3027.0] = MapNode(2443, 1057, NodeType.ROOM, 3027, HashMap(), e6f3)
            e6f3.nodes[3023.0] = MapNode(2348, 1057, NodeType.ROOM, 3023, HashMap(), e6f3)
            e6f3.nodes[3021.0] = MapNode(2348, 1135, NodeType.ROOM, 3021, HashMap(), e6f3)
            addEdge(e6f3, 3038, 3036)
            addEdge(e6f3, 3036, 3034)
            addEdge(e6f3, 3034, 3032)
            addEdge(e6f3, 3032, 3029)
            addEdge(e6f3, 3029, 3028)
            addEdge(e6f3, 3028, 3026)
            addEdge(e6f3, 3026, 3024)
            addEdge(e6f3, 3024, 3027)
            addEdge(e6f3, 3027, 3023)
            addEdge(e6f3, 3023, 3021)
            e6f3.nodes[3029.1] = MapNode(2348, 763, NodeType.ROOM, 3029, HashMap(), e6f3)
            addEdge(e6f3, 3023, 3029.1)

            // Join components
            addEdge(e6f3, -3203, 3021)
            addEdge(e6f3, -3201, 3029)
            addEdge(e6f3, -3500, -3000)

        }

        // F4 INIT

        // ROOMS HAVE NUMBERS -- 4002, 4004
        // PORTS HAVE NEGATIVE 40XX
        // WC HAVE NEGATIVE 41XX
        // STAIRS HAVE NEGATIVE 42XX
        // NONE IS ALL ELSE 4XXX (USUALLY 45XX)

        run {
            // ROW 1 (TOPMOST)

            val e6F2F4RowXDiff = 100

            e6f4.nodes[-4500.0] = MapNode(1000, 494, NodeType.NONE, -4500, HashMap(), e6f4)
            e6f4.nodes[4101.0] = MapNode(1086, 494, NodeType.ROOM, 4101, HashMap(), e6f4)
            e6f4.nodes[-4200.0] = MapNode(1040 + e6F2F4RowXDiff, 494, NodeType.STAIR, -4200, HashMap(), e6f4)
            e6f4.nodes[4907.0] = MapNode(1046 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4907, HashMap(), e6f4)
            e6f4.nodes[-4100.0] = MapNode(1118 + e6F2F4RowXDiff, 494, NodeType.WC, -4100, HashMap(), e6f4)
            e6f4.nodes[4909.0] = MapNode(1196 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4909, HashMap(), e6f4)
            e6f4.nodes[4102.0] = MapNode(1249 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4102, HashMap(), e6f4)
            e6f4.nodes[4107.0] = MapNode(1427, 494, NodeType.ROOM, 4107, HashMap(), e6f4)
            e6f4.nodes[4104.0] = MapNode(1353 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4104, HashMap(), e6f4)
            e6f4.nodes[4106.0] = MapNode(1459 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4106, HashMap(), e6f4)
            e6f4.nodes[4109.0] = MapNode(1637, 494, NodeType.ROOM, 4109, HashMap(), e6f4)
            e6f4.nodes[4108.0] = MapNode(1566 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4108, HashMap(), e6f4)
            e6f4.nodes[4110.0] = MapNode(1671 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4110, HashMap(), e6f4)
            e6f4.nodes[4112.0] = MapNode(1711 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4112, HashMap(), e6f4)
            e6f4.nodes[4114.0] = MapNode(1817 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4114, HashMap(), e6f4)
            e6f4.nodes[4116.0] = MapNode(1922 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4116, HashMap(), e6f4)
            e6f4.nodes[4113.0] = MapNode(2062, 494, NodeType.ROOM, 4113, HashMap(), e6f4)
            e6f4.nodes[4118.0] = MapNode(2030 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4118, HashMap(), e6f4)
            e6f4.nodes[4120.0] = MapNode(2134 + e6F2F4RowXDiff, 494, NodeType.ROOM, 4120, HashMap(), e6f4)
            e6f4.nodes[4119.0] = MapNode(2269, 494, NodeType.ROOM, 4119, HashMap(), e6f4)
            e6f4.nodes[-4201.0] = MapNode(2249 + e6F2F4RowXDiff, 494, NodeType.STAIR, -4201, HashMap(), e6f4)
            addEdge(e6f4, -4500, 4101)
            addEdge(e6f4, 4101, -4200)
            addEdge(e6f4, -4200, 4907)
            addEdge(e6f4, 4907, -4100)
            addEdge(e6f4, -4100, 4909)
            addEdge(e6f4, 4909, 4102)
            addEdge(e6f4, 4102, 4107)
            addEdge(e6f4, 4107, 4104)
            addEdge(e6f4, 4104, 4106)
            addEdge(e6f4, 4106, 4109)
            addEdge(e6f4, 4109, 4108)
            addEdge(e6f4, 4108, 4110)
            addEdge(e6f4, 4110, 4112)
            addEdge(e6f4, 4112, 4114)
            addEdge(e6f4, 4114, 4116)
            addEdge(e6f4, 4116, 4113)
            addEdge(e6f4, 4113, 4118)
            addEdge(e6f4, 4118, 4120)
            addEdge(e6f4, 4120, 4119)
            addEdge(e6f4, 4119, -4201)

            // ROW 2

            e6f4.nodes[-4501.0] = MapNode(1000, 1302, NodeType.NONE, -4501, HashMap(), e6f4)
            e6f4.nodes[4001.0] = MapNode(1086, 1302, NodeType.ROOM, 4001, HashMap(), e6f4)
            e6f4.nodes[-4202.0] = MapNode(1040 + e6F2F4RowXDiff, 1302, NodeType.STAIR, -4202, HashMap(), e6f4)
            e6f4.nodes[4901.0] = MapNode(1046 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4901, HashMap(), e6f4)
            e6f4.nodes[-4101.0] = MapNode(1118 + e6F2F4RowXDiff, 1302, NodeType.WC, -4101, HashMap(), e6f4)
            e6f4.nodes[4002.0] = MapNode(1249 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4002, HashMap(), e6f4)
            e6f4.nodes[4003.0] = MapNode(1320 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4003, HashMap(), e6f4)
            e6f4.nodes[4004.0] = MapNode(1353 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4004, HashMap(), e6f4)
            e6f4.nodes[4006.0] = MapNode(1459 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4006, HashMap(), e6f4)
            e6f4.nodes[4008.0] = MapNode(1566 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4008, HashMap(), e6f4)
            e6f4.nodes[4009.0] = MapNode(1638 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4009, HashMap(), e6f4)
            e6f4.nodes[4010.0] = MapNode(1671 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4010, HashMap(), e6f4)
            e6f4.nodes[4012.0] = MapNode(1711 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4012, HashMap(), e6f4)
            e6f4.nodes[4014.0] = MapNode(1817 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4014, HashMap(), e6f4)
            e6f4.nodes[4016.0] = MapNode(1922 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4016, HashMap(), e6f4)
            e6f4.nodes[4017.0] = MapNode(2057, 1302, NodeType.ROOM, 4017, HashMap(), e6f4)
            e6f4.nodes[4018.0] = MapNode(2030 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4018, HashMap(), e6f4)
            e6f4.nodes[4020.0] = MapNode(2134 + e6F2F4RowXDiff, 1302, NodeType.ROOM, 4020, HashMap(), e6f4)
            e6f4.nodes[-4203.0] = MapNode(2249 + e6F2F4RowXDiff, 1302, NodeType.STAIR, -4203, HashMap(), e6f4)
            addEdge(e6f4, -4501, 4001)
            addEdge(e6f4, 4001, -4202)
            addEdge(e6f4, -4202, 4901)
            addEdge(e6f4, 4901, -4101)
            addEdge(e6f4, -4101, 4002)
            addEdge(e6f4, 4002, 4003)
            addEdge(e6f4, 4003, 4004)
            addEdge(e6f4, 4004, 4006)
            addEdge(e6f4, 4006, 4008)
            addEdge(e6f4, 4008, 4009)
            addEdge(e6f4, 4009, 4010)
            addEdge(e6f4, 4010, 4012)
            addEdge(e6f4, 4012, 4014)
            addEdge(e6f4, 4014, 4016)
            addEdge(e6f4, 4016, 4017)
            addEdge(e6f4, 4017, 4018)
            addEdge(e6f4, 4018, 4020)
            addEdge(e6f4, 4020, -4203)

            // Right column

            e6f4.nodes[4024.0] = MapNode(2249 + e6F2F4RowXDiff, 855, NodeType.ROOM, 4024, HashMap(), e6f4)
            e6f4.nodes[4023.0] = MapNode(2249 + e6F2F4RowXDiff, 1056, NodeType.ROOM, 4023, HashMap(), e6f4)
            e6f4.nodes[4021.0] = MapNode(2249 + e6F2F4RowXDiff, 1096, NodeType.ROOM, 4021, HashMap(), e6f4)
            e6f4.nodes[4022.0] = MapNode(2249 + e6F2F4RowXDiff, 1134, NodeType.ROOM, 4022, HashMap(), e6f4)
            addEdge(e6f4, 4024, 4023)
            addEdge(e6f4, 4023, 4021)
            addEdge(e6f4, 4021, 4022)

            // Join components
            addEdge(e6f4, -4500, -4501)
            addEdge(e6f4, -4201, 4024)
            addEdge(e6f4, -4203, 4022)

        }

        // F5 INIT

        // ROOMS HAVE NUMBERS -- 5002, 5004
        // PORTS HAVE NEGATIVE 50XX
        // WC HAVE NEGATIVE 51XX
        // STAIRS HAVE NEGATIVE 52XX
        // NONE IS ALL ELSE 5XXX (USUALLY 55XX)

        run {

            // ROW 1

            val e6F2F5RowXDiff = 100

            e6f5.nodes[-5500.0] = MapNode(1000, 494, NodeType.NONE, -5500, HashMap(), e6f5)
            e6f5.nodes[5101.0] = MapNode(1086, 494, NodeType.ROOM, 5101, HashMap(), e6f5)
            e6f5.nodes[-5200.0] = MapNode(1040 + e6F2F5RowXDiff, 494, NodeType.STAIR, -5200, HashMap(), e6f5)
            e6f5.nodes[5907.0] = MapNode(1046 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5907, HashMap(), e6f5)
            e6f5.nodes[-5100.0] = MapNode(1118 + e6F2F5RowXDiff, 494, NodeType.WC, -5100, HashMap(), e6f5)
            e6f5.nodes[5103.0] = MapNode(1316, 494, NodeType.ROOM, 5103, HashMap(), e6f5)
            e6f5.nodes[5102.0] = MapNode(1249 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5102, HashMap(), e6f5)
            e6f5.nodes[5107.0] = MapNode(1427, 494, NodeType.ROOM, 5107, HashMap(), e6f5)
            e6f5.nodes[5104.0] = MapNode(1353 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5104, HashMap(), e6f5)
            e6f5.nodes[5106.0] = MapNode(1459 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5106, HashMap(), e6f5)
            e6f5.nodes[5108.0] = MapNode(1566 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5108, HashMap(), e6f5)
            e6f5.nodes[5110.0] = MapNode(1671 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5110, HashMap(), e6f5)
            e6f5.nodes[5112.0] = MapNode(1711 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5112, HashMap(), e6f5)
            e6f5.nodes[5113.0] = MapNode(1904, 494, NodeType.ROOM, 5113, HashMap(), e6f5)
            e6f5.nodes[5114.0] = MapNode(1817 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5114, HashMap(), e6f5)
            e6f5.nodes[5116.0] = MapNode(1922 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5116, HashMap(), e6f5)
            e6f5.nodes[5118.0] = MapNode(2030 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5118, HashMap(), e6f5)
            e6f5.nodes[5119.0] = MapNode(2169, 494, NodeType.ROOM, 5119, HashMap(), e6f5)
            e6f5.nodes[5120.0] = MapNode(2134 + e6F2F5RowXDiff, 494, NodeType.ROOM, 5120, HashMap(), e6f5)
            e6f5.nodes[-5201.0] = MapNode(2249 + e6F2F5RowXDiff, 494, NodeType.STAIR, -5201, HashMap(), e6f5)
            addEdge(e6f5, -5500, 5101)
            addEdge(e6f5, 5101, -5200)
            addEdge(e6f5, -5200, 5907)
            addEdge(e6f5, 5907, -5100)
            addEdge(e6f5, -5100, 5103)
            addEdge(e6f5, 5103, 5102)
            addEdge(e6f5, 5102, 5107)
            addEdge(e6f5, 5107, 5104)
            addEdge(e6f5, 5104, 5106)
            addEdge(e6f5, 5106, 5108)
            addEdge(e6f5, 5108, 5110)
            addEdge(e6f5, 5110, 5112)
            addEdge(e6f5, 5112, 5113)
            addEdge(e6f5, 5113, 5114)
            addEdge(e6f5, 5114, 5116)
            addEdge(e6f5, 5116, 5118)
            addEdge(e6f5, 5118, 5119)
            addEdge(e6f5, 5119, 5120)
            addEdge(e6f5, 5120, -5201)

            // ROW 2

            e6f5.nodes[-5501.0] = MapNode(1000, 1302, NodeType.NONE, -5501, HashMap(), e6f5)
            e6f5.nodes[5001.0] = MapNode(1086, 1302, NodeType.ROOM, 5001, HashMap(), e6f5)
            e6f5.nodes[-5202.0] = MapNode(1040 + e6F2F5RowXDiff, 1302, NodeType.STAIR, -5202, HashMap(), e6f5)
            e6f5.nodes[5901.0] = MapNode(1046 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5901, HashMap(), e6f5)
            e6f5.nodes[-5101.0] = MapNode(1118 + e6F2F5RowXDiff, 1302, NodeType.WC, -5101, HashMap(), e6f5)
            e6f5.nodes[5002.0] = MapNode(1249 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5002, HashMap(), e6f5)
            e6f5.nodes[5007.0] = MapNode(1330 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5007, HashMap(), e6f5)
            e6f5.nodes[5004.0] = MapNode(1353 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5004, HashMap(), e6f5)
            e6f5.nodes[5006.0] = MapNode(1459 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5006, HashMap(), e6f5)
            e6f5.nodes[5008.0] = MapNode(1566 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5008, HashMap(), e6f5)
            e6f5.nodes[5009.0] = MapNode(1638 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5009, HashMap(), e6f5)
            e6f5.nodes[5010.0] = MapNode(1671 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5010, HashMap(), e6f5)
            e6f5.nodes[5012.0] = MapNode(1711 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5012, HashMap(), e6f5)
            e6f5.nodes[5014.0] = MapNode(1817 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5014, HashMap(), e6f5)
            e6f5.nodes[5016.0] = MapNode(1922 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5016, HashMap(), e6f5)
            e6f5.nodes[5013.0] = MapNode(2057, 1302, NodeType.ROOM, 5013, HashMap(), e6f5)
            e6f5.nodes[5018.0] = MapNode(2030 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5018, HashMap(), e6f5)
            e6f5.nodes[5020.0] = MapNode(2134 + e6F2F5RowXDiff, 1302, NodeType.ROOM, 5020, HashMap(), e6f5)
            e6f5.nodes[5019.0] = MapNode(2268, 1302, NodeType.ROOM, 5019, HashMap(), e6f5)
            e6f5.nodes[-5203.0] = MapNode(2249 + e6F2F5RowXDiff, 1302, NodeType.STAIR, -5203, HashMap(), e6f5)
            addEdge(e6f5, -5501, 5001)
            addEdge(e6f5, 5001, -5202)
            addEdge(e6f5, -5202, 5901)
            addEdge(e6f5, 5901, -5101)
            addEdge(e6f5, -5101, 5002)
            addEdge(e6f5, 5002, 5007)
            addEdge(e6f5, 5007, 5004)
            addEdge(e6f5, 5004, 5006)
            addEdge(e6f5, 5006, 5008)
            addEdge(e6f5, 5008, 5009)
            addEdge(e6f5, 5009, 5010)
            addEdge(e6f5, 5010, 5012)
            addEdge(e6f5, 5012, 5014)
            addEdge(e6f5, 5014, 5016)
            addEdge(e6f5, 5016, 5013)
            addEdge(e6f5, 5013, 5018)
            addEdge(e6f5, 5018, 5020)
            addEdge(e6f5, 5020, 5019)
            addEdge(e6f5, 5019, -5203)

            // Right column

            e6f5.nodes[5028.0] = MapNode(2249 + e6F2F5RowXDiff, 720, NodeType.ROOM, 5028, HashMap(), e6f5)
            e6f5.nodes[5024.0] = MapNode(2249 + e6F2F5RowXDiff, 828, NodeType.ROOM, 5024, HashMap(), e6f5)
            e6f5.nodes[5022.0] = MapNode(2249 + e6F2F5RowXDiff, 1221, NodeType.ROOM, 5022, HashMap(), e6f5)

            addEdge(e6f5, 5028, 5024)
            addEdge(e6f5, 5024, 5022)


            // Join components
            e6f5.nodes[-5502.0] = MapNode(1000, 1030, NodeType.NONE, -5502, HashMap(), e6f5)
            e6f5.nodes[5003.0] = MapNode(1086, 1030, NodeType.ROOM, 5003, HashMap(), e6f5)
            addEdge(e6f5, -5502, 5003)

            addEdge(e6f5, -5500, -5502)
            addEdge(e6f5, -5502, -5501)
            addEdge(e6f5, -5201, 5028)
            addEdge(e6f5, -5203, 5022)
        }

        // STAIRS

        run {
            addEdge(2, -1200, -2200)
            addEdge(2, -2200, -3200)
            addEdge(2, -3200, -4200)
            addEdge(2, -4200, -5200)

            addEdge(2, -1201, -2201)
            addEdge(2, -2201, -3201)
            addEdge(2, -3201, -4201)
            addEdge(2, -4201, -5201)

            addEdge(2, -1202, -2202)
            addEdge(2, -2202, -3202)
            addEdge(2, -3202, -4202)
            addEdge(2, -4202, -5202)

            addEdge(2, -1203, -2203)
            addEdge(2, -2203, -3203)
            addEdge(2, -3203, -4203)
            addEdge(2, -4203, -5203)
        }

        val s: MutableSet<MapNode> = HashSet()
        for (f in buildings[1]!!.plans.values)
            for (n in f.nodes.values)
                s.add(n)

        val q: Queue<MapNode> = LinkedList()
        q.offer(buildings[1]!!.plans[1]!!.nodes[1419.0])

        while(q.isNotEmpty()) {
            val cur = q.poll()!!

            if(s.remove(cur))
                for(n in cur.adj.keys)
                    q.offer(n)
        }

        if(s.isNotEmpty()) {
            val ids = s.map { n -> n.id }.sorted()
            Log.w("MapRepository", "E7 graph is not connected; unreachable nodes:\n$ids")
        }
    }

    fun roomExists(buildingId: Int, roomId: Int): Boolean {
        if(buildings[buildingId] == null)
            throw Exception("Building ID does not exist");
        return buildings[buildingId]!!.plans[getFloor(roomId.toDouble())] != null &&
                buildings[buildingId]!!.plans[getFloor(roomId.toDouble())]!!.nodes[roomId.toDouble()] != null;
    }

    fun getNorthHeading(buildingId: Int): Int {
        return buildings[buildingId]!!.northDeg
    }

    // ? Returns marked floor plan for a given building ID and start/end room IDs
    // ? May return a list in case of routes spanning floors
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getMarkedPlan(buildingId: Int, srcId: Int, destId: Int): List<Bitmap> {

//        // Code for testing floor switching
//        return listOf(
//            (applicationContext.resources.getDrawable(R.drawable.e7f1) as BitmapDrawable).bitmap,
//            (applicationContext.resources.getDrawable(R.drawable.e7f1) as BitmapDrawable).bitmap,
//            (applicationContext.resources.getDrawable(R.drawable.e7f2) as BitmapDrawable).bitmap,
//            (applicationContext.resources.getDrawable(R.drawable.e7f3) as BitmapDrawable).bitmap,
//            (applicationContext.resources.getDrawable(R.drawable.e7f4) as BitmapDrawable).bitmap,
//            (applicationContext.resources.getDrawable(R.drawable.e7f5) as BitmapDrawable).bitmap,
//            (applicationContext.resources.getDrawable(R.drawable.e7f6) as BitmapDrawable).bitmap
//        )

        if(srcId == destId)
            return drawPath(buildingId, listOf())

        // Get nodes corresponding to src and dest targets
        val sNode = buildings[buildingId]!!.plans[getFloor(srcId.toDouble())]!!.nodes[srcId.toDouble()]!!
        val dNode = buildings[buildingId]!!.plans[getFloor(destId.toDouble())]!!.nodes[destId.toDouble()]!!

        return drawPath(buildingId, getPath(sNode, dNode))
    }

    // ? Returns marked floor plan for a given building ID, start ID, and PoI type
    // ? May return a list in case of routes spanning floors
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getMarkedPlan(buildingId: Int, srcId: Int, destType: NodeType): List<Bitmap> {
        // Get node corresponding to src
        val sNode = buildings[buildingId]!!.plans[getFloor(srcId.toDouble())]!!.nodes[srcId.toDouble()]

        return drawPath(buildingId, getPath(sNode!!, destType))
    }

    // ? Draws a line on floor plan(s) given the path
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun drawPath(buildingId: Int, path: List<MapNode>): List<Bitmap> {
        // Return all floor plans for building ID if path empty
        if(path.isEmpty())
            return buildings[buildingId]!!.plans.entries
                .sortedBy { e -> e.key }
                .map { e -> e.value }
                .map { f ->
                    val img = (applicationContext.resources.getDrawable(
                        f.plan,
                        null
                    ) as BitmapDrawable).bitmap

                    img.copy(img.config ?: Bitmap.Config.ARGB_8888, true)
                }

        val paint = Paint()
        paint.strokeWidth = WIDTH_PATH

        // Start location
        var cur = path[0]
        val startMap = (applicationContext.resources.getDrawable(cur.floor!!.plan, null) as BitmapDrawable).bitmap
        val ret: MutableList<Bitmap> = mutableListOf(
            startMap.copy(startMap.config ?: Bitmap.Config.ARGB_8888, true)
        )

        var canvas = Canvas(ret.last())

        // Draw start node
        paint.color = COLOR_START
        canvas.drawCircle(
            cur.x.toFloat(),
            cur.y.toFloat(),
            RADIUS_NODE,
            paint
        )
        paint.color = COLOR_LINE

        // Draw path
        for(i in 1..<path.size) {
            val next = path[i]

            // Draw line if both connected nodes are on same floor
            if(cur.floor == next.floor)
                canvas.drawLine(
                    cur.x.toFloat(),
                    cur.y.toFloat(),
                    next.x.toFloat(),
                    next.y.toFloat(),
                    paint
                )

            // Cross-floor edge; mark up and down nodes and switch canvas
            else {
                // Draw stair up node
                paint.color =
                    if(getFloor(cur.id.toDouble()) < getFloor(next.id.toDouble())) COLOR_STAIRS_UP
                    else COLOR_STAIRS_DOWN
                canvas.drawCircle(
                    cur.x.toFloat(),
                    cur.y.toFloat(),
                    RADIUS_NODE,
                    paint
                )


                // Switch canvas
                val nextMap = (applicationContext.resources.getDrawable(next.floor!!.plan, null) as BitmapDrawable).bitmap
                ret.add(
                    nextMap.copy(nextMap.config ?: Bitmap.Config.ARGB_8888, true)
                )
                canvas = Canvas(ret.last())

                // Draw floor start node
                paint.color = COLOR_START
                canvas.drawCircle(
                    next.x.toFloat(),
                    next.y.toFloat(),
                    RADIUS_NODE,
                    paint
                )

                // Reset color
                paint.color = COLOR_LINE
            }

            cur = next
        }

        // Draw destination node
        paint.color = COLOR_END
        canvas.drawCircle(
            cur.x.toFloat(),
            cur.y.toFloat(),
            RADIUS_NODE,
            paint
        )

        // TODO: remove the following section; used only for testing
//        val file = File(applicationContext.cacheDir, "temp.png")
//        val outStream = FileOutputStream(file)
//        copy.compress(Bitmap.CompressFormat.PNG, 100, outStream)
//        outStream.flush()
//        outStream.close()
        // TODO: end of section

        return ret
    }

    // ? For pathing to a specific room
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun getPath(src: MapNode, dest: MapNode): List<MapNode> {
        return getPath(src, { n -> n.id.toInt() == dest.id.toInt() }, dest)
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
        Log.i("MapRepository", "Pathing started")

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
            if(getFloor(cur.id) == 2)
                Log.i("MapRepository", "ID ${cur.id} with dist $curDist polled")

            if(isTarget(cur)) {
                Log.i("MapRepository", "Pathing complete with total distance $curDist")
                return generatePath(cur, parent)
            }

            // Enqueue neighbors
            for (n in cur.adj.keys) {
                val newDist = curDist + cur.adj[n]!!

                // Skip if not a relaxation
                if(srcDist.getOrDefault(n, Double.MAX_VALUE) <= newDist)
                    continue

//                // Remove if already exists
//                if(queue.contains(n))
//                    queue.remove(n)

                srcDist[n] = newDist
                parent[n] = cur
                queue.add(n)
            }
        }

        // Draw search graph
        drawSearchPath(parent)
        Log.e("MapRepository", "Path requested but not found")
        // ! Error state; should never occur
        return mutableListOf()
    }

    // ? Returns the floor number given a room ID
    private fun getFloor(roomId: Double): Int {
        // ! REPLACE THIS IF NOT ALL BUILDINGS FOLLOW THIS PATTERN
        return abs(roomId.toInt()) / 1000;
    }

    private fun getFloor(roomId: Int): Int {
        // ! REPLACE THIS IF NOT ALL BUILDINGS FOLLOW THIS PATTERN
        return abs(roomId) / 1000;
    }
}
