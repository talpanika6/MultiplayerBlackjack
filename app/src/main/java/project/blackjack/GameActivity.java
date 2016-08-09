package project.blackjack;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import project.blackjack.Models.Card;
import project.blackjack.Models.CardsPositions;
import project.blackjack.Models.Deck;
import project.blackjack.Models.Player;
import project.blackjack.Models.Turn;


public class GameActivity extends BaseActivity implements  Runnable{

    private static final String TAG = "GameActivity";
    private int NumOfCards=52;



    //firebase
    private ChildEventListener roomEventListener,playersEventListener,turnBetEventListener,deckEventListener,cardDealingEventListener,playersCardsEventListener,playersCardsHandEventListener;
    private DatabaseReference mDatabaseRef,mDatabaseRoom,mDatabasePlayers,mDatabaseTurnBet,mDatabaseTurnDeal,mDatabaseDeck,mDatabasePlayersCards;

    //objects
    private BaseActivity mContext;
    private ArrayList<Player> mPlayers;
    private ArrayList<Card> mCards;
    private Deck mDeck;
    private Player mCurrPlayer;
    private CardsPositions mCardPostions;



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
    private boolean isCardFlipped=false;
    private String mRoomName;
    private int nPlayers;
    private long currNumberOfCards=0;
    private long currNumberOfPlayersHand=0;
    private long numOfHand=0;


    //thread
    private SurfaceHolder holder;
    private Thread thread;
    private boolean locker=true;
    private Bitmap[] cardImages;
    private Bitmap mCardBack,background;
    private boolean waitingForInput=true;

    //screan
     private int height;
     private int width;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);


        //get screate Size
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
         height = displaymetrics.heightPixels;
         width = displaymetrics.widthPixels;

        //Base activity
        mContext=this;

        // Get room & players numbers from intent
        playersNumber=getIntent().getIntExtra(WaitingRoomActivity.EXTRA_PLAYER_NUMBER_KEY,0);

       // Toast.makeText(getApplicationContext(), playersNumber+" ", Toast.LENGTH_SHORT).show();

        mRoomName=getIntent().getStringExtra(WaitingRoomActivity.EXTRA_ROOM_KEY);
        if (  mRoomName==null) {
            throw new IllegalArgumentException("Must pass EXTRA_ROOM_KEY");
        }

        //firebase
        mDatabaseRef= FirebaseDatabase.getInstance().getReference();
        mDatabaseRoom=FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/");
        mDatabasePlayers = FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("players");
        mDatabaseTurnBet=FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("turn-bet");
        mDatabaseTurnDeal=FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("turn-deal");
        mDatabaseDeck=FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("deck");
        mDatabasePlayersCards=FirebaseDatabase.getInstance().getReference().child("/game/").child("/"+mRoomName+"/").child("players-cards");

        //create objects
        mPlayers=new ArrayList<>();




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

                   mContext.runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           clearBet();
                       }
                   });


                    return;
                }

                mCurrPlayer.bet=bet;
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBetText.setText("Bet: "+bet+"$");
                    }
                });



                mDatabasePlayers.child(mCurrPlayer.uid).child("bet").setValue(bet);

                String nextUid= getNextPlayer();
                ///check if max players exceeded
                 if (nextUid==null)
                 {
                     mContext.runOnUiThread(new Runnable() {
                         @Override
                         public void run() {
                             //hide bet layout from last player
                             mBetLayout.setVisibility(View.INVISIBLE);
                         }
                     });


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
                     mDatabaseTurnBet.child("uid").setValue(nextUid);
                 }

            }
        });

        //clear bet
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        clearBet();
                    }
                });

            }
        });
        //raise bet
        mRaiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bet+=10;
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBetEditText.setText(bet+"$");
                    }
                });

            }
        });
        //lower bet
        mLowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bet-=10;

                if (bet<0)
                    bet=0;

                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBetEditText.setText(bet+"$");
                    }
                });
            }
        });

        //create players positions

        mCardPostions=new CardsPositions(playersNumber);

        //start thread

        startThread();

        //get players from db
        setPlayersForGame();

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

                            //    Toast.makeText(getApplicationContext(),
                              //          "done feathcing",
                             //           Toast.LENGTH_SHORT).show();

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

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
        });

        //listen to turn deal
        setCardDealingEventListener();

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
               //     Toast.makeText(getApplicationContext(),
                    //        "child added",
                  ////          Toast.LENGTH_SHORT).show();




                    for(Player p: mPlayers)
                    {
                        if(p.uid.contains(currTurn.toString()) && !p.name.equals("Dealer"))
                            mCurrPlayer=p;
                    }

                    if(getPeekForNextPlayer()==null)
                    {
                        removeTurnBetEventListener();
                    }
                    //clear view
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //set visibily false
                            mGameLayout.setVisibility(View.INVISIBLE);
                            mBetLayout.setVisibility(View.INVISIBLE);
                        }
                    });


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
                            "child changed : ListenerForTurn",
                            Toast.LENGTH_SHORT).show();
                        //get player
                    for(Player p: mPlayers)
                    {
                        if(p.uid.contains(currTurn.toString()))
                            mCurrPlayer=p;
                    }

                    if(getPeekForNextPlayer()==null)
                    {
                        removeTurnBetEventListener();
                    }

                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //set visibily false
                            mGameLayout.setVisibility(View.INVISIBLE);
                            mBetLayout.setVisibility(View.INVISIBLE);
                        }
                    });

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
        mDatabaseTurnBet.addChildEventListener(turnBetEventListener);

    }

    private void clearBet()
    {
        bet=0.0;
        mBetEditText.setText(bet+"$");
    }

    /**
     * Game Methods
     */

    private void startGame() {

        Toast.makeText(getApplicationContext(),
                "betting is over,  starting game ...",
                Toast.LENGTH_SHORT).show();

        //set next turn
        //owner / dealer same
        Turn turn =new Turn(mPlayers.get(0).uid);
        Map<String, Object> TurnValues = turn.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/game/" + mRoomName + "/turn-deal/",TurnValues);
        mDatabaseRef.updateChildren(childUpdates);


        ///move to
        // deal cards by turns
       // setCardDealingEventListener();

    }


    private void clearPlayersHand() {
        for(Player p:mPlayers)
        {
            if (p.getHand()!=null)
                 p.getHand().clear();
        }
    }

    private void startDealing() {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mTurnText.setText("Waiting for dealer.. ");
                mBetLayout.setVisibility(View.INVISIBLE);
                mGameLayout.setVisibility(View.INVISIBLE);
            }
        });
        //add card
        //get 2 cards
        Card c1=mDeck.deal();
        Card c2=mDeck.deal();
        mCurrPlayer.addCard(c1);
        mCurrPlayer.addCard(c2);

        //update score
        mCurrPlayer.updateScore();

       // waitingForInput=true;
        mDatabasePlayers.child(mCurrPlayer.uid).child("score").setValue(mCurrPlayer.score);

        //remove Cards from Deck in DB
        mDatabaseDeck.child(c1.getStringRank()).removeValue();
        mDatabaseDeck.child(c2.getStringRank()).removeValue();

        //add hand to player
        Map<String, Object> CardsValues = mCurrPlayer.handoMap();
        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/game/" + mRoomName + "/players-cards/" +mCurrPlayer.uid, CardsValues);
        mDatabaseRef.updateChildren(childUpdates);

        //next turn
        String nextUid=getNextPlayer();
        if (nextUid!=null)
           mDatabaseTurnDeal.child("uid").setValue(nextUid);
        else
        {
            Toast.makeText(getApplicationContext(),
                    " Start Playing",
                    Toast.LENGTH_SHORT).show();


            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    mTurnText.setText("You Turn");
                    mGameLayout.setVisibility(View.VISIBLE);
                }
            });
        }


    }

    private void createDeckInDB() {

        Deck deck=new Deck();

        deck.fill();

        deck.shuffle();


        Map<String, Object> cardsValues = deck.toMap();

        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/game/" + mRoomName + "/deck/", cardsValues);
        mDatabaseRef.updateChildren(childUpdates);

    }

    private void loadBitmaps() {
        Bitmap bitCardBack = BitmapFactory.decodeResource(this.getResources(), R.mipmap.dealerdown);
        mCardBack= Bitmap.createScaledBitmap(bitCardBack, 260, 300, false);

        Bitmap back = BitmapFactory.decodeResource(this.getResources(), R.mipmap.green_table);
        background= Bitmap.createScaledBitmap(back, width, height, false);


        AssetManager assetManager = this.getAssets();

        cardImages = new Bitmap[53];

        for(int i = 1; i < 53; i++) {
            Bitmap bitmap = null;
            try {

                String fileName = "c"+i+".png";
                InputStream is=assetManager.open(fileName);
                bitmap = BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                Toast.makeText(this,"error "+e.getMessage(),Toast.LENGTH_LONG).show();
            }
            cardImages[i] = Bitmap.createScaledBitmap(bitmap, 260, 300, false);
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

    private String getPeekForNextPlayer() {
        int index=0;
        for(Player p: mPlayers)
        {
            if (mCurrPlayer== null)
                 continue;

            if(p.uid.contains(mCurrPlayer.uid))
                index= mPlayers.indexOf(p);
        }

        index++;

        if (index>=mPlayers.size())
            return null;

        return mPlayers.get(index).uid;
    }


    private Player getPlayerObjectByUid(String uid) {
        for(Player p:mPlayers)
        {
            if (p.uid.equals(uid))
                return p;
        }
        return null;
    }

    private void updatePlayersScore() {
        for(Player p:mPlayers)
        {
            p.updateScore();
        }
    }

    /**
     * Event Listeners
     */
    private void setEventListenerForDeck() {

        deckEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {


                String cardKey=dataSnapshot.getKey();
                Object cardValue=dataSnapshot.getValue();
 /*
                   Toast.makeText(getApplicationContext(),
                        "lis deck  key:"+ cardKey+" value :"+cardValue,
                       Toast.LENGTH_SHORT).show();
*/

                // [START_EXCLUDE]
                if (cardValue == null || cardKey==null) {
                    // User is null, error out
                    Log.e(TAG, "card unexpectedly null");
                    Toast.makeText(getApplicationContext(),
                            "Error: could not fetch card.",
                            Toast.LENGTH_SHORT).show();
                }

                else{
                    Card card=new Card(cardKey,Integer.parseInt(cardValue.toString()));

                    mCards.add(card);

                    //all cards are excepted from the server
                    if(mCards.size()==NumOfCards)
                    {
                        //remove listener
                        removeDeckEventListener();
                        //create deck
                        mDeck=new Deck(mCards,NumOfCards);

                        //start deal cards to dealer
                        startDealing();
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

    private void setCardDealingEventListener() {
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
                //    Toast.makeText(getApplicationContext(),
                ///            "child added: card Dealing",
                //            Toast.LENGTH_SHORT).show();

                    for(Player p: mPlayers)
                    {
                        if(p.uid.contains(currTurn.toString()))
                            mCurrPlayer=p;
                    }


                    if(mCurrPlayer.name.equals("Dealer"))
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

            //    Toast.makeText(getApplicationContext(),
               //         "child changed: card setCardDealingEventListener",
               //         Toast.LENGTH_SHORT).show();

                Object currTurn=dataSnapshot.getValue();


                // [START_EXCLUDE]
                if (currTurn == null) {
                    // User is null, error out
                    Log.e(TAG, "curr turn Player is unexpectedly null");
                    Toast.makeText(getApplicationContext(),
                            "Error: could not fetch urr turn Player.",
                            Toast.LENGTH_SHORT).show();
                } else {


                    //get player
                    for (Player p : mPlayers) {
                        if (p.uid.contains(currTurn.toString()))
                            mCurrPlayer = p;
                    }

                    if(getUid().equals(mCurrPlayer.uid))
                    {
                        //every player turn


                        mContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                mTurnText.setText("Waiting for dealer.. ");
                                mBetLayout.setVisibility(View.INVISIBLE);
                                mGameLayout.setVisibility(View.INVISIBLE);
                            }
                        });


                    }
                    getNumberOfPlayersHandFromDB();
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
                Toast.makeText(getApplicationContext(), "Failed to load Cards.", Toast.LENGTH_SHORT).show();
            }
        };
        mDatabaseTurnDeal.addChildEventListener(cardDealingEventListener);
    }

    private void getNumberOfPlayersHandFromDB() {
        roomEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                String childKey=dataSnapshot.getKey();


                if(childKey==null)
                {
                    Toast.makeText(getApplicationContext(),
                            "no players-cards key",
                            Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if (childKey.equals("players-cards"))
                    {
                      //  Toast.makeText(getApplicationContext(),
                        //        "players-cards key fetched",
                          //      Toast.LENGTH_SHORT).show();
                        currNumberOfPlayersHand=dataSnapshot.getChildrenCount();
                        removeRoomEventListener();

                        getNumberOfCardsInDeckFromDB();
                    }

                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mDatabaseRoom.addChildEventListener(roomEventListener);
    }

    private void getNumberOfCardsInDeckFromDB() {
        roomEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                String childKey=dataSnapshot.getKey();

                if(childKey==null)
                {
                    Toast.makeText(getApplicationContext(),
                            "no deck key",
                            Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if (childKey.equals("players-cards"))
                    {
                        currNumberOfPlayersHand=dataSnapshot.getChildrenCount();
                    }

                    if (childKey.equals("deck"))
                    {
                        currNumberOfCards=dataSnapshot.getChildrenCount();
                      //  Toast.makeText(getApplicationContext(),
                       //         "numOf Cards:"+currNumberOfCards,
                       //         Toast.LENGTH_SHORT).show();

                        removeRoomEventListener();

                        mCards=new ArrayList<>();
                        //call
                        setEventForDeckDealing();
                    }
                }


            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mDatabaseRoom.addChildEventListener(roomEventListener);
    }

    private void setEventForDeckDealing() {
        deckEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {


                String cardKey=dataSnapshot.getKey();
                Object cardValue=dataSnapshot.getValue();


                // [START_EXCLUDE]
                if (cardValue == null || cardKey==null) {
                    // User is null, error out
                    Log.e(TAG, "card unexpectedly null");
                    Toast.makeText(getApplicationContext(),
                            "Error: could not fetch card.",
                            Toast.LENGTH_SHORT).show();
                }

                else{
                    Card card=new Card(cardKey,Integer.parseInt(cardValue.toString()));

                    mCards.add(card);

                    //all cards are excepted from the server
                    if(mCards.size()==currNumberOfCards)
                    {
                    //    Toast.makeText(getApplicationContext(),
                    //            "Curr Cards Size :Download the new Cards"+mCards.size(),
                   //             Toast.LENGTH_SHORT).show();

                        //remove listener
                        removeDeckEventListener();
                        //create deck
                        mDeck=new Deck(mCards,(int)currNumberOfCards);

                        nPlayers=0;
                        clearPlayersHand();
                        setEventForPlayersCards();

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

    private void setEventForPlayersCards() {

        playersCardsEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {


                final String playerKey=dataSnapshot.getKey();
                numOfHand=dataSnapshot.getChildrenCount();
                nPlayers++;

          //      Toast.makeText(getApplicationContext(),
             //           "onChildAdded: Players Cards " +playerKey,
            //            Toast.LENGTH_SHORT).show();

                // [START_EXCLUDE]
                if (playerKey == null) {
                    // User is null, error out
                    Log.e(TAG, "player unexpectefdly null");
                    Toast.makeText(getApplicationContext(),
                            "Error: could not fetch player.",
                            Toast.LENGTH_SHORT).show();
                }

                else{
                    //fatching player hand

                    playersCardsHandEventListener  =new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                            String cardKey = dataSnapshot.getKey();
                            Object cardValue = dataSnapshot.getValue();

                     //       Toast.makeText(getApplicationContext(),
                     //               "onChildAdded: Players Hand - fetching hand",
                     //               Toast.LENGTH_SHORT).show();

                            // [START_EXCLUDE]
                            if (cardValue == null || cardKey == null) {
                                // User is null, error out
                                Log.e(TAG, "card unexpectedly null");
                                Toast.makeText(getApplicationContext(),
                                        "Error: could not fetch card.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Card card = new Card(cardKey, Integer.parseInt(cardValue.toString()));

                                Player p=getPlayerObjectByUid(playerKey);
                                p.addCard(card);

                            //    Toast.makeText(getApplicationContext(),
                            //            "num players hand"+currNumberOfPlayersHand,
                            //            Toast.LENGTH_SHORT).show();

                                //finish loading players cards
                                if (p.getHand().size() == numOfHand) {


                                    //remove listener per player
                                    mDatabasePlayersCards.child(playerKey);


                                    if(nPlayers==currNumberOfPlayersHand)
                                    {

                                     //   Toast.makeText(getApplicationContext(),
                                     //           " finish load Players Hand",
                                    //            Toast.LENGTH_SHORT).show();


                                        //removes listener
                                        removePlayersCardsEventListener();

                                        //update score and deal
                                        updatePlayersScore();

                                        //update canvas
                                        waitingForInput=false;

                                        //start deal cards to dealer
                                        startDealing();
                                    }
                                }
                            }
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }
                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }
                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    };
                    mDatabasePlayersCards.child(playerKey).addChildEventListener(playersCardsHandEventListener);


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
                Log.w(TAG, "PlayersCards:onCancelled", databaseError.toException());
                Toast.makeText(getApplicationContext(), "Failed to PlayersCards", Toast.LENGTH_SHORT).show();
            }
        };
        mDatabasePlayersCards.addChildEventListener(playersCardsEventListener);
    }

    /**
     * Remove Listeners
     */

    private void removeRoomEventListener() {
        if (roomEventListener!=null)
            mDatabaseRoom.removeEventListener(roomEventListener);
    }

    private void removePlayersCardsEventListener() {
        if (playersCardsEventListener!=null)
            mDatabasePlayersCards.removeEventListener(playersCardsEventListener);
    }

    private void removeCardDealingEventListener() {
        if (cardDealingEventListener!=null)
            mDatabaseTurnDeal.removeEventListener(cardDealingEventListener);
    }

    private void removeDeckEventListener() {
        if (deckEventListener!=null)
            mDatabaseDeck.removeEventListener(deckEventListener);
    }

    private void removeTurnBetEventListener() {
        if (turnBetEventListener!=null)
            mDatabaseTurnBet.removeEventListener(turnBetEventListener);
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

        removeRoomEventListener();
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

    @Override
    protected void onPause() {
        super.onPause();
        pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resume();
    }


    private void pause() {
        //CLOSE LOCKER FOR run();
        locker = false;
        while(true){
            try {
                //WAIT UNTIL THREAD DIE, THEN EXIT WHILE LOOP AND RELEASE a thread
                thread.join();
            } catch (InterruptedException e) {e.printStackTrace();
            }
            break;
        }

        thread = null;
    }

    private void resume() {
        //RESTART THREAD AND OPEN LOCKER FOR run();
        locker = true;
        thread = new Thread(this);

        thread.start();
        waitingForInput = false;
    }

    /**
     * Runnable imp.
     */



    private void startThread() {
        //place holder
        locker=true;
        holder = surface.getHolder();
        //start thread
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {

            while(locker) {
                //checks if the lockCanvas() method will be success,and if not, will check this statement again
                if (!holder.getSurface().isValid())
                    continue;

                if(waitingForInput)
                    continue;

                Canvas canvas = holder.lockCanvas();
                draw(canvas);
                holder.unlockCanvasAndPost(canvas);
                waitingForInput = true;
            }
        }


    private void draw(Canvas canvas) {


        float cardOffset =CardsPositions.cardOffset;
        float dealerOffset=CardsPositions.dealerOffset;

        int dealerFlipped=0;

        Rect dest = new Rect(0, 0,width, height);
        Paint pBack = new Paint();
        pBack.setFilterBitmap(true);
        canvas.drawBitmap(background, null, dest, pBack);

        //Pass over all players
        for(Player p: mPlayers)
        {
            ArrayList<Card> hand = p.getHand();



            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);

            float posX=mCardPostions.getPlayerPositionByTurn((int)(p.turn)).getX();
            float posY=mCardPostions.getPlayerPositionByTurn((int)(p.turn)).getY();

            //get score
            int score = p.score;

            if (p.name.equals("Dealer"))
            {//Dealer
                canvas.drawText(p.name + ": " +  score,posX-dealerOffset, posY+20, paint);


                //if(score > 21) {
               //     canvas.drawText("BUST", 20, 300 + i * 400, paint);
              //  }

            }
            else//Player
            {
                canvas.drawText(p.name + ": " +  score, posX-CardsPositions.nameOffset , posY-cardOffset, paint);


               // if(score > 21) {
                //    canvas.drawText("BUST", 20, 300 + i *(playerPlace-1)* 400, paint);
              //  }

            }

            int handIndex=0;

            if (hand==null)
                continue;

            //pass over hand player
            for(Card c: hand)
            {

                if(p.name.equals("Dealer"))
                {
                    if(dealerFlipped<1)
                        isCardFlipped=true;

                    if (isCardFlipped)
                    {
                        canvas.drawBitmap(mCardBack, posX, posY, null );
                        dealerFlipped++;
                        isCardFlipped=false;
                    }
                   else
                    {
                        canvas.drawBitmap(cardImages[c.getCardIndex()], posX+handIndex*cardOffset,posY, null );
                    }

                }
                else//player
                {
                   canvas.drawBitmap(cardImages[c.getCardIndex()],  posX+handIndex*cardOffset, posY, null );
                }

                handIndex++;
            }//end hand
        }//end players

    }


}
