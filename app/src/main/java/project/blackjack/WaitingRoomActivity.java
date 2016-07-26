package project.blackjack;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.blackjack.Models.RoomPlayers;
import project.blackjack.Models.User;

public class WaitingRoomActivity extends BaseActivity {

    private RecyclerView mPlayersRecycler;
    private DatabaseReference mDatabase;
    private PlayerAdapter mAdapter;
    private static ArrayList<RoomPlayers> Players;
    private String mRoomNameKey;
    private TextView mRommField;
    private Button createGameButton;
    private ProgressBar mProgressBar;

    public static final String EXTRA_ROOM_KEY = "room_key";
    public static final String EXTRA_PLAYER_KEY = "players_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        // Get room key from intent
        mRoomNameKey = getIntent().getStringExtra(EXTRA_ROOM_KEY);
        if (mRoomNameKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_ROOM_KEY");
        }

        //firebase
        mDatabase = FirebaseDatabase.getInstance().getReference().child("/rooms-players/").child(mRoomNameKey);
        Players=new ArrayList<>();

        //views
        mRommField=(TextView )findViewById(R.id.room_name);
        mPlayersRecycler = (RecyclerView) findViewById(R.id.recycler_players);
        mProgressBar=(ProgressBar)findViewById(R.id.waiting_progress);
        createGameButton=(Button)findViewById(R.id.button_create_game);
        mRommField.setText("Room: "+mRoomNameKey);


        createGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),GameActivity.class);
                intent.putExtra(EXTRA_PLAYER_KEY, Players);
                startActivity(intent);
            }
        });

        mProgressBar.setVisibility(View.VISIBLE);
        mPlayersRecycler.setLayoutManager(new LinearLayoutManager(this));

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
        // Listen for Players
        mAdapter = new PlayerAdapter(this, mDatabase,mProgressBar);
        mPlayersRecycler.setAdapter(mAdapter);

    }
    private static class PlayerViewHolder extends RecyclerView.ViewHolder {

        public TextView playerView;


        public PlayerViewHolder(View itemView) {
            super(itemView);

            playerView = (TextView) itemView.findViewById(R.id.player_title);

        }
    }
    private static class PlayerAdapter extends RecyclerView.Adapter<PlayerViewHolder> {

        private static final String TAG = "PlayerAdapter";
        private Context mContext;
        private DatabaseReference mDatabaseReference;
        private ChildEventListener mChildEventListener;
        private ProgressBar mProgressbar;



        private List<String> mPlayers= new ArrayList<>();

        public PlayerAdapter(final Context context, DatabaseReference ref,ProgressBar mProgressBar) {
            mContext = context;
            mDatabaseReference = ref;
            mProgressbar=mProgressBar;


            // Create child event listener
            // [START child_event_listener_recycler]
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());


                    String key = dataSnapshot.getKey();

                    //get player name
                    mDatabaseReference.child(key).addValueEventListener(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    // Get user value
                                    RoomPlayers object = dataSnapshot.getValue(RoomPlayers.class);

                                    // [START_EXCLUDE]
                                    if (object == null) {
                                        // player is null, error out
                                        Log.e(TAG, "player  is unexpectedly null");
                                        Toast.makeText(mContext,
                                                "Error: could not fetch player.",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Update RecyclerView

                                        Players.add(object);
                                        mPlayers.add(object.player);
                                        notifyItemInserted(mPlayers.size() - 1);
                                    }
                                    // [END_EXCLUDE]
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.w(TAG, "getPlayer:onCancelled", databaseError.toException());
                                }
                            });
                    // [END single_value_read]


                    //stop progrees bar max Players
                 if(mPlayers.size()>JoinRoomActivity.maxNumberOfPlayers)
                     mProgressbar.setVisibility(View.INVISIBLE);

                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                    // A player has roomved,
                    String playerKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int playerIndex = mPlayers.indexOf(playerKey);
                    if (playerIndex > -1) {
                        // Remove data from the list
                        mPlayers.remove(playerIndex);
                        Players.remove(playerIndex);

                        // Update the RecyclerView
                        notifyItemRemoved(playerIndex);
                    } else {
                        Log.w(TAG, "onChildRemoved:unknown_child:" + playerKey);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "Players:onCancelled", databaseError.toException());
                    Toast.makeText(mContext, "Failed to load players.", Toast.LENGTH_SHORT).show();
                }
            };
            mDatabaseReference.addChildEventListener(childEventListener);
            // [END child_event_listener_recycler]

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;
        }


        @Override
        public PlayerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_player, parent, false);
            return new PlayerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PlayerViewHolder holder, int position) {

            final String playerName = mPlayers.get(position);
            holder.playerView.setText("Player: "+playerName);

        }



        @Override
        public int getItemCount() { return mPlayers.size();}

        private void cleanupListener() {
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

    }
}
