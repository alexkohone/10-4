package io.golgi.example.tenfour;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by brian on 11/04/2014.
 */
public class SignalMeter extends View{
    Paint paint = new Paint();
    int actualSignalLevel = 100;
    double velocity = 0.0;
    double signalLevel = 0.0;


    public void setSignalLevel(int v){
        if(v < 0){
            v = 0;
        }
        else if(v > 100){
            v = 100;
        }
        actualSignalLevel = v;
    }


    @SuppressLint("WrongCall")

    protected void onDraw(Canvas canvas) {
        int xc = getWidth() / 2;
        int yc = (getHeight()  * 135) / 100;
        int n2 = (yc * 90) / 100;
        double theta;

        if((int)signalLevel != actualSignalLevel){
            if(signalLevel < actualSignalLevel){
                if(velocity < 0){
                    velocity += 0.5;
                }
                velocity += 0.1;
                if(velocity > 4.0){
                    velocity = 4.0;
                }
            }
            else{
                if(velocity > 0){
                    velocity -= 0.5;
                }
                velocity -= 0.1;
                if(velocity < -4.0){
                    velocity = -4.0;
                }
            }
            signalLevel += velocity;
            if(signalLevel < 0){
                signalLevel = 0;
                velocity = 0;
            }
            else if(signalLevel > 110){
                signalLevel = 110;
                velocity = 0;
            }
        }
        else{
            signalLevel = actualSignalLevel;
        }

        // DBG.write("onDraw()");
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(10.0f);

        theta = 45.0 + (80.0 * signalLevel) / 100.0;
        theta = (Math.PI * theta) / 180.0;

        int x1 = (int) (xc - Math.cos(theta) * (double) n2);
        int y1 = (int) (yc - Math.sin(theta) * (double) n2);

        canvas.drawLine(xc, yc, x1, y1, paint);
    }

    public SignalMeter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public SignalMeter(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public SignalMeter(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

}
