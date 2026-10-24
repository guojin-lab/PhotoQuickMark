package com.guojinlab.photoquickmark.model

/**
 * Editor state that must undo as one unit: normalized vertices only make sense together
 * with the rotation of the photo they were measured on. [rotation] counts clockwise
 * quarter turns, so 0..3 covers 0°/90°/180°/270°.
 */
data class EditState(val document: Document,val rotation: Int=0) {
    val quarterTurns get() = ((rotation%4)+4)%4
}

/**
 * A 90° clockwise turn maps a normalized point (x, y) to (1 - y, x).
 *
 * The same mapping is what the bitmap's own turn performs: a point drawn at (x*w, y*h)
 * lands at (h - y*h, x*w) in the new h×w bitmap. Keeping the maths normalized means the
 * photo resolution and the display scale never enter the conversion.
 * Segment ids, dimensions, units, pens and vertex ids are left untouched.
 */
fun Document.rotateClockwise() = copy(vertices=vertices.map {it.copy(x=1f-it.y,y=it.x)}).changed()

/** Counter-clockwise inverse of [rotateClockwise]: (x, y) -> (y, 1 - x). */
fun Document.rotateCounterClockwise() = copy(vertices=vertices.map {it.copy(x=it.y,y=1f-it.x)}).changed()

fun Document.rotated(clockwise: Boolean) = if(clockwise) rotateClockwise() else rotateCounterClockwise()
