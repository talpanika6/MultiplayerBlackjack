package project.blackjack.Models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tal P. on 20/07/2016.
 */

public class RoomPlayers implements Parcelable {


    public String uid;
    public String player;
    public double chips;
    public long turn;

    public HashMap<String,String> Players;

    public RoomPlayers() {}

    private RoomPlayers(Parcel in) {
        this.uid = in.readString();
        this.player = in.readString();
        this.chips = in.readDouble();
        this.turn = in.readLong();

    }

    public RoomPlayers(String uid,String player,double chips,long turn)
    {
        this.uid=uid;
        this.player=player;
        this.chips=chips;
        this.turn=turn;

    }


    // Room to map
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("player", player);
        result.put("chips",chips);
        result.put("turn",turn);

        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uid);
        parcel.writeString(player);
        parcel.writeDouble(chips);
        parcel.writeLong(turn);

    }

    public static final Parcelable.Creator<RoomPlayers> CREATOR = new Parcelable.Creator<RoomPlayers>() {
        public RoomPlayers createFromParcel(Parcel in) {
            return new RoomPlayers(in);
        }

        public RoomPlayers[] newArray(int size) {
            return new RoomPlayers[size];

        }
    };

}
