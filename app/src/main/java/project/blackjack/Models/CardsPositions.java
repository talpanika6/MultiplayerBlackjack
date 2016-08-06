package project.blackjack.Models;



import java.util.ArrayList;

/**
 * Created by Tal on 06-Aug-16.
 */

public class CardsPositions {

    public static float cardOffset=20;
    public static  float nameOffset=70;


    private ArrayList<Position> mCardPostion;


    public CardsPositions(int nPlayers)
    {
        mCardPostion=new ArrayList<>(nPlayers);
    }

    public Position getPlayerPositionByTurn(int turn)
    {
        Position pos=null;

        switch (turn)
        {

            //dealer
            case 0:
                pos =new Position (300,100);
                break;

            case 1:
                pos =new Position (400,300);
                break;

            case 2:
                  pos =new Position (300,300);
                break;

            case 3:

            case 4:
                 break;

            case 5:

                break;


           default:
               break;

        }



        return pos;
    }


}
