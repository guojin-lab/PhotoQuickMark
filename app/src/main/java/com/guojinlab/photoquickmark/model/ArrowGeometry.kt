package com.guojinlab.photoquickmark.model

import kotlin.math.hypot

const val DEFAULT_ARROW_SIZE = 12f
const val MIN_ARROW_SIZE = 8f
const val MAX_ARROW_SIZE = 30f
private const val SPREAD = 0.38f

data class ArrowShape(val tip: Point,val left: Point,val right: Point)

/** Solid arrowhead whose tip sits on [tip] and whose base is [size] back along (dx, dy); dx/dy need not be unit length. */
fun arrowShape(tip: Point,dx: Float,dy: Float,size: Float): ArrowShape? {
    val len=hypot(dx,dy)
    if(len<1e-5f || size<=0f) return null
    val ux=dx/len;val uy=dy/len
    val bx=tip.x-ux*size;val by=tip.y-uy*size
    val px=-uy*size*SPREAD;val py=ux*size*SPREAD
    return ArrowShape(tip,Point(bx+px,by+py),Point(bx-px,by-py))
}
