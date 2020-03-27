package ru.seriousdim.pathfinder.accelerometer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class AccelerometerManager implements SensorEventListener {

    private static final int BUFFER_SIZE = 500;

    protected float lastX, lastY, lastZ;
    private float dX = 0,   // calibration
                  dY = 0,
                  dZ = 0;

    // buffer vars
    private float x, y, z;
    private int cnt = 0;

    // ------- Override methods -------
    @Override
    public void onSensorChanged(SensorEvent e){
        float[] v = e.values.clone();

        float x = v[0] + dX;
        float y = v[1] + dY;
        float z = v[2] + dZ;

        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;

        this.x = x;
        this.y = y;
        this.z = z;

        if (cnt < BUFFER_SIZE-1){
            cnt++;
        } else {
            reset();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    // returns last SensorEvent params
    public Point getLastPoint(){
        return new Point(lastX, lastY, lastZ, 1);
    }

    // returns params, using buffer: average acceleration
    // since last getPoint() call
    public Point getPoint(){
        if (this.cnt == 0){
            return new Point(lastX, lastY, lastZ, 1);
        }

        Point p = new Point(this.x, this.y, this.z, this.cnt);

        reset();
        return p;
    }

    //resets buffer vars
    public void reset(){
        cnt = 0;
        x = y = z = 0;
    }

    // set dX, dY and dZ values
    public  void setdX(float dX) {
        this.dX = dX;
    }

    public  void setdY(float dY) {
        this.dY = dY;
    }

    public  void setdZ(float dZ) {
        this.dZ = dZ;
    }

}
