package project.blackjack.Models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tal on 31-Jul-16.
 */

public class Player  {

    public String uid;
    public String name;
    public long turn;
    public double bet;
    public double chips;

    public Player() {}


    public Player(String uid,String name,long turn ,double bet ,double chips)
    {
        this.uid=uid;
        this.name=name ;
        this.turn=turn;
        this.bet=bet;
        this.chips=chips;
    }


    // Room to map
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("name", name);
        result.put("turn",turn);
        result.put("bet",bet);
        result.put("chips",chips);

        return result;
    }

}
