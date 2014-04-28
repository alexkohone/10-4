package io.golgi.example.tenfour;

import android.content.Context;
import android.content.Intent;

/**
 * Created by briankelly on 10/04/2014.
 */
public class GolgiGCMBroadcastReceiver extends io.golgi.apiimpl.android.GolgiGCMBroadcastReceiver{

    public GolgiGCMBroadcastReceiver(){
        super("io.golgi.example.tenfour", "io.golgi.example.tenfour.GolgiGCMIntentService");
        DBG.write("Received PUSH(1)");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        DBG.write("Received PUSH(2)");
        GolgiService.startService(context);
        super.onReceive(context, intent);
        DBG.write("Received PUSH(3)");
    }

}

