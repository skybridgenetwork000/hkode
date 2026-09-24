package com.hkode.h3nrican3.app

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LightingColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtil {

    private fun createNewFile(path: String) {
        val lastSep = path.lastIndexOf(File.separator)
        if (lastSep > 0) {
            val dirPath = path.substring(0, lastSep)
            makeDir(dirPath)
        }

        val file = File(path)
        try {
            if (!file.exists()) file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun readFile(path: String): String {
        createNewFile(path)
        val sb = StringBuilder()
        var fr: FileReader? = null
        try {
            fr = FileReader(File(path))
            val buff = CharArray(1024)
            var length: Int
            while (fr.read(buff).also { length = it } > 0) {
                sb.append(buff, 0, length)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fr?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return sb.toString()
    }

    @JvmStatic
    fun writeFile(path: String, str: String) {
        createNewFile(path)
        var fileWriter: FileWriter? = null
        try {
            fileWriter = FileWriter(File(path), false)
            fileWriter.write(str)
            fileWriter.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fileWriter?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun copyFile(sourcePath: String, destPath: String) {
        if (!isExistFile(sourcePath)) return
        createNewFile(destPath)

        var fis: FileInputStream? = null
        var fos: FileOutputStream? = null

        try {
            fis = FileInputStream(sourcePath)
            fos = FileOutputStream(destPath, false)
            val buff = ByteArray(1024)
            var length: Int
            while (fis.read(buff).also { length = it } > 0) {
                fos.write(buff, 0, length)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fis?.close()
            } catch (_: IOException) {}
            try {
                fos?.close()
            } catch (_: IOException) {}
        }
    }

    @JvmStatic
    fun copyDir(oldPath: String, newPath: String) {
        val oldFile = File(oldPath)
        val files = oldFile.listFiles() ?: return
        val newFile = File(newPath)
        if (!newFile.exists()) {
            newFile.mkdirs()
        }
        for (file in files) {
            if (file.isFile) {
                copyFile(file.path, "$newPath/${file.name}")
            } else if (file.isDirectory) {
                copyDir(file.path, "$newPath/${file.name}")
            }
        }
    }

    @JvmStatic
    fun moveFile(sourcePath: String, destPath: String) {
        copyFile(sourcePath, destPath)
        deleteFile(sourcePath)
    }

    @JvmStatic
    fun deleteFile(path: String) {
        val file = File(path)
        if (!file.exists()) return

        if (file.isFile) {
            file.delete()
            return
        }

        val fileArr = file.listFiles()
        if (fileArr != null) {
            for (subFile in fileArr) {
                if (subFile.isDirectory) {
                    deleteFile(subFile.absolutePath)
                } else if (subFile.isFile) {
                    subFile.delete()
                }
            }
        }
        file.delete()
    }

    @JvmStatic
    fun isExistFile(path: String): Boolean {
        return File(path).exists()
    }

    @JvmStatic
    fun makeDir(path: String) {
        if (!isExistFile(path)) {
            File(path).mkdirs()
        }
    }

    @JvmStatic
    fun listDir(path: String, list: ArrayList<String>?) {
        val dir = File(path)
        if (!dir.exists() || dir.isFile) return

        val listFiles = dir.listFiles()
        if (listFiles == null || listFiles.isEmpty()) return

        if (list == null) return
        list.clear()
        for (file in listFiles) {
            list.add(file.absolutePath)
        }
    }

    @JvmStatic
    fun isDirectory(path: String): Boolean {
        if (!isExistFile(path)) return false
        return File(path).isDirectory
    }

    @JvmStatic
    fun isFile(path: String): Boolean {
        if (!isExistFile(path)) return false
        return File(path).isFile
    }

    @JvmStatic
    fun getFileLength(path: String): Long {
        if (!isExistFile(path)) return 0
        return File(path).length()
    }

    @JvmStatic
    fun getExternalStorageDir(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    @JvmStatic
    fun getPackageDataDir(context: Context): String {
        return context.getExternalFilesDir(null)?.absolutePath ?: ""
    }

    @JvmStatic
    fun getPublicDir(type: String): String {
        return Environment.getExternalStoragePublicDirectory(type).absolutePath
    }

    @JvmStatic
    fun convertUriToFilePath(context: Context, uri: Uri): String? {
        var path: String? = null
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    path = "${Environment.getExternalStorageDirectory()}/${split[1]}"
                }
            } else if (isDownloadsDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                if ("raw".equals(type, ignoreCase = true)) {
                    return split[1]
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && "msf".equals(type, ignoreCase = true)) {
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])
                    path = getDataColumn(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)
                } else {
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        docId.toLongOrNull() ?: 0L
                    )
                    path = getDataColumn(context, contentUri, null, null)
                }
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                if (contentUri != null) {
                    path = getDataColumn(context, contentUri, selection, selectionArgs)
                }
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true)) {
            path = getDataColumn(context, uri, null, null)
        } else if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
            path = uri.path
        }

        if (path != null) {
            try {
                return URLDecoder.decode(path, "UTF-8")
            } catch (_: Exception) {
                return null
            }
        }
        return null
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf(column)
        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(columnIndex)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean =
        "com.android.externalstorage.documents" == uri.authority

    private fun isDownloadsDocument(uri: Uri): Boolean =
        "com.android.providers.downloads.documents" == uri.authority

    private fun isMediaDocument(uri: Uri): Boolean =
        "com.android.providers.media.documents" == uri.authority

    @JvmStatic
    fun saveBitmap(bitmap: Bitmap, destPath: String) {
        createNewFile(destPath)
        try {
            FileOutputStream(File(destPath)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun getScaledBitmap(path: String, max: Int): Bitmap? {
        val src = BitmapFactory.decodeFile(path) ?: return null
        val width = src.width
        val height = src.height
        val rate: Float
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            rate = max / width.toFloat()
            newHeight = (height * rate).toInt()
            newWidth = max
        } else {
            rate = max / height.toFloat()
            newWidth = (width * rate).toInt()
            newHeight = max
        }
        return Bitmap.createScaledBitmap(src, newWidth, newHeight, true)
    }

    @JvmStatic
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    @JvmStatic
    fun decodeSampleBitmapFromPath(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    @JvmStatic
    fun resizeBitmapFileRetainRatio(fromPath: String, destPath: String, max: Int) {
        if (!isExistFile(fromPath)) return
        val bitmap = getScaledBitmap(fromPath, max) ?: return
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun resizeBitmapFileToSquare(fromPath: String, destPath: String, max: Int) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath) ?: return
        val bitmap = Bitmap.createScaledBitmap(src, max, max, true)
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun resizeBitmapFileToCircle(fromPath: String, destPath: String) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath) ?: return
        val bitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val color = -0xbdbdbe
        val paint = Paint().apply {
            isAntiAlias = true
            setColor(color)
        }
        val rect = Rect(0, 0, src.width, src.height)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(src.width / 2f, src.height / 2f, src.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, rect, rect, paint)
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun resizeBitmapFileWithRoundedBorder(fromPath: String, destPath: String, pixels: Int) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath) ?: return
        val bitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val color = -0xbdbdbe
        val paint = Paint().apply {
            isAntiAlias = true
            setColor(color)
        }
        val rect = Rect(0, 0, src.width, src.height)
        val rectF = RectF(rect)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, pixels.toFloat(), pixels.toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, rect, rect, paint)
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun cropBitmapFileFromCenter(fromPath: String, destPath: String, w: Int, h: Int) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath) ?: return
        val width = src.width
        val height = src.height
        if (width < w && height < h) return
        var x = 0
        var y = 0
        if (width > w) x = (width - w) / 2
        if (height > h) y = (height - h) / 2
        val cw = if (w > width) width else w
        val ch = if (h > height) height else h
        val bitmap = Bitmap.createBitmap(src, x, y, cw, ch)
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun rotateBitmapFile(fromPath: String, destPath: String, angle: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath) ?: return
        val matrix = Matrix().apply { postRotate(angle) }
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun scaleBitmapFile(fromPath: String, destPath: String, x: Float, y: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath) ?: return
        val matrix = Matrix().apply { postScale(x, y) }
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun skewBitmapFile(fromPath: String, destPath: String, x: Float, y: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath) ?: return
        val matrix = Matrix().apply { postSkew(x, y) }
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun setBitmapFileColorFilter(fromPath: String, destPath: String, color: Int) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath) ?: return
        val bitmap = Bitmap.createBitmap(src.width - 1, src.height - 1, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            colorFilter = LightingColorFilter(color, 1)
        }
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(src, 0f, 0f, paint)
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun setBitmapFileBrightness(fromPath: String, destPath: String, brightness: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath) ?: return
        val cm = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightness,
                0f, 1f, 0f, 0f, brightness,
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val config = src.config ?: Bitmap.Config.ARGB_8888
        val bitmap = Bitmap.createBitmap(src.width, src.height, config)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun setBitmapFileContrast(fromPath: String, destPath: String, contrast: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath) ?: return
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, 0f,
                0f, contrast, 0f, 0f, 0f,
                0f, 0f, contrast, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val config = src.config ?: Bitmap.Config.ARGB_8888
        val bitmap = Bitmap.createBitmap(src.width, src.height, config)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        saveBitmap(bitmap, destPath)
    }

    @JvmStatic
    fun getJpegRotate(filePath: String): Int {
        return try {
            val exif = ExifInterface(filePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: IOException) {
            0
        }
    }

    @JvmStatic
    fun createNewPictureFile(context: Context): File {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "${date.format(Date())}.jpg"
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath + File.separator + fileName)
    }
}
