package com.example.trelloclone.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.init
import com.example.trelloclone.R
import com.example.trelloclone.activities.MainActivity
import com.example.trelloclone.models.Board
//import kotlinx.android.synthetic.main.item_board.view.*
import de.hdodenhof.circleimageview.CircleImageView

open class BoardItemsAdapter(private val context: Context, private var list:ArrayList<Board>):
        RecyclerView.Adapter<RecyclerView.ViewHolder>(){


    private  var onClickListener: OnClickListener? =null
    private var onLongClickListener: OnLongClickListener? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return MyViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.item_board,
                    parent,false))
    }

    override fun getItemCount(): Int {

        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val model=list[position]

        if(holder is MyViewHolder){
            Glide
                .with(context)
                .load(model.image)
                .centerCrop()
                .placeholder(R.drawable.ic_board_place_holder)
                .into(holder.iv_board_image)

            holder.tv_name.text = model.name
            holder.tv_created_by.text = "Created by: ${model.createdBy}"

            holder.itemView.setOnClickListener{
                if(onClickListener!=null){
                    onClickListener!!.onClick(position,model)
                }
            }

            //****** for long press delete -> board

            holder.itemView.setOnLongClickListener {
                onLongClickListener?.onLongClick(position, list[position])
                true
            }

        }

    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    interface OnClickListener{
        fun onClick(position: Int,model: Board)
    }


    //******* for long press delete -> board
    interface OnLongClickListener {
        fun onLongClick(position: Int, model: Board)
    }

    fun setOnLongClickListener(listener: OnLongClickListener) {
        this.onLongClickListener = listener
    }

    //******

    private class MyViewHolder(view: View):RecyclerView.ViewHolder(view) {
        val tv_created_by: TextView = view.findViewById(R.id.tv_created_by)
        val tv_name: TextView  = view.findViewById(R.id.tv_name)
        val iv_board_image: CircleImageView = view.findViewById(R.id.iv_board_image)
    }

    //****** for search view

    // method for filtering our recyclerview items.
        fun filterList(activity: MainActivity, filterlist: ArrayList<Board>) {
            // below line is to add our filtered
            // list in our course array list.
            list = filterlist

            // below line is to notify our adapter
            // as change in recycler view data.
            notifyDataSetChanged()
        }

    //******

}