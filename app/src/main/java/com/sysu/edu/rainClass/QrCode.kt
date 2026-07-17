package com.sysu.edu.rainClass

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.bumptech.glide.Glide
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Timer
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.timerTask

class QrCode(private val imageView: ImageView? = null) {
	private var fetchQrcodeTimer: Timer? = null
	private var webSocket: WebSocket? = null
	private var loginMessage: JSONObject? = null
	private val latch = CountDownLatch(1)
	
	companion object {
		private const val TAG = "QrCode"
	}
	
	fun run(): JSONObject? {
		val client = OkHttpClient()
		val request = Request.Builder().url("wss://www.yuketang.cn/wsapp/").build()
		
		client.newWebSocket(request, object : WebSocketListener() {
			override fun onOpen(webSocket: WebSocket, response: Response) {
				Log.d(TAG, "Connection opened")
				this@QrCode.webSocket = webSocket
				
				fetchQrcode()
				fetchQrcodeTimer = Timer()
				fetchQrcodeTimer?.schedule(timerTask {
					fetchQrcode()
				}, 60_000, 60_000)
			}
			
			override fun onMessage(webSocket: WebSocket, text: String) {
				try {
					val msg = JSON.parseObject(text)
					
					if (msg.containsKey("ticket")) {
						val qrcodeData = msg.getString("qrcode")
						if (!qrcodeData.isNullOrEmpty()) {
							printQrcode(qrcodeData)
						}
					}
					
					when (msg.getString("op")) {
						"requestlogin" -> fetchQrcode()
						"loginsuccess" -> {
							loginMessage = msg
							webSocket.close(1000, "Login Success")
							fetchQrcodeTimer?.cancel()
							latch.countDown()
						}
					}
				} catch (e: Exception) {
					Log.e(TAG, "JSON 解析错误: ${e.message}")
				}
			}
			
			override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
				Log.e(TAG, "Error: ${t.message}")
				latch.countDown()
			}
			
			override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
				Log.d(TAG, "Connection closed")
				latch.countDown()
			}
		})
		latch.await()
		
		client.dispatcher.executorService.shutdown()
		
		return loginMessage
	}
	
	private fun fetchQrcode() {
		webSocket?.send(JSONObject.of("op", "requestlogin", "role", "web", "version", 1.4, "type", "qrcode").toJSONString())
	}
	
	private fun printQrcode(qrData: String) {
		try {
			//Log.d(TAG, "QRCode Data: $qrData")
			//val consoleWriter = QRCodeWriter()
			//val consoleMatrix = consoleWriter.encode(qrData, BarcodeFormat.QR_CODE, 40, 40)
			//val sb = StringBuilder("\n")
			//for (y in 0 until consoleMatrix.height) {
			//	for (x in 0 until consoleMatrix.width) {
			//		sb.append(if (consoleMatrix.get(x, y)) "██" else "  ")
			//	}
			//	sb.append("\n")
			//}
			//Log.d(TAG, "$sb")
			val size = 500
			val imageMatrix = QRCodeWriter().encode(qrData, BarcodeFormat.QR_CODE, size, size)
			val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)
			
			for (x in 0 until size) {
				for (y in 0 until size) {
					bitmap[x, y] = if (imageMatrix.get(x, y)) Color.BLACK else Color.WHITE
				}
			}
			imageView?.let { iv ->
				iv.post {
					Glide.with(iv.context).load(bitmap).into(iv)
				}
			}
		} catch (e: Exception) {
			Log.e(TAG, "无法生成二维码: ${e.message}")
		}
	}
}