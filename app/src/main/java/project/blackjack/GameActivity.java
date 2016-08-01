package project.blackjack;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import project.blackjack.Models.Player;

public class GameActivity extends BaseActivity {

    private static final String TAG = "GameActivity";

    private ChildEventListener playersEventListener;
    private ArrayList<Player> players;
    private String mRoomName;
    private DatabaseReference mDatabasePlayers;
    private Button mOkButton,mClearButton,mHitButton,mStayButton;
    private EditText mBetEditText;
    private TextView mBalanceText,mBetText,mTurnText;
    private ImageView mRaiseButton,mLowerButton;
    private RelativeLayout mGameLayout,mBetLayout;
    private HashMap<Player,Boolean> playersTurns;
    private Player currPlayer;
    private int playersNumber;
    private double bet;
    private int playerIndex=0;
    private boolean backPressed=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);


        // Get room & players numbers from intent
        playersNumber=getIntent().getIntExtra(WaitingRoomActivity.EXTRA_PLAYER_NUMBER_KEY,0);

        Toast.makeText(getApplicationContext(), playersNumber+" ", Toast.LENGTH_SHORT).show();

        mRoomName=getIntent().getStringExtra(WaitingRoomActivity.EXTRA_ROOM_KEY);
        if (  mRoomName==null) {
            throw new IllegalArgumentException("Must pass EXTRA_ROOM_KEY");
        }

        //firebase
        mDatabasePlayers = FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("players");

        players=new ArrayList<>();
        //get players from db
        setPlayersForGame();


        //views
        mOkButton=(Button)findViewById(R.id.button_bet);
        mClearButton=(Button)findViewById(R.id.button_clear);
        mHitButton=(Button)findViewById(R.id.button_hit);
        mStayButton=(Button)findViewById(R.id.button_stay);
        mBetEditText=(EditText)findViewById(R.id.edit_bet);
        mBalanceText=(TextView)findViewById(R.id.balance_text);
        mBetText=(TextView)findViewById(R.id.bet_text);
        mTurnText=(TextView)findViewById(R.id.turn_text);
        mRaiseButton=(ImageView)findViewById(R.id.image_raise);
        mLowerButton=(ImageView)findViewById(R.id.image_lower);
        mGameLayout=(RelativeLayout)findViewById(R.id.game_layout);
        mBetLayout=(RelativeLayout)findViewById(R.id.bet_layout);

        //Listeners
        //finish betting next players
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //validate for chips
                if (bet>currPlayer.chips)
                {
                    Toast.makeText(getApplicationContext(),
                            "Error: not enough chips .",
                            Toast.LENGTH_SHORT).show();

                    clearBet();

                    return;
                }

                currPlayer.bet=bet;
                mBetText.setText("Bet: "+bet+"$");

                playersTurns.put(currPlayer,true);
                mDatabasePlayers.child(currPlayer.uid).child("bet").setValue(bet);

                if (playersTurns.containsValue(false))
                {
                    //for another player
                    startBetting();
                }
                else
                    startGame();
            }
        });

        //clear bet
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                clearBet();


            }
        });
        //raise bet
        mRaiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bet+=10;

                mBetEditText.setText(bet+"$");
            }
        });
        //lower bet
        mLowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bet-=10;

                if (bet<0)
                    bet=0;

                mBetEditText.setText(bet+"$");
            }
        });


    }

    private void setPlayersForGame()
    {

        playersEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());


                String key = dataSnapshot.getKey();


                // Get user value
                String uid = dataSnapshot.getKey();




                // [START_EXCLUDE]
                if (uid == null) {
                    // User is null, error out
                    Log.e(TAG, "Player is unexpectedly null");
                    Toast.makeText(getApplicationContext(),
                            "Error: could not fetch player.",
                            Toast.LENGTH_SHORT).show();
                } else {



                        mDatabasePlayers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot DataSnapshot) {

                                Player player = DataSnapshot.getValue(Player.class);
                                players.add(player);

                                if (players.size() == playersNumber) {

                                    //start bet
                                    prepareForBetting();

                                }

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    }

                }


            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());


            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Players:onCancelled", databaseError.toException());
                Toast.makeText(getApplicationContext(), "Failed to load players.", Toast.LENGTH_SHORT).show();
            }
        };
        mDatabasePlayers.addChildEventListener(playersEventListener);

    }

    @Override
    public void onStop() {
        super.onStop();

        if (playersEventListener!=null)
            mDatabasePlayers.removeEventListener(playersEventListener);

    }
    private void prepareForBetting()
    {
        playersTurns=new HashMap<>();

        if (players.size()==0)
            Toast.makeText(this,
                    "players is null",
                    Toast.LENGTH_SHORT).show();
        /// message to players for joining to room
        for(Player key :players ) {
            Toast.makeText(this,
                    key.name +" is joins to the room",
                    Toast.LENGTH_SHORT).show();

            playersTurns.put(key,false);

        }

        //get player
        currPlayer=players.get(getNextPlayer());

        //set visibily false
        mGameLayout.setVisibility(View.INVISIBLE);
        mBetLayout.setVisibility(View.INVISIBLE);

        //for the first time
        startBetting();
    }
    private void startBetting()
    {

        if(getUid().equals(currPlayer.uid))
        {
            clearBet();

            mTurnText.setText("Your Turn");
            mBalanceText.setText("Balance: "+currPlayer.chips);
            mBetLayout.setVisibility(View.VISIBLE);

        }
        else
        {
            mTurnText.setText("Waiting for opponents ");
            mBetLayout.setVisibility(View.INVISIBLE);
        }

    }

    private void startGame()
    {

        Toast.makeText(getApplicationContext(),
                "starting game ...",
                Toast.LENGTH_SHORT).show();

        //todo load images start dill
        clearValuesOnMap();

        mGameLayout.setVisibility(View.VISIBLE);
        mBetLayout.setVisibility(View.INVISIBLE);

    }

    private void clearBet()
    {
        bet=0.0;
        mBetEditText.setText(bet+"$");
    }
    private int getNextPlayer()
    {
        return (playerIndex++) % players.size();
    }

    private void clearValuesOnMap()
    {
        for(Map.Entry<Player,Boolean> entry:playersTurns.entrySet())
        {
            playersTurns.put(entry.getKey(),false);
        }

    }

    @Override
    public void onBackPressed() {

        if (backPressed)
        {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(GameActivity.this);
            // Go to MainActivity
            startActivity(new Intent(GameActivity.this, MainActivity.class),options.toBundle());
            finish();
           //ToDo del from db the player


        }
        else
        {
            Toast.makeText(this,
                    " Press again to exit Game",
                    Toast.LENGTH_SHORT).show();

            backPressed=true;
        }

    }
}
