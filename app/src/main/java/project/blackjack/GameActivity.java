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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import project.blackjack.Models.Player;
import project.blackjack.Models.Turn;

public class GameActivity extends BaseActivity {

    private static final String TAG = "GameActivity";

    private ChildEventListener playersEventListener,turnEventListener;
    private ArrayList<Player> players;
    private String mRoomName;
    private DatabaseReference mDatabasePlayers,mDatabaseTurn;
    private Button mOkButton,mClearButton,mHitButton,mStayButton;
    private EditText mBetEditText;
    private TextView mBalanceText,mBetText,mTurnText;
    private ImageView mRaiseButton,mLowerButton;
    private RelativeLayout mGameLayout,mBetLayout;
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
        mDatabaseTurn=FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("turn");

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


                mDatabasePlayers.child(currPlayer.uid).child("bet").setValue(bet);

                String nextUid= getNextPlayer();
                ///check if max players exceeded
                 if (nextUid==null)
                 {
                     //finsh betting start game
                     //Todo start game
                     startGame();
                 }
                else
                 {///change next player in DB
                     mDatabaseTurn.child("uid").setValue(nextUid);
                 }

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

    private void setPlayersForGame() {

        playersEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

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

                            //    Toast.makeText(getApplicationContext(),
                                //        player.name +" is joins to the room",
                                //        Toast.LENGTH_SHORT).show();

                                //done featching
                                if (players.size() == playersNumber) {
                                    Toast.makeText(getApplicationContext(),
                                            "done feathcing",
                                            Toast.LENGTH_SHORT).show();
                                    //remove listener
                                    removePlayersEventListener();
                                    //sort by turn
                                     sortPlayerListByTurn();
                                    //get curr player turn
                                    setListenerForTurn();

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

    private void sortPlayerListByTurn( ) {
        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player p1, Player p2) {
             if (p1.turn == p2.turn)
                 return 0;

                return p1.turn<p2.turn ? -1 : 1;
            }
        });

    }

    @Override
    public void onStop() {
        super.onStop();

        removePlayersEventListener();
        removeTurnEventListener();

    }

    private void removeTurnEventListener() {
        if (turnEventListener!=null)
            mDatabaseTurn.removeEventListener(turnEventListener);
    }

    private void removePlayersEventListener() {
        if (playersEventListener!=null)
            mDatabasePlayers.removeEventListener(playersEventListener);
    }

    private void setListenerForTurn() {

        turnEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {

                Object currTurn=dataSnapshot.getValue();

                // [START_EXCLUDE]
                if (currTurn == null) {
                    // User is null, error out
                    Log.e(TAG, "curr turn Player is unexpectedly null");
                    Toast.makeText(getApplicationContext(),
                            "Error: could not fetch urr turn Player.",
                            Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(),
                            "child added",
                            Toast.LENGTH_SHORT).show();

                    for(Player p: players)
                    {
                        if(p.uid.contains(currTurn.toString()))
                            currPlayer=p;
                    }

                    //set visibily false
                    mGameLayout.setVisibility(View.INVISIBLE);
                    mBetLayout.setVisibility(View.INVISIBLE);

                    //for the first time
                    startBetting();

                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());


                Object currTurn=dataSnapshot.getValue();


                // [START_EXCLUDE]
                if (currTurn == null) {
                    // User is null, error out
                    Log.e(TAG, "curr turn Player is unexpectedly null");
                    Toast.makeText(getApplicationContext(),
                            "Error: could not fetch urr turn Player.",
                            Toast.LENGTH_SHORT).show();
                } else {


                    Toast.makeText(getApplicationContext(),
                            "child changed",
                            Toast.LENGTH_SHORT).show();
                        //get player
                    for(Player p: players)
                    {
                        if(p.uid.contains(currTurn.toString()))
                            currPlayer=p;
                    }

                    //set visibily false
                    mGameLayout.setVisibility(View.INVISIBLE);
                    mBetLayout.setVisibility(View.INVISIBLE);

                    //for the first time
                    startBetting();

                }

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
        mDatabaseTurn.addChildEventListener(turnEventListener);

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


        mGameLayout.setVisibility(View.VISIBLE);
        mBetLayout.setVisibility(View.INVISIBLE);

    }

    private void clearBet()
    {
        bet=0.0;
        mBetEditText.setText(bet+"$");
    }

    private String getNextPlayer()
    {
        int index=0;
        for(Player p: players)
        {
            if(p.uid.contains(currPlayer.uid))
                index= players.indexOf(p);
        }

        index++;

        if (index>=players.size())
            return null;

        return players.get(index).uid;

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
