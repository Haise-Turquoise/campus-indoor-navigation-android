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

    private fun addEdge(buildingId: Int, r1: Int, r2: Int) {
        val n1 = buildings[buildingId]!!.plans[getFloor(r1)]!!.nodes[r1]!!
        val n2 = buildings[buildingId]!!.plans[getFloor(r2)]!!.nodes[r2]!!

        if(getFloor(r1) == getFloor(r2)) {
            Log.w("MapRepository", "Attempted to add invalid stair edge $r1 -- $r2")
            return
        }

        n1.adj[n2] = 1.0;
        n2.adj[n1] = 1.0;
    }

    // TODO: Delete this function, it's used only for verification of graph models
    private fun drawGraph() {
        // Load floor plan copy
        val img = (applicationContext.resources.getDrawable(R.drawable.e7f7hi, null) as BitmapDrawable).bitmap
        val copy = img.copy(img.config ?: Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(copy)
        val paint = Paint()

        paint.strokeWidth = WIDTH_PATH
        paint.color = COLOR_LINE

        for(n in buildings[1]!!.plans[7]!!.nodes.values)
            for(nn in n.adj.keys)
                canvas.drawLine(
                    n.x.toFloat(),
                    n.y.toFloat(),
                    nn.x.toFloat(),
                    nn.y.toFloat(),
                    paint
                )

        for(n in buildings[1]!!.plans[7]!!.nodes.values) {
            // TODO: this is kind of gross, maybe add more const colors with names that make sense

            paint.color = when(n.type) {
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

        val file = File(applicationContext.cacheDir, "temp_full_graph.png")
        val outStream = FileOutputStream(file)
        copy.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.flush()
        outStream.close()
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
            R.drawable.uwlogo
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
            R.drawable.e7f1
        )

        val f1 = buildings[1]!!.plans[1]!!

        buildings[1]!!.plans[2] = FloorMap(
            2,
            HashMap(),
            R.drawable.e7f2
        )

        val f2 = buildings[1]!!.plans[2]!!

        buildings[1]!!.plans[3] = FloorMap(
            3,
            HashMap(),
            R.drawable.e7f3
        )

        val f3 = buildings[1]!!.plans[3]!!

        buildings[1]!!.plans[4] = FloorMap(
            4,
            HashMap(),
            R.drawable.e7f4hi
        )

        val f4 = buildings[1]!!.plans[4]!!

        buildings[1]!!.plans[5] = FloorMap(
            5,
            HashMap(),
            R.drawable.e7f5hi
        )

        val f5 = buildings[1]!!.plans[5]!!

        buildings[1]!!.plans[6] = FloorMap(
            6,
            HashMap(),
            R.drawable.e7f6hi
        )

        val f6 = buildings[1]!!.plans[6]!!

        buildings[1]!!.plans[7] = FloorMap(
            7,
            HashMap(),
            R.drawable.e7f7hi
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
            f1.nodes[-1000] = MapNode(222, 90, NodeType.PORT, -1000, HashMap(), f1)
            f1.nodes[-1200] = MapNode(222, 102, NodeType.STAIR, -1200, HashMap(), f1)
            f1.nodes[1324] = MapNode(222, 185, NodeType.ROOM, 1324, HashMap(), f1)
            f1.nodes[1326] = MapNode(222, 240, NodeType.ROOM, 1326, HashMap(), f1)
            f1.nodes[1327] = MapNode(222, 240, NodeType.ROOM, 1327, HashMap(), f1)
            f1.nodes[-1201] = MapNode(222, 284, NodeType.STAIR, -1201, HashMap(), f1)
            f1.nodes[-1500] = MapNode(222, 316, NodeType.NONE, -1500, HashMap(), f1)
            f1.nodes[-1501] = MapNode(222, 343, NodeType.PORT, -1501, HashMap(), f1)
            addEdge(f1, -1000, -1200)
            addEdge(f1, -1200, 1324)
            addEdge(f1, 1324, 1326)
            addEdge(f1, 1326, 1327)
            addEdge(f1, 1327, -1201)
            addEdge(f1, -1201, -1500)
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
            f1.nodes[-1202] = MapNode(345, 373, NodeType.STAIR, -1202, HashMap(), f1)

            // COLUMN 2 X 3
            addEdge(f1, -1504, -1202)

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
            f1.nodes[-1004] = MapNode(680, 316, NodeType.PORT, -1004, HashMap(), f1)
            f1.nodes[-1203] = MapNode(680, 282, NodeType.STAIR, -1202, HashMap(), f1)
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
            f2.nodes[-2200] = MapNode(222, 102, NodeType.STAIR, -2200, HashMap(), f2)
            f2.nodes[2324] = MapNode(222, 128, NodeType.ROOM, 2324, HashMap(), f2)
            f2.nodes[2328] = MapNode(222, 259, NodeType.ROOM, 2328, HashMap(), f2)
            f2.nodes[-2201] = MapNode(222, 284, NodeType.STAIR, -2201, HashMap(), f2)
            f2.nodes[2334] = MapNode(222, 352, NodeType.ROOM, 2334, HashMap(), f2)
            f2.nodes[-2000] = MapNode(222, 393, NodeType.PORT, -2000, HashMap(), f2)
            addEdge(f2, -2200, 2324)
            addEdge(f2, 2324, 2328)
            addEdge(f2, 2328, -2201)
            addEdge(f2, -2201, 2334)
            addEdge(f2, 2334, -2000)

            // ROW 1 (TOPMOST)
            f2.nodes[2322] = MapNode(250, 128, NodeType.ROOM, 2322, HashMap(), f2)
            f2.nodes[2318] = MapNode(271, 128, NodeType.ROOM, 2318, HashMap(), f2)
            f2.nodes[2316] = MapNode(294, 128, NodeType.ROOM, 2316, HashMap(), f2)
            f2.nodes[2317] = MapNode(310, 128, NodeType.ROOM, 2317, HashMap(), f2)
            f2.nodes[2314] = MapNode(316, 128, NodeType.ROOM, 2314, HashMap(), f2)
            f2.nodes[2312] = MapNode(339, 128, NodeType.ROOM, 2312, HashMap(), f2)
            f2.nodes[2357] = MapNode(356, 128, NodeType.ROOM, 2357, HashMap(), f2)
            f2.nodes[2308] = MapNode(360, 128, NodeType.ROOM, 2308, HashMap(), f2)
            f2.nodes[2306] = MapNode(384, 128, NodeType.ROOM, 2306, HashMap(), f2)
            f2.nodes[2304] = MapNode(395, 128, NodeType.ROOM, 2304, HashMap(), f2)
            f2.nodes[-2500] = MapNode(446, 128, NodeType.NONE, -2500, HashMap(), f2)
            f2.nodes[2301] = MapNode(473, 128, NodeType.ROOM, 2301, HashMap(), f2)
            f2.nodes[-2501] = MapNode(500, 128, NodeType.NONE, -2501, HashMap(), f2)
            f2.nodes[2402] = MapNode(529, 128, NodeType.ROOM, 2402, HashMap(), f2)
            f2.nodes[2404] = MapNode(551, 128, NodeType.ROOM, 2404, HashMap(), f2)
            f2.nodes[2911] = MapNode(564, 128, NodeType.ROOM, 2911, HashMap(), f2)
            f2.nodes[2406] = MapNode(574, 128, NodeType.ROOM, 2406, HashMap(), f2)
            f2.nodes[2408] = MapNode(596, 128, NodeType.ROOM, 2408, HashMap(), f2)
            f2.nodes[2409] = MapNode(596, 128, NodeType.ROOM, 2409, HashMap(), f2)
            f2.nodes[2412] = MapNode(619, 128, NodeType.ROOM, 2412, HashMap(), f2)
            f2.nodes[2414] = MapNode(641, 128, NodeType.ROOM, 2414, HashMap(), f2)
            f2.nodes[2416] = MapNode(664, 128, NodeType.ROOM, 2416, HashMap(), f2)
            f2.nodes[2418] = MapNode(686, 128, NodeType.ROOM, 2418, HashMap(), f2)
            f2.nodes[2422] = MapNode(708, 128, NodeType.ROOM, 2422, HashMap(), f2)
            f2.nodes[2423] = MapNode(725, 128, NodeType.ROOM, 2423, HashMap(), f2)
            f2.nodes[2424] = MapNode(730, 128, NodeType.ROOM, 2424, HashMap(), f2)
            f2.nodes[2426] = MapNode(753, 128, NodeType.ROOM, 2426, HashMap(), f2)
            f2.nodes[2428] = MapNode(775, 128, NodeType.ROOM, 2428, HashMap(), f2)
            f2.nodes[2432] = MapNode(786, 128, NodeType.ROOM, 2432, HashMap(), f2)

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
            f2.nodes[2302] = MapNode(395, 105, NodeType.ROOM, 2302, HashMap(), f2)
            addEdge(f2, 2304, 2302)

            // COLUMN 1 X ROW 1
            addEdge(f2, 2324, 2322)

            // INNER 1
            f2.nodes[2342] = MapNode(249, 259, NodeType.ROOM, 2342, HashMap(), f2)
            f2.nodes[2344] = MapNode(271, 259, NodeType.ROOM, 2344, HashMap(), f2)
            f2.nodes[2343] = MapNode(279, 259, NodeType.ROOM, 2343, HashMap(), f2)
            f2.nodes[2346] = MapNode(294, 259, NodeType.ROOM, 2346, HashMap(), f2)
            f2.nodes[2348] = MapNode(319, 259, NodeType.ROOM, 2348, HashMap(), f2)
            f2.nodes[2349] = MapNode(324, 259, NodeType.ROOM, 2349, HashMap(), f2)
            f2.nodes[2352] = MapNode(334, 259, NodeType.ROOM, 2352, HashMap(), f2)
            f2.nodes[2317] = MapNode(334, 218, NodeType.ROOM, 2317, HashMap(), f2)
            f2.nodes[2357] = MapNode(346, 218, NodeType.ROOM, 2357, HashMap(), f2)
            addEdge(f2, 2342, 2344)
            addEdge(f2, 2344, 2343)
            addEdge(f2, 2343, 2346)
            addEdge(f2, 2346, 2348)
            addEdge(f2, 2348, 2349)
            addEdge(f2, 2349, 2352)
            addEdge(f2, 2352, 2317)
            addEdge(f2, 2317, 2357)

            // COLUMN 1 X INNER 1
            addEdge(f2, 2328, 2342)

            // COLUMN 2
            f2.nodes[-2502] = MapNode(446, 218, NodeType.NONE, -2502, HashMap(), f2)
            f2.nodes[-2202] = MapNode(446, 373, NodeType.STAIR, -2202, HashMap(), f2)
            f2.nodes[-2503] = MapNode(446, 393, NodeType.NONE, -2503, HashMap(), f2)
            addEdge(f2, -2502, -2202)
            addEdge(f2, -2202, -2503)

            // ROW 1 X COLUMN 2
            addEdge(f2, -2500, -2502)
            // INNER 1 X COLUMN 2
            addEdge(f2, 2357, -2502)
            // COLUMN 1 X COLUMN 2
            addEdge(f2, -2000, -2503)

            // COLUMN 3
            f2.nodes[-2203] = MapNode(500, 150, NodeType.STAIR, -2203, HashMap(), f2)
            f2.nodes[-2100] = MapNode(500, 185, NodeType.WC, -2100, HashMap(), f2)
            f2.nodes[-2101] = MapNode(500, 195, NodeType.WC, -2101, HashMap(), f2)
            f2.nodes[-2504] = MapNode(500, 260, NodeType.NONE, -2504, HashMap(), f2)
            f2.nodes[-2505] = MapNode(500, 314, NodeType.NONE, -2505, HashMap(), f2)
            f2.nodes[-2506] = MapNode(500, 393, NodeType.NONE, -2506, HashMap(), f2)
            addEdge(f2, -2203, -2100)
            addEdge(f2, -2100, -2101)
            addEdge(f2, -2101, -2504)
            addEdge(f2, -2504, -2505)
            addEdge(f2, -2505, -2506)

            // ROW 1 X COLUMN 3
            addEdge(f2, -2501, -2203)

            // ROW 2
            f2.nodes[2466] = MapNode(530, 260, NodeType.ROOM, 2466, HashMap(), f2)
            f2.nodes[-2102] = MapNode(538, 260, NodeType.WC, -2102, HashMap(), f2)
            f2.nodes[2464] = MapNode(551, 260, NodeType.ROOM, 2464, HashMap(), f2)
            f2.nodes[2466] = MapNode(530, 260, NodeType.ROOM, 2466, HashMap(), f2)
            f2.nodes[2462] = MapNode(563, 260, NodeType.ROOM, 2462, HashMap(), f2)
            f2.nodes[2916] = MapNode(572, 260, NodeType.ROOM, 2916, HashMap(), f2)
            f2.nodes[2458] = MapNode(597, 260, NodeType.ROOM, 2458, HashMap(), f2)
            f2.nodes[2409] = MapNode(679, 260, NodeType.ROOM, 2409, HashMap(), f2)
            f2.nodes[2453] = MapNode(736, 260, NodeType.ROOM, 2453, HashMap(), f2)
            f2.nodes[2452] = MapNode(767, 260, NodeType.ROOM, 2452, HashMap(), f2)
            f2.nodes[2448] = MapNode(786, 260, NodeType.ROOM, 2448, HashMap(), f2)
            f2.nodes[2917] = MapNode(803, 260, NodeType.ROOM, 2917, HashMap(), f2)
            f2.nodes[2446] = MapNode(814, 260, NodeType.ROOM, 2446, HashMap(), f2)
            addEdge(f2, 2466, -2102)
            addEdge(f2, -2102, 2464)
            addEdge(f2, 2464, 2466)
            addEdge(f2, 2466, 2462)
            addEdge(f2, 2462, 2916)
            addEdge(f2, 2916, 2458)
            addEdge(f2, 2458, 2409)
            addEdge(f2, 2409, 2453)
            addEdge(f2, 2453, 2452)
            addEdge(f2, 2452, 2448)
            addEdge(f2, 2448, 2917)
            addEdge(f2, 2917, 2446)

            // COLUMN 3 X ROW 2
            addEdge(f2, -2504, 2466)

            // COLUMN 4 (TOP RIGHT)
            f2.nodes[2429] = MapNode(786, 141, NodeType.ROOM, 2429, HashMap(), f2)
            f2.nodes[2434] = MapNode(786, 141, NodeType.ROOM, 2434, HashMap(), f2)
            f2.nodes[2433] = MapNode(786, 164, NodeType.ROOM, 2433, HashMap(), f2)
            f2.nodes[2436] = MapNode(786, 164, NodeType.ROOM, 2436, HashMap(), f2)
            f2.nodes[2437] = MapNode(786, 188, NodeType.ROOM, 2437, HashMap(), f2)
            f2.nodes[2438] = MapNode(786, 188, NodeType.ROOM, 2438, HashMap(), f2)
            f2.nodes[2439] = MapNode(786, 211, NodeType.ROOM, 2439, HashMap(), f2)
            f2.nodes[2442] = MapNode(786, 211, NodeType.ROOM, 2442, HashMap(), f2)
            f2.nodes[2441] = MapNode(786, 234, NodeType.ROOM, 2441, HashMap(), f2)
            f2.nodes[2444] = MapNode(786, 234, NodeType.ROOM, 2444, HashMap(), f2)
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
            f2.nodes[2472] = MapNode(595, 314, NodeType.ROOM, 2472, HashMap(), f2)
            f2.nodes[2456] = MapNode(641, 314, NodeType.ROOM, 2456, HashMap(), f2)
            f2.nodes[2454] = MapNode(652, 314, NodeType.ROOM, 2454, HashMap(), f2)
            addEdge(f2, 2472, 2456)
            addEdge(f2, 2456, 2454)

            // COLUMN 3 X ROW 3
            addEdge(f2, -2505, 2472)

            // ROW 4
            f2.nodes[-2001] = MapNode(473, 393, NodeType.PORT, -2001, HashMap(), f2)
            // COLUMN 2 X ROW 4
            addEdge(f2, -2503, -2001)
            // ROW 4 X COLUMN 3
            addEdge(f2, -2001, -2506)

            // COLUMN 5
            f2.nodes[-2204] = MapNode(679, 260, NodeType.STAIR, -2204, HashMap(), f2)
            f2.nodes[-2002] = MapNode(679, 393, NodeType.PORT, -2002, HashMap(), f2)
            addEdge(f2, -2204, -2002)
            // ROW 2 X COLUMN 5
            addEdge(f2, 2409, -2204)
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
            f3.nodes[3324] = MapNode(175, 128, NodeType.ROOM, 3324, HashMap(), f3)
            f3.nodes[3326] = MapNode(175, 141, NodeType.ROOM, 3326, HashMap(), f3)
            f3.nodes[3327] = MapNode(175, 145, NodeType.ROOM, 3327, HashMap(), f3)
            f3.nodes[3328] = MapNode(175, 163, NodeType.ROOM, 3328, HashMap(), f3)
            f3.nodes[3332] = MapNode(175, 187, NodeType.ROOM, 3332, HashMap(), f3)
            f3.nodes[3343] = MapNode(175, 187, NodeType.ROOM, 3343, HashMap(), f3)
            f3.nodes[3334] = MapNode(175, 210, NodeType.ROOM, 3334, HashMap(), f3)
            f3.nodes[3336] = MapNode(175, 233, NodeType.ROOM, 3336, HashMap(), f3)
            f3.nodes[3338] = MapNode(175, 256, NodeType.ROOM, 3338, HashMap(), f3)
            addEdge(f3, 3324, 3326)
            addEdge(f3, 3326, 3327)
            addEdge(f3, 3327, 3328)
            addEdge(f3, 3328, 3332)
            addEdge(f3, 3332, 3343)
            addEdge(f3, 3343, 3334)
            addEdge(f3, 3334, 3336)
            addEdge(f3, 3336, 3338)

            // ROW 1 (TOPMOST)
            f3.nodes[3921] = MapNode(175, 128, NodeType.ROOM, 3921, HashMap(), f3)
            f3.nodes[-3500] = MapNode(221, 128, NodeType.NONE, -3500, HashMap(), f3)
            f3.nodes[3322] = MapNode(250, 128, NodeType.ROOM, 3322, HashMap(), f3)
            f3.nodes[3318] = MapNode(272, 128, NodeType.ROOM, 3318, HashMap(), f3)
            f3.nodes[3316] = MapNode(295, 128, NodeType.ROOM, 3316, HashMap(), f3)
            f3.nodes[3314] = MapNode(318, 128, NodeType.ROOM, 3314, HashMap(), f3)
            f3.nodes[3312] = MapNode(340, 128, NodeType.ROOM, 3312, HashMap(), f3)
            f3.nodes[3308] = MapNode(361, 128, NodeType.ROOM, 3308, HashMap(), f3)
            f3.nodes[3306] = MapNode(384, 128, NodeType.ROOM, 3306, HashMap(), f3)
            f3.nodes[3353] = MapNode(384, 128, NodeType.ROOM, 3353, HashMap(), f3)
            f3.nodes[3304] = MapNode(406, 128, NodeType.ROOM, 3304, HashMap(), f3)
            f3.nodes[3302] = MapNode(418, 128, NodeType.ROOM, 3302, HashMap(), f3)
            f3.nodes[3303] = MapNode(421, 128, NodeType.ROOM, 3303, HashMap(), f3)
            f3.nodes[-3501] = MapNode(491, 128, NodeType.NONE, -3501, HashMap(), f3)
            f3.nodes[-3502] = MapNode(501, 128, NodeType.NONE, -3502, HashMap(), f3)
            f3.nodes[3402] = MapNode(528, 128, NodeType.ROOM, 3402, HashMap(), f3)
            f3.nodes[3404] = MapNode(552, 128, NodeType.ROOM, 3404, HashMap(), f3)
            f3.nodes[3911] = MapNode(564, 128, NodeType.ROOM, 3911, HashMap(), f3)
            f3.nodes[3406] = MapNode(574, 128, NodeType.ROOM, 3406, HashMap(), f3)
            f3.nodes[3408] = MapNode(597, 128, NodeType.ROOM, 3408, HashMap(), f3)
            f3.nodes[3412] = MapNode(618, 128, NodeType.ROOM, 3412, HashMap(), f3)
            f3.nodes[3414] = MapNode(641, 128, NodeType.ROOM, 3414, HashMap(), f3)
            f3.nodes[3417] = MapNode(645, 128, NodeType.ROOM, 3417, HashMap(), f3)
            f3.nodes[3416] = MapNode(664, 128, NodeType.ROOM, 3416, HashMap(), f3)
            f3.nodes[3419] = MapNode(680, 128, NodeType.ROOM, 3419, HashMap(), f3)
            f3.nodes[3418] = MapNode(686, 128, NodeType.ROOM, 3418, HashMap(), f3)
            f3.nodes[3423] = MapNode(700, 128, NodeType.ROOM, 3423, HashMap(), f3)
            f3.nodes[3422] = MapNode(708, 128, NodeType.ROOM, 3422, HashMap(), f3)
            f3.nodes[3424] = MapNode(731, 128, NodeType.ROOM, 3424, HashMap(), f3)
            f3.nodes[3427] = MapNode(731, 128, NodeType.ROOM, 3427, HashMap(), f3)
            f3.nodes[3426] = MapNode(753, 128, NodeType.ROOM, 3426, HashMap(), f3)
            f3.nodes[3428] = MapNode(775, 128, NodeType.ROOM, 3428, HashMap(), f3)
            f3.nodes[3431] = MapNode(775, 128, NodeType.ROOM, 3431, HashMap(), f3)
            f3.nodes[3432] = MapNode(796, 128, NodeType.ROOM, 3432, HashMap(), f3)
            f3.nodes[3433] = MapNode(804, 128, NodeType.ROOM, 3433, HashMap(), f3)
            f3.nodes[3434] = MapNode(816, 128, NodeType.ROOM, 3434, HashMap(), f3)
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
            addEdge(f3, 3404, 3406)
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
            f3.nodes[3921] = MapNode(221, 104, NodeType.ROOM, 3921, HashMap(), f3)
            addEdge(f3, -3500, 3921)
            f3.nodes[-3200] = MapNode(221, 104, NodeType.STAIR, -3200, HashMap(), f3)
            addEdge(f3, 3921, -3200)

            f3.nodes[-3000] = MapNode(491, 100, NodeType.PORT, -3000, HashMap(), f3)
            addEdge(f3, -3501, -3000)

            // Bottom left
            f3.nodes[-3503] = MapNode(222, 256, NodeType.NONE, -3503, HashMap(), f3)
            f3.nodes[-3504] = MapNode(222, 272, NodeType.NONE, -3504, HashMap(), f3)
            f3.nodes[-3201] = MapNode(222, 283, NodeType.STAIR, -3201, HashMap(), f3)
            f3.nodes[-3001] = MapNode(222, 295, NodeType.PORT, -3001, HashMap(), f3)
            addEdge(f3, -3503, -3504)
            addEdge(f3, -3504, -3201)
            addEdge(f3, -3201, -3001)
            f3.nodes[3342] = MapNode(241, 272, NodeType.ROOM, 3342, HashMap(), f3)
            addEdge(f3, -3504, 3342)
            f3.nodes[3344] = MapNode(293, 272, NodeType.ROOM, 3344, HashMap(), f3)
            f3.nodes[-3505] = MapNode(300, 272, NodeType.NONE, -3505, HashMap(), f3)
            f3.nodes[3346] = MapNode(309, 272, NodeType.ROOM, 3346, HashMap(), f3)
            f3.nodes[-3506] = MapNode(320, 272, NodeType.NONE, -3506, HashMap(), f3)
            f3.nodes[3348] = MapNode(359, 272, NodeType.ROOM, 3348, HashMap(), f3)
            f3.nodes[3352] = MapNode(375, 272, NodeType.ROOM, 3352, HashMap(), f3)
            f3.nodes[3354] = MapNode(426, 272, NodeType.ROOM, 3354, HashMap(), f3)
            addEdge(f3, 3342, 3344)
            addEdge(f3, 3344, -3505)
            addEdge(f3, -3505, 3346)
            addEdge(f3, 3346, -3506)
            addEdge(f3, -3506, 3348)
            addEdge(f3, 3348, 3352)
            addEdge(f3, 3352, 3354)
            f3.nodes[3343] = MapNode(300, 228, NodeType.ROOM, 3343, HashMap(), f3)
            f3.nodes[3353] = MapNode(320, 228, NodeType.ROOM, 3353, HashMap(), f3)
            addEdge(f3, -3505, 3343)
            addEdge(f3, -3506, 3353)
            addEdge(f3, 3343, 3353)

            // Bottom left x Column 1
            addEdge(f3, -3503, 3338)

            // COLUMN 2
            f3.nodes[-3203] = MapNode(501, 151, NodeType.STAIR, -3203, HashMap(), f3)
            f3.nodes[-3100] = MapNode(501, 190, NodeType.WC, -3100, HashMap(), f3) // combined both "rooms" into one node
            f3.nodes[-3507] = MapNode(501, 259, NodeType.NONE, -3507, HashMap(), f3)
            f3.nodes[-3508] = MapNode(501, 272, NodeType.NONE, -3508, HashMap(), f3)
            f3.nodes[-3204] = MapNode(501, 372, NodeType.STAIR, -3204, HashMap(), f3)
            f3.nodes[-3002] = MapNode(501, 392, NodeType.PORT, -3002, HashMap(), f3)
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
            f3.nodes[3458] = MapNode(529, 259, NodeType.ROOM, 3458, HashMap(), f3)
            f3.nodes[3914] = MapNode(539, 259, NodeType.ROOM, 3914, HashMap(), f3)
            f3.nodes[3456] = MapNode(550, 259, NodeType.ROOM, 3456, HashMap(), f3)
            f3.nodes[3916] = MapNode(571, 259, NodeType.ROOM, 3916, HashMap(), f3)
            f3.nodes[3454] = MapNode(573, 259, NodeType.ROOM, 3454, HashMap(), f3)
            f3.nodes[3452] = MapNode(597, 259, NodeType.ROOM, 3452, HashMap(), f3)
            f3.nodes[3448] = MapNode(618, 259, NodeType.ROOM, 3448, HashMap(), f3)
            f3.nodes[3449] = MapNode(637, 259, NodeType.ROOM, 3449, HashMap(), f3)
            f3.nodes[3446] = MapNode(641, 259, NodeType.ROOM, 3446, HashMap(), f3)
            f3.nodes[3444] = MapNode(664, 259, NodeType.ROOM, 3444, HashMap(), f3)
            f3.nodes[-3509] = MapNode(680, 259, NodeType.NONE, -3509, HashMap(), f3)
            f3.nodes[-3206] = MapNode(680, 284, NodeType.STAIR, -3206, HashMap(), f3)
            f3.nodes[-3003] = MapNode(680, 396, NodeType.PORT, -3003, HashMap(), f3)
            f3.nodes[3447] = MapNode(700, 259, NodeType.ROOM, 3447, HashMap(), f3)
            f3.nodes[3443] = MapNode(732, 259, NodeType.ROOM, 3443, HashMap(), f3)
            f3.nodes[3442] = MapNode(767, 259, NodeType.ROOM, 3442, HashMap(), f3)
            f3.nodes[3439] = MapNode(774, 259, NodeType.ROOM, 3439, HashMap(), f3)
            f3.nodes[3438] = MapNode(790, 259, NodeType.ROOM, 3438, HashMap(), f3)
            f3.nodes[3437] = MapNode(804, 259, NodeType.ROOM, 3437, HashMap(), f3)
            f3.nodes[3917] = MapNode(804, 259, NodeType.ROOM, 3917, HashMap(), f3)
            f3.nodes[3436] = MapNode(816, 259, NodeType.ROOM, 3436, HashMap(), f3)
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
            f4.nodes[4324] = MapNode(507, 433, NodeType.ROOM, 4324, HashMap(), f4)
            f4.nodes[4043] = MapNode(616, 433, NodeType.ROOM, 4043, HashMap(), f4)
            f4.nodes[4921] = MapNode(616, 433, NodeType.ROOM, 4921, HashMap(), f4)
            f4.nodes[-4500] = MapNode(755, 433, NodeType.NONE, -4500, HashMap(), f4)
            f4.nodes[4322] = MapNode(849, 433, NodeType.ROOM, 4322, HashMap(), f4)
            f4.nodes[4318] = MapNode(928, 433, NodeType.ROOM, 4318, HashMap(), f4)
            f4.nodes[4316] = MapNode(1007, 433, NodeType.ROOM, 4316, HashMap(), f4)
            f4.nodes[4314] = MapNode(1078, 433, NodeType.ROOM, 4314, HashMap(), f4)
            f4.nodes[4312] = MapNode(1155, 433, NodeType.ROOM, 4312, HashMap(), f4)
            f4.nodes[4308] = MapNode(1231, 433, NodeType.ROOM, 4308, HashMap(), f4)
            f4.nodes[4306] = MapNode(1307, 433, NodeType.ROOM, 4306, HashMap(), f4)
            f4.nodes[4304] = MapNode(1382, 433, NodeType.ROOM, 4304, HashMap(), f4)
            f4.nodes[4302] = MapNode(1420, 433, NodeType.ROOM, 4302, HashMap(), f4)
            f4.nodes[4053] = MapNode(1441, 433, NodeType.ROOM, 4053, HashMap(), f4)
            f4.nodes[4301] = MapNode(1703, 433, NodeType.ROOM, 4301, HashMap(), f4)
            f4.nodes[4402] = MapNode(1800, 433, NodeType.ROOM, 4402, HashMap(), f4)
            f4.nodes[4404] = MapNode(1875, 433, NodeType.ROOM, 4404, HashMap(), f4)
            f4.nodes[4911] = MapNode(1920, 433, NodeType.ROOM, 4911, HashMap(), f4)
            f4.nodes[4406] = MapNode(1950, 433, NodeType.ROOM, 4406, HashMap(), f4)
            f4.nodes[4408] = MapNode(2028, 433, NodeType.ROOM, 4408, HashMap(), f4)
            f4.nodes[4417] = MapNode(2028, 433, NodeType.ROOM, 4417, HashMap(), f4)
            f4.nodes[4412] = MapNode(2104, 433, NodeType.ROOM, 4412, HashMap(), f4)
            f4.nodes[4414] = MapNode(2180, 433, NodeType.ROOM, 4414, HashMap(), f4)
            f4.nodes[4416] = MapNode(2256, 433, NodeType.ROOM, 4416, HashMap(), f4)
            f4.nodes[4418] = MapNode(2335, 433, NodeType.ROOM, 4418, HashMap(), f4)
            f4.nodes[4422] = MapNode(2410, 433, NodeType.ROOM, 4422, HashMap(), f4)
            f4.nodes[4433] = MapNode(2460, 433, NodeType.ROOM, 4433, HashMap(), f4)
            f4.nodes[4424] = MapNode(2484, 433, NodeType.ROOM, 4424, HashMap(), f4)
            f4.nodes[4426] = MapNode(2561, 433, NodeType.ROOM, 4426, HashMap(), f4)
            f4.nodes[4428] = MapNode(2636, 433, NodeType.ROOM, 4428, HashMap(), f4)
            f4.nodes[4432] = MapNode(2713, 433, NodeType.ROOM, 4432, HashMap(), f4)
            f4.nodes[4434] = MapNode(2787, 433, NodeType.ROOM, 4434, HashMap(), f4)
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
            f4.nodes[4919] = MapNode(755, 370, NodeType.ROOM, 4919, HashMap(), f4)
            f4.nodes[-4200] = MapNode(755, 350, NodeType.STAIR, -4200, HashMap(), f4)
            addEdge(f4, -4500, 4919)
            addEdge(f4, 4919, -4200)

            // ROW 2
            f4.nodes[4338] = MapNode(507, 901, NodeType.ROOM, 4338, HashMap(), f4)
            f4.nodes[4043] = MapNode(755, 901, NodeType.ROOM, 4043, HashMap(), f4)
            f4.nodes[4342] = MapNode(907, 901, NodeType.ROOM, 4342, HashMap(), f4)
            f4.nodes[4346] = MapNode(1137, 901, NodeType.ROOM, 4346, HashMap(), f4)
            f4.nodes[4053] = MapNode(1261, 901, NodeType.ROOM, 4053, HashMap(), f4)
            f4.nodes[4352] = MapNode(1365, 901, NodeType.ROOM, 4352, HashMap(), f4)
            f4.nodes[4356] = MapNode(1539, 901, NodeType.ROOM, 4356, HashMap(), f4)
            f4.nodes[-4501] = MapNode(1703, 901, NodeType.NONE, -4501, HashMap(), f4)
            f4.nodes[4458] = MapNode(1835, 901, NodeType.ROOM, 4458, HashMap(), f4)
            f4.nodes[-4100] = MapNode(1835, 901, NodeType.WC, -4100, HashMap(), f4)
            f4.nodes[4916] = MapNode(1944, 901, NodeType.ROOM, 4916, HashMap(), f4)
            f4.nodes[4446] = MapNode(2011, 901, NodeType.ROOM, 4446, HashMap(), f4)
            f4.nodes[4444] = MapNode(2151, 901, NodeType.ROOM, 4444, HashMap(), f4)
            f4.nodes[4417] = MapNode(2312, 901, NodeType.ROOM, 4417, HashMap(), f4)
            f4.nodes[4437] = MapNode(2460, 901, NodeType.ROOM, 4437, HashMap(), f4)
            f4.nodes[4442] = MapNode(2610, 901, NodeType.ROOM, 4442, HashMap(), f4)
            f4.nodes[4438] = MapNode(2687, 901, NodeType.ROOM, 4438, HashMap(), f4)
            f4.nodes[4917] = MapNode(2729, 901, NodeType.ROOM, 4917, HashMap(), f4)
            f4.nodes[4436] = MapNode(2765, 901, NodeType.ROOM, 4436, HashMap(), f4)
            addEdge(f4, 4338, 4043)
            addEdge(f4, 4043, 4342)
            addEdge(f4, 4342, 4346)
            addEdge(f4, 4346, 4053)
            addEdge(f4, 4053, 4352)
            addEdge(f4, 4352, 4356)
            addEdge(f4, 4356, -4501)
            addEdge(f4, -4501, 4458)
            addEdge(f4, 4458, -4100)
            addEdge(f4, -4100, 4916)
            addEdge(f4, 4916, 4446)
            addEdge(f4, 4446, 4444)
            addEdge(f4, 4444, 4417)
            addEdge(f4, 4417, 4437)
            addEdge(f4, 4437, 4442)
            addEdge(f4, 4442, 4438)
            addEdge(f4, 4438, 4917)
            addEdge(f4, 4917, 4436)

            // ROW 1 X ROW 2
            addEdge(f4, 4324, 4338)
            addEdge(f4, 4434, 4436)

            // Left and right offshoots from Row 2
            f4.nodes[-4201] = MapNode(755, 967, NodeType.STAIR, -4201, HashMap(), f4)
            f4.nodes[-4000] = MapNode(755, 1347, NodeType.PORT, -4000, HashMap(), f4)
            addEdge(f4, 4043, -4201)
            addEdge(f4, -4000, -4201)

            f4.nodes[-4202] = MapNode(2312, 967, NodeType.STAIR, -4202, HashMap(), f4)
            f4.nodes[-4001] = MapNode(2312, 1347, NodeType.PORT, -4001, HashMap(), f4)
            addEdge(f4, 4417, -4202)
            addEdge(f4, -4001, -4202)
        
            // Middle Column
            f4.nodes[-4002] = MapNode(1703, 365, NodeType.PORT, -4002, HashMap(), f4)
            f4.nodes[-4203] = MapNode(1703, 515, NodeType.STAIR, -4203, HashMap(), f4)
            f4.nodes[-4101] = MapNode(1703, 651, NodeType.WC, -4101, HashMap(), f4)
            f4.nodes[-4204] = MapNode(1703, 1267, NodeType.STAIR, -4204, HashMap(), f4)
            f4.nodes[-4003] = MapNode(1703, 1337, NodeType.PORT, -4003, HashMap(), f4)
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
            f5.nodes[5324] = MapNode(507, 433, NodeType.ROOM, 5324, HashMap(), f5)
            f5.nodes[5343] = MapNode(616, 433, NodeType.ROOM, 5343, HashMap(), f5)
            f5.nodes[5921] = MapNode(616, 433, NodeType.ROOM, 5921, HashMap(), f5)
            f5.nodes[-5500] = MapNode(755, 433, NodeType.NONE, -5500, HashMap(), f5)
            f5.nodes[5322] = MapNode(849, 433, NodeType.ROOM, 5322, HashMap(), f5)
            f5.nodes[5318] = MapNode(928, 433, NodeType.ROOM, 5318, HashMap(), f5)
            f5.nodes[5316] = MapNode(1007, 433, NodeType.ROOM, 5316, HashMap(), f5)
            f5.nodes[5314] = MapNode(1078, 433, NodeType.ROOM, 5314, HashMap(), f5)
            f5.nodes[5312] = MapNode(1155, 433, NodeType.ROOM, 5312, HashMap(), f5)
            f5.nodes[5308] = MapNode(1231, 433, NodeType.ROOM, 5308, HashMap(), f5)
            f5.nodes[5306] = MapNode(1307, 433, NodeType.ROOM, 5306, HashMap(), f5)
            f5.nodes[5304] = MapNode(1382, 433, NodeType.ROOM, 5304, HashMap(), f5)
            f5.nodes[5302] = MapNode(1420, 433, NodeType.ROOM, 5302, HashMap(), f5)
            f5.nodes[5353] = MapNode(1441, 433, NodeType.ROOM, 5353, HashMap(), f5)
            f5.nodes[5301] = MapNode(1703, 433, NodeType.ROOM, 5301, HashMap(), f5)
            f5.nodes[5402] = MapNode(1800, 433, NodeType.ROOM, 5402, HashMap(), f5)
            f5.nodes[5404] = MapNode(1875, 433, NodeType.ROOM, 5404, HashMap(), f5)
            f5.nodes[5911] = MapNode(1920, 433, NodeType.ROOM, 5911, HashMap(), f5)
            f5.nodes[5406] = MapNode(1950, 433, NodeType.ROOM, 5406, HashMap(), f5)
            f5.nodes[5408] = MapNode(2028, 433, NodeType.ROOM, 5408, HashMap(), f5)
            f5.nodes[5411] = MapNode(2088, 433, NodeType.ROOM, 5411, HashMap(), f5)
            f5.nodes[5412] = MapNode(2104, 433, NodeType.ROOM, 5412, HashMap(), f5)
            f5.nodes[5413] = MapNode(2165, 433, NodeType.ROOM, 5413, HashMap(), f5)
            f5.nodes[5414] = MapNode(2180, 433, NodeType.ROOM, 5414, HashMap(), f5)
            f5.nodes[5416] = MapNode(2256, 433, NodeType.ROOM, 5416, HashMap(), f5)
            f5.nodes[5417] = MapNode(2300, 433, NodeType.ROOM, 5417, HashMap(), f5)
            f5.nodes[5418] = MapNode(2335, 433, NodeType.ROOM, 5418, HashMap(), f5)
            f5.nodes[5419] = MapNode(2410, 433, NodeType.ROOM, 5419, HashMap(), f5)
            f5.nodes[5422] = MapNode(2410, 433, NodeType.ROOM, 5422, HashMap(), f5)
            f5.nodes[5421] = MapNode(2460, 433, NodeType.ROOM, 5421, HashMap(), f5)
            f5.nodes[5424] = MapNode(2484, 433, NodeType.ROOM, 5424, HashMap(), f5)
            f5.nodes[5426] = MapNode(2561, 433, NodeType.ROOM, 5426, HashMap(), f5)
            f5.nodes[5423] = MapNode(2620, 433, NodeType.ROOM, 5423, HashMap(), f5)
            f5.nodes[5428] = MapNode(2636, 433, NodeType.ROOM, 5428, HashMap(), f5)
            f5.nodes[5427] = MapNode(2675, 433, NodeType.ROOM, 5427, HashMap(), f5)
            f5.nodes[5432] = MapNode(2713, 433, NodeType.ROOM, 5432, HashMap(), f5)
            f5.nodes[5434] = MapNode(2775, 433, NodeType.ROOM, 5434, HashMap(), f5)
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
            f5.nodes[5919] = MapNode(755, 370, NodeType.ROOM, 5919, HashMap(), f5)
            f5.nodes[-5200] = MapNode(755, 350, NodeType.STAIR, -5200, HashMap(), f5)
            addEdge(f5, -5500, 5919)
            addEdge(f5, 5919, -5200)

            // ROW 2
            f5.nodes[5338] = MapNode(507, 885, NodeType.ROOM, 5338, HashMap(), f5)
            f5.nodes[5343] = MapNode(752, 885, NodeType.ROOM, 5343, HashMap(), f5)
            f5.nodes[5342] = MapNode(913, 885, NodeType.ROOM, 5342, HashMap(), f5)
            f5.nodes[5344] = MapNode(1139, 885, NodeType.ROOM, 5344, HashMap(), f5)
            f5.nodes[5353] = MapNode(1255, 885, NodeType.ROOM, 5353, HashMap(), f5)
            f5.nodes[5352] = MapNode(1368, 885, NodeType.ROOM, 5352, HashMap(), f5)
            f5.nodes[-5501] = MapNode(1703, 885, NodeType.NONE, -5501, HashMap(), f5)
            f5.nodes[5458] = MapNode(1796, 885, NodeType.ROOM, 5458, HashMap(), f5)
            f5.nodes[5914] = MapNode(1834, 885, NodeType.ROOM, 5914, HashMap(), f5)
            f5.nodes[5456] = MapNode(1875, 885, NodeType.ROOM, 5456, HashMap(), f5)
            f5.nodes[5916] = MapNode(1942, 885, NodeType.ROOM, 5916, HashMap(), f5)
            f5.nodes[5454] = MapNode(1943, 885, NodeType.ROOM, 5454, HashMap(), f5)
            f5.nodes[5452] = MapNode(2025, 885, NodeType.ROOM, 5452, HashMap(), f5)
            f5.nodes[5451] = MapNode(2088, 885, NodeType.ROOM, 5451, HashMap(), f5)
            f5.nodes[5448] = MapNode(2103, 885, NodeType.ROOM, 5448, HashMap(), f5)
            f5.nodes[5449] = MapNode(2170, 885, NodeType.ROOM, 5449, HashMap(), f5)
            f5.nodes[5446] = MapNode(2179, 885, NodeType.ROOM, 5446, HashMap(), f5)
            f5.nodes[5444] = MapNode(2258, 885, NodeType.ROOM, 5444, HashMap(), f5)
            f5.nodes[5447] = MapNode(2312, 885, NodeType.ROOM, 5447, HashMap(), f5)
            f5.nodes[5443] = MapNode(2412, 885, NodeType.ROOM, 5443, HashMap(), f5)
            f5.nodes[5441] = MapNode(2468, 885, NodeType.ROOM, 5441, HashMap(), f5)
            f5.nodes[5442] = MapNode(2612, 885, NodeType.ROOM, 5442, HashMap(), f5)
            f5.nodes[5439] = MapNode(2615, 885, NodeType.ROOM, 5439, HashMap(), f5)
            f5.nodes[5437] = MapNode(2674, 885, NodeType.ROOM, 5437, HashMap(), f5)
            f5.nodes[5438] = MapNode(2688, 885, NodeType.ROOM, 5438, HashMap(), f5)
            f5.nodes[5436] = MapNode(2775, 885, NodeType.ROOM, 5436, HashMap(), f5)
            addEdge(f5, 5338, 5343)
            addEdge(f5, 5343, 5342)
            addEdge(f5, 5342, 5344)
            addEdge(f5, 5344, 5353)
            addEdge(f5, 5353, 5352)
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
            f5.nodes[-5201] = MapNode(755, 967, NodeType.STAIR, -5201, HashMap(), f5)
            f5.nodes[-5000] = MapNode(755, 1347, NodeType.PORT, -5000, HashMap(), f5)
            addEdge(f5, 5343, -5201)
            addEdge(f5, -5000, -5201)

            f5.nodes[-5202] = MapNode(2312, 967, NodeType.STAIR, -5202, HashMap(), f5)
            f5.nodes[-5001] = MapNode(2312, 1347, NodeType.PORT, -5001, HashMap(), f5)
            addEdge(f5, 5447, -5202)
            addEdge(f5, -5001, -5202)

            // Middle Column
            f5.nodes[-5203] = MapNode(1703, 515, NodeType.STAIR, -5203, HashMap(), f5)
            f5.nodes[-5101] = MapNode(1703, 651, NodeType.WC, -5101, HashMap(), f5)
            f5.nodes[-5204] = MapNode(1703, 1267, NodeType.STAIR, -5204, HashMap(), f5)
            f5.nodes[-5003] = MapNode(1703, 1337, NodeType.PORT, -5003, HashMap(), f5)
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

            f6.nodes[6324] = MapNode(595, 433, NodeType.ROOM, 6324, HashMap(), f6)
            f6.nodes[6921] = MapNode(693, 433, NodeType.ROOM, 6921, HashMap(), f6)
            f6.nodes[6323] = MapNode(719, 433, NodeType.ROOM, 6323, HashMap(), f6)
            f6.nodes[-6500] = MapNode(756, 433, NodeType.NONE, -6500, HashMap(), f6)
            f6.nodes[6321] = MapNode(826, 433, NodeType.ROOM, 6321, HashMap(), f6)
            f6.nodes[6322] = MapNode(848, 433, NodeType.ROOM, 6322, HashMap(), f6)
            f6.nodes[6318] = MapNode(927, 433, NodeType.ROOM, 6318, HashMap(), f6)
            f6.nodes[6317] = MapNode(927, 433, NodeType.ROOM, 6317, HashMap(), f6)
            f6.nodes[6316] = MapNode(1002, 433, NodeType.ROOM, 6316, HashMap(), f6)
            f6.nodes[6314] = MapNode(1081, 433, NodeType.ROOM, 6314, HashMap(), f6)
            f6.nodes[6349] = MapNode(1091, 433, NodeType.ROOM, 6349, HashMap(), f6)
            f6.nodes[6312] = MapNode(1156, 433, NodeType.ROOM, 6312, HashMap(), f6)
            f6.nodes[6309] = MapNode(1220, 433, NodeType.ROOM, 6309, HashMap(), f6)
            f6.nodes[6308] = MapNode(1231, 433, NodeType.ROOM, 6308, HashMap(), f6)
            f6.nodes[6306] = MapNode(1305, 433, NodeType.ROOM, 6306, HashMap(), f6)
            f6.nodes[6304] = MapNode(1381, 433, NodeType.ROOM, 6304, HashMap(), f6)
            f6.nodes[6302] = MapNode(1422, 433, NodeType.ROOM, 6302, HashMap(), f6)
            f6.nodes[6301] = MapNode(1703, 433, NodeType.ROOM, 6301, HashMap(), f6)
            f6.nodes[6402] = MapNode(1800, 433, NodeType.ROOM, 6402, HashMap(), f6)
            f6.nodes[6404] = MapNode(1877, 433, NodeType.ROOM, 6404, HashMap(), f6)
            f6.nodes[6911] = MapNode(1919, 433, NodeType.ROOM, 6911, HashMap(), f6)
            f6.nodes[6406] = MapNode(1951, 433, NodeType.ROOM, 6406, HashMap(), f6)
            f6.nodes[6408] = MapNode(2027, 433, NodeType.ROOM, 6408, HashMap(), f6)
            f6.nodes[6411] = MapNode(2043, 433, NodeType.ROOM, 6411, HashMap(), f6)
            f6.nodes[6412] = MapNode(2105, 433, NodeType.ROOM, 6412, HashMap(), f6)
            f6.nodes[6417] = MapNode(2173, 433, NodeType.ROOM, 6417, HashMap(), f6)
            f6.nodes[6414] = MapNode(2180, 433, NodeType.ROOM, 6414, HashMap(), f6)
            f6.nodes[6416] = MapNode(2257, 433, NodeType.ROOM, 6416, HashMap(), f6)
            f6.nodes[6418] = MapNode(2326, 433, NodeType.ROOM, 6418, HashMap(), f6)
            f6.nodes[6422] = MapNode(2408, 433, NodeType.ROOM, 6422, HashMap(), f6)
            f6.nodes[6421] = MapNode(2429, 433, NodeType.ROOM, 6421, HashMap(), f6)
            f6.nodes[6424] = MapNode(2484, 433, NodeType.ROOM, 6424, HashMap(), f6)
            f6.nodes[6423] = MapNode(2528, 433, NodeType.ROOM, 6423, HashMap(), f6)
            f6.nodes[6426] = MapNode(2559, 433, NodeType.ROOM, 6426, HashMap(), f6)
            f6.nodes[6427] = MapNode(2627, 433, NodeType.ROOM, 6427, HashMap(), f6)
            f6.nodes[6428] = MapNode(2637, 433, NodeType.ROOM, 6428, HashMap(), f6)
            f6.nodes[6432] = MapNode(2675, 433, NodeType.ROOM, 6432, HashMap(), f6)
            f6.nodes[6434] = MapNode(2746, 433, NodeType.ROOM, 6434, HashMap(), f6)
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
            f6.nodes[6919] = MapNode(756, 370, NodeType.ROOM, 6919, HashMap(), f6)
            f6.nodes[-6200] = MapNode(756, 354, NodeType.STAIR, -6200, HashMap(), f6)
            addEdge(f6, -6500, 6919)
            addEdge(f6, 6919, -6200)

            // COLUMN 1
            
            f6.nodes[6326] = MapNode(595, 433, NodeType.ROOM, 6326, HashMap(), f6)
            f6.nodes[6328] = MapNode(595, 521, NodeType.ROOM, 6328, HashMap(), f6)
            f6.nodes[6332] = MapNode(595, 680, NodeType.ROOM, 6332, HashMap(), f6)
            f6.nodes[6333] = MapNode(595, 742, NodeType.ROOM, 6333, HashMap(), f6)
            f6.nodes[6334] = MapNode(595, 759, NodeType.ROOM, 6334, HashMap(), f6)
            f6.nodes[6336] = MapNode(595, 840, NodeType.ROOM, 6336, HashMap(), f6)
            f6.nodes[6338] = MapNode(595, 876, NodeType.ROOM, 6338, HashMap(), f6)
            addEdge(f6, 6326, 6328)
            addEdge(f6, 6328, 6332)
            addEdge(f6, 6332, 6333)
            addEdge(f6, 6333, 6334)
            addEdge(f6, 6334, 6336)
            addEdge(f6, 6336, 6338)

            // ROW 1 X COLUMN 1
            addEdge(f6, 6324, 6326)

            // ROW 2
            f6.nodes[6339] = MapNode(757, 876, NodeType.ROOM, 6339, HashMap(), f6)
            f6.nodes[6342] = MapNode(851, 876, NodeType.ROOM, 6342, HashMap(), f6)
            f6.nodes[6343] = MapNode(862, 876, NodeType.ROOM, 6343, HashMap(), f6)
            f6.nodes[6344] = MapNode(927, 876, NodeType.ROOM, 6344, HashMap(), f6)
            f6.nodes[6346] = MapNode(1002, 876, NodeType.ROOM, 6346, HashMap(), f6)
            f6.nodes[6349] = MapNode(1067, 876, NodeType.ROOM, 6349, HashMap(), f6)
            f6.nodes[6348] = MapNode(1080, 876, NodeType.ROOM, 6348, HashMap(), f6)
            f6.nodes[6352] = MapNode(1154, 876, NodeType.ROOM, 6352, HashMap(), f6)
            f6.nodes[6353] = MapNode(1172, 876, NodeType.ROOM, 6353, HashMap(), f6)
            f6.nodes[6354] = MapNode(1232, 876, NodeType.ROOM, 6354, HashMap(), f6)
            f6.nodes[6356] = MapNode(1307, 876, NodeType.ROOM, 6356, HashMap(), f6)
            f6.nodes[6358] = MapNode(1381, 876, NodeType.ROOM, 6358, HashMap(), f6)
            f6.nodes[6362] = MapNode(1422, 876, NodeType.ROOM, 6362, HashMap(), f6)
            f6.nodes[6364] = MapNode(1542, 876, NodeType.ROOM, 6364, HashMap(), f6)
            f6.nodes[-6501] = MapNode(1703, 876, NodeType.NONE, -6501, HashMap(), f6)
            f6.nodes[6472] = MapNode(1801, 876, NodeType.ROOM, 6472, HashMap(), f6)
            f6.nodes[6914] = MapNode(1835, 876, NodeType.ROOM, 6914, HashMap(), f6)
            f6.nodes[6468] = MapNode(1876, 876, NodeType.ROOM, 6468, HashMap(), f6)
            f6.nodes[6916] = MapNode(1944, 876, NodeType.ROOM, 6916, HashMap(), f6)
            f6.nodes[6466] = MapNode(1954, 876, NodeType.ROOM, 6466, HashMap(), f6)
            f6.nodes[6464] = MapNode(2029, 876, NodeType.ROOM, 6464, HashMap(), f6)
            f6.nodes[6459] = MapNode(2041, 876, NodeType.ROOM, 6459, HashMap(), f6)
            f6.nodes[6462] = MapNode(2104, 876, NodeType.ROOM, 6462, HashMap(), f6)
            f6.nodes[6457] = MapNode(2173, 876, NodeType.ROOM, 6457, HashMap(), f6)
            f6.nodes[6458] = MapNode(2375, 876, NodeType.ROOM, 6458, HashMap(), f6)
            f6.nodes[6456] = MapNode(2260, 876, NodeType.ROOM, 6456, HashMap(), f6)
            f6.nodes[6453] = MapNode(2313, 876, NodeType.ROOM, 6453, HashMap(), f6)
            f6.nodes[6447] = MapNode(2527, 876, NodeType.ROOM, 6447, HashMap(), f6)
            f6.nodes[6454] = MapNode(2613, 876, NodeType.ROOM, 6454, HashMap(), f6)
            f6.nodes[6452] = MapNode(2675, 876, NodeType.ROOM, 6452, HashMap(), f6)
            f6.nodes[6448] = MapNode(2734, 876, NodeType.ROOM, 6448, HashMap(), f6)
            addEdge(f6, 6339, 6342)
            addEdge(f6, 6342, 6343)
            addEdge(f6, 6343, 6344)
            addEdge(f6, 6344, 6346)
            addEdge(f6, 6346, 6349)
            addEdge(f6, 6349, 6348)
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
            f6.nodes[-6201] = MapNode(757, 967, NodeType.STAIR, -6201, HashMap(), f6)
            f6.nodes[-6000] = MapNode(757, 1347, NodeType.PORT, -6000, HashMap(), f6)
            addEdge(f6, 6339, -6201)
            addEdge(f6, -6000, -6201)

            f6.nodes[-6202] = MapNode(2313, 967, NodeType.STAIR, -6202, HashMap(), f6)
            f6.nodes[-6001] = MapNode(2313, 1347, NodeType.PORT, -6001, HashMap(), f6)
            addEdge(f6, 6453, -6202)
            addEdge(f6, -6001, -6202)

            // COLUMN 2 
            f6.nodes[-6203] = MapNode(1703, 515, NodeType.STAIR, -6203, HashMap(), f6)
            f6.nodes[-6101] = MapNode(1703, 651, NodeType.WC, -6101, HashMap(), f6)
            f6.nodes[6303] = MapNode(1482, 651, NodeType.ROOM, 6303, HashMap(), f6)
            f6.nodes[-6204] = MapNode(1703, 1267, NodeType.STAIR, -6204, HashMap(), f6)
            f6.nodes[-6003] = MapNode(1703, 1337, NodeType.PORT, -6003, HashMap(), f6)
            addEdge(f6, 6301, -6203)
            addEdge(f6, -6203, -6101)
            addEdge(f6, -6101, -6501)
            addEdge(f6, -6101, 6303)
            addEdge(f6, -6501, -6204)
            addEdge(f6, -6204, -6003)      

            // COLUMN 3
            
            f6.nodes[6436] = MapNode(2675, 433, NodeType.ROOM, 6436, HashMap(), f6)
            f6.nodes[6438] = MapNode(2675, 521, NodeType.ROOM, 6438, HashMap(), f6)
            f6.nodes[6442] = MapNode(2675, 680, NodeType.ROOM, 6442, HashMap(), f6)
            f6.nodes[6443] = MapNode(2675, 742, NodeType.ROOM, 6443, HashMap(), f6)
            f6.nodes[6444] = MapNode(2675, 759, NodeType.ROOM, 6444, HashMap(), f6)
            f6.nodes[6446] = MapNode(2675, 840, NodeType.ROOM, 6446, HashMap(), f6)
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

            f7.nodes[7921] = MapNode(602, 433, NodeType.ROOM, 7921, HashMap(), f7)
            f7.nodes[7318] = MapNode(766, 433, NodeType.ROOM, 7318, HashMap(), f7)
            f7.nodes[7321] = MapNode(766, 433, NodeType.ROOM, 7321, HashMap(), f7)
            f7.nodes[7316] = MapNode(851, 433, NodeType.ROOM, 7316, HashMap(), f7)
            f7.nodes[7319] = MapNode(857, 433, NodeType.ROOM, 7319, HashMap(), f7)
            f7.nodes[7314] = MapNode(927, 433, NodeType.ROOM, 7314, HashMap(), f7)
            f7.nodes[7317] = MapNode(940, 433, NodeType.ROOM, 7317, HashMap(), f7)
            f7.nodes[7312] = MapNode(990, 433, NodeType.ROOM, 7312, HashMap(), f7)
            f7.nodes[7308] = MapNode(1041, 433, NodeType.ROOM, 7308, HashMap(), f7)
            f7.nodes[7306] = MapNode(1246, 433, NodeType.ROOM, 7306, HashMap(), f7)
            f7.nodes[7919] = MapNode(1246, 433, NodeType.ROOM, 7919, HashMap(), f7)
            f7.nodes[7304] = MapNode(1357, 433, NodeType.ROOM, 7304, HashMap(), f7)
            f7.nodes[7302] = MapNode(1394, 433, NodeType.ROOM, 7302, HashMap(), f7)
            f7.nodes[7301] = MapNode(1630, 433, NodeType.ROOM, 7301, HashMap(), f7)
            f7.nodes[7402] = MapNode(1800, 433, NodeType.ROOM, 7402, HashMap(), f7)
            f7.nodes[7404] = MapNode(1874, 433, NodeType.ROOM, 7404, HashMap(), f7)
            f7.nodes[7911] = MapNode(1918, 433, NodeType.ROOM, 7911, HashMap(), f7)
            f7.nodes[7406] = MapNode(1953, 433, NodeType.ROOM, 7406, HashMap(), f7)
            f7.nodes[7408] = MapNode(2027, 433, NodeType.ROOM, 7408, HashMap(), f7)
            f7.nodes[7409] = MapNode(2058, 433, NodeType.ROOM, 7409, HashMap(), f7)
            f7.nodes[7414] = MapNode(2143, 433, NodeType.ROOM, 7414, HashMap(), f7)
            f7.nodes[-7500] = MapNode(2317, 433, NodeType.NONE, -7500, HashMap(), f7)
            f7.nodes[7418] = MapNode(2408, 433, NodeType.ROOM, 7418, HashMap(), f7)
            f7.nodes[7419] = MapNode(2478, 433, NodeType.ROOM, 7419, HashMap(), f7)
            f7.nodes[7421] = MapNode(2553, 433, NodeType.ROOM, 7421, HashMap(), f7)
            f7.nodes[7422] = MapNode(2561, 433, NodeType.ROOM, 7422, HashMap(), f7)
            f7.nodes[7424] = MapNode(2598, 433, NodeType.ROOM, 7424, HashMap(), f7)
            f7.nodes[7423] = MapNode(2629, 433, NodeType.ROOM, 7423, HashMap(), f7)
            f7.nodes[7426] = MapNode(2675, 433, NodeType.ROOM, 7426, HashMap(), f7)
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
            f7.nodes[7922] = MapNode(600, 304, NodeType.ROOM, 7922, HashMap(), f7)
            f7.nodes[7324] = MapNode(564, 304, NodeType.ROOM, 7324, HashMap(), f7)
            f7.nodes[7318] = MapNode(669, 304, NodeType.ROOM, 7318, HashMap(), f7)
            addEdge(f7, 7324, 7922)
            addEdge(f7, 7922, 7318)
            f7.nodes[7412] = MapNode(2143, 386, NodeType.ROOM, 7412, HashMap(), f7)
            f7.nodes[7416] = MapNode(2143, 386, NodeType.ROOM, 7416, HashMap(), f7)
            addEdge(f7, 7414, 7412)
            addEdge(f7, 7412, 7416)

            // ROW 2

            f7.nodes[7338] = MapNode(602, 880, NodeType.ROOM, 7338, HashMap(), f7)
            f7.nodes[7337] = MapNode(691, 880, NodeType.ROOM, 7337, HashMap(), f7)
            f7.nodes[7339] = MapNode(752, 880, NodeType.ROOM, 7339, HashMap(), f7)
            f7.nodes[7341] = MapNode(857, 880, NodeType.ROOM, 7341, HashMap(), f7)
            f7.nodes[7342] = MapNode(898, 880, NodeType.ROOM, 7342, HashMap(), f7)
            f7.nodes[7343] = MapNode(938, 880, NodeType.ROOM, 7343, HashMap(), f7)
            f7.nodes[7344] = MapNode(990, 880, NodeType.ROOM, 7344, HashMap(), f7)
            f7.nodes[7346] = MapNode(1077, 880, NodeType.ROOM, 7346, HashMap(), f7)
            f7.nodes[7349] = MapNode(1144, 880, NodeType.ROOM, 7349, HashMap(), f7)
            f7.nodes[7348] = MapNode(1154, 880, NodeType.ROOM, 7348, HashMap(), f7)
            f7.nodes[7363] = MapNode(1223, 880, NodeType.ROOM, 7363, HashMap(), f7)
            f7.nodes[7352] = MapNode(1225, 880, NodeType.ROOM, 7352, HashMap(), f7)
            f7.nodes[7354] = MapNode(1305, 880, NodeType.ROOM, 7354, HashMap(), f7)
            f7.nodes[7356] = MapNode(1382, 880, NodeType.ROOM, 7356, HashMap(), f7)
            f7.nodes[7358] = MapNode(1458, 880, NodeType.ROOM, 7358, HashMap(), f7)
            f7.nodes[7362] = MapNode(1498, 880, NodeType.ROOM, 7362, HashMap(), f7)
            f7.nodes[-7501] = MapNode(1630, 880, NodeType.NONE, -7501, HashMap(), f7)
            f7.nodes[7462] = MapNode(1799, 880, NodeType.ROOM, 7462, HashMap(), f7)
            f7.nodes[7914] = MapNode(1832, 880, NodeType.ROOM, 7914, HashMap(), f7)
            f7.nodes[7458] = MapNode(1877, 880, NodeType.ROOM, 7458, HashMap(), f7)
            f7.nodes[7916] = MapNode(1942, 880, NodeType.ROOM, 7916, HashMap(), f7)
            f7.nodes[7456] = MapNode(1950, 880, NodeType.ROOM, 7456, HashMap(), f7)
            f7.nodes[7454] = MapNode(2028, 880, NodeType.ROOM, 7454, HashMap(), f7)
            f7.nodes[7453] = MapNode(2080, 880, NodeType.ROOM, 7453, HashMap(), f7)
            f7.nodes[7452] = MapNode(2104, 880, NodeType.ROOM, 7452, HashMap(), f7)
            f7.nodes[7451] = MapNode(2176, 880, NodeType.ROOM, 7451, HashMap(), f7)
            f7.nodes[7448] = MapNode(2188, 880, NodeType.ROOM, 7448, HashMap(), f7)
            f7.nodes[7449] = MapNode(2214, 880, NodeType.ROOM, 7449, HashMap(), f7)
            f7.nodes[7446] = MapNode(2268, 880, NodeType.ROOM, 7446, HashMap(), f7)
            f7.nodes[-7502] = MapNode(2317, 880, NodeType.NONE, -7502, HashMap(), f7)
            f7.nodes[7447] = MapNode(2402, 880, NodeType.ROOM, 7447, HashMap(), f7)
            f7.nodes[7443] = MapNode(2476, 880, NodeType.ROOM, 7443, HashMap(), f7)
            f7.nodes[7441] = MapNode(2553, 880, NodeType.ROOM, 7441, HashMap(), f7)
            f7.nodes[7444] = MapNode(2611, 880, NodeType.ROOM, 7444, HashMap(), f7)
            f7.nodes[7442] = MapNode(2675, 880, NodeType.ROOM, 7442, HashMap(), f7)
            f7.nodes[7917] = MapNode(2732, 880, NodeType.ROOM, 7917, HashMap(), f7)
            f7.nodes[7438] = MapNode(2764, 880, NodeType.ROOM, 7438, HashMap(), f7)
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
            addEdge(f7, 7462, 7458)
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
            f7.nodes[-7200] = MapNode(752, 965, NodeType.STAIR, -7200, HashMap(), f7)
            addEdge(f7, 7339, -7200)

            // COLUMNS

            f7.nodes[7326] = MapNode(602, 537, NodeType.ROOM, 7326, HashMap(), f7)
            f7.nodes[7328] = MapNode(602, 572, NodeType.ROOM, 7328, HashMap(), f7)
            f7.nodes[7331] = MapNode(602, 656, NodeType.ROOM, 7331, HashMap(), f7)
            f7.nodes[7334] = MapNode(602, 755, NodeType.ROOM, 7334, HashMap(), f7)
            f7.nodes[7336] = MapNode(602, 834, NodeType.ROOM, 7336, HashMap(), f7)
            addEdge(f7, 7921, 7326)
            addEdge(f7, 7326, 7328)
            addEdge(f7, 7328, 7331)
            addEdge(f7, 7331, 7334)
            addEdge(f7, 7334, 7336)
            addEdge(f7, 7336, 7338)

            f7.nodes[7353] = MapNode(990, 558, NodeType.ROOM, 7353, HashMap(), f7)
            f7.nodes[7347] = MapNode(990, 618, NodeType.ROOM, 7347, HashMap(), f7)
            f7.nodes[7351] = MapNode(990, 618, NodeType.ROOM, 7351, HashMap(), f7)
            addEdge(f7, 7312, 7353)
            addEdge(f7, 7353, 7347)
            addEdge(f7, 7347, 7351)
            addEdge(f7, 7351, 7344)

            f7.nodes[-7201] = MapNode(1630, 513, NodeType.STAIR, -7201, HashMap(), f7)
            f7.nodes[7303] = MapNode(1630, 626, NodeType.ROOM, 7303, HashMap(), f7)
            f7.nodes[-7100] = MapNode(1630, 649, NodeType.WC, -7100, HashMap(), f7)
            f7.nodes[7363] = MapNode(1630, 719, NodeType.ROOM, 7363, HashMap(), f7)
            f7.nodes[-7202] = MapNode(1630, 1017, NodeType.STAIR, -7201, HashMap(), f7)
            addEdge(f7, 7301, -7201)
            addEdge(f7, -7201, 7303)
            addEdge(f7, 7303, -7100)
            addEdge(f7, -7100, 7363)
            addEdge(f7, 7363, -7501)
            addEdge(f7, -7501, -7202)

            f7.nodes[7417] = MapNode(2317, 529, NodeType.ROOM, 7417, HashMap(), f7)
            f7.nodes[7411] = MapNode(2317, 655, NodeType.ROOM, 7411, HashMap(), f7)
            f7.nodes[7413] = MapNode(2317, 655, NodeType.ROOM, 7413, HashMap(), f7)
            f7.nodes[-7203] = MapNode(2317, 963, NodeType.STAIR, -7203, HashMap(), f7)
            addEdge(f7, -7500, 7417)
            addEdge(f7, 7417, 7411)
            addEdge(f7, 7411, 7413)
            addEdge(f7, 7413, -7502)
            addEdge(f7, -7502, -7203)

            f7.nodes[7428] = MapNode(2675, 600, NodeType.ROOM, 7326, HashMap(), f7)
            f7.nodes[7431] = MapNode(2675, 605, NodeType.ROOM, 7328, HashMap(), f7)
            f7.nodes[7432] = MapNode(2675, 637, NodeType.ROOM, 7331, HashMap(), f7)
            f7.nodes[7434] = MapNode(2675, 716, NodeType.ROOM, 7334, HashMap(), f7)
            f7.nodes[7433] = MapNode(2675, 748, NodeType.ROOM, 7336, HashMap(), f7)
            f7.nodes[7436] = MapNode(2675, 794, NodeType.ROOM, 7336, HashMap(), f7)
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

        val s: MutableSet<MapNode> = HashSet()
        for (f in buildings[1]!!.plans.values)
            for (n in f.nodes.values)
                s.add(n)

        val q: Queue<MapNode> = LinkedList()
        q.offer(buildings[1]!!.plans[1]!!.nodes[1419])

        while(q.isNotEmpty()) {
            val cur = q.poll()!!

            if(s.remove(cur))
                for(n in cur.adj.keys)
                    q.offer(n)
        }

        if(s.isNotEmpty()) {
            val ids = s.map { n -> n.id }
            Log.w("MapRepository", "E7 graph is not connected; unreachable nodes:\n$ids")
        }
    }

    fun roomExists(buildingId: Int, roomId: Int): Boolean {
        if(buildings[buildingId] == null)
            throw Exception("Building ID does not exist");
        return buildings[buildingId]!!.plans[getFloor(roomId)] != null &&
                buildings[buildingId]!!.plans[getFloor(roomId)]!!.nodes[roomId] != null;
    }

    fun getNorthHeading(buildingId: Int): Int {
        return buildings[buildingId]!!.northDeg
    }

    // ? Returns marked floor plan for a given building ID and start/end room IDs
    // ? May return a list in case of routes spanning floors
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getMarkedPlan(buildingId: Int, srcId: Int, destId: Int): Map<Int, Bitmap> {

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
        val sNode = buildings[buildingId]!!.plans[getFloor(srcId)]!!.nodes[srcId]!!
        val dNode = buildings[buildingId]!!.plans[getFloor(destId)]!!.nodes[destId]!!

        return drawPath(buildingId, getPath(sNode, dNode))
    }

    // ? Returns marked floor plan for a given building ID, start ID, and PoI type
    // ? May return a list in case of routes spanning floors
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getMarkedPlan(buildingId: Int, srcId: Int, destType: NodeType): Map<Int, Bitmap> {
        // Get node corresponding to src
        val sNode = buildings[buildingId]!!.plans[getFloor(srcId)]!!.nodes[srcId]

        return drawPath(buildingId, getPath(sNode!!, destType))
    }

    // ? Draws a line on floor plan(s) given the path
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun drawPath(buildingId: Int, path: List<MapNode>): Map<Int, Bitmap> {
        // Load and copy list of floor plans
        val copy = buildings[buildingId]!!.plans.entries
            .associate { e ->
                val img = (applicationContext.resources.getDrawable(
                    e.value.plan,
                    null
                ) as BitmapDrawable).bitmap

                e.key to img.copy(img.config ?: Bitmap.Config.ARGB_8888, true)
            }

        if(path.isEmpty())
            return copy

        val paint = Paint()
        paint.strokeWidth = WIDTH_PATH

        // Start location
        var cur = path[0]

        var canvas = Canvas(copy[getFloor(cur.id)]!!)

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
            if(getFloor(cur.id) == getFloor(next.id))
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
                    if(getFloor(cur.id) < getFloor(next.id)) COLOR_STAIRS_UP
                    else COLOR_STAIRS_DOWN
                canvas.drawCircle(
                    cur.x.toFloat(),
                    cur.y.toFloat(),
                    RADIUS_NODE,
                    paint
                )

                // Switch canvas
                canvas = Canvas(copy[getFloor(next.id)]!!)

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

        return copy
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

        Log.i("MapRepository", "Path found: " + ret.reversed().fold("") {
            acc, item -> "${acc}\n${item.id}"
        })

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
            Log.i("MapRepository", "ID ${cur.id} with dist $curDist polled")

            if(isTarget(cur)) {
                Log.i("MapRepository", "Pathing complete")
                return generatePath(cur, parent)
            }

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

        Log.e("MapRepository", "Path requested but not found")
        // ! Error state; should never occur
        return mutableListOf()
    }

    // ? Returns the floor number given a room ID
    private fun getFloor(roomId: Int): Int {
        // ! REPLACE THIS IF NOT ALL BUILDINGS FOLLOW THIS PATTERN
        return abs(roomId) / 1000;
    }
}
