package project.blackjack;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;

import project.blackjack.Models.RoomPlayers;

public class GameActivity extends BaseActivity {

    private ArrayList<RoomPlayers> players;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);


        // Get room key from intent
        players = getIntent().getParcelableArrayListExtra(WaitingRoomActivity.EXTRA_PLAYER_KEY);
        if (players == null) {
            throw new IllegalArgumentException("Must pass EXTRA_ROOM_KEY");
        }

        for(RoomPlayers key :players ) {
            Toast.makeText(this,
                  key.player +" is joins to the room",
                    Toast.LENGTH_SHORT).show();
        }

    }
}
