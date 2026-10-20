package com.lsama.measurephoto.editor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.lsama.measurephoto.model.*
import com.lsama.measurephoto.model.Point
import com.lsama.measurephoto.render.Renderer
import kotlin.math.*

class PhotoCanvas(context: Context): View(context) {
    var bitmap: Bitmap?=null
    var document=Document()
    var selected: String?=null
    var drawMode=true
    var pen=Pen()
    var onCommit: (Document)->Unit={}
    var onSelect: (String?)->Unit={}
    var onDimension: (String)->Unit={}
    var onLineCreated: (String)->Unit={}
    var fitToken=0
    private var lastFit=-1
    private val dp=resources.displayMetrics.density
    private var scale=1f;private var tx=0f;private var ty=0f;private var base=1f
    private var fittedWidth=0;private var fittedHeight=0
    private var down=Point(0f,0f);private var previous=down
    private var start: Point?=null;private var end: Point?=null
    private var startId: String?=null;private var snapId: String?=null
    private var hitVertex: String?=null;private var hitLine: String?=null
    private var original=Document();private var draft: Document?=null
    private var moved=false
    private var magAnchor: Point?=null;private var magFocus: Point?=null;private var magSnap=false
    private val gate=GestureGate();private var span=0f;private var center=Point(0f,0f)
    private val handleHit=26f*dp;private val handleRadius=7f*dp
    private fun transform()=Transform(bitmap?.width?.toFloat()?:1f,bitmap?.height?.toFloat()?:1f,scale,tx,ty)
    /** Labels are drawn in bitmap space, so they shrink as the user pinches in and keep a constant size on screen. */
    private fun zoom()=scale/max(base,1e-4f)
    fun fit() { val b=bitmap?:return; if(width==0||height==0) return; base=min(width.toFloat()/b.width,height.toFloat()/b.height)*0.94f;scale=base;tx=(width-b.width*scale)/2;ty=(height-b.height*scale)/2;lastFit=fitToken;fittedWidth=width;fittedHeight=height;invalidate() }
    override fun onSizeChanged(w: Int,h: Int,ow: Int,oh: Int) {super.onSizeChanged(w,h,ow,oh);fit()}
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas);val b=bitmap?:return
        if(lastFit!=fitToken || fittedWidth!=width || fittedHeight!=height) fit()
        val doc=draft?:document
        canvas.save();canvas.translate(tx,ty);canvas.scale(scale,scale);canvas.drawBitmap(b,0f,0f,Paint(Paint.FILTER_BITMAP_FLAG));Renderer.annotations(canvas,doc,b.width.toFloat(),b.height.toFloat(),zoom());canvas.restore()
        val t=transform();val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {strokeWidth=2*dp;color=0xFF2F655C.toInt();style=Paint.Style.STROKE}
        doc.segments.find {it.id==selected}?.let {s->listOf(s.start,s.end).forEach {id-> val q=t.screen(doc.vertex(id).point);p.style=Paint.Style.FILL;p.color=if(id==hitVertex) 0xFF2F655C.toInt() else Color.WHITE;canvas.drawCircle(q.x,q.y,handleRadius,p);p.style=Paint.Style.STROKE;p.color=0xFF2F655C.toInt();canvas.drawCircle(q.x,q.y,handleRadius,p) } }
        start?.let {a->end?.let {b2->val q=t.screen(a);val r=t.screen(b2);p.color=pen.color;p.strokeWidth=pen.width*min(b.width,b.height)/360*scale;p.strokeCap=Paint.Cap.ROUND;canvas.drawLine(q.x,q.y,r.x,r.y,p)}}
        snapId?.let {id->doc.vertices.find {it.id==id}?.let {val q=t.screen(it.point);p.color=0xFF2F655C.toInt();p.strokeWidth=2*dp;canvas.drawCircle(q.x,q.y,12*dp,p)}}
        drawMagnifier(canvas,doc)
    }
    /** Loupe over the finger while drawing a line or moving a handle; it magnifies the same bitmap, annotations and in-progress line 2x. */
    private fun drawMagnifier(c: Canvas,doc: Document) {
        val b=bitmap?:return;val anchor=magAnchor?:return;val focus=magFocus?:return
        val radius=64f*dp
        if(width<2*radius+12*dp || height<2*radius+12*dp) return
        val margin=radius+6*dp
        val cx=anchor.x.coerceIn(margin,width-margin)
        val above=anchor.y-radius-56f*dp
        val cy=if(above>=margin) above else (anchor.y+radius+56f*dp).coerceIn(margin,height-margin)
        val clip=Path().apply {addCircle(cx,cy,radius,Path.Direction.CW)}
        c.save();c.clipPath(clip)
        c.drawRect(cx-radius,cy-radius,cx+radius,cy+radius,Paint().apply {color=0xFF111614.toInt()})
        c.translate(cx,cy);c.scale(2f,2f);c.translate(-focus.x,-focus.y)
        c.translate(tx,ty);c.scale(scale,scale)
        c.drawBitmap(b,0f,0f,Paint(Paint.FILTER_BITMAP_FLAG))
        Renderer.annotations(c,doc,b.width.toFloat(),b.height.toFloat(),zoom())
        if(drawMode) start?.let {a->end?.let {b2->c.drawLine(a.x*b.width,a.y*b.height,b2.x*b.width,b2.y*b.height,Paint(Paint.ANTI_ALIAS_FLAG).apply {color=pen.color;strokeWidth=pen.width*min(b.width,b.height)/360*scale;strokeCap=Paint.Cap.ROUND})}}
        c.restore()
        c.drawCircle(cx,cy,radius+1.5f*dp,Paint(Paint.ANTI_ALIAS_FLAG).apply {style=Paint.Style.STROKE;strokeWidth=5f*dp;color=0x55000000})
        c.drawCircle(cx,cy,radius,Paint(Paint.ANTI_ALIAS_FLAG).apply {style=Paint.Style.STROKE;strokeWidth=2f*dp;color=Color.WHITE})
        val arm=10f*dp;val guide=Paint(Paint.ANTI_ALIAS_FLAG).apply {strokeWidth=3f*dp;color=0x66000000}
        c.drawLine(cx-arm,cy,cx+arm,cy,guide);c.drawLine(cx,cy-arm,cx,cy+arm,guide)
        val marker=Paint(Paint.ANTI_ALIAS_FLAG).apply {strokeWidth=1.5f*dp;color=if(magSnap) 0xFF4CAF50.toInt() else Color.WHITE}
        c.drawLine(cx-arm,cy,cx+arm,cy,marker);c.drawLine(cx,cy-arm,cx,cy+arm,marker)
        c.drawCircle(cx,cy,5f*dp,Paint(marker).apply {style=Paint.Style.STROKE})
    }
    private fun inside(p: Point)=p.x in 0f..1f && p.y in 0f..1f
    private fun snap(p: Point,exclude: String?=null): Vertex? {
        val t=transform();val held=document.vertices.find {it.id==snapId && it.id!=exclude}
        return if(held!=null && t.screen(held.point).distance(p)<24*dp) held else t.snap(document,p,16*dp,exclude)
    }
    /** Handles of the selected line win; pressing any other endpoint selects and grabs it in one gesture. */
    private fun handleAt(p: Point,t: Transform): Pair<String,String>? {
        fun pick(s: Segment): Pair<String,String>? {
            val v=listOf(document.vertex(s.start),document.vertex(s.end)).minByOrNull {t.screen(it.point).distance(p)} ?: return null
            return if(t.screen(v.point).distance(p)<=handleHit) s.id to v.id else null
        }
        document.segments.find {it.id==selected}?.let {pick(it)}?.let {return it}
        return document.segments.asReversed().firstNotNullOfOrNull {pick(it)}
    }
    override fun performClick(): Boolean {super.performClick();return true}
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(bitmap==null) return true
        parent.requestDisallowInterceptTouchEvent(true)
        val p=Point(e.x,e.y);val t=transform()
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN->{
                gate.allUp();original=document;draft=null;down=p;previous=p;moved=false
                hitVertex=null;hitLine=null;start=null;end=null;startId=null;snapId=null;magAnchor=null;magFocus=null;magSnap=false
                if(!drawMode) handleAt(p,t)?.let {(seg,vertex)->if(seg!=selected) {selected=seg;onSelect(seg)};hitVertex=vertex}
                hitLine=document.segments.minByOrNull {s->lineDistance(p,t.screen(document.vertex(s.start).point),t.screen(document.vertex(s.end).point))}?.takeIf {s->lineDistance(p,t.screen(document.vertex(s.start).point),t.screen(document.vertex(s.end).point))<12*dp}?.id
                if(drawMode && inside(t.inverse(p))) {val v=snap(p);val a=v?.point?:t.inverse(p);start=a;startId=v?.id;magAnchor=p;magFocus=t.screen(a);magSnap=v!=null}
            }
            MotionEvent.ACTION_POINTER_DOWN->{
                gate.secondPointer();start=null;end=null;draft=null;snapId=null;magAnchor=null;magFocus=null
                if(e.pointerCount>=2) {span=hypot(e.getX(1)-e.getX(0),e.getY(1)-e.getY(0));center=Point((e.getX(0)+e.getX(1))/2,(e.getY(0)+e.getY(1))/2)}
            }
            MotionEvent.ACTION_MOVE->{
                if(gate.blocked) {
                    if(e.pointerCount>=2) {val nextSpan=hypot(e.getX(1)-e.getX(0),e.getY(1)-e.getY(0));val nextCenter=Point((e.getX(0)+e.getX(1))/2,(e.getY(0)+e.getY(1))/2);val newScale=(scale*nextSpan/max(1f,span)).coerceIn(base,base*8);val ratio=newScale/scale;tx=nextCenter.x-(center.x-tx)*ratio;ty=nextCenter.y-(center.y-ty)*ratio;scale=newScale;span=nextSpan;center=nextCenter}
                } else {
                    if(p.distance(down)>ViewConfiguration.get(context).scaledTouchSlop) moved=true
                    if(drawMode && start!=null) {
                        val v=snap(p);val at=v?.point?:t.inverse(p).bounded()
                        magAnchor=p;magFocus=t.screen(at);magSnap=v!=null
                        if(moved) {snapId=v?.id;end=at}
                    }
                    else if(moved) {
                        if(!drawMode && hitVertex!=null) {
                            val v=snap(p,hitVertex);snapId=v?.id
                            val next=original.move(hitVertex!!,v?.point?:t.inverse(p).bounded())
                            draft=next
                            magAnchor=p;magFocus=t.screen(next.vertex(hitVertex!!).point);magSnap=v!=null
                        }
                        else if(!drawMode) {tx+=p.x-previous.x;ty+=p.y-previous.y}
                    }
                }
                previous=p
            }
            MotionEvent.ACTION_UP->{
                if(!gate.blocked) {
                    if(moved && drawMode && start!=null) {val v=snap(p);snapId=v?.id;end=v?.point?:t.inverse(p).bounded()}
                    if(moved && !drawMode && hitVertex!=null) {val v=snap(p,hitVertex);snapId=v?.id;draft=original.move(hitVertex!!,v?.point?:t.inverse(p).bounded())}
                    if(!moved) {performClick();onSelect(hitLine);if(hitLine!=null && hitVertex==null) onDimension(hitLine!!)}
                    else if(drawMode && start!=null && end!=null && t.screen(start!!).distance(t.screen(end!!))>=8*dp) {val next=document.add(start!!,end!!,startId,snapId,pen);if(next!=document) {onCommit(next);onLineCreated(next.segments.last().id)}}
                    else if(!drawMode && draft!=null) {var next=draft!!;if(hitVertex!=null && snapId!=null) {val merged=next.merge(hitVertex!!,snapId!!);next=if(merged==next) original else merged};if(next!=original) onCommit(next)}
                }
                start=null;end=null;draft=null;snapId=null;hitVertex=null;magAnchor=null;magFocus=null;magSnap=false;gate.allUp()
            }
            MotionEvent.ACTION_CANCEL->{start=null;end=null;draft=null;snapId=null;hitVertex=null;magAnchor=null;magFocus=null;magSnap=false;gate.allUp()}
        }
        invalidate();return true
    }
}
