package project.blackjack.Models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tal P. on 20/07/2016.
 */

public class RoomPlayers {


    public String uid;
    public String player;
    public long turn;

    public HashMap<String,String> Players;

    public RoomPlayers() {}

    public RoomPlayers(String uid,String player,long turn)
    {
        this.uid=uid;
        this.player=player;
        this.turn=turn;

    }




    // Room to map
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("player", player);
        result.put("turn",turn);

        return result;
    }
}
