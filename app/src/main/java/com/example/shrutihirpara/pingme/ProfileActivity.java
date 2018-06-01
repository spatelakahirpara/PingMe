package com.example.shrutihirpara.pingme;

import android.content.Intent;
import android.icu.text.DateFormat;
import android.media.Image;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {



    private ImageView mProfileImage;
    private TextView mProfileName;
    private TextView mProfileStatus;
    private Button mProfileSendReqBtn;
    private Button mDeclineBtn;


    //--------------FIREBASE-----------------------
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mFriendsReqDatabase;
    private String mCurrent_state;
    private FirebaseUser mCurrent_user;
    private DatabaseReference mNotificationDatabse;
    private DatabaseReference mRootRef;

    private DatabaseReference mFriendDatabse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id= getIntent().getStringExtra("user_id");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUsersDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendsReqDatabase= FirebaseDatabase.getInstance().getReference().child("Freind_req");
        mCurrent_user= FirebaseAuth.getInstance().getCurrentUser();

        mFriendDatabse=FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabse= FirebaseDatabase.getInstance().getReference().child("notifications");

        mProfileImage= (ImageView) findViewById(R.id.profile_image);
        mProfileName = (TextView ) findViewById(R.id.profile_displayName);
        mProfileStatus= (TextView) findViewById(R.id.profile_displayStatus);
        mProfileSendReqBtn = (Button) findViewById(R.id.profile_send_req_btn);
        mDeclineBtn= (Button) findViewById(R.id.profile_decline_btn);

        mDeclineBtn.setVisibility(View.INVISIBLE);
        mDeclineBtn.setEnabled(false);

        mCurrent_state= "not_friends";

        mUsersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String display_name= dataSnapshot.child("name").getValue().toString();
                String display_status= dataSnapshot.child("status").getValue().toString();
                String display_image= dataSnapshot.child("image").getValue().toString();


                mProfileName.setText(display_name);
                mProfileStatus.setText(display_status);
                Picasso.with(ProfileActivity.this).load(display_image).placeholder(R.drawable.default_image).into(mProfileImage);

                //----------------------FRIEND LIST----------------------------------
                mFriendsReqDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(user_id)){

                            String request_type= dataSnapshot.child(user_id).child("request_type").getValue().toString();
                            if(request_type.equals("received")){

                                mCurrent_state= "req_received";
                                mProfileSendReqBtn.setText("ACCEPT FRIEND REQUEST");

                                mDeclineBtn.setVisibility(View.VISIBLE);
                                mDeclineBtn.setEnabled(true);
                            }
                            else if(request_type.equals("sent")){
                                mCurrent_state="req_sent";
                                mProfileSendReqBtn.setText("CANCEL FRIEND REQUEST");
                            }
                        }else{
                            mFriendDatabse.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    if(dataSnapshot.hasChild(user_id)){
                                        mCurrent_state = "friends";
                                        mProfileSendReqBtn.setText("UNFRIEND THIS PERSON");
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }



            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {

                 mProfileSendReqBtn.setEnabled(false);


                //---------------------NOT FRIENDS------------------------
                if(mCurrent_state.equals("not_friends")){
                    mProfileSendReqBtn.setEnabled(false);

                    mFriendsReqDatabase.child(mCurrent_user.getUid()).child(user_id).child("request_type").setValue("sent")
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){

                                mFriendsReqDatabase.child(user_id).child(mCurrent_user.getUid()).child("request_type").setValue("received")
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                HashMap<String, String> notificationData= new HashMap<>();
                                                notificationData.put("from", mCurrent_user.getUid());
                                                notificationData.put("type","request");

                                                mNotificationDatabse.child(user_id).push().setValue(notificationData)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {

                                                            }
                                                        });
                                                mProfileSendReqBtn.setEnabled(true);

                                                mCurrent_state= "req_sent";
                                                mProfileSendReqBtn.setText("CANCEL FRIEND REQUEST");

                                            }
                                        });


                            }else{

                                Toast.makeText(ProfileActivity.this, "fail",Toast.LENGTH_LONG).show();

                            }
                            mProfileSendReqBtn.setEnabled(true);

                        }
                    });


                }
                //--------------------CANCEL REQUEST----------------------

                if(mCurrent_state.equals("req_sent")) {

                    mFriendsReqDatabase.child(mCurrent_user.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendsReqDatabase.child(user_id).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrent_state = "not_friends";
                                    mProfileSendReqBtn.setText("SEND FRIEND REQUEST");
                                }
                            });
                        }
                    });
                }
                //--------------------UNFRIEND THE PERSON---------------------

                if(mCurrent_state.equals("friends")) {

                    mFriendDatabse.child(mCurrent_user.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendDatabse.child(user_id).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrent_state = "not_friends";
                                    mProfileSendReqBtn.setText("SEND FRIEND REQUEST");


                                }
                            });
                        }
                    });
                }
                //-------------------------DECLINE FRIEND REQUEST.....not working-----------------
                if(mCurrent_state.equals("req_received")) {

                    mFriendsReqDatabase.child(mCurrent_user.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendsReqDatabase.child(user_id).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrent_state = "not_friends";
                                    mProfileSendReqBtn.setText("SEND FRIEND REQUEST");


                                }
                            });
                        }
                    });
                }

                    //---------------REQUEST RECIEVED STATE----------------------
                if(mCurrent_state.equals("req_received")){

                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                    Map friendsMap = new HashMap();
                    friendsMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id + "/date", currentDate);
                    friendsMap.put("Friends/" + user_id + "/"  + mCurrent_user.getUid() + "/date", currentDate);


                    friendsMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id, null);
                    friendsMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid(), null);


                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {


                            if(databaseError == null){

                                mProfileSendReqBtn.setEnabled(true);
                                mCurrent_state = "friends";
                                mProfileSendReqBtn.setText("Unfriend this Person");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);

                            } else {

                                String error = databaseError.getMessage();

                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();


                            }

                        }
                    });

                }

            }
        });



    }

}
