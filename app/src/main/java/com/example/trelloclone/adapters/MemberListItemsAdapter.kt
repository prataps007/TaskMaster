package com.example.trelloclone.adapters

import android.content.Context
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trelloclone.R
import com.example.trelloclone.models.User
import com.example.trelloclone.utils.Constants
import de.hdodenhof.circleimageview.CircleImageView
import org.w3c.dom.Text

open class MemberListItemsAdapter (
    private val context : Context,
    private var list: ArrayList<User>
        ):RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var onClickListener : OnClickListener ? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_members,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if(holder is MyViewHolder){
            Glide
                .with(context)
                .load(model.image)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(holder.iv_member_image)

            holder.tv_member_name.text=model.name
            holder.tv_member_email.text = model.email

            if(model.selected){
                holder.iv_selected_member.visibility = View.VISIBLE
            }
            else{
                holder.iv_selected_member.visibility = View.GONE
            }

            holder.itemView.setOnClickListener{
                if(onClickListener != null){
                    if(model.selected){
                        onClickListener!!.onClick(position,model,Constants.UN_SELECT)
                    }
                    else{
                        onClickListener!!.onClick(position,model,Constants.SELECT)
                    }
                }
            }
        }
    }

    fun setOnItemClickListener(onClickListener : OnClickListener) {
        this.onClickListener = onClickListener
    }

    class MyViewHolder(view: View):RecyclerView.ViewHolder(view) {
        val iv_selected_member: ImageView = view.findViewById(R.id.iv_selected_member)
        var tv_member_email: TextView = view.findViewById(R.id.tv_member_email)
        val tv_member_name: TextView = view.findViewById(R.id.tv_member_name)
        val iv_member_image: CircleImageView = view.findViewById(R.id.iv_member_image)
    }

    interface OnClickListener {
        fun onClick(position: Int, user: User, action: String)
    }
}