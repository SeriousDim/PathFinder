package ru.seriousdim.pathfinder;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    int count = 0; // variable for using with debugger

    private SensorManager manager;
    private Sensor accel, gravity, orientation, magnetic;

    private TextView    sens, errors,
                        accelData, gravityData, orienData, geomagData, clearData;

    private LineChart accelX, clearAccel;

    private String debug_info[] = new String[5];

    public float[]  accel_data = new float[3],
                    gravity_data = new float[3],
                    linear_data = new float[3],
                    orientation_data = new float[3],
                    geomag_data = new float[3];

    private Timer timer;
    private TimerTask task;
    private List<Sensor> sensors;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Отладка датчиков");

        // ------ getting sensor manager and sensors ------
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);

        accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gravity = manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        orientation = manager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        magnetic = manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);

        sensors = manager.getSensorList(Sensor.TYPE_ALL);

        // ------ getting views ------
        sens = findViewById(R.id.sensors);
        errors = findViewById(R.id.errors);
        accelData = findViewById(R.id.accelData);
        clearData = findViewById(R.id.clearData);
        orienData = findViewById(R.id.orientationData);
        gravityData = findViewById(R.id.gravityData);
        geomagData = findViewById(R.id.geomagData);

        accelX = findViewById(R.id.accelX);
        clearAccel = findViewById(R.id.clearAccel);

        // ------ getting string resources ------
        debug_info[0] = getResources().getString(R.string.accel);
        debug_info[1] = getResources().getString(R.string.clear);
        debug_info[2] = getResources().getString(R.string.gravity);
        debug_info[3] = getResources().getString(R.string.orientation);
        debug_info[4] = getResources().getString(R.string.geomag);

        // ------ settings ------
        showSensorList();
        registerListeners();

        initCharts();
    }



    @Override
    protected void onResume()
    {
        super.onResume();

        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        calcClearAcceleration(0.8f);

                        updateCharts();

                        showValues();
                    }
                });
            }
        };
        timer.schedule(task, 0, 100);
        count++;
    }



    @Override
    protected void onPause()
    {
        super.onPause();

        manager.unregisterListener(this);
        timer.cancel();
    }



    @Override
    public void onSensorChanged(SensorEvent e)
    {
        switch (e.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                for (int i=0; i<3; i++)
                    this.accel_data[i] = e.values[i];
                break;
            case Sensor.TYPE_GRAVITY:
                for (int i=0; i<3; i++)
                    this.gravity_data[i] = e.values[i];
                break;
            case Sensor.TYPE_ORIENTATION:
                for (int i=0; i<3; i++)
                    this.orientation_data[i] = e.values[i];
                break;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                for (int i=0; i<3; i++)
                    this.geomag_data[i] = e.values[i];
                break;
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }



    private void calcClearAcceleration(float alpha)
    {
        for (int i=0; i<3; i++){
            float bufA[] = accel_data.clone();
            float bufG[] = gravity_data.clone();
            float g = alpha * bufG[i] + (1 - alpha) * bufA[i];
            linear_data[i] = bufA[i] - g;
        }
    }



    private void registerListeners()
    {
        if (accel != null)
            manager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        else
            errors.append("No accelerometer found.\n");

        if (gravity != null)
            manager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_NORMAL);
        else
            errors.append("No gravity sensor found.\n");

        if (orientation != null)
            manager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_NORMAL);
        else
            errors.append("No orientation sensor found.\n");

        if (magnetic != null)
            manager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_NORMAL);
        else
            errors.append("No geomagnetic rotation sensor found.\n");
    }



    private void showSensorList()
    {
        for (Sensor s: sensors){
            sens.append(s.getName()+"\t\t"+s.getType()+"\n");
        }
    }



    private void setSensorData(TextView t, float[] f, int ind)
    {
        float buf[] = f.clone();
        t.setText(String.format(
                debug_info[ind], buf[0], buf[1], buf[2]
        ));
    }



    private void showValues()
    {
        setSensorData(accelData, accel_data, 0);
        setSensorData(clearData, linear_data, 1);
        setSensorData(gravityData, gravity_data, 2);
        setSensorData(orienData, orientation_data, 3);
        setSensorData(geomagData, geomag_data, 4);
    }



    private void initCharts(){
        // ------ init acceleration chart ------
        accelX.setBorderColor(0x18473Aff);
        accelX.setBorderWidth(3);
        accelX.setTouchEnabled(true);
        accelX.setDragEnabled(true);
        accelX.getDescription().setEnabled(false);

        XAxis x = accelX.getXAxis();
        x.enableGridDashedLine(10, 10, 0);

        YAxis y = accelX.getAxisLeft();
        y.enableGridDashedLine(10, 10, 0);
        y.setAxisMaximum(11f);
        y.setAxisMinimum(-11f);

        ArrayList<ILineDataSet> sets = initAccelChart();

        LineData data = new LineData(sets);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value+"";
            }
        });
        accelX.setData(data);

        // ------ init claer accel chart ------

    }



    private ArrayList<ILineDataSet> initAccelChart(){

        ArrayList<ILineDataSet> set = new ArrayList<>();

        LineDataSet dataX = new LineDataSet(new ArrayList<Entry>(), "X");
        LineDataSet dataY = new LineDataSet(new ArrayList<Entry>(), "Y");
        LineDataSet dataZ = new LineDataSet(new ArrayList<Entry>(), "Z");

        dataX.setDrawCircles(false);
        dataY.setDrawCircles(false);
        dataZ.setDrawCircles(false);

        dataX.setColor(Color.RED);
        dataY.setColor(Color.BLUE);
        dataZ.setColor(Color.GREEN);

        set.add(dataX);
        set.add(dataY);
        set.add(dataZ);

        return set;
    }



    private void updateCharts()
    {
        float xd = accel_data[0];
        float yd = accel_data[1];
        float zd = accel_data[2];

        LineData data = accelX.getLineData();
        ArrayList<Entry> setX = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(0))).getValues();
        ArrayList<Entry> setY = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(1))).getValues();
        ArrayList<Entry> setZ = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(2))).getValues();

        setX.add(new Entry((setX.size()-1)/10f, xd));
        setY.add(new Entry((setY.size()-1)/10f, yd));
        setZ.add(new Entry((setZ.size()-1)/10f, zd));

        ((LineDataSet) (data.getDataSetByIndex(0))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(1))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(2))).notifyDataSetChanged();

        accelX.getData().notifyDataChanged();
        accelX.notifyDataSetChanged();
        accelX.moveViewToX((float)(setX.size()-1));
    }



}
