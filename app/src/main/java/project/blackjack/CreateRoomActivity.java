package project.blackjack;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import project.blackjack.Models.Room;
import project.blackjack.Models.RoomPlayers;
import project.blackjack.Models.User;

public class CreateRoomActivity extends BaseActivity implements View.OnClickListener  {

    private static final String TAG = "CreateRoomActivity";
    private static final String REQUIRED = "Required";

    private EditText mRoomNameField;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        //firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //fields
        Button mCreateRoom=(Button)findViewById(R.id.button_create_room);
        mRoomNameField=(EditText)findViewById(R.id.field_room_name);

        //Listener
        mCreateRoom.setOnClickListener(this);
    }

    private boolean validateForm() {
        boolean result = true;
        if (TextUtils.isEmpty(mRoomNameField.getText().toString())) {
            mRoomNameField.setError(REQUIRED);
            result = false;
        } else {
            mRoomNameField.setError(null);
        }

        return result;
    }

    private void createRoom()
    {

        if(!validateForm())
            return;

        // [START single_value_read]
        final String userId = getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        // [START_EXCLUDE]
                        if (user == null) {
                            // User is null, error out
                            Log.e(TAG, "User " + userId + " is unexpectedly null");
                            Toast.makeText(CreateRoomActivity.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Write new post
                            writeRoomToDB(userId, user.username,mRoomNameField.getText().toString());
                            //start activity
                            Intent intent = new Intent(getApplicationContext(), WaitingRoomActivity.class);
                            intent.putExtra(WaitingRoomActivity.EXTRA_ROOM_KEY, mRoomNameField.getText().toString());
                            startActivity(intent);
                        }


                        // [END_EXCLUDE]
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                    }
                });
        // [END single_value_read]

    }

    private void writeRoomToDB(String userId, String username,String roomName)
    {
        //create /rooms/key/room details
        //create /rooms-players/room/uid/user details
        //String key = mDatabase.child("rooms").push().getKey();
        Room room=new Room(userId,username,Room.State.Opened);
        RoomPlayers roomPlayer=new RoomPlayers(userId,username,1);

        Map<String, Object> roomValues = room.toMap();
        Map<String, Object> roomPlayersValues = roomPlayer.toMap();
        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/rooms-players/"+roomName+"/"+userId,roomPlayersValues);
        childUpdates.put("/rooms/"+roomName,roomValues);
        mDatabase.updateChildren(childUpdates);
    }


    @Override
    public void onClick(View view)
    {

        switch (view.getId())
        {

            case R.id.button_create_room:

                createRoom();

                break;

        }

    }
}
