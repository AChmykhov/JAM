package com.example.jam

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class MainActivity : AppCompatActivity() {
    lateinit var server: receiverServer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runServer()
    }

    inner class receiverServer @Throws(IOException::class) constructor() : NanoHTTPD(63343) {

        init {
            start(SOCKET_READ_TIMEOUT, false)
        }

        fun stpServer() {
//            val queue = Volley.newRequestQueue(this@MainActivity)
//            val ip = getIP()
//            val stringRequest = StringRequest(
//                Request.Method.POST, "http://$ip:63342/?Exit=true",
//                com.android.volley.Response.Listener { response ->
//                    this.stop()
//                },
//                com.android.volley.Response.ErrorListener { error ->
//                    runOnUiThread(
//                        Toast.makeText(
//                            this@MainActivity,
//                            "exit error " + error.toString(),
//                            Toast.LENGTH_SHORT
//                        )::show
//                    )
//                })
//            queue.add(stringRequest)
            this.stop()
        }


        override fun serve(session: IHTTPSession): Response {
            val params = session.parameters
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "Message received",
                    Toast.LENGTH_SHORT
                ).show()
            }
            runOnUiThread {
                Toast.makeText(this@MainActivity, params.toString(), Toast.LENGTH_SHORT).show()
            }
            if (params.containsKey("message")) {
                params["message"]?.get(0)?.let {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            return newFixedLengthResponse("200 OK")
        }
    }

    fun runServer() {
        server = receiverServer()
    }


    fun getIP(): String? {
        val dataIP = findViewById<TextInputEditText>(R.id.ipInput)
        return dataIP.text.toString()
    }

    fun getMessageText(): String? {
        val dataMessage = findViewById<TextInputEditText>(R.id.messageInput)
        return dataMessage.text.toString()
    }

    fun sendMessage(@Suppress("UNUSED_PARAMETER") view: View) {
        val wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (getIP() == "") {
            Toast.makeText(this, "No IP address entered", Toast.LENGTH_LONG).show()
        } else {
            if (!(wifiManager.isWifiEnabled)) {
                Toast.makeText(
                    this,
                    "No connection to Wi-Fi network",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val queue = Volley.newRequestQueue(this@MainActivity)
                val ip = getIP()
                val messageText = getMessageText()
                val stringRequest = StringRequest(
                    Request.Method.POST, "http://$ip:63342/?message=true$messageText",
                    Response.Listener { response ->
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, response, Toast.LENGTH_SHORT).show()
                        }
                    },
                    Response.ErrorListener { error ->
                        runOnUiThread(
                            Toast.makeText(
                                this@MainActivity,
                                "exit error " + error.toString(),
                                Toast.LENGTH_SHORT
                            )::show
                        )
                    })
                queue.add(stringRequest)
            }
        }
    }

    override fun onBackPressed() {
        close()
        super.onBackPressed()
    }

    fun close() {
        server.stpServer()
//        mediaplayer.stop()
        finish()
    }

}
