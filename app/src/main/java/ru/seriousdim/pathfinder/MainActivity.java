package ru.seriousdim.pathfinder;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /*
        TODO: посмотреть линейный акселерометр
        TODO: посмотреть вычислительный фильтр
        TODO: TYPE_ORIENTATION устрел, использовать TYPE_GYROSCOPE
     */

    private SensorManager manager;
    private Sensor accel, linear, gyro, gravity;

    private TextView accel_x, accel_y, accel_z;
    private TextView gyro_a, gyro_t, gyro_k;
    private TextView gr_x, gr_y, gr_z;
    private TextView real_x, real_y, real_z;
    private TextView sens;

    public float[] accel_data = new float[3];
    public float[] gyro_data = new float[3];
    public float[] gravity_data = new float[3];
    public float[] linear_data = new float[3];

    private Timer timer;
    private TimerTask task;

    private List<Sensor> sensors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Sensor debug window");

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);

        accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gravity = manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        linear = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        sensors = manager.getSensorList(Sensor.TYPE_ALL);

        accel_x = findViewById(R.id.accel_x);
        accel_y = findViewById(R.id.accel_y);
        accel_z = findViewById(R.id.accel_z);

        gyro_a = findViewById(R.id.gyro_a);
        gyro_t = findViewById(R.id.gyro_t);
        gyro_k = findViewById(R.id.gyro_k);

        gr_x = findViewById(R.id.grav_x);
        gr_y = findViewById(R.id.grav_y);
        gr_z = findViewById(R.id.grav_z);

        real_x = findViewById(R.id.real_x);
        real_y = findViewById(R.id.real_y);
        real_z = findViewById(R.id.real_z);

        sens = findViewById(R.id.sensors);

        for (Sensor s: sensors){
            sens.append(s.getName()+" | "+s.getType()+"\n");
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        if (accel != null)
            manager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        else
            ((TextView)findViewById(R.id.accel_label)).setTextColor(getResources().getColor(android.R.color.holo_red_light));

        if (gyro != null)
            manager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
        else
            ((TextView)findViewById(R.id.gyro_label)).setTextColor(getResources().getColor(android.R.color.holo_red_light));

        if (gravity != null)
            manager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_NORMAL);
        else
            ((TextView)findViewById(R.id.gravity_label)).setTextColor(getResources().getColor(android.R.color.holo_red_light));

        if (linear != null)
            manager.registerListener(this, linear, SensorManager.SENSOR_DELAY_NORMAL);
        else
            ((TextView)findViewById(R.id.real_label)).setTextColor(getResources().getColor(android.R.color.holo_red_light));

        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        accel_x.setText(accel_data[0]+"");
                        accel_y.setText(accel_data[1]+"");
                        accel_z.setText(accel_data[2]+"");

                        gyro_a.setText(gyro_data[0]+"");
                        gyro_t.setText(gyro_data[1]+"");
                        gyro_k.setText(gyro_data[2]+"");

                        gr_x.setText(gravity_data[0]+"");
                        gr_y.setText(gravity_data[1]+"");
                        gr_z.setText(gravity_data[2]+"");

                        real_x.setText(linear_data[0]+"");
                        real_y.setText(linear_data[1]+"");
                        real_z.setText(linear_data[2]+"");
                    }
                });
            }
        };
        timer.schedule(task, 0, 100);
    }

    @Override
    protected void onPause(){
        super.onPause();

        manager.unregisterListener(this);
        timer.cancel();
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        switch (e.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                for (int i=0; i<3; i++)
                    this.accel_data[i] = e.values[i];
                break;
            case Sensor.TYPE_GYROSCOPE:
                for (int i=0; i<3; i++)
                    this.gyro_data[i] = e.values[i];
                break;
            case Sensor.TYPE_GRAVITY:
                for (int i=0; i<3; i++)
                    this.gravity_data[i] = e.values[i];
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                for (int i=0; i<3; i++)
                    this.linear_data[i] = e.values[i];
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
