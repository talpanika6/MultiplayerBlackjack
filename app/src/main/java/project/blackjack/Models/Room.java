package project.blackjack.Models;

import java.util.HashMap;
import java.util.Map;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
/**
 * Created by Tal P. on 19/07/2016.
 */
@IgnoreExtraProperties
public class Room {

    public enum State {Opened, Joined, Created}

    public String uid;
    public String owner;
   // public String roomName;
    public String state;
    public HashMap<String,String> Players;

    public Room() {}



    public Room(String uid,String owner,State state)
    {
        this.uid=uid;
        this.owner=owner;
     //   this.roomName=roomName;
        this.state=stateToString(state);
    }

    private String stateToString(State state)
    {
        String result;

        switch(state)
        {


            case Opened:
                result="Opened";
                break;

            case Joined:
                result="Joined";
                break;

            case Created:
                result="Created";
                break;

                default:   result="Created";
                    break;
        }

        return result;
    }

    // Room to map
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("owner", owner);
       // result.put("name",roomName);
        result.put("state",state);

        return result;
    }

}
