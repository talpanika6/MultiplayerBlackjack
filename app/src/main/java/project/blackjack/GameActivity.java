package project.blackjack;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import project.blackjack.Models.Player;
import project.blackjack.Models.RoomPlayers;

public class GameActivity extends BaseActivity {

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
    private double bet;
    private int playerIndex=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);




        // Get room & players key from intent
        players = getIntent().getParcelableArrayListExtra(WaitingRoomActivity.EXTRA_PLAYER_KEY);
        mRoomName=getIntent().getStringExtra(WaitingRoomActivity.EXTRA_ROOM_KEY);
        if (players == null && mRoomName==null) {
            throw new IllegalArgumentException("Must pass EXTRA_ROOM_KEY && EXTRA_PLAYER_KEY");
        }
        playersTurns=new HashMap<>();
        /// message to players for joining to room
        for(Player key :players ) {
            Toast.makeText(this,
                  key.name +" is joins to the room",
                    Toast.LENGTH_SHORT).show();

            playersTurns.put(key,false);

        }

        //get player
         currPlayer=players.get(getNextPlayer());

        //firebase
        mDatabasePlayers = FirebaseDatabase.getInstance().getReference().child("Game").child(mRoomName).child("Players");

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
}
