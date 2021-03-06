package com.cookandroid.formydear

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.HashMap
import java.util.regex.Pattern
;
class RegisterActivity : AppCompatActivity() {

    private lateinit var mFirebaseAuth: FirebaseAuth // 파이어베이스 인증 처리
    private lateinit var mDatabaseRef: DatabaseReference // 실시간 데이터 베이스
    private lateinit var analytics: FirebaseAnalytics
    private var storage: FirebaseStorage? = FirebaseStorage.getInstance() // 사진 폴더 만들기

    private lateinit var mEtEmail: EditText // 회원 가입 입력 필드(이메일)
    private lateinit var mEtName:EditText // 이름 입력 필드
    private lateinit var mEtChildName:EditText // 아동 이름 입력 필드
    private lateinit var mEtPwd: EditText // 회원 가입 입력 필드(비밀번호)
    private lateinit var mEtPwdCheck: EditText // 비밀번호 확인 필드
    private lateinit var mBtnConfirmID : Button //이메일 중복확인 버튼
    private lateinit var mBtnRegister: Button // 회원 가입 버튼
    private lateinit var mBtnLogin: Button // 로그인으로 가기 버튼
    private var test: Int = 0 // 이메일 중복 검사 버튼을 눌렀는지 확인

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mFirebaseAuth = FirebaseAuth.getInstance()

        mDatabaseRef = FirebaseDatabase.getInstance().getReference("ForMyDear").child("UserAccount")
        mEtEmail = findViewById<EditText>(R.id.etEmail)
        mBtnConfirmID = findViewById<Button>(R.id.btnConfirmID)
        mEtName=findViewById<EditText>(R.id.etName)
        mEtChildName=findViewById<EditText>(R.id.etChildName)
        mEtPwd = findViewById<EditText>(R.id.etPwd)
        mEtPwdCheck = findViewById<EditText>(R.id.etPwdCheck)
        mBtnRegister = findViewById<Button>(R.id.btnRegister)
        mBtnLogin = findViewById<Button>(R.id.btnLogin)

        var pattern : Pattern = android.util.Patterns.EMAIL_ADDRESS

        //이메일 중복 확인 버튼

        mBtnConfirmID.setOnClickListener {
            mDatabaseRef.orderByChild("userEmail").equalTo("${mEtEmail.text.toString()}")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {

                    }
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var value = snapshot.getValue()
                        if(!pattern.matcher(mEtEmail.text.toString()).matches()){
                            Toast.makeText(this@RegisterActivity,"이메일 형식으로 입력하세요",Toast.LENGTH_SHORT).show()
                        }else if(value != null){
                            Toast.makeText(this@RegisterActivity,"이미 가입되어 있는 이메일입니다",Toast.LENGTH_SHORT).show()
                        }else{
                            //가입 가능한 아이디이면 이메일 입력 창 비활성화 후 나머지 창 활성화
                            test = 1
                            Toast.makeText(this@RegisterActivity,"사용 가능한 이메일입니다",Toast.LENGTH_SHORT).show()

                        }
                    }
                })
        }
        mBtnRegister.setOnClickListener(View.OnClickListener {
            startRegister()
        })

        mBtnLogin.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

    }

    private fun startRegister() {
        // 회원가입 처리 시작
        var strEmail: String = mEtEmail.getText().toString()
        var strName: String = mEtName.getText().toString()
        var strChildName: String = mEtChildName.getText().toString()
        var strPwd: String = mEtPwd.getText().toString()
        var strPwdCheck: String = mEtPwdCheck.getText().toString()



        if(strEmail.equals("")||strName.equals("")||strPwd.equals("")||strPwdCheck.equals("")){
            Toast.makeText(this, "모든 항목을 입력해주세요", Toast.LENGTH_SHORT).show()
        }
        else if(!strPwd.equals(strPwdCheck)){
            Toast.makeText(this, "비밀번호를 확인해주세요", Toast.LENGTH_SHORT).show()
        }
        else if(test==0){
            Toast.makeText(this, "이메일 중복검사를 완료해주세요", Toast.LENGTH_SHORT).show()
        }
        else {

            mFirebaseAuth?.createUserWithEmailAndPassword(strEmail, strPwd)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        var firebaseUser: FirebaseUser? = mFirebaseAuth.currentUser
                        var account = UserAccount()
                        account.userId = firebaseUser?.uid.toString()
                        account.userEmail = firebaseUser?.email.toString()
                        account.userName = strName
                        account.userChildName = strChildName
                        account.userPwd = strPwd
                        account.userPhotoUri=""
                        account.userChildGender=""
                        account.userChildRel=""
                        account.userChildChar=""

                        // setValue : database에 insert (삽입) 행위
                        mDatabaseRef.child(firebaseUser?.uid.toString())
                            .setValue(account)
//                        storage?.reference?.child(account.userId).activeUploadTasks {
//                            Log.d("storage", "이미지 삭제완료")
//                        }
                        //uploadData()

                        Toast.makeText(this, "$strName 님, 가입을 축하합니다", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish() // 현재 액티비티 파괴

                    } else {
                        Toast.makeText(this, "회원가입에 실패하셨습니다", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    //uploadData위한 변수
    var imgUrl : String = ""
    var audUrl : String = ""
    var postId: String = ""

    private var postDataList = arrayListOf<PostData>(
        PostData("${mFirebaseAuth!!.currentUser!!.uid}", "음식", "", "", "", "", "", "", 1, ""),
    )
    /*
    private fun uploadData(){
        mDatabaseRef.child("UserAccount").child("${mFirebaseAuth!!.currentUser!!.uid}")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //파이어베이스의 데이터를 가져옴
                    var user: UserAccount? = snapshot.getValue(UserAccount::class.java)
                    Log.d("택", "${user!!.userId.toString()}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("Tag", "Failed")
                }
            })

        val hashMap : HashMap<String, Any> = HashMap()

        var strTitle: String = edtTitle.text.toString()
        var strContent = edtContent.text.toString()
        var strPostId: String = postId

        hashMap.put("postId", strPostId)
        hashMap.put("postPhotoUri", downloadUrl.toString())
        hashMap.put("postAudioUri", downloadAudioUrl)
        hashMap.put("uid", mFirebaseAuth!!.currentUser!!.uid)
        hashMap.put("postTitle", strTitle)
        hashMap.put("postContent", strContent)
        hashMap.put("categoryName", selectedItem.toString())
        hashMap.put("timestamp", timestamp)
        hashMap.put("star", 1)

        mDatabaseRef.ref.child("PostData").child("${mFirebaseAuth!!.currentUser!!.uid}").child("${selectedItem}").child(postId).setValue(hashMap)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    Toast.makeText(this, "업로드", Toast.LENGTH_SHORT).show()
                }
            }

    }*/

}