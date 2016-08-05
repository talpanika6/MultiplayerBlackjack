package project.blackjack.Models;

import android.renderscript.Sampler;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tal P. on 17/07/2016.
 */

public class Deck {

    private ArrayList<Card> cards;
    private int numCards;

    public Deck() {
    }

    public Deck(ArrayList<Card> cards, int nOfCards)
    {
        this.cards=cards;
        this.numCards=nOfCards;
    }

    @Exclude
    public Map<String, Object> toMap()
    {
        HashMap<String, Object> result = new HashMap<>();

        for(Card c: cards)
        {
            result.put(c.getStringRank(), c.getSuit());
        }
        return result;
    }


    @Exclude
    public void fill() {
        cards = new ArrayList<>(52);

        for(int r=1; r < 14; r++) {
            for (int s = 1; s <= 4; s++) {
                cards.add(new Card(r, s));
                numCards++;
            }
        }
      //  numCards = 52;
    }
    @Exclude
    public void shuffle() {
        for(int i = 0; i < numCards - 1; i++)  {
            int r = (int)((numCards-i)*Math.random()+i);
            Card temp = cards.get(i);
            cards.set(i,cards.get(r));
            cards.set(r,temp);
        }
    }
    @Exclude
    public Card deal() {
        if(numCards == 0){
            fill();
            shuffle();
        }
        numCards--;
        return cards.get(numCards);
    }


    public int getNumCards() {
        return cards.size();
    }
}
