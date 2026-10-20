package com.guojinlab.photoquickmark.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class LabelPlacementTest {
    @Test fun capsuleIsCentredOnTheSegmentMidpoint() {
        val g=labelGeometry(Point(20f,60f),Point(100f,220f),80f,24f)
        assertEquals(Point(60f,140f),g.center)
    }
    @Test fun strokeDirectionDoesNotFlipTheLabel() {
        val a=Point(20f,60f);val b=Point(100f,220f)
        assertEquals(labelAngle(a,b),labelAngle(b,a),0.001f)
    }
    @Test fun textIsNeverUpsideDown() {
        val a=Point(180f,180f)
        for(deg in 0 until 360 step 15) {
            val rad=Math.toRadians(deg.toDouble())
            val b=Point((180+120*cos(rad)).toFloat(),(180+120*sin(rad)).toFloat())
            val angle=labelAngle(a,b)
            assertTrue("$deg° folds to $angle°",angle>-90.001f && angle<=90.001f)
        }
    }
    @Test fun horizontalLinesStayHorizontalBothWays() {
        assertEquals(0f,labelAngle(Point(10f,50f),Point(200f,50f)),0f)
        assertEquals(0f,labelAngle(Point(200f,50f),Point(10f,50f)),0f)
    }
    @Test fun verticalLinesReadBottomToTop() {
        assertEquals(90f,labelAngle(Point(100f,100f),Point(100f,200f)),0f)
        assertEquals(90f,labelAngle(Point(100f,200f),Point(100f,100f)),0f)
    }
    @Test fun photoResolutionDoesNotChangeNormalizedGeometry() {
        val small=labelGeometry(Point(20f,60f),Point(30f,70f),80f,30f)
        val big=labelGeometry(Point(200f,600f),Point(300f,700f),800f,300f)
        assertEquals(small.angle,big.angle,0f)
        assertEquals(small.center.x*10,big.center.x,.0001f)
        assertEquals(small.center.y*10,big.center.y,.0001f)
    }
}
