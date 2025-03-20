package com.example.navigationsolution.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.navigationsolution.NO_PATH
import kotlin.math.max
import kotlin.math.min

class IndoorViewModel: ViewModel() {
    var from: Int = NO_PATH
    var to: Int = NO_PATH
    var buildingFrom: Int = 1
    var buildingTo: Int = 1
    var floor = MutableLiveData(0)
    var maxFloor: Int = 0
    var compassEnabled = MutableLiveData(true)

    var liveFloor: LiveData<Int> = floor
    var liveCompassEnabled: LiveData<Boolean> = compassEnabled


    fun updatePath(newFrom: Int = from,
                   newTo: Int = to,
                   newBuildingFrom: Int = buildingFrom,
                   newBuildingTo: Int = buildingTo,
                   newMaxFloor: Int = maxFloor) {
        from = newFrom
        to = newTo
        buildingFrom = newBuildingFrom
        buildingTo = newBuildingTo
        maxFloor = newMaxFloor
    }

    fun incrFloor() {
        floor.value = min(maxFloor, floor.value?.plus(1) as Int)
    }

    fun decrFloor() {
        floor.value = max(0, floor.value?.minus(1) as Int)
    }

    fun setFloor(newFloor: Int) {
        floor.value = newFloor
    }

    fun toggleCompass() {
        compassEnabled.value = !compassEnabled.value!!
    }

    /*fun updateBuilding(newBuildingCode: String) {
        from = NO_PATH
        to = NO_PATH
        buildingId = getBuildingResourceId(newBuildingCode)
        setFloor(0)
    }

    private fun getBuildingResourceId(buildingCode: String): Int {
        return when (buildingCode) {
            "MC" -> R.drawable.mc_map // we can add actual buildings when we get
            "DC" -> R.drawable.dc_map
            "SLC" -> R.drawable.slc_map
            "E5" -> R.drawable.e5_map
            "E7" -> R.drawable.e7f1.png
            "QNC" -> R.drawable.qnc_map
            else -> R.drawable.default_map
        }
    }*/
}