package com.guojinlab.photoquickmark.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class ArrowGeometryTest {
    @Test fun horizontalHeadIsSymmetricAndPointsAlongTheLine() {
        val a=arrowShape(Point(280f,180f),20f,0f,12f)!!
        assertEquals(280f,a.tip.x,0f);assertEquals(180f,a.tip.y,0f)
        assertEquals(268f,a.left.x,.001f);assertEquals(268f,a.right.x,.001f)
        assertEquals(180f+12f*.38f,a.left.y,.001f)
        assertEquals(180f-12f*.38f,a.right.y,.001f)
    }
    @Test fun baseSitsExactlySizeBehindTheTipInAnyDirection() {
        val dirs=listOf(20f to 0f,0f to -20f,-20f to 0f,14.142f to 14.142f)
        for((dx,dy) in dirs) {
            val a=arrowShape(Point(100f,100f),dx,dy,DEFAULT_ARROW_SIZE)!!
            val len=hypot(dx,dy)
            val mid=Point((a.left.x+a.right.x)/2,(a.left.y+a.right.y)/2)
            assertEquals("base distance",DEFAULT_ARROW_SIZE,mid.distance(a.tip),.001f)
            assertEquals("left and right are equidistant from the tip",a.left.distance(a.tip),a.right.distance(a.tip),.001f)
            assertEquals("base midpoint lies behind the tip",-dx/len,(mid.x-a.tip.x)/DEFAULT_ARROW_SIZE,.001f)
            assertEquals("base midpoint lies behind the tip",-dy/len,(mid.y-a.tip.y)/DEFAULT_ARROW_SIZE,.001f)
        }
    }
    @Test fun scalingArrowSizeScalesTheHead() {
        val small=arrowShape(Point(200f,100f),1f,0f,8f)!!
        val large=arrowShape(Point(200f,100f),1f,0f,30f)!!
        assertEquals(8f,200f-small.left.x,.001f)
        assertEquals(30f,200f-large.left.x,.001f)
        assertEquals(30f*.38f,large.left.y-100f,.001f)
        assertEquals(30f/8f,(200f-large.left.x)/(200f-small.left.x),.001f)
    }
    @Test fun degenerateInputsProduceNoArrow() {
        assertNull(arrowShape(Point(1f,1f),0f,0f,12f))
        assertNull(arrowShape(Point(1f,1f),10f,0f,0f))
    }
}
