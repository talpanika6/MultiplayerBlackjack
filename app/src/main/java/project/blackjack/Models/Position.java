package project.blackjack.Models;

/**
 * Created by Tal on 06-Aug-16.
 */

public class Position {

    private float  x;
    private float  y;

    public Position(float x, float y)
    {
        this.x=x;
        this.y=y;
    }

    public float getX(){return x;}
    public float getY(){return y;}

}
