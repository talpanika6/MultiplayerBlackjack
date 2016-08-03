package project.blackjack.Models;

import com.google.firebase.auth.api.model.StringList;
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
        this.rank=String.valueOf(r+"_"+s);
        this.suit=s;
    }

    /*
    @Exclude
    public String toString() {
        String val;
        // empty string because 1 indexed I dont like this
        String[] suitList = {"", "Clubs", "Diamonds", "Hearts", "Spades"}; // This is kinda dumb
        if(rank == 1) val = "Ace";
        else if(rank == 11) val = "Jack";
        else if(rank == 12) val = "Queen";
        else if(rank == 13) val = "King";
        else val = String.valueOf(rank);
        return val + " of " + suitList[suit];
    }
*/
    @Exclude
    public int getIntRank()
    {
        String split[]= rank.split("_");

        return Integer.getInteger(split[0]);
    }

    public String getStringRank()
    {

        return rank;
    }

    public int getSuit()
    {
        return suit;
    }

    @Exclude
    public Map<String, Integer> toMap() {
        HashMap<String, Integer> result = new HashMap<>();
        result.put(rank, suit);

        return result;
    }



    @Exclude
    public int getCardIndex()
    {
        int r=getIntRank();

        return (r - 1)*4 + suit ;

    }
}
