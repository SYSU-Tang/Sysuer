package com.miyuyan.sysuer

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class UncaughtExceptionHandlerContentProvider : ContentProvider() {
	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String?>?): Int {
		return 0
	}
	
	override fun getType(uri: Uri): String {
		return ""
	}
	
	override fun insert(uri: Uri, values: ContentValues?): Uri? {
		return null
	}
	
	override fun onCreate(): Boolean {
		Thread.setDefaultUncaughtExceptionHandler(this.context?.let<Context, MyCustomCrashHandler> { MyCustomCrashHandler(it, Thread.getDefaultUncaughtExceptionHandler()) })
		return true
	}
	
	override fun query(uri: Uri,
	                   projection: Array<String?>?,
	                   selection: String?,
	                   selectionArgs: Array<String?>?,
	                   sortOrder: String?): Cursor? {
		return null
	}
	
	override fun update(uri: Uri,
	                    values: ContentValues?,
	                    selection: String?,
	                    selectionArgs: Array<String?>?): Int {
		return 0
	}
	
	internal class MyCustomCrashHandler(context: Context,
	                                    private val defaultHandler: Thread.UncaughtExceptionHandler?) :
		Thread.UncaughtExceptionHandler {
		private val app: Context = context.applicationContext
		override fun uncaughtException(thread: Thread,
		                               e: Throwable) { // We are now safely being called after Crashlytics does its own thing.
			// Whoever is the last handler on Thread.getDefaultUncaughtExceptionHandler() will execute first on uncaught exceptions.
			// Firebase Crashlytics will handle its own behavior first before calling ours in its own 'finally' block.
			// You can choose to propagate upwards (it will kill the app by default) or do your own thing and propagate if needed.
			try {
				val sw = StringWriter()
				val pw = PrintWriter(sw)
				e.printStackTrace(pw)
				pw.close()
				app.startActivity(Intent(app, CrashActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
									  .putExtra("crash", "$sw"))
				defaultHandler?.uncaughtException(thread, e)
				Process.killProcess(Process.myPid())
				exitProcess(10) //do your own thing.
			} catch (_: Exception) {
			} finally {
				defaultHandler?.uncaughtException(thread, e)
			}
		}
	}
}
