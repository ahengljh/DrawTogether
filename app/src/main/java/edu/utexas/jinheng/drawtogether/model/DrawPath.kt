package edu.utexas.jinheng.drawtogether.model

data class DrawPath(
    var color: String = "",
    var points: ArrayList<DrawPoint> = ArrayList()
) {
}