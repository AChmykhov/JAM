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
    var keyPair = generateKeys()
    var symmetricalKey: SecretKey? = generateSymKey()
    var localIv: ByteArray? = null
    var externalMessage = ":(("

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runServer()
        findViewById<TextView>(R.id.myIP).text = getLocalIpAddress().toString()
    }

    fun showMessage(msg: String) {
        runOnUiThread(
            Toast.makeText(
                this@MainActivity,
                msg,
                Toast.LENGTH_SHORT
            )::show
        )
        System.err.println(msg)
        Log.e("Toast", msg)
    }

    fun showErrorMessage(e: java.lang.Exception, msg: String = "") {
        runOnUiThread(
            Toast.makeText(
                this@MainActivity,
                "$msg: $e",
                Toast.LENGTH_SHORT
            )::show
        )
        e.printStackTrace()
        Log.e("Exception", e.toString())
    }


    fun urlEncode(text: ByteArray?): String {
        if (text == null) {
            showMessage("ByteArray for url encoding is NULL")
            return ""
        }
        return URLEncoder.encode(Base64.encodeToString(text, URL_SAFE),"UTF-8")
    }

    fun urlDecode(text: String?): ByteArray? {
        if (text == null) {
            showMessage("ByteArray for url encoding is NULL")
            return null
        }
        return Base64.decode(URLDecoder.decode(text,"UTF-8"), URL_SAFE)
    }

    inner class ReceiverServer @Throws(IOException::class) constructor() : NanoHTTPD(63342) {

        init {
            start(SOCKET_READ_TIMEOUT, false)
        }

        fun stpServer() {
            //Add here end of session with ndns server
            this.stop()
        }

        fun decodeMessageFromParams(params: MutableMap<String, MutableList<String>>, keyPair: KeyPair) : String {
            val encryptedMessage = urlDecode(params["message"]?.get(0))
            val encryptedExternalSymKey = urlDecode(params["key"]?.get(0))
            val externalIv =
                IvParameterSpec(
                    urlDecode(params["symIv"]?.get(0))
                )
            val externalSymKey =
                decryptKey(
                    encryptedExternalSymKey,
                    keyPair.private
                )
            return decryptMessage(encryptedMessage, externalSymKey, externalIv)
        }

        override fun serve(session: IHTTPSession): Response {
            val params = session.parameters
//            showMessage("Message received. Params: $params")
            when {
                params.containsKey("message") -> {  //Tyk
                    try {
                        externalMessage = decodeMessageFromParams(params, this@MainActivity.keyPair!!)
                        showMessage("message is " + externalMessage)
                    } catch (e: java.lang.Exception) {
                        showErrorMessage(e, "message obtaining failed")
                    }

                    return newFixedLengthResponse("200 OK")
                }
                params.containsKey("newMessage") -> {
                    showMessage("newMessage signal received")
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


    fun getIP(): String {
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
            showErrorMessage(e, "Hi, there is exception with RSA keys: ")
        }


        return keyPair
    }

    fun generateSymKey(): SecretKey? {
        val keygen = KeyGenerator.getInstance("AES")
        keygen.init(256)
        val key = keygen.generateKey()
        return key
    }


    fun encryptKey(symKey: ByteArray?, publicKey: PublicKey): ByteArray? {
        if (symKey == null) {
            showMessage("ByteArray for url encoding is NULL")
            return null
        }
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val ciphertext: ByteArray = cipher.doFinal(symKey)
        return ciphertext
    }

    fun encryptMessage(messageText: String, symKey: SecretKey): Pair<ByteArray, ByteArray> {
        println("my message is " + messageText)
        val plaintext: ByteArray = messageText.toByteArray()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, symKey)
        val iv = cipher.iv
        println("iv before is " + urlEncode(cipher.iv))
        val ciphertext: ByteArray = cipher.doFinal(plaintext)
        println("iv after is " + urlEncode(cipher.iv))
        return Pair(ciphertext, iv)
    }

    fun encryptMessage(messageText: String?): ByteArray? {
        if (messageText == null) {
            showMessage("ByteArray for url encoding is NULL")
            return null
        }
        val (encMsg, iv) = encryptMessage(messageText, symmetricalKey!!)
        localIv = iv
        return encMsg
    }

    fun decryptKey(encryptedSymmetricalKey: ByteArray?, privateKey: PrivateKey?): SecretKey {
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
            showMessage("ByteArray for url encoding is NULL")
            return ""
        } else {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, key, externalIvParameterSpec)
            val plaintext: ByteArray = cipher.doFinal(ciphertext)
            return plaintext.toString()
        }
    }

    fun encodeAndSend(messageText: String, queue: RequestQueue, pubKey: PublicKey, symKey: SecretKey, iv: ByteArray, ip: String) {
        val encryptedSymKey = urlEncode(
            encryptKey(
                symKey.encoded,
                pubKey
            )
        )
        val stringRequest = StringRequest(
            Request.Method.POST,
            "http://$ip:63342/?message=$messageText&key=$encryptedSymKey&symIv=${urlEncode(iv)}",
            Response.Listener { secondResponse ->
                showMessage("Message sended with code $secondResponse")
            },
            Response.ErrorListener { error ->
                showErrorMessage(error, "exit error")
            })
        queue.add(stringRequest)
    }

    fun encodeAndSend(messageText: String, queue: RequestQueue, pubKey: PublicKey, ip: String) {
        encodeAndSend(messageText, queue, pubKey, symmetricalKey!!, localIv!!, ip)
    }

    fun sendMessageInternal() {
        val queue = Volley.newRequestQueue(this@MainActivity)
        val ip = getIP()
        val messageText = urlEncode(encryptMessage(getMessageText()))
        println(messageText)
        val stringRequest = StringRequest(
            Request.Method.POST, "http://$ip:63342/?newMessage=true",
            Response.Listener { response ->
                showMessage("public key received")
                try {
                    val pubKey = KeyFactory.getInstance("RSA").generatePublic(
                        X509EncodedKeySpec(
                            urlDecode(response)
                        )
                    )
                    showMessage("pubKey: $pubKey")
                    encodeAndSend(messageText, queue, pubKey, ip)
                } catch (e: java.lang.Exception) {
                    showErrorMessage(e,"exception in encrypting data")
                }
            },
            Response.ErrorListener { error ->
                showErrorMessage(error, "exit error number 2")
            })
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
