package com.example.trelloclone.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.SyncStateContract.Constants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trelloclone.R
import com.example.trelloclone.adapters.CardMemberListItemsAdapter
import com.example.trelloclone.dialogs.LabelColorListDialog
import com.example.trelloclone.dialogs.MembersListDialog
import com.example.trelloclone.firebase.FireStoreClass
import com.example.trelloclone.models.*
import com.example.trelloclone.utils.Constants.BOARD_DETAIL
import com.example.trelloclone.utils.Constants.BOARD_MEMBERS_LIST
import com.example.trelloclone.utils.Constants.CARD_LIST_ITEM_POSITION
import com.example.trelloclone.utils.Constants.SELECT
import com.example.trelloclone.utils.Constants.TASK_LIST_ITEM_POSITION
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CardDetailsActivity : BaseActivity() {
    
    private lateinit var mBoardDetails : Board
    private var mTaskListPosition = -1
    private var mCardPosition = -1
    private var mSelectedColor = ""
    private lateinit var mMembersDetailList : ArrayList<User>
    private var mSelectedDueDateMilliSeconds  : Long = 0

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)
        
        getIntentData()
        setUpActionBar()

        val et_name_card_details : EditText = findViewById(R.id.et_name_card_details)
        et_name_card_details.setText(
            mBoardDetails.taskList[mTaskListPosition]
                .cards[mCardPosition].name)

        et_name_card_details.setSelection(et_name_card_details.text.toString().length)

        mSelectedColor = mBoardDetails.taskList[mTaskListPosition]
            .cards[mCardPosition].labelColor

        if(mSelectedColor.isNotEmpty()){
            setColor()
        }

        val btn_update_card_details : Button = findViewById(R.id.btn_update_card_details)
        btn_update_card_details.setOnClickListener{
            if(et_name_card_details.text.toString().isNotEmpty()){
                updateCardDetails()
            }
            else{
                Toast.makeText(this@CardDetailsActivity,"Enter a card name",Toast.LENGTH_SHORT).show()
            }
        }

        val tv_select_label_color : TextView = findViewById(R.id.tv_select_label_color)
        tv_select_label_color.setOnClickListener{
            labelColorsListDialog()
        }

        val tv_select_members : TextView = findViewById(R.id.tv_select_members)
        tv_select_members.setOnClickListener{
            membersListDialog()
        }

        setUpSelectedMembersList()

        mSelectedDueDateMilliSeconds = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].dueDate

        if(mSelectedDueDateMilliSeconds>0){
            val simpleDateFormat = SimpleDateFormat("dd//MM/yyyy",Locale.ENGLISH)
            val selectedDate = simpleDateFormat.format(Date(mSelectedDueDateMilliSeconds))

            val tv_select_due_date = findViewById<TextView>(R.id.tv_select_due_date)
            tv_select_due_date.text = selectedDate
        }

        val tv_select_due_date : TextView = findViewById(R.id.tv_select_due_date)
        tv_select_due_date.setOnClickListener{
            showDatePicker()
        }
    }

    fun addUpdateTaskListSuccess(){
        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }


    private fun setUpActionBar(){
        val toolbar_card_details_activity : Toolbar = findViewById(R.id.toolbar_card_details_activity)
        setSupportActionBar(toolbar_card_details_activity)

        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name
            
        }
        toolbar_card_details_activity.setNavigationOnClickListener{onBackPressed()}
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card,menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun colorsList():ArrayList<String>{
        val colorsList: ArrayList<String> = ArrayList()
        colorsList.add("#43C86F")
        colorsList.add("#0C90F1")
        colorsList.add("#F72400")
        colorsList.add("#7A8089")
        colorsList.add("#D57C1D")
        colorsList.add("#770000")
        colorsList.add("#0022F8")

        return colorsList
    }

    private fun setColor(){
        val tv_select_label_color : TextView = findViewById(R.id.tv_select_label_color)
        tv_select_label_color.text = ""
        tv_select_label_color.setBackgroundColor(Color.parseColor(mSelectedColor))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_delete_card ->{
                alertDialogForDeleteCard(mBoardDetails
                    .taskList[mTaskListPosition].cards[mCardPosition].name)

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun getIntentData(){
        if(intent.hasExtra(BOARD_DETAIL)){
            mBoardDetails = intent.getParcelableExtra(BOARD_DETAIL)!!
        }
        if(intent.hasExtra(TASK_LIST_ITEM_POSITION)){
            mTaskListPosition = intent.getIntExtra(TASK_LIST_ITEM_POSITION,-1)
        }
        if(intent.hasExtra(CARD_LIST_ITEM_POSITION)){
            mCardPosition = intent.getIntExtra(CARD_LIST_ITEM_POSITION,-1)
        }
        if(intent.hasExtra(BOARD_MEMBERS_LIST)){
            mMembersDetailList = intent.getParcelableArrayListExtra(
                BOARD_MEMBERS_LIST
            )!!
        }
    }

    private fun membersListDialog(){
        var cardAssignedMembersList = mBoardDetails
            .taskList[mTaskListPosition]
            .cards[mCardPosition].assignedTo

        if(cardAssignedMembersList.size>0){
            for(i in mMembersDetailList.indices){
                for (j in cardAssignedMembersList){
                    if(mMembersDetailList[i].id == j){
                        mMembersDetailList[i].selected = true
                    }
                }
            }
        }
        else{
            for (i in cardAssignedMembersList.indices){
                    mMembersDetailList[i].selected = false
            }
        }

        val listDialog = object: MembersListDialog(
            this,
            mMembersDetailList,
            resources.getString(R.string.select_members),
        ){
            override fun onItemSelected(user: User, action: String) {
                if(action == SELECT){
                    if(!mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition]
                            .assignedTo.contains(user.id)){
                        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition]
                            .assignedTo.add(user.id)
                    }
                }
                else{
                    mBoardDetails
                        .taskList[mTaskListPosition]
                        .cards[mCardPosition]
                        .assignedTo.remove(user.id)

                    for(i in mMembersDetailList.indices){
                        if(mMembersDetailList[i].id == user.id){
                            mMembersDetailList[i].selected = false
                        }
                    }
                }

                setUpSelectedMembersList()

            }

        }

        listDialog.show()
    }

    private fun updateCardDetails(){
        val et_name_card_details : EditText = findViewById(R.id.et_name_card_details)
        val card = Card(
            et_name_card_details.text.toString(),
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].createdBy,
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo,
            mSelectedColor,
            mSelectedDueDateMilliSeconds
        )

        val taskList : ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size-1)


        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition] = card

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this@CardDetailsActivity,mBoardDetails)
    }

    private fun deleteCard(){
        val cardsList : ArrayList<Card> = mBoardDetails.taskList[mTaskListPosition].cards

        cardsList.removeAt(mCardPosition)
        val taskList : ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size-1)

        taskList[mTaskListPosition].cards = cardsList

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this@CardDetailsActivity,mBoardDetails)
    }

    private fun alertDialogForDeleteCard(cardName: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.alert))
        builder.setMessage(
            resources.getString(
                R.string.confirmation_message_to_delete_card,
                cardName
            )
        )

        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton(resources.getString(R.string.yes)){
            dialogInterface, _->
            dialogInterface.dismiss()
            deleteCard()
        }

        builder.setNegativeButton(resources.getString(R.string.no)){
            dialogInterface, _->
            dialogInterface.dismiss()
        }

        // create alert dialog
        val alertDialog : AlertDialog = builder.create()
        alertDialog.setCancelable(false)  // will not allow user to cancel after
        alertDialog.show()  // show the dialog to UI
    }

    private fun labelColorsListDialog(){
        val colorsList : ArrayList<String> = colorsList()
        val listDialog = object :  LabelColorListDialog(
            this,
            colorsList,
            resources.getString(R.string.str_select_label_color),
            mSelectedColor
        ){
            override fun onItemSelected(color: String) {
                mSelectedColor = color
                setColor()
            }
        }

        listDialog.show()
    }

    private fun setUpSelectedMembersList(){
        val cardAssignedMemberList =
            mBoardDetails
                .taskList[mTaskListPosition]
                .cards[mCardPosition].assignedTo

        val selectedMembersList : ArrayList<SelectedMembers> = ArrayList()

        for(i in mMembersDetailList.indices){
            for(j in cardAssignedMemberList){
                if(mMembersDetailList[i].id == j){
                    val selectedMembers = SelectedMembers(
                        mMembersDetailList[i].id,
                        mMembersDetailList[i].image
                    )
                    selectedMembersList.add(selectedMembers)
                }
            }
        }

        val tv_select_members : TextView = findViewById(R.id.tv_select_members)
        val rv_selected_members_list : RecyclerView = findViewById(R.id.rv_selected_members_list)

        if(selectedMembersList.size>0){
            selectedMembersList.add(SelectedMembers("",""))

            tv_select_members.visibility = View.GONE

            rv_selected_members_list.visibility = View.VISIBLE

            rv_selected_members_list.layoutManager = GridLayoutManager(
                this,
                6
            )

            val adapter = CardMemberListItemsAdapter(
                this, selectedMembersList, true)

            rv_selected_members_list.adapter = adapter
            adapter.setOnClickListener(
                object : CardMemberListItemsAdapter.OnClickListener{
                    override fun onClick() {
                        membersListDialog()
                    }
                }
            )
        }
        else{
            tv_select_members.visibility = View.VISIBLE
            rv_selected_members_list.visibility = View.GONE
        }
    }

    private fun showDatePicker(){
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener{ view,year,monthOfYear,dayOfMonth ->
                val sDayOfMonth = if(dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                val sMonthOfYear =
                    if((monthOfYear+1)<10) "0${monthOfYear+1}" else "${monthOfYear+1}"

                val selectedDate = "$sDayOfMonth/$sMonthOfYear/$year"

                val tv_select_due_date = findViewById<TextView>(R.id.tv_select_due_date)
                tv_select_due_date.text = selectedDate

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val theDate = sdf.parse(selectedDate)
                mSelectedDueDateMilliSeconds = theDate!!.time
            },
            year,month,day
        )
        dpd.show()
    }
}