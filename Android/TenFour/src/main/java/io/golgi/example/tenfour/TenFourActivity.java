package io.golgi.example.tenfour;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.openmindnetworks.golgi.api.GolgiAPI;
import com.openmindnetworks.golgi.api.GolgiException;
import com.openmindnetworks.golgi.api.GolgiTransportOptions;

import java.util.Timer;
import java.util.TimerTask;

import io.golgi.example.tenfour.gen.CellId;
import io.golgi.example.tenfour.gen.RadioDevice;
import io.golgi.example.tenfour.gen.TenFourService.*;
import io.golgi.example.tenfour.gen.TenFourService;
import io.golgi.example.tenfour.gen.VoxPacket;


public class TenFourActivity extends ActionBarActivity {
    SharedPreferences sharedPrefs;
    private static TenFourActivity theActivity = null;
    private boolean golgiReady = false;
    private boolean inFg = false;
    private String ourId;
    private GolgiTransportOptions stdGto;
    private int[] digitIds = {
            R.drawable.led_0,
            R.drawable.led_1,
            R.drawable.led_2,
            R.drawable.led_3,
            R.drawable.led_4,
            R.drawable.led_5,
            R.drawable.led_6,
            R.drawable.led_7,
            R.drawable.led_8,
            R.drawable.led_9,
    };
    private ImageView tensIv;
    private ImageView unitsIv;
    private Recorder currentRecorder;
    private Timer refreshTimer;
    private SignalMeter signalMeter;


    private Handler serviceStartedHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            DBG.write("serviceStartedHandler()");
            if(msg.what != 0) {
                golgiReady = true;
                if(GolgiService.getOnlineState(TenFourActivity.this)) {
                    registerOnChannel(GolgiService.getChannel(TenFourActivity.this));
                }
                else{
                    unregisterDevice();
                }
            }
            else{
                Toast.makeText(TenFourActivity.this, "Failed to start Golgi Service", Toast.LENGTH_LONG).show();
            }
        }
    };

    protected void registerOnChannel(int ch) {
        if (golgiReady) {
            RadioDevice dev = new RadioDevice();

            dev.setChannel(ch);
            dev.setDevId(ourId);
            dev.setLat(0.0);
            dev.setLng(0.0);

            register.sendTo(new register.ResultReceiver() {
                                @Override
                                public void failure(GolgiException ex) {

                                }

                                @Override
                                public void success(CellId result) {

                                }
                            },
                    stdGto,
                    "SERVER",
                    dev
            );
        }
    }

    protected void unregisterDevice() {
        if (golgiReady) {
            RadioDevice dev = new RadioDevice();

            dev.setChannel(GolgiService.getChannel(this));
            dev.setDevId(ourId);
            dev.setLat(0.0);
            dev.setLng(0.0);

            unregister.sendTo(new unregister.ResultReceiver() {
                                @Override
                                public void failure(GolgiException ex) {
                                    DBG.write("Device unregister failure: " + ex.getErrText());
                                }

                                @Override
                                public void success() {
                                    DBG.write("Device unregistered OK");
                                }
                            },
                    stdGto,
                    "SERVER",
                    dev
            );
        }
    }


    protected static void golgiServiceStarted(boolean success){
        DBG.write("golgiServiceStarted: " + success);
        if(theActivity != null && theActivity.serviceStartedHandler != null) {
            theActivity.serviceStartedHandler.sendEmptyMessage(success ? 1 : 0);
        }
    }


    /*
    private updateComplete.RequestReceiver inboundUpdateComplete = new updateComplete.RequestReceiver(){

        @Override
        public void receiveFrom(updateComplete.ResultSender resultSender, int hack){
            DBG.write("********************* update complete");
            resultSender.success();
            allTilesLoadedFromThread(1);
        }
    };
    */


    private Handler redrawHandler = new Handler(){
        public void handleMessage(Message msg){
            if(inFg){
                if(signalMeter != null) {
                    signalMeter.setSignalLevel(GolgiService.isPlayingAudio() ? 100 : 0);
                    signalMeter.invalidate();
                }
            }
        }
    };

    private void startRefreshTimer(){
        signalMeter = (SignalMeter)findViewById(R.id.signalMeter);
        if(refreshTimer == null){
            DBG.write("Starting refreshTimer()");
            refreshTimer = new Timer();
            refreshTimer.schedule(new TimerTask(){
                @Override
                public void run() {
                    redrawHandler.sendEmptyMessage(0);
                }
            }, 20, 20);
        }
    }

    private void stopRefreshTimer(){
        if(refreshTimer != null){
            DBG.write("Stopping refreshTimer()");
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }




    private void setVisibility(){
        boolean online = GolgiService.getOnlineState(this);
        int v = (online) ? View.VISIBLE : View.INVISIBLE;
        findViewById(R.id.channelLinearLayout).setVisibility(v);
        findViewById(R.id.talkTextView).setVisibility(v);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theActivity = this;
        setContentView(R.layout.activity_ten_four);
        ourId = GolgiService.getGolgiId(this);
        stdGto = new GolgiTransportOptions();
        stdGto.setValidityPeriod(60);

    }

    @Override
    protected void onDestroy(){
        if(theActivity == this){
            theActivity = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ten_four, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        DBG.write("onResume()");
        super.onResume();
        GolgiAPI.usePersistentConnection();
        inFg = true;

        GolgiService.startService(this);

        startRefreshTimer();

        setVisibility();
        CheckBox cb = (CheckBox)findViewById(R.id.onlineCheckBox);
        cb.setChecked(GolgiService.getOnlineState(this));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                GolgiService.setOnlineState(TenFourActivity.this, b);
                setVisibility();
                if (GolgiService.getOnlineState(TenFourActivity.this)) {
                    registerOnChannel(GolgiService.getChannel(TenFourActivity.this));
                } else {
                    unregisterDevice();
                }
            }
        });

        tensIv = (ImageView)this.findViewById(R.id.tensImageView);
        unitsIv = (ImageView)this.findViewById(R.id.unitsImageView);

        SeekBar sb = (SeekBar)this.findViewById(R.id.channelSeekBar);

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int channel = progress + 1;
                DBG.write("Channel: " + channel);
                tensIv.setImageResource(digitIds[channel / 10]);
                unitsIv.setImageResource(digitIds[channel % 10]);
                GolgiService.setChannel(TenFourActivity.this, channel);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                DBG.write("Ok, register on channel " + GolgiService.getChannel(TenFourActivity.this));
                registerOnChannel(GolgiService.getChannel(TenFourActivity.this));
            }
        });
        sb.setProgress(GolgiService.getChannel(this) - 1);

        TextView talkButton = (TextView)findViewById(R.id.talkTextView);
        talkButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if(action == MotionEvent.ACTION_DOWN){
                    DBG.write("TALK");
                    v.setBackgroundResource(R.drawable.green_button_pressed);
                    currentRecorder = new Recorder();
                }
                else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
                    v.setBackgroundResource(R.drawable.green_button);
                    currentRecorder.stopRecording();
                    DBG.write("We got " + currentRecorder.getAudioData().length + " samples at " + currentRecorder.getRate() + " samples per second");
                    // GolgiService.playAudio(currentRecorder.getAudioData());
                    short[] audio = currentRecorder.getAudioData();
                    byte[] voxData = new byte[audio.length * 2];
                    VoxPacket vp = new VoxPacket();
                    vp.setDevId(ourId);
                    vp.setChannel(GolgiService.getChannel(TenFourActivity.this));
                    vp.setMsgId("" + System.currentTimeMillis());
                    vp.setPktMax(1);
                    vp.setPktNum(1);

                    int j = 0;
                    for(int i = 0; i < audio.length; i++){
                        short sval = audio[i];
                        voxData[j++] = (byte)(sval & 0xff);
                        voxData[j++] = (byte)((sval >> 8)& 0xff);
                    }

                    vp.setVoxData(voxData);

                    uploadPacket.sendTo(new uploadPacket.ResultReceiver() {
                        @Override
                        public void failure(GolgiException ex) {
                            DBG.write("uploadPacket failed: " + ex.getErrText());
                        }

                        @Override
                        public void success() {
                            DBG.write("uploadPacket success");
                        }
                    },
                            stdGto,
                            "SERVER",
                            vp);


                    {
                        int rate = 16000;
                        short[] audioData = new short[(rate * 250)/1000];
                        for(int i = 0; i < audioData.length; i++){
                            audioData[i] = (short)((Math.random() * 2000) - 1000.0);
                        }
                        GolgiService.playAudio(audioData);
                    }

                }
                return true;
            }
        });

    }

    @Override
    public void onPause(){
        DBG.write("onPause()");
        super.onPause();
        inFg = false;
        GolgiAPI.useEphemeralConnection();
        stopRefreshTimer();
    }



}
