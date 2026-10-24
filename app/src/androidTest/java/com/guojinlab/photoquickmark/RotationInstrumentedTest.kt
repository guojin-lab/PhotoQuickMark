package com.guojinlab.photoquickmark

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.guojinlab.photoquickmark.data.Database as AppDatabase
import com.guojinlab.photoquickmark.data.MIGRATION_1_2
import com.guojinlab.photoquickmark.model.Document
import com.guojinlab.photoquickmark.model.Pen
import com.guojinlab.photoquickmark.model.Point
import com.guojinlab.photoquickmark.model.labelAngle
import com.guojinlab.photoquickmark.model.rotateClockwise
import com.guojinlab.photoquickmark.render.Photos
import com.guojinlab.photoquickmark.render.Renderer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.*

class RotationInstrumentedTest {
    private fun load(path: String,rotation: Int,max: Int=1000)=Photos.decode(path,max,rotation)
    private fun redBlockFile(dir: File): File {
        val source=Bitmap.createBitmap(400,200,Bitmap.Config.ARGB_8888)
        val c=Canvas(source);c.drawColor(Color.WHITE)
        c.drawRect(0f,0f,8f,8f,Paint().apply {color=Color.RED})
        val file=File(dir,"quarter-turn.png")
        file.outputStream().use {source.compress(Bitmap.CompressFormat.PNG,100,it)}
        source.recycle()
        return file
    }
    private fun assertRed(bitmap: Bitmap,x: Int,y: Int,why: String) {
        val pixel=bitmap.getPixel(x,y)
        assertTrue("$why: expected red at ($x,$y) but was #${Integer.toHexString(pixel)}",Color.red(pixel)>200 && Color.green(pixel)<80 && Color.blue(pixel)<80)
    }

    @Test fun turningABitmapSwapsSidesAndMovesTheMarkerCorner() {
        val source=Bitmap.createBitmap(40,20,Bitmap.Config.ARGB_8888)
        val c=Canvas(source);c.drawColor(Color.WHITE)
        c.drawRect(0f,0f,8f,8f,Paint().apply {color=Color.RED})
        try {
            val clockwise=Photos.turn(source,1)
            assertEquals(20,clockwise.width);assertEquals(40,clockwise.height)
            assertRed(clockwise,16,4,"top-left moves to top-right after a clockwise turn")
            assertEquals(Color.WHITE,clockwise.getPixel(4,4))
            clockwise.recycle()

            val counter=Photos.turn(source,3)
            assertEquals(20,counter.width);assertEquals(40,counter.height)
            assertRed(counter,3,36,"top-left moves to bottom-left after a counter-clockwise turn")
            counter.recycle()

            assertSame("A zero turn must not copy the bitmap",source,Photos.turn(source,0))
            assertSame("A full turn must not copy the bitmap",source,Photos.turn(source,4))
        } finally {source.recycle()}
    }

    @Test fun decodeAppliesTheSavedQuarterTurnBeforeScaling() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val dir=context.cacheDir
        val file=redBlockFile(dir)
        try {
            val upright=load(file.path,0)
            assertEquals(400,upright.width);assertEquals(200,upright.height)
            assertRed(upright,4,4,"source keeps its orientation")
            upright.recycle()

            val clockwise=load(file.path,1)
            assertEquals(200,clockwise.width);assertEquals(400,clockwise.height)
            assertRed(clockwise,196,4,"clockwise decode turns the marker with the photo")
            clockwise.recycle()

            val counter=load(file.path,3)
            assertEquals(200,counter.width);assertEquals(400,counter.height)
            assertRed(counter,3,396,"counter-clockwise decode turns the marker the other way")
            counter.recycle()

            // Scaling runs after the turn, so the long side follows the turned bitmap.
            val small=load(file.path,1,100)
            assertEquals(50,small.width);assertEquals(100,small.height)
            assertRed(small,49,1,"scaled clockwise decode keeps the marker corner")
            small.recycle()
        } finally {file.delete()}
    }

    @Test fun rotatedAnnotationsFollowTheLineAndTheLabelStaysUpright() {
        val width=400f;val height=300f
        var doc=Document().add(Point(.2f,.3f),Point(.8f,.7f),null,null,Pen(width=6f))
        doc=doc.update(doc.segments.single().id){it.copy(dimension="2200")}
        val before=Renderer.label(doc,doc.segments.single(),width,height)!!
        assertEquals(width/2,before.center.x,.01f);assertEquals(height/2,before.center.y,.01f)
        var turned=doc
        repeat(4) {index->
            turned=turned.rotateClockwise()
            val w=if(index%2==0) height else width
            val h=if(index%2==0) width else height
            val geometry=Renderer.label(turned,turned.segments.single(),w,h)!!
            assertEquals("turn ${index+1}: the pill stays on the midpoint",w/2,geometry.center.x,.01f)
            assertEquals("turn ${index+1}: the pill stays on the midpoint",h/2,geometry.center.y,.01f)
            assertTrue("turn ${index+1}: angle ${geometry.angle}° must stay readable",geometry.angle>-90f && geometry.angle<=90f)
            var expected=(before.angle-90f*(index+1))%180f
            if(expected>90f) expected-=180f
            if(expected<=-90f) expected+=180f
            assertEquals("turn ${index+1}: the label turns with the line",expected,geometry.angle,.02f)

            val bitmap=Bitmap.createBitmap(w.toInt(),h.toInt(),Bitmap.Config.ARGB_8888)
            try {
                val canvas=Canvas(bitmap);canvas.drawColor(Color.WHITE)
                Renderer.annotations(canvas,turned,w,h)
                // The capsule is rotated too, so count its ink inside the turned bounding box.
                val ink=turned.segments.single().pen.color or 0xFF000000.toInt()
                val rad=Math.toRadians(geometry.angle.toDouble())
                val hw=geometry.width/2;val hh=geometry.height/2
                val extX=abs(cos(rad)).toFloat()*hw+abs(sin(rad)).toFloat()*hh
                val extY=abs(sin(rad)).toFloat()*hw+abs(cos(rad)).toFloat()*hh
                var painted=0
                for(y in (geometry.center.y-extY).toInt()..(geometry.center.y+extY).toInt())
                    for(x in (geometry.center.x-extX).toInt()..(geometry.center.x+extX).toInt())
                        if(x in 0 until bitmap.width && y in 0 until bitmap.height && bitmap.getPixel(x,y)==ink) painted++
                assertTrue("turn ${index+1}: the pill is painted at the new midpoint (found $painted ink pixels)",painted>20)
            } finally {bitmap.recycle()}
        }
        // Four turns bring the geometry and the label angle back to the start.
        val restored=Renderer.label(turned,turned.segments.single(),width,height)!!
        assertEquals(before.center.x,restored.center.x,.01f);assertEquals(before.angle,restored.angle,.02f)
        assertEquals(labelAngle(doc.vertex(doc.segments.single().start).point,doc.vertex(doc.segments.single().end).point),labelAngle(turned.vertex(turned.segments.single().start).point,turned.vertex(turned.segments.single().end).point),.02f)
    }

    @Test fun migrationFromVersionOneAddsRotationAndKeepsExistingPhotos() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="rotation-migration-test.db"
        context.deleteDatabase(name)
        val file=context.getDatabasePath(name).apply {parentFile?.mkdirs()}
        SQLiteDatabase.openOrCreateDatabase(file,null).use { db->
            db.execSQL("CREATE TABLE IF NOT EXISTS `Project` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `Photo` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `originalPath` TEXT NOT NULL, `thumbnailPath` TEXT NOT NULL, `orientedWidth` INTEGER NOT NULL, `orientedHeight` INTEGER NOT NULL, `exifOrientation` INTEGER NOT NULL, `roomName` TEXT NOT NULL, `note` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `document` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`projectId`) REFERENCES `Project`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_Photo_projectId` ON `Photo` (`projectId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `ExportRecord` (`id` TEXT NOT NULL, `json` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42,'a7dbdac9d43ed7d0ecd7935b06ca2b5c')")
            db.execSQL("INSERT INTO Project (id,name,createdAt,updatedAt) VALUES('p1','旧项目',1,1)")
            db.execSQL("INSERT INTO Photo (id,projectId,originalPath,thumbnailPath,orientedWidth,orientedHeight,exifOrientation,roomName,note,sortOrder,createdAt,updatedAt,document) VALUES('photo1','p1','/tmp/a.jpg','/tmp/a.thumb.jpg',400,300,1,'厨房','备注',0,1,1,'')")
            db.version=1
        }
        val room=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val dao=room.dao()
            val existing=runBlocking {dao.allPhotos()}.single()
            assertEquals("旧数据必须保留", "photo1",existing.id)
            assertEquals("升级后的照片默认不旋转",0,existing.rotation)
            assertEquals("旧项目必须保留","旧项目",runBlocking {dao.projects().first()}.single().name)
            runBlocking {dao.save(existing.copy(rotation=3))}
            assertEquals("rotation 必须能持久化",3,runBlocking {dao.allPhotos()}.single().rotation)
        } finally {room.close();context.deleteDatabase(name)}
    }
}
