package com.example.trelloclone.adapters

import android.annotation.SuppressLint
import android.content.ClipData.Item
import android.content.Context
import android.content.res.Resources
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trelloclone.R
import com.example.trelloclone.activities.TaskListActivity
import com.example.trelloclone.models.Card
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.tasks.Task
import org.w3c.dom.Text
import java.util.Collections

open class TaskListItemsAdapter(
    private val context: Context,
    private var list:ArrayList<com.example.trelloclone.models.Task>):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mPositionDraggedFrom = -1
    private var mPositionDraggedTo = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_task,parent,false)

        val layoutParams = LinearLayout.LayoutParams(
            (parent.width * 0.7).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT
        )

        layoutParams.setMargins((15.toDP().toPx()),0,(40.toDP()).toPx(),0)

        view.layoutParams = layoutParams

        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val model = list[position]

        if(holder is MyViewHolder){
            if(position == list.size-1){
                holder.tv_add_task_list.visibility = View.VISIBLE
                holder.ll_task_item.visibility = View.GONE
            }
            else{
                holder.tv_add_task_list.visibility = View.GONE
                holder.ll_task_item.visibility = View.VISIBLE
            }

            holder.tv_task_list_title.text = model.title

            holder.tv_add_task_list.setOnClickListener{
                holder.tv_add_task_list.visibility=View.GONE
                holder.cv_add_task_list_name.visibility = View.VISIBLE
            }

            holder.ib_close_list_name.setOnClickListener{
                holder.tv_add_task_list.visibility=View.VISIBLE
                holder.cv_add_task_list_name.visibility = View.GONE
            }

            holder.ib_done_list_name.setOnClickListener{
               val listName = holder.et_task_list_name.text.toString()

                if(listName.isNotEmpty()){
                    if(context is TaskListActivity){
                        context.createTaskList(listName)
                    }
                }
                else{
                    Toast.makeText(context,"Please enter list name",Toast.LENGTH_SHORT).show()
                }
            }

            holder.ib_edit_list_name.setOnClickListener{
                holder.et_edit_task_list_name.setText(model.title)
                holder.ll_title_view.visibility = View.GONE
                holder.cv_edit_task_list_name.visibility = View.VISIBLE
            }

            holder.ib_close_editable_view.setOnClickListener{
                holder.ll_title_view.visibility = View.VISIBLE
                holder.cv_edit_task_list_name.visibility = View.GONE
            }

            holder.ib_done_edit_list_name.setOnClickListener{
                val listName = holder.et_edit_task_list_name.text.toString()
                if(listName.isNotEmpty()){
                    if(context is TaskListActivity){
                        context.updateTaskList(position,listName,model)
                    }
                }
                else{
                    Toast.makeText(context,"Please enter list name",Toast.LENGTH_SHORT).show()
                }
            }

            holder.ib_delete_list.setOnClickListener{
                alertDialogForDeleteList(position,model.title)
            }

            holder.tv_add_card.setOnClickListener{
                holder.tv_add_card.visibility = View.GONE
                holder.cv_add_card.visibility = View.VISIBLE
            }

            holder.ib_close_card_name.setOnClickListener{
                holder.tv_add_card.visibility = View.VISIBLE
                holder.cv_add_card.visibility = View.GONE
            }

            holder.ib_done_card_name.setOnClickListener{
                val cardName = holder.et_card_name.text.toString()
                if(cardName.isNotEmpty()){
                    if(context is TaskListActivity){
                        // add a card
                        context.addCardToTaskList(position,cardName)
                    }
                }
                else{
                    Toast.makeText(context,"Please enter card name",Toast.LENGTH_SHORT).show()
                }
            }

            holder.rv_card_list.layoutManager = LinearLayoutManager(context)
            holder.rv_card_list.setHasFixedSize(true)

            val adapter = CardListItemsAdapter(context,model.cards)
            holder.rv_card_list.adapter = adapter

            adapter.setOnClickListener(
                object : CardListItemsAdapter.OnClickListener{

                    override fun onClick(cardPosition: Int) {
                        if(context is TaskListActivity){
                           // val cardPosition = position
                            context.cardDetails(position, cardPosition)
                        }
                    }

                }
            )

            val dividerItemDecoration = DividerItemDecoration(context,
                  DividerItemDecoration.VERTICAL)
            holder.rv_card_list.addItemDecoration(dividerItemDecoration)

            val helper = ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,0
                ){
                    override fun onMove(
                        recyclerView: RecyclerView,
                        dragged: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean {
                        val draggedPosition = dragged.adapterPosition
                        val targetPosition = target.adapterPosition

                        if(mPositionDraggedFrom == -1){
                            mPositionDraggedFrom = draggedPosition
                        }
                        mPositionDraggedTo = targetPosition
                        Collections.swap(list[position].cards,
                             draggedPosition, targetPosition)

                        adapter.notifyItemMoved(draggedPosition,targetPosition)

                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    }

                    override fun clearView(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder
                    ) {
                        super.clearView(recyclerView, viewHolder)

                        if(mPositionDraggedFrom != -1 && mPositionDraggedTo != -1
                            && mPositionDraggedFrom != mPositionDraggedTo){
                            (context as TaskListActivity).updateCardsInTaskList(
                                position,
                                list[position].cards
                            )
                        }

                        mPositionDraggedFrom = -1
                        mPositionDraggedTo = -1
                    }

                }
            )

            helper.attachToRecyclerView(holder.rv_card_list)
        }
    }

    private fun alertDialogForDeleteList(position:Int,title:String){
        val builder = AlertDialog.Builder(context)
        // set title for alert dialog
        builder.setTitle("Alert")
        // set message for alert dialog
        builder.setMessage("Are you sure you want to delete $title")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        // performing positive action
        builder.setPositiveButton("Yes"){dialogInterface, which->
            dialogInterface.dismiss()    // dialog will be dismissed

            if(context is TaskListActivity){
                context.deleteTaskList(position)
            }
        }

        // performing negative action
        builder.setNegativeButton("No"){
            dialogInterface, which ->
            dialogInterface.dismiss()  // dialog will be dismissed
        }

        // create the alertDialog
        val alertDialog: AlertDialog = builder.create()
        // set other dialog properties
        alertDialog.setCancelable(false)  // will not allow user to cancel after creation
        alertDialog.show()  // show the dialog to UI
    }

    private fun Int.toDP() : Int =
        (this / Resources.getSystem().displayMetrics.density).toInt()

    private fun Int.toPx():Int=
        (this * Resources.getSystem().displayMetrics.density).toInt()

    class MyViewHolder(view: View):RecyclerView.ViewHolder(view) {
        val rv_card_list: RecyclerView = view.findViewById(R.id.rv_card_list)
        val et_card_name: EditText = view.findViewById(R.id.et_card_name)
        val ib_done_card_name: ImageButton = view.findViewById(R.id.ib_done_card_name)
        val ib_close_card_name: ImageButton = view.findViewById(R.id.ib_close_card_name)
        val cv_add_card: CardView = view.findViewById(R.id.cv_add_card)
        val tv_add_card: TextView = view.findViewById(R.id.tv_add_card)
        val ib_delete_list: ImageView = view.findViewById(R.id.ib_delete_list)
        val ib_done_edit_list_name: ImageButton = view.findViewById(R.id.ib_done_edit_list_name)
        val ib_close_editable_view: ImageButton = view.findViewById(R.id.ib_close_editable_view)
        val cv_edit_task_list_name: CardView = view.findViewById(R.id.cv_edit_task_list_name)
        val ll_title_view: LinearLayout = view.findViewById(R.id.ll_title_view)
        val et_edit_task_list_name: EditText = view.findViewById(R.id.et_edit_task_list_name)
        val ib_edit_list_name: ImageButton = view.findViewById(R.id.ib_edit_list_name)
        val et_task_list_name: EditText = view.findViewById(R.id.et_task_list_name)
        val ib_done_list_name: ImageButton = view.findViewById(R.id.ib_done_list_name)
        val ib_close_list_name: ImageButton = view.findViewById(R.id.ib_close_list_name)
        var cv_add_task_list_name: CardView = view.findViewById(R.id.cv_add_task_list_name)
        val tv_task_list_title: TextView = view.findViewById(R.id.tv_task_list_title)
        val ll_task_item: LinearLayout = view.findViewById(R.id.ll_task_item)
        val tv_add_task_list: TextView = view.findViewById(R.id.tv_add_task_list)
    }

}