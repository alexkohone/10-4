package io.golgi.example.tenfour;

import android.util.Log;

/**
 * Created by briankelly on 10/04/2014.
 */
public class DBG {
    public static void write(String where, String str){
        Log.i(where, str);
    }

    public static void write(String str){
        write("10-4", str);
    }

}

