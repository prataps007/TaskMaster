package com.example.trelloclone.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent

import android.graphics.drawable.Drawable
import android.net.Uri

import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.trelloclone.R
import com.example.trelloclone.activities.CreateBoardActivity.Companion.PICK_IMAGE_REQUEST
import com.example.trelloclone.adapters.TaskListItemsAdapter
import com.example.trelloclone.firebase.FireStoreClass
import com.example.trelloclone.models.Board
import com.example.trelloclone.models.Card
import com.example.trelloclone.models.Task
import com.example.trelloclone.models.User
import com.example.trelloclone.utils.Constants
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.FirebaseFirestore


class TaskListActivity : BaseActivity() {

    private lateinit var mBoardDetails : Board
    private lateinit var mBoardDocumentId: String
    lateinit var mAssignedMemberDetailList: ArrayList<User>

    private var selectedBackgroundImageUri: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        if(intent.hasExtra(Constants.DOCUMENT_ID)){
            mBoardDocumentId = intent.getStringExtra(Constants.DOCUMENT_ID).toString()
        }


        showProgressDialog(resources.getString(R.string.please_wait))
        // Set initial background if it exists


        FireStoreClass().getBoardDetails(this,mBoardDocumentId)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                data?.data?.let { imageUri ->
                    // Set the selected image URI as the background
                    setBackgroundImage(imageUri.toString())

                    // Upload the image to your storage (e.g., Firebase Storage)
                    uploadBackgroundImage(imageUri)
                }
            }
            else if (requestCode == UNSPLASH_IMAGE_PICKER_REQUEST_CODE) {
                // Handle Unsplash image selection
                val unsplashImageUrl = data?.getStringExtra("BACKGROUND_IMAGE")
                unsplashImageUrl?.let {
                    // Set the Unsplash image URL as the background
                    setBackgroundImage(it)
                    // Optionally upload or store this URL in Firestore or other storage
                    updateBoardBackgroundImage(mBoardDocumentId, it)
                }
            }
            else if (requestCode == MEMBER_REQUEST_CODE || requestCode == CARD_DETAILS_REQUEST_CODE) {
                showProgressDialog(resources.getString(R.string.please_wait))
                FireStoreClass().getBoardDetails(this, mBoardDocumentId)
            }
        } else {
            Log.e("Cancelled", "Cancelled")
        }
    }

    private fun uploadBackgroundImage(imageUri: Uri) {
        // Your code to upload the image to Firebase Storage (or other storage solutions)
        // Get the download URL after successful upload, and update mBoardDetails.backgroundImage with it
        // Finally, save the updated task details to Firestore
        updateBoardBackgroundImage(mBoardDocumentId, imageUri.toString())
    }

    // Ensure FireStoreClass has the necessary method to update background image
    fun updateBoardBackgroundImage(documentId: String, imageUrl: String) {
        val updates = HashMap<String, Any>()

        updates[Constants.BACKGROUND_IMAGE] = imageUrl

        FirebaseFirestore.getInstance().collection(Constants.BOARDS)
            .document(documentId)
            .update(updates)
            .addOnSuccessListener {
                Log.i("Update", "Background image updated successfully")
                Toast.makeText(this,"Background image updated successfully!",Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Log.w("Update", "Error updating background image", e)
            }
    }


    private fun setBackgroundImage(imageUrl: String) {
        val taskListLayout: LinearLayout = findViewById(R.id.task_list_board_bg)
        taskListLayout.setBackgroundResource(0) // Clear existing background


        Glide.with(this)
            .load(imageUrl)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    taskListLayout.background = resource // Set the background image
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Handle placeholder if needed
                }
            })

        // Save the image URI to the task details
       // mBoardDetails.background = imageUri.toString()
    }



    fun cardDetails(taskListPosition: Int,cardPosition:Int){
        val intent = Intent(this,CardDetailsActivity::class.java)
        intent.putExtra(Constants.BOARD_DETAIL,mBoardDetails)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION,taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION,cardPosition)
        intent.putExtra(Constants.BOARD_MEMBERS_LIST, mAssignedMemberDetailList)
        startActivityForResult(intent, CARD_DETAILS_REQUEST_CODE)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_members,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_members ->{
                val intent = Intent(this,MembersActivity::class.java)
                intent.putExtra(Constants.BOARD_DETAIL,mBoardDetails)
                startActivityForResult(intent, MEMBER_REQUEST_CODE)
                return true
            }
            R.id.action_bg ->{
//                openImagePicker()
                showBackgroundOptionDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showBackgroundOptionDialog() {
        val options = arrayOf("Pick from Local Device", "Pick from unsplash")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose Background Image From")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    // Option 1: Pick Image from Local Device
                    openImagePicker()  // Assuming this method already opens the image picker for local device
                }
                1 -> {
                    // Option 2: Pick Image from Another Source (add your custom logic here)
                    val intent = Intent(this, UnsplashPhotoPickerActivity::class.java)

                    startActivityForResult(intent, UNSPLASH_IMAGE_PICKER_REQUEST_CODE)
                }
            }
        }
        builder.show()
    }




    //******** for board background
    private fun openImagePicker() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT)
//        intent.type = "image/*"
//        startActivityForResult(intent, PICK_IMAGE_REQUEST)

        var galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
    }


    private fun setUpActionBar(){
        val toolbar_task_list_activity : Toolbar = findViewById(R.id.toolbar_task_list_activity)
        setSupportActionBar(toolbar_task_list_activity)

        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)

            actionBar.title = mBoardDetails.name
        }

       // val toolbar_my_profile_activity : Toolbar = findViewById(R.id.toolbar_my_profile_activity)
        toolbar_task_list_activity.setNavigationOnClickListener{onBackPressed()}
    }

    fun boardDetails(board: Board){

        mBoardDetails = board

        //if(mBoardDetails.image != ""){
            setBackgroundImage(mBoardDetails.background)
        //}

        hideProgressDialog()
        setUpActionBar()

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().getAssignedMembersListDetails(this, mBoardDetails.assignedTo)

    }

    fun addUpdateTaskListSuccess(){

        hideProgressDialog()

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().getBoardDetails(this,mBoardDetails.documentId)

    }

    fun createTaskList(taskListName:String){
        val task = Task(taskListName,FireStoreClass().getCurrentUserId())
        mBoardDetails.taskList.add(0,task)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)

        showProgressDialog(resources.getString(R.string.please_wait))

        FireStoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    fun updateTaskList(position:Int, listName:String, model:Task){
        val task = Task(listName,model.createdBy)

        mBoardDetails.taskList[position] = task
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)

        showProgressDialog(resources.getString(R.string.please_wait))

        FireStoreClass().addUpdateTaskList(this,mBoardDetails)
        Toast.makeText(this,"list updated successfully", Toast.LENGTH_SHORT).show()

    }

    fun deleteTaskList(position: Int){
        mBoardDetails.taskList.removeAt(position)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this,mBoardDetails)
        Toast.makeText(this,"list deleted successfully", Toast.LENGTH_SHORT).show()
    }

    fun addCardToTaskList(position:Int, cardName:String){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)

        val cardAssignedUsersList:ArrayList<String> = ArrayList()
        cardAssignedUsersList.add(FireStoreClass().getCurrentUserId())

        val card = Card(cardName, FireStoreClass().getCurrentUserId(), cardAssignedUsersList)

        val cardsList = mBoardDetails.taskList[position].cards
        cardsList.add(card)

        val task = Task(
            mBoardDetails.taskList[position].title,
            mBoardDetails.taskList[position].createdBy,
            cardsList
        )

        mBoardDetails.taskList[position] = task

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    fun boardMembersDetailsList(list : ArrayList<User>){
        mAssignedMemberDetailList = list
        hideProgressDialog()

        val addTaskList = Task(resources.getString(R.string.add_list))
        mBoardDetails.taskList.add(addTaskList)

        val rv_task_list : RecyclerView = findViewById(R.id.rv_task_list)

        rv_task_list.layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        rv_task_list.setHasFixedSize(true)

        val adapter = TaskListItemsAdapter(this,mBoardDetails.taskList)
        rv_task_list.adapter = adapter

    }

    fun updateCardsInTaskList(taskListPosition: Int, cards : ArrayList<Card>){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)

        mBoardDetails.taskList[taskListPosition].cards = cards

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    companion object{
        const val MEMBER_REQUEST_CODE: Int = 13
        const val CARD_DETAILS_REQUEST_CODE : Int = 14

        const val UNSPLASH_IMAGE_PICKER_REQUEST_CODE: Int = 123 // Or any unique value
    }


}