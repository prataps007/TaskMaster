package com.example.trelloclone.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trelloclone.R
import com.example.trelloclone.adapters.LabelColorListItemsAdapter
import org.w3c.dom.Text

abstract class LabelColorListDialog (
    context: Context,
    private var list: ArrayList<String>,
    private val title : String = "",
    private var mSelectedColor: String = ""
        ): Dialog(context){

            private var adapter: LabelColorListItemsAdapter? = null

    override fun onCreate(savedInstanceState : Bundle?){
        super.onCreate(savedInstanceState)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_list,null)

        setContentView(view)
        setCanceledOnTouchOutside(true)
        setCancelable(true)
        setUpRecyclerView(view)
    }

    private fun setUpRecyclerView(view: View){
        val tvTitle : TextView = view.findViewById(R.id.tvTitle)
        tvTitle.text = title

        val rvList : RecyclerView = view.findViewById(R.id.rvList)
        rvList.layoutManager = LinearLayoutManager(context)
        adapter = LabelColorListItemsAdapter(context,list,mSelectedColor)
        rvList.adapter = adapter

        adapter!!.onItemClickListener =
            object : LabelColorListItemsAdapter.OnItemClickListener{
                override fun onClick(position: Int, color: String) {
                    dismiss()
                    onItemSelected(color)
                }
            }
    }

    protected abstract fun onItemSelected(color:String)

        }