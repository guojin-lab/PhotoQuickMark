package com.guojinlab.photoquickmark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.guojinlab.photoquickmark.editor.PhotoCanvas
import com.guojinlab.photoquickmark.model.Document
import com.guojinlab.photoquickmark.model.Pen
import com.guojinlab.photoquickmark.model.Point
import com.guojinlab.photoquickmark.render.Renderer
import org.junit.Assert.*
import org.junit.Test

class CanvasInteractionTest {
    private fun line()=Document().add(Point(.2f,.5f),Point(.8f,.5f),null,null,Pen()).let {d->d.update(d.segments.single().id){it.copy(dimension="100")}}

    @Test fun draggingAHandleMovesOnlyThatEndpointAndNeverPans() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context=ApplicationProvider.getApplicationContext<android.content.Context>()
            val pixels=(360*context.resources.displayMetrics.density).toInt()
            val parent=FrameLayout(context);val view=PhotoCanvas(context);parent.addView(view)
            val bitmap=Bitmap.createBitmap(360,360,Bitmap.Config.ARGB_8888)
            try {
                val before=line();val startId=before.segments.single().start;val endId=before.segments.single().end
                view.bitmap=bitmap;view.document=before;view.selected=before.segments.single().id;view.drawMode=false
                var result=before;view.onCommit={result=it;view.document=it}
                parent.layout(0,0,pixels,pixels);view.layout(0,0,pixels,pixels);view.fit()
                val scale=pixels/360f*.94f;val margin=(pixels-360*scale)/2
                val x=margin+.2f*360*scale;val y=margin+.5f*360*scale
                val dx=40*context.resources.displayMetrics.density;val dy=-60*context.resources.displayMetrics.density
                val now=SystemClock.uptimeMillis()
                listOf(MotionEvent.ACTION_DOWN to Point(0f,0f),MotionEvent.ACTION_MOVE to Point(dx,dy),MotionEvent.ACTION_UP to Point(dx,dy)).withIndex().forEach {(i,pair)->
                    val (action,target)=pair
                    val e=MotionEvent.obtain(now,now+i*100L,action,x+target.x,y+target.y,0);view.dispatchTouchEvent(e);e.recycle()
                }
                assertEquals("The far endpoint must not move",before.vertex(endId),result.vertex(endId))
                assertEquals(dx/(360*scale),result.vertex(startId).x-before.vertex(startId).x,.02f)
                assertEquals(dy/(360*scale),result.vertex(startId).y-before.vertex(startId).y,.02f)
                val beforeLabel=Renderer.label(before,before.segments.single(),360f,360f)!!
                val afterLabel=Renderer.label(result,result.segments.single(),360f,360f)!!
                assertEquals(beforeLabel.center.x+dx/(2*scale),afterLabel.center.x,.02f)
            } finally {bitmap.recycle()}
        }
    }

    @Test fun solidArrowheadsCapBothEndsAndTheStrokeStopsAtTheirBase() {
        val d=Document().add(Point(80f/360f,180f/360f),Point(280f/360f,180f/360f),null,null,Pen(width=20f,arrowSize=20f))
        val s=d.segments.single()
        val b=Bitmap.createBitmap(360,360,Bitmap.Config.ARGB_8888)
        try {
            val c=Canvas(b);c.drawColor(Color.WHITE);Renderer.annotations(c,d,360f,360f)
            assertEquals("head is solid",s.pen.color,b.getPixel(270,180))
            for(x in 282..290) assertEquals("nothing past the tip",Color.WHITE,b.getPixel(x,180))
            for(x in 70..78) assertEquals("nothing before the start tip",Color.WHITE,b.getPixel(x,180))
            assertEquals("head is triangular, not a wide stroke",Color.WHITE,b.getPixel(280,190))
            assertEquals("trimmed stroke stays behind the head",Color.WHITE,b.getPixel(270,190))
            assertEquals("start head is filled",s.pen.color,b.getPixel(90,180))
            assertEquals("line spans between the heads",s.pen.color,b.getPixel(180,180))
        } finally {b.recycle()}
    }
    @Test fun capsuleIsCentredOnTheLineAndOnlyHidesItsOwnSpan() {
        val d=line();val s=d.segments.single();val g=Renderer.label(d,s,360f,360f)!!
        assertEquals(180f,g.center.x,.001f);assertEquals(180f,g.center.y,.001f);assertEquals(0f,g.angle,0f)
        val b=Bitmap.createBitmap(360,360,Bitmap.Config.ARGB_8888)
        try {
            val c=Canvas(b);c.drawColor(Color.WHITE);Renderer.annotations(c,d,360f,360f)
            assertEquals("Midpoint is covered by the opaque pill",s.pen.color,b.getPixel(180,180))
            assertEquals("Line outside the pill stays visible",s.pen.color,b.getPixel(80,180))
        } finally {b.recycle()}
    }
}
