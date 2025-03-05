package com.example.navigationsolution.viewmodels

import androidx.lifecycle.ViewModel
import com.example.navigationsolution.NO_PATH
import kotlin.math.max
import kotlin.math.min

class IndoorViewModel: ViewModel() {
    var from: Int = NO_PATH
    var to: Int = NO_PATH
    var buildingId: Int = -1
    var floor: Int = 0
    var maxFloor: Int = 0

    fun updatePath(newFrom: Int = from,
                   newTo: Int = to,
                   newBuildingId: Int = buildingId,
                   newFloor: Int = floor,
                   newMaxFloor: Int = maxFloor) {
        from = newFrom
        to = newTo
        buildingId = newBuildingId
        maxFloor = newMaxFloor

        floor = min(0, max(newFloor, newMaxFloor))
    }
}