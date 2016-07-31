package project.blackjack;

import android.content.Context;
import android.content.Intent;
import android.os.health.PackageHealthStats;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import project.blackjack.Models.Room;
import project.blackjack.Models.RoomPlayers;
import project.blackjack.Models.User;

public class JoinRoomActivity extends BaseActivity  {

    public static final int minNumberOfPlayers=1;
    public static final int maxNumberOfPlayers=5;

    private static final String TAG = "JoinRoomActivity";

    private DatabaseReference mDatabase,mDatabaseNew;
    private RecyclerView mRoomsRecycler;
    private RoomAdapter mAdapter;
    private HashMap<String,Boolean> myRooms;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_room);

        // Initialize Database
        mDatabaseNew= FirebaseDatabase.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("/rooms/");

        //views
        Button mJoinRoomButton = (Button) findViewById(R.id.button_join_room);
        mRoomsRecycler = (RecyclerView) findViewById(R.id.recycler_rooms);
        myRooms=new HashMap<>();

        //Listener
        mJoinRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                joinRoom();
            }
        });
        mRoomsRecycler.setLayoutManager(new LinearLayoutManager(this));

    }


    @Override
    public void onStart() {
        super.onStart();
        updateRoomsUI();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Clean up  listener
        mAdapter.cleanupListener();
    }


    private void updateRoomsUI()
    {
        // Listen for Roomss
        mAdapter = new RoomAdapter(this, mDatabase,myRooms);
        mRoomsRecycler.setAdapter(mAdapter);

    }


    private void joinRoom()
      {

         if(!myRooms.containsValue(true))
           return;

          String roomName=null;

          for(Map.Entry<String,Boolean> entry:myRooms.entrySet())
          {
             if(entry.getValue())
             {
                 roomName=entry.getKey();
                 break;
             }

          }

          getNumberOfPlayersPerRoom(roomName);


      }
    private void getNumberOfPlayersPerRoom(final String room)
    {

        mDatabaseNew.child("rooms-players").child(room).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        long numberOfplayer = dataSnapshot.getChildrenCount();


                        // [START_EXCLUDE]
                        if (numberOfplayer ==0) {
                            // room is null, error out
                            Log.e(TAG,  numberOfplayer + " is unexpectedly null");
                            Toast.makeText(JoinRoomActivity.this,numberOfplayer+
                                    " Error: could not fetch number of players",
                                    Toast.LENGTH_SHORT).show();

                            return;
                        }

                        validateRoom(room,numberOfplayer);


                        // [END_EXCLUDE]
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getRoom:onCancelled", databaseError.toException());
                    }
                });
        // [END single_value_read]



    }
    private void validateRoom(final String roomName,final long numberOfPlayerInRoom)
    {


        mDatabase.child(roomName).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        Room room = dataSnapshot.getValue(Room.class);

                        // [START_EXCLUDE]
                        if (room == null) {
                            // room is null, error out
                            Log.e(TAG, "Room " + room + " is unexpectedly null");
                            Toast.makeText(JoinRoomActivity.this,
                                    "Error: could not fetch room",
                                    Toast.LENGTH_SHORT).show();
                        } else {

                          if(!checkForRoomState(roomName,room.state,numberOfPlayerInRoom)) {
                              Toast.makeText(JoinRoomActivity.this,
                                      "This Room is Full!!!, Check for another room..",
                                      Toast.LENGTH_SHORT).show();
                              return;
                          }

                            addPlayerToRoom(roomName,numberOfPlayerInRoom);

                        }

                        // [END_EXCLUDE]
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getRoom:onCancelled", databaseError.toException());
                    }
                });
        // [END single_value_read]

    }

    private boolean checkForRoomState(String room,String state,long numberOfPlayers )
    {

        switch (state)
        {
            case "Opened":
                mDatabase.child(room).child("state").setValue("Joined");
                return true;

            case "Joined":
                return ((numberOfPlayers>=minNumberOfPlayers) || numberOfPlayers<=maxNumberOfPlayers);

            case "Created":
                return false;
        }

        return false;
    }

    private void addPlayerToRoom(final String roomName,final long turn) {
        // [START single_value_read]
        final String userId = getUid();
        mDatabaseNew.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        // [START_EXCLUDE]
                        if (user == null) {
                            // User is null, error out
                            Log.e(TAG, "User " + userId + " is unexpectedly null");
                            Toast.makeText(JoinRoomActivity.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Write new post
                            writePlayerToRooms(userId, user.username, roomName,turn);
                            //start activity

                            Intent intent = new Intent(getApplicationContext(), WaitingRoomActivity.class);
                            intent.putExtra(WaitingRoomActivity.EXTRA_ROOM_KEY, roomName);
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

    private void writePlayerToRooms(String userId, String username,String roomName,long turn)
    {
        double chips=1000;
        //String key = mDatabase.child("room-players").child(roomName).push().getKey();
        RoomPlayers roomPlayer=new RoomPlayers(userId,username,++turn);

        Map<String, Object> roomPlayersValues = roomPlayer.toMap();
        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/rooms-players/"+roomName+"/"+userId,roomPlayersValues);
        mDatabaseNew.updateChildren(childUpdates);
    }


    private static class RoomViewHolder extends RecyclerView.ViewHolder {

        public TextView roomView;
        public RadioButton radioView;

        public RoomViewHolder(View itemView) {
            super(itemView);

            roomView = (TextView) itemView.findViewById(R.id.room_title);
            radioView = (RadioButton) itemView.findViewById(R.id.room_radio_btn);
        }
    }

    private static class RoomAdapter extends RecyclerView.Adapter<RoomViewHolder> {

        private static final String TAG = "RoomAdapter";
        private Context mContext;
        private DatabaseReference mDatabaseReference;
        private ChildEventListener mChildEventListener;
        private RadioButton lastCheckedRB = null;
        private HashMap<String,Boolean> myHashRoom;

        private List<String> mRooms = new ArrayList<>();

        public RoomAdapter(final Context context, DatabaseReference ref,HashMap<String,Boolean> myRooms) {
            mContext = context;
            mDatabaseReference = ref;
            myHashRoom=myRooms;

            // Create child event listener
            // [START child_event_listener_recycler]
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

                   // if(mRooms.isEmpty())
                      //  Toast.makeText(mContext, "Rooms not available", Toast.LENGTH_SHORT).show();

                    // Update RecyclerView
                    mRooms.add(dataSnapshot.getKey());
                    myHashRoom.put(dataSnapshot.getKey(),false);

                    notifyItemInserted(mRooms.size() - 1);
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                    // A room has roomved,
                    String roomKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int roomIndex = mRooms.indexOf(roomKey);
                    if (roomIndex > -1) {
                        // Remove data from the list
                        mRooms.remove(roomIndex);
                        myHashRoom.remove(roomKey);

                        // Update the RecyclerView
                        notifyItemRemoved(roomIndex);
                    } else {
                        Log.w(TAG, "onChildRemoved:unknown_child:" + roomKey);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "Rooms:onCancelled", databaseError.toException());
                    Toast.makeText(mContext, "Failed to load rooms.", Toast.LENGTH_SHORT).show();
                }
            };
            mDatabaseReference.addChildEventListener(childEventListener);
            // [END child_event_listener_recycler]

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;
        }


        @Override
        public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_room, parent, false);
            return new RoomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RoomViewHolder holder, int position) {

            final String roomName = mRooms.get(position);
            holder.roomView.setText(roomName);


            holder.radioView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RadioButton checked_rb = (RadioButton) view;
                    if(lastCheckedRB != null){
                        lastCheckedRB.setChecked(false);

                    }
                    clearValuesOnMap();
                   // Toast.makeText(mContext, roomName +" activate", Toast.LENGTH_SHORT).show();
                    myHashRoom.put(roomName,true);
                    lastCheckedRB = checked_rb;
                }


            });

        }

        private void clearValuesOnMap()
        {
            for(Map.Entry<String,Boolean> entry:myHashRoom.entrySet())
            {
                myHashRoom.put(entry.getKey(),false);
            }

        }

        @Override
        public int getItemCount() { return mRooms.size();}

        private void cleanupListener() {
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

    }

}
