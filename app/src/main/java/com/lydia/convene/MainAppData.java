package com.lydia.convene;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Created by Lydia on 06/12/2015.
 */


public class MainAppData extends Application {

    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();

        //To read or write to SharedPreferences you first need to get/create SharedPrefs
         prefs = getSharedPreferences("com.lydia.convene.storage",
                Context.MODE_PRIVATE);

        //This is where we read current SP and populate our list of notes
    }

    public SharedPreferences getMyPrefs(){
        return prefs;
    }

}
