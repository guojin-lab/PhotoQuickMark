package com.guojinlab.photoquickmark.model

import kotlin.math.*

const val DEFAULT_LABEL_TEXT = 14f
const val MIN_LABEL_TEXT = 10f
const val MAX_LABEL_TEXT = 40f

/** Rotated capsule in image space, centred on the segment midpoint and aligned with the line. */
data class LabelGeometry(val center: Point,val width: Float,val height: Float,val angle: Float)

/**
 * Line direction folded into (-90°, 90°], the only range in which rotated glyphs read upright.
 *
 * atan2 yields (-180°, 180°] in screen space (y grows downwards), so a direction inside
 * (90°, 270°) — i.e. (90°, 180°] plus the mirrored (-180°, -90°] band — would print the
 * text upside down. Removing that half turn fixes it: the capsule is centred on the segment
 * midpoint and a 180° flip leaves a capsule visually unchanged, so only the glyphs turn
 * around while the label stays exactly on the line. The modulo first strips whole half
 * turns, which makes the same rule valid for angles outside atan2's range.
 */
fun labelAngle(a: Point,b: Point): Float {
    val raw=Math.toDegrees(atan2((b.y-a.y).toDouble(),(b.x-a.x).toDouble())).toFloat()
    val folded=raw%180f
    return when {
        folded>90f->folded-180f   // (90°, 180°): flip so the text reads up-right
        folded<=-90f->folded+180f // (-180°, -90°]: mirror band, flip so it reads down-right
        else->folded              // (-90°, 90°]: already readable, +90° is read bottom-to-top
    }
}

fun labelGeometry(a: Point,b: Point,width: Float,height: Float)=
    LabelGeometry(Point((a.x+b.x)/2,(a.y+b.y)/2),width,height,labelAngle(a,b))
