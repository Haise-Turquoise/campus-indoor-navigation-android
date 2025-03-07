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
import java.util.Objects
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

        n1.adj[n2] = .0;
        n2.adj[n1] = .0;
    }

    // TODO: Delete this function, it's used only for verification of graph models
    private fun drawGraph() {
        // Load floor plan copy
        val img = (applicationContext.resources.getDrawable(R.drawable.e7f2, null) as BitmapDrawable).bitmap
        val copy = img.copy(img.config ?: Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(copy)
        val paint = Paint()

        paint.strokeWidth = 2f
        paint.color = LINE_COLOR

        for(n in buildings[-1]!!.plans[2]!!.nodes.values)
            for(nn in n.adj.keys)
                canvas.drawLine(
                    n.x.toFloat(),
                    n.y.toFloat(),
                    nn.x.toFloat(),
                    nn.y.toFloat(),
                    paint
                )

        paint.color = NODE_COLOR

        for(n in buildings[-1]!!.plans[2]!!.nodes.values) {
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

        buildings[-1]!!.plans[2] = FloorMap(
            2,
            HashMap(),
            R.drawable.e7f2
        )

        val f2 = buildings[-1]!!.plans[2]!!

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
        // WC HAVE NEGATIVE 11XX
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
            addEdge(-1, -1200, -2200)
            addEdge(-1, -1201, -2201)
            addEdge(-1, -1202, -2202)
            addEdge(-1, -1203, -2204)
        }

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
        paint.color = LINE_COLOR
        paint.strokeWidth = 2f

        // Start location
        var cur = path[0]

        var canvas = Canvas(copy[getFloor(cur.id)]!!)

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
            else
                canvas = Canvas(copy[getFloor(next.id)]!!)

            cur = next
        }

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
        return abs(roomId) / 1000;
    }
}
