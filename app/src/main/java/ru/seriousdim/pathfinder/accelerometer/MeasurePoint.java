package ru.seriousdim.pathfinder.accelerometer;

public class MeasurePoint {

    private float x, y, z;
    private float speedBefore, speedAfter;
    private float distance;
    private float acceleration;
    private long interval;

    public MeasurePoint(float x, float y, float z, float speedBefore, long interval) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.speedBefore = speedBefore;
        this.interval = interval;
        this.speedAfter = 0;
        calc();
    }

    private void calc(){
        this.acceleration = (float)Math.sqrt(this.x*this.x + this.y*this.y* + this.z*this.z);
        float t = ((float)interval/1000f);
        speedAfter = speedBefore + this.acceleration * t;
        distance = speedBefore * t + acceleration * t * t * 0.5f;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getSpeedBefore() {
        return speedBefore;
    }

    public float getSpeedAfter() {
        return speedAfter;
    }

    public float getDistance() {
        return distance;
    }

    public float getAcceleration() {
        return acceleration;
    }

    public long getInterval() {
        return interval;
    }
}
