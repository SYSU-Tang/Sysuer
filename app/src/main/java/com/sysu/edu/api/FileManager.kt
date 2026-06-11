package com.sysu.edu.api

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object FileManager {
	@JvmStatic fun readAssets(context: Context, file: String): String {
		val jsJSON = StringBuilder()
		try {
			val input = InputStreamReader(context.assets.open(file))
			val buffer = BufferedReader(input)
			var line: String?
			while ((buffer.readLine().also { line = it }) != null) jsJSON.append(line)
			input.close()
			buffer.close()
		} catch (_: IOException) {
		}
		return "$jsJSON"
	}
}
