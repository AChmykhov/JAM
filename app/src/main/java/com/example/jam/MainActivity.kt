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
import com.android.volley.RequestQueue
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
    lateinit var queue: RequestQueue
    var keyPair: KeyPair = generateKeys()
    var symmetricalKey: SecretKey = generateSymKey()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runServer()
        queue = Volley.newRequestQueue(this@MainActivity)
        findViewById<TextView>(R.id.myIP).text = getLocalIpAddress().toString()
    }

    fun showMessage(msg: String) {
        runOnUiThread {
            Toast.makeText(
                this@MainActivity,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
        System.err.println(msg)
        Log.e("Toast", msg)
    }

    fun showErrorMessage(e: java.lang.Exception, msg: String = "") {
        runOnUiThread {
            Toast.makeText(
                this@MainActivity,
                "$msg: $e",
                Toast.LENGTH_SHORT
            ).show()
        }
        e.printStackTrace()
        Log.e("Exception", e.toString())
    }


    fun urlEncode(text: ByteArray): String {
        return URLEncoder.encode(Base64.encodeToString(text, URL_SAFE), "UTF-8")
    }

    fun urlDecode(text: String): ByteArray {
        return Base64.decode(URLDecoder.decode(text, "UTF-8"), URL_SAFE)
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
            when {
                params.containsKey("message") -> {
                    try {
                        val externalMessage =
                            decodeMessageFromParams(params, this@MainActivity.keyPair.private)
                        showMessage("message is `$externalMessage`")
                    } catch (e: java.lang.Exception) {
                        showErrorMessage(e, "message obtaining failed")
                    }

                    return newFixedLengthResponse("200 OK")
                }
                params.containsKey("newMessage") -> {
                    val publKey = this@MainActivity.keyPair.public.encoded
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

    fun getIP(): String {
        val dataIP = findViewById<TextInputEditText>(R.id.ipInput)
        return dataIP.text.toString()
    }

    fun getMessageText(): String {
        val dataMessage = findViewById<TextInputEditText>(R.id.messageInput)
        return dataMessage.text.toString()
    }

    fun generateKeys(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(1024)
        return keyGen.generateKeyPair()
    }

    fun generateSymKey(): SecretKey {
        val keygen = KeyGenerator.getInstance("AES")
        keygen.init(256)
        return keygen.generateKey()
    }

    fun encryptKey(symKey: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(symKey)
    }

    fun encryptMessage(
        messageText: String,
        symKey: SecretKey
    ): Triple<ByteArray, ByteArray, ByteArray> {
        val plaintext: ByteArray = messageText.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, symKey)
        val ciphertext: ByteArray = cipher.doFinal(plaintext)
        return Triple(ciphertext, cipher.iv, symKey.encoded)
    }

    fun encodeAndSend(
        encodedMessage: Triple<ByteArray, ByteArray, ByteArray>,
        pubKey: PublicKey,
        ip: String
    ) {
        val (encMessage, iv, encodedSymKey) = encodedMessage
        val encryptedSymKey = encryptKey(encodedSymKey, pubKey)
        val stringRequest = StringRequest(
            Request.Method.POST,
            "http://$ip:63342/?message=${urlEncode(encMessage)}&key=${urlEncode(encryptedSymKey)}&symIv=${urlEncode(
                iv
            )}",
            Response.Listener { response ->
                showMessage("Message sent with code $response")
            },
            Response.ErrorListener { error ->
                showErrorMessage(error, "exit error")
            }
        )
        queue.add(stringRequest)
    }

    fun decryptKey(encryptedSymmetricalKey: ByteArray, privateKey: PrivateKey): SecretKey {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val plaintext: ByteArray = cipher.doFinal(encryptedSymmetricalKey)
        return SecretKeySpec(plaintext, "RSA")
    }


    fun decryptMessage(
        ciphertext: ByteArray,
        iv: ByteArray,
        encryptedSymKey: ByteArray,
        privateKey: PrivateKey
    ): String {
        val symKey = decryptKey(encryptedSymKey, privateKey)
        return decryptMessage(ciphertext, iv, symKey)
    }

    fun decryptMessage(ciphertext: ByteArray, iv: ByteArray, symKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, symKey, ivParameterSpec)
        val plaintext: ByteArray = cipher.doFinal(ciphertext)
        return plaintext.toString(Charsets.UTF_8)
    }

    fun decodeMessageFromParams(params: Map<String, List<String>>, privateKey: PrivateKey): String {
        if (!(params.containsKey("message") && params.containsKey("key") && params.containsKey("symIv"))) {
            throw IllegalArgumentException("Missing parameters for incoming message")
        }
        val encryptedMessage = urlDecode(params.getValue("message")[0])
        val encryptedExternalSymKey = urlDecode(params.getValue("key")[0])
        val externalIv = urlDecode(params.getValue("symIv")[0])
        return decryptMessage(encryptedMessage, externalIv, encryptedExternalSymKey, privateKey)
    }

    fun sendMessageInternal() {
        val ip = getIP()
        val msg: String = getMessageText()
        val encodedMessage = encryptMessage(msg, symmetricalKey)
        val stringRequest = StringRequest(
            Request.Method.POST, "http://$ip:63342/?newMessage=true",
            Response.Listener { response ->
                try {
                    val pubKey = KeyFactory.getInstance("RSA").generatePublic(
                        X509EncodedKeySpec(
                            urlDecode(response)
                        )
                    )
                    encodeAndSend(encodedMessage, pubKey, ip)
                } catch (e: java.lang.Exception) {
                    showErrorMessage(e, "exception in encrypting data")
                }
            },
            Response.ErrorListener { error ->
                showErrorMessage(error, "Failed to get public key")
            }
        )
        queue.add(stringRequest)
    }

    fun sendMessage(@Suppress("UNUSED_PARAMETER") view: View) {
        val wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (getIP() == "") {
            showMessage("No IP address entered")
            return
        }
        if (!wifiManager.isWifiEnabled) {
            showMessage("No connection to Wi-Fi network")
            return
        }
        sendMessageInternal()
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


