package project.blackjack;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import project.blackjack.Models.Card;
import project.blackjack.Models.Deck;
import project.blackjack.Models.Player;
import project.blackjack.Models.RoomPlayers;
import project.blackjack.Models.Turn;

public class GameActivity extends BaseActivity implements Runnable{

    private static final String TAG = "GameActivity";
    private final int NumOfCards=52;

    //firebase
    private ChildEventListener playersEventListener,turnBetEventListener,deckEventListener,cardDealingEventListener;
    private DatabaseReference mDatabaseRef,mDatabasePlayers,mDatabaseTurn,mDatabaseDeck,mDatabasePlayersCards;

    //objects
    private ArrayList<Player> mPlayers;
    private ArrayList<Card> mCards;
    private Deck mDeck;
    private Player mCurrPlayer;
    private HashMap<String,ArrayList<Card>> mPlayersCards;

    //views
    private Button mOkButton,mClearButton,mHitButton,mStayButton;
    private EditText mBetEditText;
    private TextView mBalanceText,mBetText,mTurnText;
    private ImageView mRaiseButton,mLowerButton;
    private RelativeLayout mGameLayout,mBetLayout;
    private SurfaceView surface;

    //variables
    private int playersNumber;
    private double bet;
    private boolean backPressed=false;
    private String mRoomName;

    //thread
    private SurfaceHolder holder;
    private Thread thread;
    private boolean locker=true;
    private Bitmap[] cardImages;
    private Bitmap mCardBack;


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
        mDatabaseRef= FirebaseDatabase.getInstance().getReference();
        mDatabasePlayers = FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("players");
        mDatabaseTurn=FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("turn");
        mDatabaseDeck=FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("deck");
        mDatabasePlayersCards=FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("players-cards");

        //create objects
        mPlayers=new ArrayList<>();
        mPlayersCards=new HashMap<>();

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
        surface=(SurfaceView)findViewById(R.id.gameview);


        //load images
        loadBitmaps();

        //Listeners
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //validate for chips
                if (bet>mCurrPlayer.chips)
                {
                    Toast.makeText(getApplicationContext(),
                            "Error: not enough chips .",
                            Toast.LENGTH_SHORT).show();

                    clearBet();

                    return;
                }

                mCurrPlayer.bet=bet;
                mBetText.setText("Bet: "+bet+"$");


                mDatabasePlayers.child(mCurrPlayer.uid).child("bet").setValue(bet);

                String nextUid= getNextPlayer();
                ///check if max players exceeded
                 if (nextUid==null)
                 {
                     // start game

                     startGame();
                 }
                else
                 {///change next player in DB
                     Player p=getPlayerObjectByUid(nextUid);
                     if(p.name.equals("Dealer")) {
                         //if dealer pass him
                         nextUid = getNextPlayer();
                     }
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



    /**
     * Init methods
     */
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
                            mPlayers.add(player);

                            //    Toast.makeText(getApplicationContext(),
                            //        player.name +" is joins to the room",
                            //        Toast.LENGTH_SHORT).show();

                            //done featching
                            if (mPlayers.size() == playersNumber) {
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
    /**
     * Bet methods
     */
    private void startBetting() {

        if(getUid().equals(mCurrPlayer.uid))
        {
            clearBet();

            mTurnText.setText("Your Turn");
            mBalanceText.setText("Balance: "+mCurrPlayer.chips);
            mBetLayout.setVisibility(View.VISIBLE);

        }
        else
        {
            mTurnText.setText("Waiting for opponents ");
            mBetLayout.setVisibility(View.INVISIBLE);
        }

    }

    private void sortPlayerListByTurn( ) {
        Collections.sort(mPlayers, new Comparator<Player>() {
            @Override
            public int compare(Player p1, Player p2) {
             if (p1.turn == p2.turn)
                 return 0;

                return p1.turn<p2.turn ? -1 : 1;
            }
        });

    }

    private void setListenerForTurn() {

        turnBetEventListener = new ChildEventListener() {
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

                    for(Player p: mPlayers)
                    {
                        if(p.uid.contains(currTurn.toString()) && !p.name.equals("Dealer"))
                            mCurrPlayer=p;
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
                    for(Player p: mPlayers)
                    {
                        if(p.uid.contains(currTurn.toString()))
                            mCurrPlayer=p;
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
        mDatabaseTurn.addChildEventListener(turnBetEventListener);

    }

    private void clearBet()
    {
        bet=0.0;
        mBetEditText.setText(bet+"$");
    }

    /**
     * Game Methods
     */

    private void startGame()
    {

        //hide bet layout from last player
        mBetLayout.setVisibility(View.INVISIBLE);
        //remove listener
        removeTurnBetEventListener();

        //
        Toast.makeText(getApplicationContext(),
                "betting is over,  starting game ...",
                Toast.LENGTH_SHORT).show();

        //set next turn
        //owner / dealer same
        mDatabaseTurn.child("uid").setValue(mPlayers.get(0).uid);

        // deal cards by turns
        setCardDealingEventListener();

    }

    private void setEventListenerForDeck() {

        deckEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {

                Card card=dataSnapshot.getValue(Card.class);

                // [START_EXCLUDE]
                if (card == null) {
                    // User is null, error out
                    Log.e(TAG, "card unexpectedly null");
                    Toast.makeText(getApplicationContext(),
                            "Error: could not fetch card.",
                            Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(),
                            "child added",
                            Toast.LENGTH_SHORT).show();

                      mCards.add(card);

                    //all cards are excepted from the server
                    if(mCards.size()==NumOfCards)
                    {
                        //remove listener
                        removeDeckEventListener();
                        //create deck
                        mDeck=new Deck(mCards,NumOfCards);
                        //shuffle cards
                        mDeck.shuffle();
                        //start deal cards to dealer
                        startDealerDealing();
                    }

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
                Toast.makeText(getApplicationContext(), "Failed to load Cards.", Toast.LENGTH_SHORT).show();
            }
        };
        mDatabaseDeck.addChildEventListener(deckEventListener);
    }

    private void setCardDealingEventListener()
    {
        cardDealingEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {

                Object currTurn=dataSnapshot.getValue();

                //only dealer
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

                    for(Player p: mPlayers)
                    {
                        if(p.uid.contains(currTurn.toString()))
                            mCurrPlayer=p;
                    }


                    if(getUid().equals(mCurrPlayer.uid))
                    {
                        //create deck
                        createDeckInDB();

                        //create cards
                        mCards=new ArrayList<>();

                        //getting deck from db
                        setEventListenerForDeck();
                    }



                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                //every player turn
                //Todo get deck
                //Todo get players-card && update score in each player and his hand

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
                Toast.makeText(getApplicationContext(), "Failed to load Cards.", Toast.LENGTH_SHORT).show();
            }
        };
        mDatabaseTurn.addChildEventListener(cardDealingEventListener);
    }


    private void startDealerDealing()
    {
        //start the thread for draw in canvas
        //where to put
        startThread();

        //add card
        //get 2 cards
        Card c1=mDeck.deal();
        Card c2=mDeck.deal();
        mCurrPlayer.addCard(c1);
        mCurrPlayer.addCard(c2);

        //update score
        mCurrPlayer.upddateScore();
        mDatabasePlayers.child(mCurrPlayer.uid).child("score").setValue(mCurrPlayer.score);

        //remove Cards from Deck in DB
        mDatabaseDeck.child(c1.getStringRank()).removeValue();
        mDatabaseDeck.child(c2.getStringRank()).removeValue();

        //save cards in DB
        for(Card c :mCurrPlayer.getHand()) {

            Map<String, Object> CardsValues = c.toMap();
            Map<String, Object> childUpdates = new HashMap<>();

            childUpdates.put("/game/" + mRoomName + "/players-cards/" +mCurrPlayer.uid, CardsValues);
            mDatabaseRef.updateChildren(childUpdates);
        }


        //next turn
        mDatabaseTurn.child("uid").setValue(getNextPlayer());

    }


    private void createDeckInDB() {

        Deck deck=new Deck();

        deck.fill();


        Map<String, Object> cardsValues = deck.toMap();

        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/game/" + mRoomName + "/deck/", cardsValues);
        mDatabaseRef.updateChildren(childUpdates);

    }

    private void loadBitmaps() {
        Bitmap bitCardBack = BitmapFactory.decodeResource(this.getResources(), R.drawable.dealerdown);
        mCardBack= Bitmap.createScaledBitmap(bitCardBack, 352, 400, false);
        AssetManager assetManager = this.getAssets();

        cardImages = new Bitmap[53];

        for(int i = 0; i < 53; i++) {
            Bitmap bitmap = null;
            try {
                String fileName = "c"+i+".png";
                InputStream is=assetManager.open(fileName);
                bitmap = BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                Toast.makeText(this,"error "+e.getMessage(),Toast.LENGTH_LONG).show();
            }
            cardImages[i] = Bitmap.createScaledBitmap(bitmap, 320, 360, false);
        }
    }

    private String getNextPlayer() {
        int index=0;
        for(Player p: mPlayers)
        {
            if(p.uid.contains(mCurrPlayer.uid))
                index= mPlayers.indexOf(p);
        }

        index++;

        if (index>=mPlayers.size())
            return null;

        return mPlayers.get(index).uid;

    }

    private Player getPlayerObjectByUid(String uid)
    {
        for(Player p:mPlayers)
        {
            if (p.uid.equals(uid))
                return p;
        }
        return null;
    }


    /**
     * Remove Listeners
     */

    private void removeCardDealingEventListener() {
        if (cardDealingEventListener!=null)
            mDatabaseDeck.removeEventListener(cardDealingEventListener);
    }

    private void removeDeckEventListener() {
        if (deckEventListener!=null)
            mDatabaseDeck.removeEventListener(deckEventListener);
    }

    private void removeTurnBetEventListener() {
        if (turnBetEventListener!=null)
            mDatabaseTurn.removeEventListener(turnBetEventListener);
    }

    private void removePlayersEventListener() {
        if (playersEventListener!=null)
            mDatabasePlayers.removeEventListener(playersEventListener);
    }

    /**
     * Activity methods
     */
    @Override
    public void onStop() {
        super.onStop();

        removeCardDealingEventListener();
        removePlayersEventListener();
        removeTurnBetEventListener();
        removeDeckEventListener();

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

    /**
     * Runnable imp.
     */

    private void startThread() {
        //place holder
        holder = surface.getHolder();
        //start thread
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
/*
        try {
            while(locker) {
                //checks if the lockCanvas() method will be success,and if not, will check this statement again
                if (!holder.getSurface().isValid())
                    continue;

                if(waitingForInput)
                    continue;

                Canvas canvas = holder.lockCanvas();
                draw(canvas);
                holder.unlockCanvasAndPost(canvas);
              //  waitingForInput = true;
            }
        } catch (InterruptedException e) {
            Log.i("run",e.getMessage());
        }
        */
    }


    private void draw(Canvas canvas) {

        /*
        canvas.drawColor(Color.rgb(0, 135, 0));
        int playerPlace=4;
        for( int i=0; i<game.getPlayers().size(); i++ )
        {
            ArrayList<Card> hand = game.getPlayers().get(i).getHand();

            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(50);

            //get score
            int score = game.score(game.getPlayers().get(i));



            if (i<1)
            {//Dealer
                canvas.drawText(game.getPlayers().get(i).getName() + ": " +  score, 20, 100 + i * 400, paint);
                // canvas.drawText("Bet: " + game.getPlayers().get(i).getChips(), 1200, 100 + i * 400, paint);

                if(game.getPlayers().get(i).equals(game.getCurrentPlayer())) {
                    canvas.drawBitmap(mArrowTurn, 20, 150 + i * 400, null);
                }

                if(score > 21) {
                    canvas.drawText("BUST", 20, 300 + i * 400, paint);
                }
            }
            else//Player
            {
                canvas.drawText(game.getPlayers().get(i).getName() + ": " +  score, 20, 100 + i*(playerPlace-1) * 400, paint);
                // canvas.drawText("Bet: " + game.getPlayers().get(i).getChips(), 1200, 100 + i *(playerPlace-1)* 400, paint);

                if(game.getPlayers().get(i).equals(game.getCurrentPlayer())) {
                    canvas.drawBitmap(mArrowTurn, 20, 150 + i *(playerPlace-1)* 400, null);
                }

                if(score > 21) {
                    canvas.drawText("BUST", 20, 300 + i *(playerPlace-1)* 400, paint);
                }
            }


            // canvas.drawText("Count: " + game.cardCounting, 800, 100, paint);



            for( int j=0; j < hand.size(); j++ )
            {
                Card c = hand.get(j);


                if( i == 0 && j == 0 && game.isHoleFlipped)
                {
                    canvas.drawBitmap(mCardBack, 520 + j * 50, 200 + i * 400, null );
                }
                else
                {

                    if (i<1)
                        canvas.drawBitmap(cardImages[c.getCardIndex()], 520 + j * 50, 200 + i * 400, null );
                    else
                        canvas.drawBitmap(cardImages[c.getCardIndex()], 520 + j * 50, 100 + i *playerPlace* 300, null );
                }
            }
        }
        */
    }

}
