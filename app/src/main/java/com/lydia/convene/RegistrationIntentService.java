package com.lydia.convene;

/**
 * Created by Lydia on 04/12/2015.
 */

import android.app.Activity;
import android.app.Application;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;



public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    private MainActivity main;

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences Pref = getSharedPreferences("com.lydia.convene.storage",
                MODE_PRIVATE);

        try {
            // Initially this call goes out to the network to retrieve the token,
            // subsequent calls are local.

            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            Log.i(TAG, "GCM Registration Token: " + token);

            //sendRegistrationToServer(token);
            Pref.edit().putString("token", token).apply();

            //Pref = getSharedPreferences("com.lydia.convene.storage", MODE_PRIVATE);
            Log.d("TOKENTAG AFTERSAVE", Pref.getString("token", null));

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server.
            Pref.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, true).apply();

        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            Pref.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false).apply();
        }
    }

    /**
     * Persist registration to third-party servers.
     * <p/>
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */

}