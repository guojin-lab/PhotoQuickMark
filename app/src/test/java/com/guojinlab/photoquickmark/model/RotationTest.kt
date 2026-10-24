package com.guojinlab.photoquickmark.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RotationTest {
    private fun edge()=Document().add(Point(.1f,.2f),Point(.6f,.9f),null,null,Pen(width=5f)).let {d->
        d.update(d.segments.single().id){it.copy(dimension="2200",unit=DimensionUnit.CM,textSize=28f)}
    }
    private fun assertClose(expected: Point,actual: Point,why: String) {
        assertEquals(why,expected.x,actual.x,1e-6f);assertEquals(why,expected.y,actual.y,1e-6f)
    }

    @Test fun clockwiseMapsCornersLikeTheBitmapTurn() {
        val d=Document(vertices=listOf(Vertex(id="tl",x=0f,y=0f),Vertex(id="tr",x=1f,y=0f),Vertex(id="bl",x=0f,y=1f),Vertex(id="br",x=1f,y=1f)))
        val r=d.rotateClockwise().vertices.associateBy {it.id}
        assertClose(Point(1f,0f),r.getValue("tl").point,"top-left moves to top-right")
        assertClose(Point(1f,1f),r.getValue("tr").point,"top-right moves to bottom-right")
        assertClose(Point(0f,0f),r.getValue("bl").point,"bottom-left moves to top-left")
        assertClose(Point(0f,1f),r.getValue("br").point,"bottom-right stays bottom-left")
    }

    @Test fun counterClockwiseIsTheExactInverse() {
        val d=edge();val roundTrip=d.rotateClockwise().rotateCounterClockwise()
        d.vertices.forEach {v->assertClose(v.point,roundTrip.vertex(v.id).point,"vertex ${v.id}")}
        assertEquals(d.segments.map {it.id},roundTrip.segments.map {it.id})
        assertEquals(d.segments.single().dimension,roundTrip.segments.single().dimension)
        assertEquals(d.segments.single().unit,roundTrip.segments.single().unit)
        assertEquals(d.segments.single().pen,d.segments.single().pen)
    }

    @Test fun fourTurnsReturnToTheOriginalPosition() {
        val d=edge();var r=d;repeat(4){r=r.rotateClockwise()}
        d.vertices.forEach {v->assertClose(v.point,r.vertex(v.id).point,"vertex ${v.id} after 4 turns")}
    }

    @Test fun turningKeepsIdsDimensionsUnitsAndPen() {
        val d=edge();val s=d.segments.single();val r=d.rotateClockwise()
        assertEquals(d.vertices.map {it.id},r.vertices.map {it.id})
        val t=r.segments.single()
        assertEquals(s.id,t.id);assertEquals(s.start,t.start);assertEquals(s.end,t.end)
        assertEquals("2200",t.dimension);assertEquals(DimensionUnit.CM,t.unit);assertEquals(28f,t.textSize);assertEquals(s.pen,t.pen)
    }

    @Test fun labelsStayUprightAndTurnWithTheLine() {
        val a=Point(.1f,.2f);val b=Point(.7f,.75f)
        val before=labelAngle(a,b)
        val d=Document().add(a,b,null,null,Pen());val turned=d.rotateClockwise()
        val s=turned.segments.single()
        val after=labelAngle(turned.vertex(s.start).point,turned.vertex(s.end).point)
        assertTrue("rotated label angle $after° must stay readable",after>-90f && after<=90f)
        // A quarter turn rotates the line by 90°; modulo the 180° flip the difference is ±90°.
        var delta=(after-before)%180f
        if(delta>90f) delta-=180f
        if(delta<=-90f) delta+=180f
        assertEquals("label direction follows the turned line",90f,abs(delta),0.02f)
    }

    @Test fun rotationAndDocumentUndoAsOneEntry() {
        val d=edge();val h=History(EditState(d,0))
        h.commit(EditState(d.rotateClockwise(),1))
        assertEquals(1,h.current.rotation)
        val original=d.vertices[0].point;val moved=h.current.document.vertex(d.vertices[0].id).point
        assertClose(Point(1f-original.y,original.x),moved,"committed vertex uses the clockwise mapping")
        h.undo()
        assertEquals(0,h.current.rotation)
        d.vertices.forEach {v->assertClose(v.point,h.current.document.vertex(v.id).point,"undo ${v.id}")}
        h.redo()
        assertEquals(1,h.current.rotation)
        val again=d.rotateClockwise()
        again.vertices.forEach {v->assertClose(v.point,h.current.document.vertex(v.id).point,"redo ${v.id}")}
    }

    @Test fun turningFourTimesIsACompleteUndoEntrySequence() {
        val d=edge();val h=History(EditState(d,0))
        repeat(4){h.commit(EditState(h.current.document.rotateClockwise(),(h.current.rotation+1)%4))}
        assertEquals(0,h.current.rotation)
        repeat(4){h.undo()}
        assertEquals(0,h.current.rotation)
        d.vertices.forEach {v->assertClose(v.point,h.current.document.vertex(v.id).point,"restored ${v.id}")}
    }
}
