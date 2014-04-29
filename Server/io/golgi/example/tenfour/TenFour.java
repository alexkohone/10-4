package io.golgi.example.tenfour;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.color.CMMException;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import com.openmindnetworks.golgi.api.GolgiAPI;
import com.openmindnetworks.golgi.api.GolgiAPIHandler;
import com.openmindnetworks.golgi.api.GolgiAPINetworkImpl;
import com.openmindnetworks.golgi.api.GolgiException;
import com.openmindnetworks.golgi.api.GolgiTransportOptions;
import com.openmindnetworks.slingshot.ntl.NTL;

import io.golgi.example.tenfour.gen.*;
import io.golgi.example.tenfour.gen.TenFourService.*;


public class TenFour extends Thread implements GolgiAPIHandler{
    private Object syncObj = new Object();
    private String devKey = null;
    private String appKey = null;
    private GolgiTransportOptions stdGto;
    private GolgiTransportOptions hourGto;
    private GolgiTransportOptions dayGto;
    private Hashtable<String,RadioDevice> deviceHash = new Hashtable<String,RadioDevice>();
    
	private register.RequestReceiver inboundRegister = new register.RequestReceiver() {
        @Override
        public void receiveFrom(register.ResultSender resultSender, RadioDevice device) {
            System.out.println("Register from: '" + device.getDevId() + "' on channel " + device.getChannel());
            synchronized(syncObj){
                deviceHash.remove(device.getDevId());
                deviceHash.put(device.getDevId(), device);
            }
            CellId cellId = new CellId();
            cellId.setId("XXX");
            resultSender.success(cellId);
        }
	};
	
	private unregister.RequestReceiver inboundUnregister = new unregister.RequestReceiver() {
        @Override
        public void receiveFrom(unregister.ResultSender resultSender, RadioDevice device) {
            System.out.println("Unregister from: '" + device.getDevId() + "' on channel " + device.getChannel());
            synchronized(syncObj){
                deviceHash.remove(device.getDevId());
            }
            resultSender.success();
        }
	};
	
	
	private uploadPacket.RequestReceiver inboundUploadPacket = new uploadPacket.RequestReceiver() {
        
        @Override
        public void receiveFrom(uploadPacket.ResultSender resultSender, VoxPacket pkt) {
            resultSender.success();
            System.out.println("Voice Packet arrived on channel " + pkt.getChannel());
            synchronized(syncObj){
                for(Enumeration<String> e = deviceHash.keys(); e.hasMoreElements();){
                    RadioDevice d = deviceHash.get(e.nextElement());
                    if(d.getChannel() == pkt.getChannel()){
                        broadcastPacket.sendTo(new broadcastPacket.ResultReceiver(){
                            @Override
                            public void success() {
                                System.out.println("broadcastPacket(): OK");
                            }

                            @Override
                            public void failure(GolgiException ex) {
                                System.out.println("Failed to send: " + ex.getErrText());
                            }
                        
                        }, stdGto, d.getDevId(), pkt);
                    }
                }
            }
        }
    };
	
    private void looper(){
        Class<GolgiAPI> apiRef = GolgiAPI.class;
        GolgiAPINetworkImpl impl = new GolgiAPINetworkImpl();
        GolgiAPI.setAPIImpl(impl);
        stdGto = new GolgiTransportOptions();
        stdGto.setValidityPeriod(60);

        hourGto = new GolgiTransportOptions();
        hourGto.setValidityPeriod(3600);

        dayGto = new GolgiTransportOptions();
        dayGto.setValidityPeriod(86400);

        register.registerReceiver(inboundRegister);
        unregister.registerReceiver(inboundUnregister);
        uploadPacket.registerReceiver(inboundUploadPacket);
        
        GolgiAPI.register(devKey,
                          appKey,
                          "SERVER",
                          this);
        
        while(true){
        	synchronized(this){
        		try{
        			this.wait(1000);
        		}
        		catch(InterruptedException iex){
        		}
        	}
        }
    }
	
	public TenFour(String[] args){
        for(int i = 0; i < args.length; i++){
        	if(args[i].compareTo("-devKey") == 0){
        		devKey = args[i+1];
        		i++;
        	}
        	else if(args[i].compareTo("-appKey") == 0){
        		appKey = args[i+1];
        		i++;
        	}
        	else{
        		System.err.println("Zoikes, unrecognised option '" + args[i] + "'");
        		System.exit(-1);;
        	}
        }
        if(devKey == null){
        	System.out.println("No -devKey specified");
        	System.exit(-1);
        }
        else if(appKey == null){
        	System.out.println("No -appKey specified");
        	System.exit(-1);
        }
    }
	
	public static void main(String[] args) {
		new TenFour(args).looper();
	}

	@Override
	public void registerFailure() {
		System.out.println("Golgi Registration FAILED");
		System.exit(-1);
		
	}

	@Override
	public void registerSuccess() {
		System.out.println("Golgi Registration OK");
		
	}
}