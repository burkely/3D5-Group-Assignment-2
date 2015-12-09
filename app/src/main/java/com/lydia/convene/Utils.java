package com.lydia.convene;

/**
 * Created by Lydia on 13/11/2015.
 */


import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Utils {

    private Context context;
    private SharedPreferences sharedPref;

    private static final String KEY_SESSION_ID = "sessionId", KEY_USER_ID = "fromUserID";

    private static final String TAG_SELF = "self", TAG_CONVENE_REQ = "convene?", TAG_LOCATION_REQ = "location",
            TAG_MESSAGE = "message", TAG_LOCATION_RESPONSE = "locationResponse", TAG_CONVENE_RESPONSE = "conveneResponse";


    public Utils(Context context) {
        this.context = context;
        sharedPref = context.getSharedPreferences("com.lydia.convene.storage",
                Context.MODE_PRIVATE);

    }

    public void storeSessionId(String sessionId) {

        Editor editor = sharedPref.edit();
        editor.clear();
        editor.putString(KEY_SESSION_ID, sessionId);
        editor.commit();
    }

    public void storeMyId(String UserId){

        Editor editor = sharedPref.edit();
        editor.clear();
        editor.putString(KEY_USER_ID, UserId);
        editor.commit();
    }

    public String getSessionId() {
        return sharedPref.getString(KEY_SESSION_ID, null);
    }

    public String getMyId(){ return sharedPref.getString(KEY_USER_ID, null);}

    public String getSendMessageJSON(String message, String flag) {
        String json = null;
        Log.d("In getsendmessage", flag);
        try {
            JSONObject jObj = new JSONObject();
            jObj.put("flag", flag);
            jObj.put("sessionId", getSessionId());
            jObj.put("message", message);

            json = jObj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }


}