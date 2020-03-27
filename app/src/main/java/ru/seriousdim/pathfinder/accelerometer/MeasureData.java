package ru.seriousdim.pathfinder.accelerometer;

import java.util.LinkedList;

public class MeasureData {

    private LinkedList<Point> accelData;
    private LinkedList<MeasurePoint> data;

    private long interval;

    public MeasureData(long interval){
        this.interval = interval;
        accelData = new LinkedList<>();
        data = new LinkedList<>();
    }

    public void addPoint(Point p){
        accelData.add(p);
    }

    public void process(){
        for (int i=0; i<accelData.size(); i++){
            Point p = accelData.get(i);
            float speed = 0;

            if (i > 0){
                speed = data.get(i-1).getSpeedAfter();
            }

            data.add(new MeasurePoint(p.getX(), p.getY(), p.getZ(), speed, interval));
        }
    }

    public float getLastSpeed(){
        return data.getLast().getSpeedAfter();
    }

}
