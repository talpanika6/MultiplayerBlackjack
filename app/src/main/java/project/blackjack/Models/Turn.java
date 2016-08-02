package project.blackjack.Models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tal on 02-Aug-16.
 */

public class Turn {

    public String uid;


    public Turn() {}


        public Turn(String uid)
        {
            this.uid=uid;
        }

        // Turn to map
        @Exclude
        public Map<String, Object> toMap() {
            HashMap<String, Object> result = new HashMap<>();
            result.put("uid", uid);


            return result;

        }
    }