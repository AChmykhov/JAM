package com.example.jam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.ListFragment
import kotlinx.android.synthetic.main.home_fragment.*


class HomeFragment : ListFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.home_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val names = resources.getStringArray(R.array.names)
//        contact_list.adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_1, names)
//        contact_list.setOnItemClickListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val options = navOptions {
//            anim {
//                enter = R.anim.slide_in_right
//                exit = R.anim.slide_out_left
//                popEnter = R.anim.slide_in_left
//                popExit = R.anim.slide_out_right }
//        }
//        view.findViewById<Button>(R.id.navigate_destination_button)?.setOnClickListener {
//            findNavController().navigate(R.id.dialogFragment, null, options)
//        }

//        view.findViewById<Button>(R.id.sendButton)?.setOnClickListener(
//            Navigation.createNavigateOnClickListener(R.id.action_to_dialog, null)
//        )

    }
}