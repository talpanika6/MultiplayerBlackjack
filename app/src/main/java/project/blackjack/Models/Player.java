package project.blackjack.Models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tal on 31-Jul-16.
 */

public class Player  implements Parcelable{

    public String uid;
    public String name;
    public long turn;
    public double bet;
    public double chips;


    private Player(Parcel in) {
        this.uid = in.readString();
        this.name = in.readString();
        this.turn = in.readLong();
        this.bet=in.readDouble();
        this.chips=in.readDouble();

    }

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

    @Exclude
    @Override
    public int describeContents() {
        return 0;
    }
    @Exclude
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uid);
        parcel.writeString(name);
        parcel.writeLong(turn);
        parcel.writeDouble(bet);
        parcel.writeDouble(chips);

    }
    @Exclude
    public static final Parcelable.Creator<Player> CREATOR = new Parcelable.Creator<Player>() {
        public Player createFromParcel(Parcel in) {
            return new Player(in);
        }

        public Player[] newArray(int size) {
            return new Player[size];

        }
    };
}
