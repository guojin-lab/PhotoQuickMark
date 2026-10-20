package com.lsama.measurephoto.render

import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.exifinterface.media.ExifInterface
import com.lsama.measurephoto.model.*
import com.lsama.measurephoto.model.Point
import kotlin.math.*

object Photos {
    fun info(path: String): Triple<Int,Int,Int> {
        val opts=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeFile(path,opts)
        require(opts.outWidth>0 && opts.outHeight>0) { "图片已损坏或格式不支持" }
        val orientation=ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION,1)
        return if(orientation in 5..8) Triple(opts.outHeight,opts.outWidth,orientation) else Triple(opts.outWidth,opts.outHeight,orientation)
    }
    fun matrix(orientation: Int)=Matrix().apply {
        when(orientation) {
            2->setScale(-1f,1f);3->setRotate(180f);4->{setRotate(180f);postScale(-1f,1f)}
            5->{setRotate(90f);postScale(-1f,1f)};6->setRotate(90f)
            7->{setRotate(-90f);postScale(-1f,1f)};8->setRotate(-90f)
        }
    }
    fun decode(path: String,maxEdge: Int): Bitmap {
        val (w,h,o)=info(path)
        var sample=1;while(max(w,h)/sample>maxEdge*2) sample*=2
        val raw=BitmapFactory.decodeFile(path,BitmapFactory.Options().apply { inSampleSize=sample;inMutable=true;inPreferredConfig=Bitmap.Config.ARGB_8888 }) ?: error("无法解码照片")
        val oriented=Bitmap.createBitmap(raw,0,0,raw.width,raw.height,matrix(o),true)
        if(oriented!==raw) raw.recycle()
        val scale=min(1f,maxEdge.toFloat()/max(oriented.width,oriented.height))
        val resized=if(scale<1) Bitmap.createScaledBitmap(oriented,max(1,(oriented.width*scale).roundToInt()),max(1,(oriented.height*scale).roundToInt()),true) else oriented
        if(resized!==oriented) oriented.recycle()
        if(resized.isMutable) return resized
        return resized.copy(Bitmap.Config.ARGB_8888,true).also { resized.recycle() }
    }
}
object Renderer {
    private const val LABEL_TEXT = 14f
    private const val LABEL_PAD_X = 8f
    private const val LABEL_PAD_Y = 4f
    private fun textPaint(size: Float)=Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color=Color.WHITE;textSize=size;typeface=Typeface.create("sans-serif",Typeface.BOLD)
    }
    /** White reads as the label colour, except on near-white pills where it would disappear. */
    private fun textColor(background: Int): Int {
        val r=(background shr 16) and 0xFF;val g=(background shr 8) and 0xFF;val b=background and 0xFF
        return if(0.299f*r+0.587f*g+0.114f*b>200) 0xFF232725.toInt() else Color.WHITE
    }
    fun label(doc: Document,s: Segment,w: Float,h: Float): LabelGeometry? {
        val text=s.displayDimension ?: return null
        val a=doc.vertex(s.start);val b=doc.vertex(s.end);val unit=min(w,h)/360f
        val p=textPaint(LABEL_TEXT*unit)
        // Capsule fits the bold text plus 8/4 padding; clamped only for very long values.
        val width=min(w,p.measureText(text)+2*LABEL_PAD_X*unit)
        val height=min(h,(p.fontMetrics.descent-p.fontMetrics.ascent)+2*LABEL_PAD_Y*unit)
        return labelGeometry(Point(a.x*w,a.y*h),Point(b.x*w,b.y*h),width,height)
    }
    fun annotations(c: Canvas,doc: Document,w: Float,h: Float) {
        val unit=min(w,h)/360f
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
        val fill=Paint(Paint.ANTI_ALIAS_FLAG).apply {style=Paint.Style.FILL}
        c.save();c.clipRect(0f,0f,w,h)
        doc.segments.forEach { s->
            val a=doc.vertex(s.start);val b=doc.vertex(s.end)
            val ax=a.x*w;val ay=a.y*h;val bx=b.x*w;val by=b.y*h
            val dx=bx-ax;val dy=by-ay;val len=hypot(dx,dy)
            val size=(if(s.pen.arrowSize>0f) s.pen.arrowSize else DEFAULT_ARROW_SIZE)*unit
            p.color=s.pen.color;p.strokeWidth=s.pen.width*unit
            val head=arrowShape(Point(bx,by),dx,dy,size)
            // Trim the stroke by the arrow length so it stops at each base instead of piercing the tip.
            if(head!=null && len>size*2.5f) {
                val ux=dx/len;val uy=dy/len
                c.drawLine(ax+ux*size,ay+uy*size,bx-ux*size,by-uy*size,p)
                fill.color=s.pen.color
                triangle(c,head,fill)
                arrowShape(Point(ax,ay),-dx,-dy,size)?.let {triangle(c,it,fill)}
            } else c.drawLine(ax,ay,bx,by,p)
        }
        // Second pass: opaque capsules sit above every line, including crossing ones.
        val capsule=Paint(Paint.ANTI_ALIAS_FLAG).apply {style=Paint.Style.FILL}
        doc.segments.forEach { s->label(doc,s,w,h)?.let { g->
            val text=s.displayDimension!!;val t=textPaint(LABEL_TEXT*unit)
            val maxText=(g.width-2*LABEL_PAD_X*unit).coerceAtLeast(1f)
            if(t.measureText(text)>maxText) t.textSize*=max(0.1f,maxText/t.measureText(text))
            val color=s.pen.color or 0xFF000000.toInt()
            val cx=g.center.x;val cy=g.center.y
            c.save()
            // Rotate around the midpoint so the pill follows the line; the folded angle keeps text upright.
            c.rotate(g.angle,cx,cy)
            val left=cx-g.width/2;val top=cy-g.height/2
            val r=RectF(left,top,left+g.width,top+g.height)
            capsule.color=color
            val radius=min(g.width,g.height)/2f
            c.drawRoundRect(r,radius,radius,capsule)
            val x=cx-t.measureText(text)/2;val y=cy-(t.fontMetrics.ascent+t.fontMetrics.descent)/2
            t.color=textColor(color);c.drawText(text,x,y,t)
            c.restore()
        } }
        c.restore()
    }
    private fun triangle(c: Canvas,a: ArrowShape,p: Paint) {
        val path=Path().apply {moveTo(a.tip.x,a.tip.y);lineTo(a.left.x,a.left.y);lineTo(a.right.x,a.right.y);close()}
        c.drawPath(path,p)
    }
    fun text(text: String,width: Int,size: Float,color: Int=0xFF232725.toInt()): StaticLayout {
        val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {this.color=color;textSize=size;typeface=Typeface.create("sans-serif",Typeface.NORMAL)}
        return StaticLayout.Builder.obtain(text,0,text.length,paint,max(1,width)).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(size*0.25f,1f).setIncludePad(false).build()
    }
}
