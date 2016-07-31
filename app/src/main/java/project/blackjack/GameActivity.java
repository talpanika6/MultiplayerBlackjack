package project.blackjack;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import project.blackjack.Models.Player;
import project.blackjack.Models.RoomPlayers;

public class GameActivity extends BaseActivity {

    private ArrayList<Player> players;
    private String mRoomName;
    private DatabaseReference mDatabasePlayers;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);




        // Get room key from intent
        players = getIntent().getParcelableArrayListExtra(WaitingRoomActivity.EXTRA_PLAYER_KEY);
        mRoomName=getIntent().getStringExtra(WaitingRoomActivity.EXTRA_ROOM_KEY);
        if (players == null && mRoomName==null) {
            throw new IllegalArgumentException("Must pass EXTRA_ROOM_KEY && EXTRA_PLAYER_KEY");
        }

        for(Player key :players ) {
            Toast.makeText(this,
                  key.name +" is joins to the room",
                    Toast.LENGTH_SHORT).show();
        }
        //firebase
        mDatabasePlayers = FirebaseDatabase.getInstance().getReference().child("Game").child(mRoomName).child("Players");

    }
}
