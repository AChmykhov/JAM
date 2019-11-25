package com.example.jam

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Base64
import android.util.Base64.DEFAULT
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.URLEncoder
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


class MainActivity : AppCompatActivity() {
    lateinit var server: receiverServer
    var keyPair = generateKeys()
    var symmetricalKey = generateSymKey()
    var localIv: ByteArray? = null
    var localRsaIv: ByteArray? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runServer()
        findViewById<TextView>(R.id.myIP).text = getLocalIpAddress().toString()
    }

    inner class receiverServer @Throws(IOException::class) constructor() : NanoHTTPD(63342) {

        init {
            start(SOCKET_READ_TIMEOUT, false)
        }

        fun stpServer() {
            //Add here end of session with ndns server
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
            if (params.containsKey("message")) {                                                      //Tyk!
                params["message"]?.get(0)?.let {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }
                return newFixedLengthResponse("200 OK")
            } else if (params.containsKey("newMessage")) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "newMessage signal received",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                val publKey = this@MainActivity.keyPair?.private?.encoded
                val publicKeyText = Base64.encodeToString(publKey, DEFAULT)

                return newFixedLengthResponse(publicKeyText)
            } else
                return newFixedLengthResponse("200 OK")
        }
    }

    fun runServer() {
        server = receiverServer()
    }

    fun getLocalIpAddress(): String? {
        try {

            val wifiManager: WifiManager =
                getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
            return ipToString(wifiManager.connectionInfo.ipAddress)
        } catch (ex: Exception) {
            Log.e("IP Address", ex.toString())
        }

        return null
    }

    fun ipToString(i: Int): String {
        return (i and 0xFF).toString() + "." +
                (i shr 8 and 0xFF) + "." +
                (i shr 16 and 0xFF) + "." +
                (i shr 24 and 0xFF)

    }


    fun getIP(): String? {
        val dataIP = findViewById<TextInputEditText>(R.id.ipInput)
        return dataIP.text.toString()
    }

    fun getMessageText(): String? {
        val dataMessage = findViewById<TextInputEditText>(R.id.messageInput)
        return dataMessage.text.toString()
    }

    fun generateKeys(): KeyPair? {
        var keyPair: KeyPair? = null
        try {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(1024)
            keyPair = keyGen.generateKeyPair()
        } catch (e: java.lang.Exception) {
            println(e)
        }

        return keyPair
    }

    fun generateSymKey(): SecretKey? {
        val keygen = KeyGenerator.getInstance("AES")
        keygen.init(256)
        val key = keygen.generateKey()
        return key
    }


    fun encryptKey(symmetricalKey: ByteArray?, publicKey: String?): String {
        val decodedPublKey = SecretKeySpec(Base64.decode(publicKey, DEFAULT), "RSA")
        val cipher = Cipher.getInstance("RSA/EBC/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, decodedPublKey)
        val ciphertext: ByteArray = cipher.doFinal(symmetricalKey)
        localRsaIv = cipher.iv
        val ciphertextString = Base64.encodeToString(ciphertext, DEFAULT)
        return ciphertextString
    }

    fun encryptMessage(messageText: String): String {


        val plaintext: ByteArray = messageText.toByteArray()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, symmetricalKey)
        val ciphertext: ByteArray = cipher.doFinal(plaintext)
        localIv = cipher.iv
        val ciphertextString = ciphertext.toString()
        return ciphertextString
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
                val messageText = URLEncoder.encode(encryptMessage(getMessageText()!!), "UTF-8")

                val stringRequest = StringRequest(
                    Request.Method.POST, "http://$ip:63342/?newMessage=true",
                    Response.Listener { response ->
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "public key received",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        val stringRequest = StringRequest(
                            Request.Method.POST,
                            "http://$ip:63342/?message=$messageText&key=${encryptKey(
                                symmetricalKey!!.getEncoded(),
                                response
                            )}&symmIv=$localIv&asymmIv=$localRsaIv",
                            Response.Listener { response ->
                                runOnUiThread {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Message sended with code $response",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            },
                            Response.ErrorListener { error ->
                                runOnUiThread(
                                    Toast.makeText(
                                        this@MainActivity,
                                        "exit error$error",
                                        Toast.LENGTH_SHORT
                                    )::show
                                )
                            })
                        queue.add(stringRequest)

                    },
                    Response.ErrorListener { error ->
                        runOnUiThread(
                            Toast.makeText(
                                this@MainActivity,
                                "exit error$error",
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
