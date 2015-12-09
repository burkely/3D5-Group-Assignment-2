package com.lydia.convene;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.GraphRequest;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.google.android.gms.common.GooglePlayServicesUtil.getErrorDialog;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private Context context;
    private Bitmap bitmap = null;
    private AccessTokenTracker accessTokenTracker;
    private String sessionId;
    // LogCat tag - http://developer.android.com/tools/debugging/debugging-log.html
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_SHARED_PREF = "ANDROID_WEB_CHAT";
    private static final int KEY_MODE_PRIVATE = 0;
    private SharedPreferences sharedPref;
    // Client name
    private String name;
    private String userId;
    private TextView info;
    private ListView lvFriend;
    private LoginButton loginButton;
    private Button btnSend;
    private ImageView imageView1;
    private RoundImage roundedImage;
    private WebSocketClient client;
    // The CallbackManager is used to manage the callbacks used in the app.
    private CallbackManager callbackManager;
    private Utils utils;
    private ArrayAdapter adapter;
    private ArrayList friendListArray = new ArrayList();
    private GoogleApiClient mGoogleApiClient;
    private String friendToMeet;
    private boolean mResolvingError;
    private String androidId;
    // JSON flags to identify the kind of JSON response
    private static final String TAG_SELF = "self", TAG_CONVENE_REQ = "convene?", TAG_LOCATION_REQ = "location",
            TAG_MESSAGE = "message", TAG_LOCATION_RESPONSE = "locationResponse", TAG_CONVENE_RESPONSE = "conveneResponse";

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private Map<String, Object> friendMap = new HashMap<String, Object>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Android Unique ID
        androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        //method that gets the correct release key hash for the app
        //printKeyHash();

        // Initialize the SDK before executing any other operations,
        // especially, if you're using Facebook UI elements.
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(com.lydia.convene.R.layout.activity_main);

        if (checkPlayServices()) {
            //add google play services:
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .addOnConnectionFailedListener(this)
                    .build();
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken newAccessToken) {
                updateWithToken(newAccessToken);
            }
        };

        //recover the saved state
        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        //initialize our instance of CallbackManager
        callbackManager = CallbackManager.Factory.create();

        //use findViewById to initialize the widgets
        info = (TextView) findViewById(com.lydia.convene.R.id.info);

        loginButton = (LoginButton) findViewById(com.lydia.convene.R.id.login_button);
        btnSend = (Button) findViewById(R.id.btnSend);
        lvFriend = (ListView) findViewById(R.id.lvFriend);

        //initialize imageview widget with empty profile pic
        imageView1 = (ImageView) findViewById(R.id.profileimage);

        //get user permission to access to friends list
        loginButton.setReadPermissions(Arrays.asList("user_friends"));

        //populate adapater with our array friends list
        //NOTE: simple list item 1 = Android predefined TextView resource id
        friendListArray.clear();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friendListArray);
        lvFriend = (ListView) findViewById(R.id.lvFriend);
        lvFriend.setAdapter(adapter);

        Profile profile = Profile.getCurrentProfile();
        if (profile == null) {
            imageView1.setBackgroundResource(R.mipmap.default_profile);
            friendListArray.clear();
            adapter.notifyDataSetChanged();
            info.setText("Please Log In to continue");
        } else {
            getProfileInformation();
        }

        utils = new Utils(getApplicationContext());
        //Log.d("tag", utils.toString());

        lvFriend.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view, int position, long id) {
                int friendListID = (int) id;
                friendToMeet = (lvFriend.getItemAtPosition(friendListID).toString());
            }
        });

        //create a callback to handle the results of the login attempts and
        // register it with the CallbackManager
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
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


        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sharedPref = getSharedPreferences("com.lydia.convene.storage",
                        MODE_PRIVATE);
                boolean sentToken = sharedPref
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Log.d("onRecieve BroadcastTAG", "app ready to get downstream messages");
                } else {
                    Log.d("onRecieve BroadcastTAG", "ERROrrrrrrrrrrrr");
                }
            }
        };

    }

    //for broadcast receiver
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.d("HELLO", "Hiiiiiii");
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    //To keep track of the boolean across activity restarts
    // (such as when the user rotates the screen)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    private void updateWithToken(AccessToken currentAccessToken) {
        if (currentAccessToken != null) {
            getProfileInformation();
        } else {
            //imageView1.setBackgroundResource(R.mipmap.default_profile);
            //info.setText("Please Log In");
            friendListArray.clear();
            //adapter.notifyDataSetChanged();
        }
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
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLAY_SERVICES_RESOLUTION_REQUEST) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // MAKE SURE YOU CLOSE THE SOCKET UPON EXITING
        if (client != null & client.isConnected()) {
            client.disconnect();
        }

        accessTokenTracker.stopTracking();

        sharedPref = getSharedPreferences("com.lydia.convene.storage",
                MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();

    }

    public void sendTokenToServer() {
        sharedPref = getSharedPreferences("com.lydia.convene.storage",MODE_PRIVATE);
        String token = null;
        while(token==null) {
            token = sharedPref.getString("token", null);
        }
        Log.d("TOKEN", token + "bla");

        while (userId == null) {
            //wait
        }

        String message = token + " " + userId + " " + androidId;
        sendMessageToServer(message, "tokens");
    }

    public void sendMessageToServer(String message, String flag) {
        Log.d("IN SendMessageServer", message);
        if (client != null) {
            Log.d("SENDMESSAGESERVER", "sending mesage");
            Log.d("tag flag", flag);
            String JSONMessage = utils.getSendMessageJSON(message, flag);
            client.send(JSONMessage);
        }
    }


    //simple function to convert message in byte form to hex chars
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
                Log.d("ACMAIN. PARSING JSON", "flag is self, session ID received");
                Log.d("ACMAIN parsing json", jObj.toString());
                sessionId = jObj.getString("sessionId");
                Log.d("tag", "sessionId= " + sessionId);
                // Save the session id in shared preferences
                utils.storeSessionId(sessionId);

                Log.e(TAG, "Your session id: " + utils.getSessionId());

            } else if (flag.equalsIgnoreCase(TAG_CONVENE_REQ)) {
                // If the flag is 'convene_req' then we received a request to meet
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
            } else if (flag.equalsIgnoreCase(TAG_MESSAGE)) {
                // if the flag is 'message', new message received
                String fromName = name;
                String message = jObj.getString("message");
                String sessionId = jObj.getString("sessionId");
                boolean isSelf = true;

                // Checking if the message was sent by you
                if (!sessionId.equals(utils.getSessionId())) {
                    fromName = jObj.getString("name");
                    isSelf = false;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getProfileInformation() {

        GraphRequest meRequest = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {

                            name = response.getJSONObject().getString("name").toString();
                            userId = response.getJSONObject().getString("id").toString();
                            info.setText(name + "  " + userId);

                            utils.storeMyId(userId);

                            JSONObject data = response.getJSONObject().getJSONObject("picture").getJSONObject("data");
                            String url = data.getString("url");
                            URL imageURL = new URL(url);

                            Log.d("IC URL", imageURL.toString());

                            new DownloadImageTask().execute(imageURL.toString());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id, name, picture.type(normal)");
        meRequest.setParameters(parameters);
        meRequest.executeAsync();


        //get friends list
        GraphRequest friendRequest = GraphRequest.newMyFriendsRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONArrayCallback() {
                    @Override
                    public void onCompleted(JSONArray jarray,
                                            GraphResponse response) {
                        try {
                            JSONArray jsonArray = response.getJSONObject().getJSONArray("data");
                            friendListArray.clear();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject friendObject = jsonArray.getJSONObject(i);

                                //Log.i("FRIENDDDDdata ", friendObject.toString());
                                String friendName = friendObject.getString("name");
                                String friendID = friendObject.getString("id");
                                friendMap.put(friendName, friendID);
                                friendListArray.add(friendName);
                            }

                            //repopulate listview adpater with friends list
                            adapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
        Bundle params = new Bundle();
        params.putString("fields", "id,name,friends");
        friendRequest.setParameters(params);
        friendRequest.executeAsync();

        new createSockConnection().execute();
    }

    public void sendConveneReq(View view) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        if (friendToMeet != null) {
            alertDialogBuilder
                    //set dialog message
                    .setMessage("Do you want to meet " + friendToMeet + "?")
                            //sets whether this dialog is cancelable with the back button
                    .setCancelable(true)
                    .setPositiveButton("Send Request", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked: clear alert dialog
                            dialog.cancel();
                            //then send get info and send request
                            //TODO SEND REQUEST
                            String friendFbId = friendMap.get(friendToMeet).toString();
                            sendMessageToServer(userId, TAG_CONVENE_REQ);
                        }
                    });
        } else {
            alertDialogBuilder
                    //set dialog message
                    .setMessage("Select Friend to Meet")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked: clear alert dialog
                            dialog.cancel();
                        }
                    });
        }

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();

    } //end of sendConveneReq()


    public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        protected void onPreExecute() {
        }

        protected Bitmap doInBackground(String... urls) {

            String urldisplay = urls[0];
            bitmap = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", "image download error");
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap result) {
            //set image of your imageview
            roundedImage = new RoundImage(result);
            imageView1.setBackground(roundedImage);
        }
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        Log.i("TAGOOGLE", "This device IS supported.");
        return true;
    }

    public class createSockConnection extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... params) {
            //Creating web socket client. This will have callback methods
            String firstName;

            if (name != null) {
                String arr[] = name.split(" ");
                firstName = arr[0];
            } else {
                firstName = "default";
            }

            client = new WebSocketClient(URI.create(WsConfig.URL_WEBSOCKET
                    + firstName), new WebSocketClient.Listener() {

                @Override
                public void onConnect() {
                    Log.d("ACMAIN, ONCONNECTED", "connected to client");
                    sendTokenToServer();
                }

                //On receiving the message from web socket server
                @Override
                public void onMessage(String message) {
                    Log.d("ACMAIN in ONMASSAGE", "parsing message from server");
                    parseMessage(message);

                }

                @Override
                public void onMessage(byte[] data) {
                    Log.i(TAG, String.format("Got binary message! %s",
                            bytesToHex(data)));

                    // Message will be in JSON format
                    parseMessage(bytesToHex(data));
                }

                //Called when the connection is terminated
                @Override
                public void onDisconnect(int code, String reason) {
                    // clear the session id from shared preferences
                    utils.storeSessionId(null);
                    Log.d("TAG DISCONNECT", "client disconnected");
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error! : " + error);
                    error.printStackTrace();
                }
            }, null);

            client.connect();
            return null;
        }
    }
}



  /*  void printKeyHash(){

        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.lydia.convene",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.i("Digest: ", Base64.encodeToString(md.digest(), 0));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Test", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.e("Test", e.getMessage());
        }

    }*/

