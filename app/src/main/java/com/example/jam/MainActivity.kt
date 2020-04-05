package com.example.jam

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView : RecyclerView = findViewById(R.id.contactList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = Adapter(generateFakeValues())

    }

    private fun generateFakeValues(): List<String> {
        val values = mutableListOf<String>()
        for(i in 0..100) {
            values.add("$i element")
        }
        return values
    }

    class Adapter(private val values: List<String>): RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun getItemCount() = values.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.contact, parent, false)
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder?.textView?.text = values[position]
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var textView: TextView? = null
            init {
                textView = itemView?.findViewById(R.id.contactName)
            }
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
        cnd.show(manager, "myDialog")
        return super.onOptionsItemSelected(item)
    }
}