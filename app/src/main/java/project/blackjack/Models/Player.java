package project.blackjack.Models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
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
    public int score;

    private ArrayList<Card> hand;

    public Player() {}


    public Player(String uid,String name,long turn ,double bet ,double chips,int score)
    {
        this.uid=uid;
        this.name=name ;
        this.turn=turn;
        this.bet=bet;
        this.chips=chips;
        this.score=score;

        hand = new ArrayList<>();
    }

    public int getPlayerScore() {
        int sum=0;

        for(Card h:hand)
        {
            //check for score

            if (h.getIntRank()==1)
            {
                //Ace can be 11
                int temp=11;

                if((temp+sum)>21)
                {
                    //ace is 1
                    sum+=h.getIntRank();
                }
                else
                {
                    //ace is 11
                    sum+=temp;
                }
            }
             //      Jack                         Queen               King
            else if((h.getIntRank()==11) || (h.getIntRank()==12) || (h.getIntRank()==13))
            {
                sum+=10;
            }
            else
            {//2-19=0
              sum+=h.getIntRank();
            }
        }

        return sum;
    }

    public void addCard(Card card) {

        if (hand==null)
            hand=new ArrayList<>();

        hand.add(card);

    }

    public void updateScore()
    {
        this.score=getPlayerScore();
    }

    public ArrayList<Card> getHand() {
        return hand;
    }

    public Map<String, Object> handoMap()
    {
        HashMap<String, Object> result = new HashMap<>();

        for(Card c: hand)
        {
            result.put(c.getStringRank(), c.getSuit());
        }
        return result;
    }

    // Player to map
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
