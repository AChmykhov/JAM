package com.example.jam

//import android.util.Base64.DEFAULT
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Base64
import android.util.Base64.URL_SAFE
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
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class MainActivity : AppCompatActivity() {
    lateinit var server: ReceiverServer
    var keyPair = generateKeys()
    var symmetricalKey = generateSymKey()
    var localIv: ByteArray? = null
    var externalMessage = ":(("


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runServer()
        findViewById<TextView>(R.id.myIP).text = getLocalIpAddress().toString()
    }

    fun urlEncode(text: ByteArray?): String {
        if (text == null) {
            runOnUiThread(
                Toast.makeText(
                    this@MainActivity,
                    "ByteArray for url encoding is NULL",
                    Toast.LENGTH_SHORT
                )::show
            )
            return ""
        } else {
            val result = URLEncoder.encode(
                Base64.encodeToString(text, URL_SAFE),
                "UTF-8"
            )
            return result
        }
    }

    fun urlDecode(text: String?): ByteArray? {
        if (text == null) {
            runOnUiThread(
                Toast.makeText(
                    this@MainActivity,
                    "ByteArray for url encoding is NULL",
                    Toast.LENGTH_SHORT
                )::show
            )
            return null
        } else {
            val result = Base64.decode(
                URLDecoder.decode(
                    text,
                    "UTF-8"
                ), URL_SAFE
            )
            return result
        }
    }

    inner class ReceiverServer @Throws(IOException::class) constructor() : NanoHTTPD(63342) {

        init {
            start(SOCKET_READ_TIMEOUT, false)
        }

        fun stpServer() {
            //Add here end of session with ndns server
            this.stop()
        }


        override fun serve(session: IHTTPSession): Response {
            val params = session.parameters
//            runOnUiThread {
//                Toast.makeText(
//                    this@MainActivity,
//                    "Message received",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//            runOnUiThread {
//                Toast.makeText(this@MainActivity, params.toString(), Toast.LENGTH_SHORT).show()
//            }
            when {
                params.containsKey("message") -> {                                                      //Tyk
                    try {
                        val encryptedMessage = urlDecode(params["message"]?.get(0))
                        val encryptedExternalSymKey = urlDecode(params["key"]?.get(0))
                        val externalIv =
                            IvParameterSpec(
                                urlDecode(params["symIv"]?.get(0))
                            )
                        val externalSymKey =
                            decryptKey(
                                encryptedExternalSymKey,
                                this@MainActivity.keyPair?.private
                            )
                        externalMessage =
                            decryptMessage(encryptedMessage, externalSymKey, externalIv)
                    } catch (e: java.lang.Exception) {
//                        runOnUiThread {
//                            Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT).show()
//                        }
//                        System.err.println(e.stackTrace)
                        e.printStackTrace()
                    }

//                    runOnUiThread(
//                        Toast.makeText(
//                            this@MainActivity,
//                            externalMessage,
//                            Toast.LENGTH_SHORT
//                        )::show
//                    )
                    println("message is " + externalMessage)

                    return newFixedLengthResponse("200 OK")
                }
                params.containsKey("newMessage") -> {
//                    runOnUiThread(
//                        Toast.makeText(
//                            this@MainActivity,
//                            "newMessage signal received",
//                            Toast.LENGTH_SHORT
//                        )::show
//                    )
                    val publKey = this@MainActivity.keyPair?.public?.encoded
                    return newFixedLengthResponse(urlEncode(publKey))
                }
                else -> return newFixedLengthResponse("200 OK")
            }
        }
    }

    fun runServer() {
        server = ReceiverServer()
    }

    fun getLocalIpAddress(): String? {
        try {

            val wifiManager: WifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "Hi, there is exception with RSA keys: " + e.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
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


    fun encryptKey(symmetricalKey: ByteArray?, publicKey: PublicKey): ByteArray? {
        if (symmetricalKey == null) {
            runOnUiThread(
                Toast.makeText(
                    this@MainActivity,
                    "ByteArray for url encoding is NULL",
                    Toast.LENGTH_SHORT
                )::show
            )
            return null
        }
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val ciphertext: ByteArray = cipher.doFinal(symmetricalKey)
        return ciphertext
    }

    fun encryptMessage(messageText: String?): ByteArray? {
        if (messageText == null) {
            runOnUiThread(
                Toast.makeText(
                    this@MainActivity,
                    "ByteArray for url encoding is NULL",
                    Toast.LENGTH_SHORT
                )::show
            )
            return null
        }
        println("my message is " + messageText)
        val plaintext: ByteArray = messageText.toByteArray()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, symmetricalKey)
        localIv = cipher.iv
        println("iv before is " + urlEncode(cipher.iv))
        val ciphertext: ByteArray = cipher.doFinal(plaintext)
        println("iv after is " + urlEncode(cipher.iv))
        return ciphertext
    }

    fun decryptKey(
        encryptedSymmetricalKey: ByteArray?,
        privateKey: PrivateKey?
    ): SecretKey {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val plaintext: ByteArray = cipher.doFinal(encryptedSymmetricalKey)
        return SecretKeySpec(plaintext, "RSA")

    }

    fun decryptMessage(
        ciphertext: ByteArray?,
        key: SecretKey,
        externalIvParameterSpec: IvParameterSpec
    ): String {
        if (ciphertext == null) {
            runOnUiThread(
                Toast.makeText(
                    this@MainActivity,
                    "ByteArray for url encoding is NULL",
                    Toast.LENGTH_SHORT
                )::show
            )
            return ""
        } else {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, key, externalIvParameterSpec)
            val plaintext: ByteArray = cipher.doFinal(ciphertext)
            return plaintext.toString()
        }
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
                val messageText = urlEncode(encryptMessage(getMessageText()))
                println(messageText)
                val stringRequest = StringRequest(
                    Request.Method.POST, "http://$ip:63342/?newMessage=true",
                    Response.Listener { response ->
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "public key received",
                                Toast.LENGTH_SHORT
                            )::show
                        }
                        try {
                            val pubKey = KeyFactory.getInstance("RSA").generatePublic(
                                X509EncodedKeySpec(
                                    urlDecode(response)
                                )
                            )
                            println("pubKey:" + pubKey)
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "println $pubKey",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }

                            val encryptedSymmetricalKey = urlEncode(
                                encryptKey(
                                    symmetricalKey!!.encoded,
                                    pubKey
                                )
                            )
                            val stringRequest1 = StringRequest(
                                Request.Method.POST,
                                "http://$ip:63342/?message=$messageText&key=$encryptedSymmetricalKey&symIv=${urlEncode(
                                    localIv
                                )}",
                                Response.Listener { secondResponse ->
                                    runOnUiThread {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Message sended with code $secondResponse",
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
                            queue.add(stringRequest1)
                        } catch (e: java.lang.Exception) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "exception in encrypting data: " + e.toString(),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    },
                    Response.ErrorListener { error ->
                        runOnUiThread(
                            Toast.makeText(
                                this@MainActivity,
                                "exit error number 2 $error",
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
