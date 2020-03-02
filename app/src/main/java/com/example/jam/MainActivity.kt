package com.example.jam

//import android.util.Base64.DEFAULT
import android.content.Context
import android.content.res.Resources
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Base64
import android.util.Base64.URL_SAFE
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
import android.view.Menu
import android.view.MenuItem
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.material.navigation.NavigationView
import android.widget.ArrayAdapter
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import kotlinx.android.synthetic.main.activity_main.*
import androidx.navigation.fragment.DialogFragmentNavigator

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val contactlist = arrayOf<String>("A", "AA", "AAAA", "AAAAAAAAAAAAAAAAAAAAA")

//        val contactlist = resources.getStringArray(R.array.names)
          contact_list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, contactlist)
          contact_list.setOnItemClickListener { parent, view, position, id ->
              Toast.makeText(this, contactlist[position], Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val retValue = super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.upper_menu, menu)
        return retValue
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val cnd = CreateNewDialogFragment()
        val manager = supportFragmentManager
        val name = ""
        val IP = ""
        cnd.show(manager, "myDialog")

        return super.onOptionsItemSelected(item)
    }

}
