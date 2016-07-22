package project.blackjack;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import project.blackjack.Models.User;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private DatabaseReference mDatabase;

    private String mUsernameWelcome;
    private CoordinatorLayout mViewField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // set an exit transition
            getWindow().setEnterTransition(new Fade(Fade.OUT));
            // set an exit transition
            getWindow().setExitTransition(new  Fade(Fade.IN));
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Fields
         Button mCreateRoomButton;
         Button mJoinRoomButton;
         Button mLogOutButton;

        //firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //views
        mViewField=(CoordinatorLayout)findViewById(R.id.main_view);
        mCreateRoomButton=(Button)findViewById(R.id.button_create_room);
        mJoinRoomButton=(Button)findViewById(R.id.button_join_room);
        mLogOutButton=(Button)findViewById(R.id.button_log_out);

        // Click listeners
        mCreateRoomButton.setOnClickListener(this);
        mJoinRoomButton.setOnClickListener(this);
        mLogOutButton.setOnClickListener(this);


        //update user details
        updateUi();
    }

    /**
     * Update user Details
     */
    private void updateUi()
    {
        // [START single_value_read]
        final String userId = getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        // [START_EXCLUDE]
                        if (user == null) {
                            // User is null, error out
                            Log.e(TAG, "User " + userId + " is unexpectedly null");
                            Toast.makeText(MainActivity.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            //Update User Data to UI
                            mUsernameWelcome=getResources().getString(R.string.title_welcome)+" "+ usernameFromEmail(user.username);
                            showSnackbar(mUsernameWelcome);
                            Log.d(TAG,mUsernameWelcome);
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                    }
                });
        // [END single_value_read]

    }

    private void showSnackbar(String title)
    {

        Snackbar snackbar = Snackbar.make(mViewField, title, Snackbar.LENGTH_LONG);

        // Changing action button text color
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    private String usernameFromEmail(String email) {
        if (email.contains("@")) {
            return email.split("@")[0];
        } else {
            return email;
        }
    }

    /**
     * User Log out
     */
   private void logOut()
   {
       Log.d(TAG,"Log out - back to SignInActivity");

       FirebaseAuth.getInstance().signOut();
       ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this);
       startActivity(new Intent(this, SignInActivity.class),options.toBundle());

       finish();
   }

    /**
     * Click Listeners
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.button_create_room:

                startActivity(new Intent(this, CreateRoomActivity.class));

                break;

            case R.id.button_join_room:
                startActivity(new Intent(this, JoinRoomActivity.class));

                break;

            case R.id.button_log_out:
                logOut();
                break;
        }
    }
}
