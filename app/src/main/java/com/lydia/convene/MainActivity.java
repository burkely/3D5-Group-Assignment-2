package com.lydia.convene;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;


public class MainActivity extends Activity {

    private TextView info;
    private LoginButton loginButton;
    // The CallbackManager is used to manage the callbacks used in the app.
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        // Initialize the SDK before executing any other operations,
        // especially, if you're using Facebook UI elements.

        //initialize our instance of CallbackManager
        callbackManager = CallbackManager.Factory.create();

        setContentView(com.lydia.convene.R.layout.activity_main);
        //use findViewById to initialize the widgets
        info = (TextView) findViewById(com.lydia.convene.R.id.info);
        loginButton = (LoginButton) findViewById(com.lydia.convene.R.id.login_button);

        //create a callback to handle the results of the login attempts and
        // register it with the CallbackManager
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                info.setText("User ID:  " +
                        loginResult.getAccessToken().getUserId() + "\n" +
                        "Auth Token: " + loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
                info.setText("Login attempt cancelled.");
            }

            @Override
            public void onError(FacebookException e) {
                info.setText("Login attempt failed.");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(com.lydia.convene.R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == com.lydia.convene.R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /*Tapping the login button starts off a new Activity, which returns a
    result. To receive and handle the result, override the onActivityResult
    method of your Activity and ass its parameters to the onActivityResult
    method of CallbackManager*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    /* // make the API call
    new GraphRequest(
            AccessToken.getCurrentAccessToken(),
    "...?fields={lattitude, longitude}",
            null,
    HttpMethod.GET,
            new GraphRequest.Callback() {
        public void onCompleted(GraphResponse response) {
            //handle the result
        }
    }
    ).executeAsync():*/
}
