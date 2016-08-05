package project.blackjack.Models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tal on 03-Aug-16.
 */

public class Card {

    private String rank;
    private int suit;

    public Card(String rank, int suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public Card(int r,int s)
    {
        this(String.valueOf(r+"-"+s),s);
    }


    @Exclude
    public int getIntRank()
    {
        String [] split= rank.split("-");

        return Integer.parseInt(split[0]);
    }
    @Exclude
    public String getStringRank()
    {

        return rank;
    }

    public int getSuit()
    {
        return suit;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put(rank, suit);

        return result;
    }



    @Exclude
    public int getCardIndex()
    {
        int r=getIntRank();

        return (r - 1)*4 + suit ;

    }

    public String toString()
    {
        return "rank: "+rank+", suit: "+suit;
    }
}
