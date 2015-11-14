package com.lydia.convene;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;


public class MainActivity extends Activity {

    // LogCat tag - http://developer.android.com/tools/debugging/debugging-log.html
    private static final String TAG = MainActivity.class.getSimpleName();
    // Client name
    private String name = null;

    private TextView info;
    private LoginButton loginButton;
    private Button btnSend;

    private WebSocketClient client;
    // The CallbackManager is used to manage the callbacks used in the app.
    private CallbackManager callbackManager;
    private Utils utils;

    // JSON flags to identify the kind of JSON response
    private static final String TAG_SELF = "self", TAG_CONVENE_REQ = "convene?", TAG_LOCATION_REQ = "location",
            TAG_LOCATION_RESPONSE = "locationResponse", TAG_CONVENE_RESPONSE = "conveneResponse";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("test", "test");
        FacebookSdk.sdkInitialize(getApplicationContext());
        // Initialize the SDK before executing any other operations,
        // especially, if you're using Facebook UI elements.
        setContentView(com.lydia.convene.R.layout.activity_main);

        //initialize our instance of CallbackManager
        callbackManager = CallbackManager.Factory.create();

        //use findViewById to initialize the widgets
        info = (TextView) findViewById(com.lydia.convene.R.id.info);
        loginButton = (LoginButton) findViewById(com.lydia.convene.R.id.login_button);
        btnSend = (Button) findViewById(R.id.btnSend);

        utils = new Utils(getApplicationContext());
        Log.d("INIT", "inti");
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

        /**
         * Creating web socket client. This will have callback methods
         * */
        Log.d("BEGIN CONN", "creating client websocket connection");
        try {
            client = new WebSocketClient(URI.create(WsConfig.URL_WEBSOCKET
                    + URLEncoder.encode(name, "UTF-8")), new WebSocketClient.Listener() {

                @Override
            public void onConnect() {

            }

            /**
             * On receiving the message from web socket server
             * */
            @Override
            public void onMessage(String message) {
                Log.d(TAG, String.format("Got string message! %s", message));

                parseMessage(message);
            }

            @Override
            public void onMessage(byte[] data) {
                Log.d(TAG, String.format("Got binary message! %s",
                        bytesToHex(data)));

                // Message will be in JSON format
                parseMessage(bytesToHex(data));
            }

            /**
             * Called when the connection is terminated
             * */
            @Override
            public void onDisconnect(int code, String reason) {
                // clear the session id from shared preferences
                utils.storeSessionId(null);
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error! : " + error);
            }

            }, null);
            Log.d("BEGIN..", "made a client");
        }

        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.connect();
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


    public void sendConveneReq() {
        // ask userX to meet. Send to server to pass on
        // Sending message to web socket server
        sendMessageToServer(utils.getSendMessageJSON("convene?"));
    }

    @Override
    protected void onDestroy() {
        super.onStop();
        // MAKE SURE YOU CLOSE THE SOCKET UPON EXITING
        if(client != null & client.isConnected()){
            client.disconnect();
        }
    }

    private void sendMessageToServer(String message) {
        if (client != null && client.isConnected()) {
            client.send(message);
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Parsing the JSON message received from server The intent of message will
     * be identified by JSON node 'flag'. flag = self, message belongs to the
     * person. flag = new, a new person joined the conversation. flag = message,
     * a new message received from server. flag = exit, somebody left the
     * conversation.
     */
    private void parseMessage(final String msg) {

        try {
            JSONObject jObj = new JSONObject(msg);

            // JSON node 'flag'
            String flag = jObj.getString("flag");

            // if flag is 'self', this JSON contains session id
            if (flag.equalsIgnoreCase(TAG_SELF)) {

                String sessionId = jObj.getString("sessionId");

                // Save the session id in shared preferences
                utils.storeSessionId(sessionId);

                Log.e(TAG, "Your session id: " + utils.getSessionId());

            } else if (flag.equalsIgnoreCase(TAG_CONVENE_REQ)) {
                // If the flag is 'conven_req' then we received a request to meet
                // a friend
                String name = jObj.getString("name");

                // alert dialog to give option to meet or not meet friend
                //or notification if paused, in background
            } else if (flag.equalsIgnoreCase(TAG_LOCATION_REQ)) {
                // if the flag is 'location request', friend suggested meeting point
                String fromName = name;
                String message = jObj.getString("message");
                String sessionId = jObj.getString("sessionId");
                String location = jObj.getString("location");
                //alert that shows friend asked to meet.
                //or notification if paused, in background
            } else if (flag.equalsIgnoreCase(TAG_CONVENE_RESPONSE)) {
                // If the flag is 'exit', somebody left the conversation
                String name = jObj.getString("name");
                String message = jObj.getString("response");
                //alert that friend responded
                //or notification if paused, in background
            } else if (flag.equalsIgnoreCase(TAG_LOCATION_RESPONSE)) {
                // If the flag is 'exit', somebody left the conversation
                String name = jObj.getString("name");
                String message = jObj.getString("response");
                //alert that friend responded
                //or notification if paused, in background
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}