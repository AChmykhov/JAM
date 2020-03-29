package com.example.jam

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val contactlist = arrayOf<String>("A", "AA", "AAAA", "AAAAAAAAAAAAAAAAAAAAA")

        val recyclerView : RecyclerView = findViewById(R.id.contactList)
        recyclerView.layoutManager = LinearLayoutManager(this)
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