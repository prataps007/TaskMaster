package com.example.trelloclone.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trelloclone.R
import com.example.trelloclone.activities.TaskListActivity
import com.example.trelloclone.models.Card
import com.example.trelloclone.models.SelectedMembers

open class CardListItemsAdapter (
    private val context: Context,
    private var list: ArrayList<Card>

    ):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_card,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val model = list[position]

        if(holder is MyViewHolder){

            if(model.labelColor.isNotEmpty()){
                holder.view_label_color.visibility = View.VISIBLE
                holder.view_label_color.setBackgroundColor(Color.parseColor(model.labelColor))
            }
            else{
                holder.view_label_color.visibility = View.GONE
            }

            holder.tv_card_name.text = model.name

            if((context as TaskListActivity)
                    .mAssignedMemberDetailList.size > 0){
                val selectedMembersList : ArrayList<SelectedMembers> = ArrayList()

                for(i in context.mAssignedMemberDetailList.indices){
                    for(j in model.assignedTo){
                        if(context.mAssignedMemberDetailList[i].id == j){
                            val selectedMembers = SelectedMembers(
                                context.mAssignedMemberDetailList[i].id,
                                context.mAssignedMemberDetailList[i].image
                            )

                            selectedMembersList.add(selectedMembers)
                        }
                    }
                }

                if(selectedMembersList.size>0){
                    if(selectedMembersList.size == 1 && selectedMembersList[0].id == model.createdBy){
                        holder.rv_card_selected_members_list.visibility = View.GONE
                    }
                    else{
                        holder.rv_card_selected_members_list.visibility = View.VISIBLE

                        holder.rv_card_selected_members_list.layoutManager =
                            GridLayoutManager(context,4)

                        val adapter = CardMemberListItemsAdapter(context, selectedMembersList,false)

                        holder.rv_card_selected_members_list.adapter = adapter

                        adapter.setOnClickListener(
                            object : CardMemberListItemsAdapter.OnClickListener{
                                override fun onClick() {
                                    if(onClickListener != null){
                                        onClickListener!!.onClick(position)
                                    }
                                }
                            }
                        )
                    }
                }
                else{
                    holder.rv_card_selected_members_list.visibility = View.GONE
                }
            }

            holder.itemView.setOnClickListener{
                if(onClickListener!=null){
                    onClickListener!!.onClick(position)
                }
            }
        }
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    interface OnClickListener{
        fun onClick(position:Int)
    }

    class MyViewHolder(view: View):RecyclerView.ViewHolder(view) {

        val rv_card_selected_members_list: RecyclerView = view.findViewById(R.id.rv_card_selected_members_list)
        val view_label_color: View = view.findViewById(R.id.view_label_color)
        val tv_card_name: TextView = view.findViewById(R.id.tv_card_name)
    }
}