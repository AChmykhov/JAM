package com.example.jam

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.Navigation
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.create_new_dialog_fragment.*





class CreateNewDialogFragment : DialogFragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.create_new_dialog_fragment, container, false)
    }


    fun getName() : String{
 //       val name = name_Input.text.toString()
        val name = view?.findViewById<TextInputEditText>(R.id.name_Input)
        return name?.text.toString()
    }

    fun getIP() : String{
        val IP = view?.findViewById<TextInputEditText>(R.id.ip_Input)
        return IP?.text.toString()
    }

    fun go(view: View){
        if (getIP() == ""){
            val toast = Toast.makeText(
                activity,
                "No IP entered", Toast.LENGTH_SHORT
            )
            toast.show()
            return
        }
        if (getName() == ""){
            val toast = Toast.makeText(
                activity,
                "No name entered", Toast.LENGTH_SHORT
            )
            toast.show()
            return
        }
        dialog?.cancel()
    }


}