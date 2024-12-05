package com.example.trelloclone.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trelloclone.R
import com.example.trelloclone.adapters.BoardItemsAdapter
import com.example.trelloclone.firebase.FireStoreClass
import com.example.trelloclone.models.Board
import com.example.trelloclone.models.User
import com.example.trelloclone.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {


    //***** for search view in app_bar main
                private lateinit var boardsRecyclerView : RecyclerView
                private lateinit var mAdapter: BoardItemsAdapter
                private lateinit var mBoardsList: ArrayList<Board>

                private lateinit var mBoardDetailsAfterUserLeft : Board
    //******


    companion object{
        const val My_PROFILE_REQUEST_CODE:Int = 11
        const val CREATE_BOARD_REQUEST_CODE: Int = 12
    }

    private lateinit var mUserName : String
    private lateinit var mSharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setUpActionBar()

        val nav_view : NavigationView = findViewById(R.id.nav_view)
        nav_view.setNavigationItemSelectedListener(this)

        mSharedPreferences = this.getSharedPreferences(Constants.PROJECT_PREFERENCES, Context.MODE_PRIVATE)

        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED,false)

        if(tokenUpdated){
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStoreClass().loadUserData(this,true)
        }
        else{
//            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
//                updateFCMToken(token)

            FirebaseMessaging.getInstance()
                .token.addOnSuccessListener(this@MainActivity){
                    token ->
                    updateFCMToken(token)
                }
        }

        FireStoreClass().loadUserData(this@MainActivity,true)


        val fab_create_board : FloatingActionButton = findViewById(R.id.fab_create_board)
        fab_create_board.setOnClickListener{
            val intent = Intent(this,CreateBoardActivity::class.java)

            intent.putExtra(Constants.NAME,mUserName)
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }

    }


    fun populateBoardsListToUI(boardsList:ArrayList<Board>){

        hideProgressDialog()

        //***** for search view
            mBoardsList = ArrayList(boardsList)
        //*****

        val rv_boards_list : RecyclerView = findViewById(R.id.rv_boards_list)
        val tv_no_boards_available : TextView = findViewById(R.id.tv_no_boards_available)

        if(boardsList.size>0){

            rv_boards_list.visibility = View.VISIBLE
            tv_no_boards_available.visibility = View.GONE

            rv_boards_list.layoutManager = LinearLayoutManager(this)
            rv_boards_list.setHasFixedSize(true)

            val adapter = BoardItemsAdapter(this,boardsList)

            //******* for search view
                   mAdapter = adapter
            //******

            rv_boards_list.adapter = adapter

            adapter.setOnClickListener(object:BoardItemsAdapter.OnClickListener{
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity,TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID,model.documentId)
                    startActivity(intent)
                }
            })

            // ****** for long press delete or leave -> Board
                adapter.setOnLongClickListener(object : BoardItemsAdapter.OnLongClickListener {
                    override fun onLongClick(position: Int, model: Board) {
                        // Implement logic to delete the board
                        val currentBoard = boardsList[position]

                        // if current profile user is the one who created
                        // this board , then only he can delete it
                        if(currentBoard.createdBy.equals(mUserName)) {
                            showDeleteConfirmationDialog(model)
                        }
                        else{
                            // leave the membership of board created by another user
                            showLeaveBoardConfirmationDialog(model)
                        }
                    }
                })

            rv_boards_list.adapter = adapter

            //*******

        }
        else{
            rv_boards_list.visibility = View.GONE
            tv_no_boards_available.visibility = View.VISIBLE
        }
    }

    //******* for long press leave -> board
    private fun showLeaveBoardConfirmationDialog(board: Board) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Leave Board")
        builder.setMessage("Are you sure you want to leave this board?")

        builder.setPositiveButton("Leave") { dialog, which ->
            // Call a function to leave the board from Firestore
            leaveBoard(board)
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun leaveBoard(board: Board) {
        showProgressDialog("Leaving board...")

        FireStoreClass().leaveBoard(this, board)
    }



    fun leaveMembershipSuccess(board: Board,user: User, membersList: ArrayList<User>) {
        hideProgressDialog()

        mBoardDetailsAfterUserLeft = board

        //Toast.makeText(this, "Board left successfully", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)

        // Refresh the boards list after deletion
        FireStoreClass().getBoardsList(this)

        // send notification to all other members of that board that current user left
        // After removing user from board, get that board members and send notifications

        for (member in membersList) {
            SendNotificationToUserAsyncTask(user.name, member.fcmToken, "Board Member Left", "${user.name} has left the board ${board.name}.").execute()
        }

        //finish()
    }

    private inner class SendNotificationToUserAsyncTask(
        private val userName: String,
        private val token: String,
        private val title: String,
        private val message: String
    ) : AsyncTask<Any, Void, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog(resources.getString(R.string.please_wait))
        }

        override fun doInBackground(vararg params: Any?): String {
            var result: String
            var connection: HttpURLConnection? = null
            try {
                val url = URL(Constants.FCM_BASE_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.doOutput = true
                connection.doInput = true
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"

                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty(
                    Constants.FCM_AUTHORIZATION, "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}"
                )

                connection.useCaches = false

                val wr = DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                val dataObject = JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE, title)
                dataObject.put(Constants.FCM_KEY_MESSAGE, message)

                jsonRequest.put(Constants.FCM_KEY_DATA, dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO, token)

                wr.writeBytes(jsonRequest.toString())
                wr.flush()
                wr.close()

                val httpResult: Int = connection.responseCode
                if (httpResult == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream

                    val reader = BufferedReader(
                        InputStreamReader(inputStream)
                    )

                    val sb = StringBuilder()
                    var line: String
                    try {
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line + "\n")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        try {
                            inputStream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }

                    result = sb.toString()

                } else {
                    result = connection.responseMessage
                }
            } catch (e: SocketTimeoutException) {
                result = "Connection Timeout"
            } catch (e: Exception) {
                result = "Error: " + e.message
            } finally {
                connection?.disconnect()
            }

            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            hideProgressDialog()
            Log.e("JSON Response Result", result!!)
        }
    }




    //******* for long press delete -> board
    private fun showDeleteConfirmationDialog(board: Board) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Board")
        builder.setMessage("Are you sure you want to delete this board?")

        builder.setPositiveButton("Delete") { dialog, which ->
            // Call a function to delete the board from Firestore
            deleteBoard(board)
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun deleteBoard(board: Board) {
        showProgressDialog("Deleting board...")

        FireStoreClass().deleteBoard(this, board)
    }

    fun boardDeletedSuccessfully() {
        hideProgressDialog()

        //Toast.makeText(this, "Board deleted successfully", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)

        // Refresh the boards list after deletion
        FireStoreClass().getBoardsList(this)

        //finish()
    }



    // ********

    private fun setUpActionBar(){
        val toolbar_main_activity : Toolbar = findViewById(R.id.toolbar_main_activity)
        setSupportActionBar(toolbar_main_activity)

        toolbar_main_activity.setNavigationIcon(R.drawable.ic_action_navigation_menu)
        toolbar_main_activity.setNavigationOnClickListener{
            // Toggle drawer

            toggleDrawer()

        }
    }

    private fun toggleDrawer(){
        val drawer_layout : DrawerLayout = findViewById(R.id.drawer_layout)
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        val drawer_layout : DrawerLayout = findViewById(R.id.drawer_layout)
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        else{
            doubleBackToExit()
        }
    }

    fun updateNavigationUserDetails(user : User, readBoardsList:Boolean){

        hideProgressDialog()
        mUserName=user.name

        val nav_user_image : CircleImageView = findViewById(R.id.nav_user_image)
        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(nav_user_image)

        val tv_username : TextView = findViewById(R.id.tv_username)
        tv_username.text = user.name

        if(readBoardsList){
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStoreClass().getBoardsList(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== Activity.RESULT_OK
            && requestCode== My_PROFILE_REQUEST_CODE){
            FireStoreClass().loadUserData(this)
        }
        else if(resultCode == Activity.RESULT_OK && requestCode== CREATE_BOARD_REQUEST_CODE){
            FireStoreClass().getBoardsList(this)
        }
        else
        {
            Log.e("Cancelled","Cancelled")
        }
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawer_layout : DrawerLayout = findViewById(R.id.drawer_layout)
        when(item.itemId){
            R.id.nav_my_profile -> {
                startActivityForResult(Intent(this,MyProfileActivity::class.java),
                    My_PROFILE_REQUEST_CODE)
            }
            R.id.nav_sign_out ->{
                FirebaseAuth.getInstance().signOut()

                mSharedPreferences.edit().clear().apply()


                val intent = Intent(this,IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

                startActivity(intent)
                finish()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)

        return true
    }

    fun tokenUpdateSuccess(){
        hideProgressDialog()

        val editor : SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED,true)
        editor.apply()

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().loadUserData(this,true)
    }

    private fun updateFCMToken(token:String){
        val userHashMap = HashMap<String,Any>()
        userHashMap[Constants.FCM_TOKEN] = token
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().updateUserProfileData(this,userHashMap)
    }


    //******** for search view

    // calling on create option menu
    // layout to inflate our menu file.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // below line is to get our inflater
        val inflater = menuInflater

        // inside inflater we are inflating our menu file.
        inflater.inflate(R.menu.search_menu, menu)

        // below line is to get our menu item.
        val searchItem: MenuItem = menu.findItem(R.id.actionSearch)

        // getting search view of our item.
        val searchView: SearchView = searchItem.getActionView() as SearchView

        // below line is to call set on query text listener method.
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(msg: String): Boolean {
                // inside on query text change method we are
                // calling a method to filter our recycler view.
                filter(msg)
                return false
            }
        })
        return true
    }

    private fun filter(text: String) {
        // creating a new array list to filter our data.
        val filteredlist: ArrayList<Board> = ArrayList()

        // running a for loop to compare elements.
        for (item in mBoardsList) {
            // checking if the entered string matched with any item of our recycler view.
            if (item.name.toLowerCase().contains(text.toLowerCase())) {
                // if the item is matched we are
                // adding it to our filtered list.
                filteredlist.add(item)
            }
        }
        if (filteredlist.isEmpty()) {
            // if no item is added in filtered list we are
            // displaying a toast message as no data found.
            Toast.makeText(this, "No Data Found..", Toast.LENGTH_SHORT).show()
        } else {
            // at last we are passing that filtered
            // list to our adapter class.
            mAdapter.filterList(this, filteredlist)  // in BoardItemsAdapter
        }
    }
}