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
}