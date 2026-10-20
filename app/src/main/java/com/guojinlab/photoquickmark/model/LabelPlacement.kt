package com.guojinlab.photoquickmark.model

import kotlin.math.*

const val DEFAULT_LABEL_TEXT = 14f
const val MIN_LABEL_TEXT = 10f
const val MAX_LABEL_TEXT = 40f

/** Rotated capsule in image space, centred on the segment midpoint and aligned with the line. */
data class LabelGeometry(val center: Point,val width: Float,val height: Float,val angle: Float)

/**
 * Line direction folded into (-90°, 90°]. A 180° flip keeps a capsule looking the same
 * but turns the glyphs upside down, so the folded angle is what actually gets drawn.
 */
fun labelAngle(a: Point,b: Point): Float {
    val deg=Math.toDegrees(atan2((b.y-a.y).toDouble(),(b.x-a.x).toDouble())).toFloat()
    return when {deg>90f->deg-180f;deg<=-90f->deg+180f;else->deg}
}

fun labelGeometry(a: Point,b: Point,width: Float,height: Float)=
    LabelGeometry(Point((a.x+b.x)/2,(a.y+b.y)/2),width,height,labelAngle(a,b))
