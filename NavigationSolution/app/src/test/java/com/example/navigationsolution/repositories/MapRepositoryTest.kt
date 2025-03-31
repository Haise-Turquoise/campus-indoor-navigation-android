package com.example.navigationsolution.repositories

import MapRepository
import android.util.Log
import org.junit.Test
import org.junit.Assert.*
import java.util.LinkedList
import java.util.Queue

class MapRepositoryTest {
    @Test
    fun testConnectivity() {
        for(building in MapRepository.buildings.values) {
            // Add all nodes to set
            val s: MutableSet<MapRepository.MapNode> = HashSet()
            for (f in building.plans.values)
                for (n in f.nodes.values)
                    s.add(n)

            // Initialize queue with an arbitrary room node
            val q: Queue<MapRepository.MapNode> = LinkedList()
            q.offer(building.plans[building.plans.keys.firstOrNull()]!!.
                nodes[building.plans[1]!!.nodes.keys.firstOrNull()]
            )

            // BFS
            while(q.isNotEmpty()) {
                val cur = q.poll()!!

                if(s.remove(cur))
                    for(n in cur.adj.keys)
                        q.offer(n)
            }

            assertTrue("Graph of building with ID ${building.id} is disconnected", s.isEmpty())
        }
    }

    @Test
    fun testCheckExistenceE7() {
        assertNotNull("E7 does not exist in data or not properly initalized", MapRepository.buildingNameToId["E7"])
        try{
            assertTrue(MapRepository.roomExists(MapRepository.buildingNameToId["E7"]!!, 1331))
            assertTrue(MapRepository.roomExists(MapRepository.buildingNameToId["E7"]!!, 2322))
            assertTrue(MapRepository.roomExists(MapRepository.buildingNameToId["E7"]!!, 7326))
            assertTrue(MapRepository.roomExists(MapRepository.buildingNameToId["E7"]!!, 6454))
        } catch (_: AssertionError) {
            assertTrue("Room should exist, but found otherwise", false)
        }
    }

    @Test
    fun testCheckNonExistenceE7() {
        assertNotNull("E7 does not exist in data or not properly initialized", MapRepository.buildingNameToId["E7"])
        try {
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E7"]!!, 6999))
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E7"]!!, 9000))
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E7"]!!, 7999))
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E7"]!!, 1999))
        } catch (_: AssertionError) {
            assertTrue("Room should not exist, but found otherwise", false)
        }
    }

    @Test
    fun testCheckExistenceE6() {
        assertNotNull("E6 does not exist in data or not properly initialized", MapRepository.buildingNameToId["E6"])
        try {
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E6"]!!, 1104))
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E6"]!!, 1909))
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E6"]!!, 3109))
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E6"]!!, 4003))
        } catch (_: AssertionError) {
            assertTrue("Room should exist, but found otherwise", false)
        }
    }

    @Test
    fun testCheckNonExistenceE6() {
        assertNotNull("E6 does not exist in data or not properly initialized", MapRepository.buildingNameToId["E6"])
        try {
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E6"]!!, 6999))
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E6"]!!, 9000))
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E6"]!!, 7999))
            assertFalse(MapRepository.roomExists(MapRepository.buildingNameToId["E6"]!!, 1999))
        } catch (_: AssertionError) {
            assertTrue("Room should not exist, but found otherwise", false)
        }
    }
}