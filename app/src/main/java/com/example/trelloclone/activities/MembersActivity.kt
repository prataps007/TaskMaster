package com.example.trelloclone.activities

import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trelloclone.R
import com.example.trelloclone.adapters.MemberListItemsAdapter
import com.example.trelloclone.firebase.FireStoreClass
import com.example.trelloclone.models.Board
import com.example.trelloclone.models.User
import com.example.trelloclone.utils.Constants
import com.google.android.play.core.integrity.e
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL


class MembersActivity : BaseActivity() {

    private lateinit var mBoardDetails : Board
    private lateinit var mAssignedMembersList: ArrayList<User>
    private var anyChangesMade : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_members)

        if(intent.hasExtra(Constants.BOARD_DETAIL)){
            mBoardDetails = intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)!!

            showProgressDialog(resources.getString(R.string.please_wait))
            FireStoreClass().getAssignedMembersListDetails(this,mBoardDetails.assignedTo)
        }

        setUpActionBar()

    }

    fun setupMembersList(list:ArrayList<User>){
        mAssignedMembersList = list

        hideProgressDialog()

        val rv_members_list : RecyclerView = findViewById(R.id.rv_members_list)
        rv_members_list.layoutManager = LinearLayoutManager(this)
        rv_members_list.setHasFixedSize(true)

        val adapter = MemberListItemsAdapter(this,list)
        rv_members_list.adapter = adapter
    }

    fun memberDetails(user:User){
        mBoardDetails.assignedTo.add(user.id)
        FireStoreClass().assignMemberToBoard(this,mBoardDetails,user)
    }

    private fun setUpActionBar(){
        val toolbar_members_activity : Toolbar = findViewById(R.id.toolbar_members_activity)
        setSupportActionBar(toolbar_members_activity)

        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)

            actionBar.title = resources.getString(R.string.members)

        }

        toolbar_members_activity.setNavigationOnClickListener{onBackPressed()}
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_member,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_add_member->{
                dialogSearchMember()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun dialogSearchMember(){

        val dialog = Dialog(this)

        dialog.setContentView(R.layout.dialog_search_member)
        val tv_add : TextView = dialog.findViewById(R.id.tv_add)

        tv_add.setOnClickListener{
            val et_email_search_member : EditText = dialog.findViewById(R.id.et_email_search_member)
            val email = et_email_search_member.text.toString()

            if(email.isNotEmpty()){
                dialog.dismiss()
                showProgressDialog(resources.getString(R.string.please_wait))
                FireStoreClass().getMemberDetails(this,email)

            }
            else{
                Toast.makeText(
                    this@MembersActivity,
                    "Please enter members email address",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val tv_cancel : TextView = dialog.findViewById(R.id.tv_cancel)
        tv_cancel.setOnClickListener{
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onBackPressed() {
        if(anyChangesMade){
            setResult(Activity.RESULT_OK)
        }
        super.onBackPressed()
    }

    fun memberAssignSuccess(user: User){
        hideProgressDialog()
        mAssignedMembersList.add(user)

        anyChangesMade = true
        setupMembersList(mAssignedMembersList)


        SendNotificationToUserAsyncTask(mBoardDetails.name, user.fcmToken).execute()
    }


    private inner class SendNotificationToUserAsyncTask(val boardName : String, val token : String) : AsyncTask<Any,Void,String>(){

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog(resources.getString(R.string.please_wait))
        }

        override fun doInBackground(vararg params: Any?): String {
            var result : String
            var connection : HttpURLConnection?=null
            try{
                val url = URL(Constants.FCM_BASE_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.doOutput = true
                connection.doInput = true
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"

                connection.setRequestProperty("Content-Type","application/json")
                connection.setRequestProperty("charset","utf-8")
                connection.setRequestProperty("Accept","application/json")

                connection.setRequestProperty(
                    Constants.FCM_AUTHORIZATION, "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}"
                )

                connection.useCaches = false

                val wr = DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                val dataObject = JSONObject()

                dataObject.put(Constants.FCM_KEY_TITLE,"Assigned to the board $boardName")
                dataObject.put(Constants.FCM_KEY_MESSAGE,"You have been assigned to the Board by ${mAssignedMembersList[0].name}")

                jsonRequest.put(Constants.FCM_KEY_DATA,dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO,token)

                wr.writeBytes(jsonRequest.toString())
                wr.flush()
                wr.close()

                val httpResult : Int = connection.responseCode
                if(httpResult == HttpURLConnection.HTTP_OK){
                    val inputStream = connection.inputStream

                    val reader = BufferedReader(
                        InputStreamReader(inputStream)
                    )

                    val sb = StringBuilder()
                    var line : String
                    try{
                        while(reader.readLine().also {line=it} != null){
                            sb.append(line+"\n")
                        }
                    }catch (e:IOException){
                        e.printStackTrace()
                    }
                    finally {
                        try{
                            inputStream.close()
                        }catch (e:IOException){
                            e.printStackTrace()
                        }
                    }

                    result = sb.toString()

                }else{
                    result = connection.responseMessage
                }
            }
            catch (e:SocketTimeoutException){
                result = "Connection Timeout"
            }
            catch (e: Exception){
                result = "Error: "+ e.message
            }
            finally {
                connection?.disconnect()
            }

            return result

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            hideProgressDialog()
            Log.e("JSON Response Result",result!!)
        }

    }

}