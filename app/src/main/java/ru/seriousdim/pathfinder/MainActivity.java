package ru.seriousdim.pathfinder;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    int count = 0; // variable for using with debugger

    /** Boolean variables */
    private boolean moveAccel = true,
                    moveClear = true;
    private boolean started = false;

    /** BigDecimal constants */
    private BigDecimal ONE = BigDecimal.ONE;
    private BigDecimal THOUSAND = new BigDecimal("1000");
    private BigDecimal KA = new BigDecimal("0.03"); // Kalman filter coefficient for accelerometer
    private BigDecimal KG = new BigDecimal("0.015");
    private BigDecimal KC = new BigDecimal("0.015"); // Kalman coefficient for filtering clear acceleration
    private BigDecimal VELOCITY_THRESHOLD = new BigDecimal("0.001");
    private BigDecimal GRAVITY_EARTH = new BigDecimal("9.80665");

    /** Time calculation for velocity */
    private long lastTime;
    private long time; // interval (difference) in ms (System.currentTimeMillis())

    /** Color constants */
    private int COLOR_GREEN = 0xff32CD32,
                COLOR_PINK = 0xffFF69B4,
                COLOR_CYAN = 0xff40E0D0;

    /** Sensors */
    private SensorManager manager;
    private Sensor accel, gravity, orientation, magnetic;

    /** GUI variables */
    private TextView    sens, errors,
                        accelData, gravityData, orienData, geomagData, clearData, velData, calibData, accelActual;

    private Button accelStop, clearStop, calibrateButton;

    private EditText value;

    /** Chart views */
    private LineChart accelX, clearAccel;

    /** Debug template strings */
    private String debug_info[] = new String[7];

    /** Sensor data */
    public BigDecimal[] accel_data = new BigDecimal[3],
                        gravity_data = new BigDecimal[3],
                        linear_data = new BigDecimal[3],
                        opt_accel_data = new BigDecimal[3],
                        velocity = new BigDecimal[3],
                        opt_linear_data = new BigDecimal[3],
                        calib_data = new BigDecimal[3],
                        actual_accel = new BigDecimal[3];

    public float[]  orientation_data = new float[3],
                    geomag_data = new float[3];


    /** Other variables */
    private Timer timer;
    private TimerTask task;
    private List<Sensor> sensors;

    /** Interval to get new values */
    private int updateTime = 100; // in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Отладка датчиков");

        /** init some BigDecimal arrays and values */
        for (int i=0; i<3; i++) {
            accel_data[i] = BigDecimal.ZERO;
            gravity_data[i] = BigDecimal.ZERO;
            linear_data[i] = BigDecimal.ZERO;
            opt_linear_data[i] = BigDecimal.ZERO;
            opt_accel_data[i] = BigDecimal.ZERO;
            velocity[i] = BigDecimal.ZERO;
        }

        lastTime = System.currentTimeMillis();

        /** getting sensor manager and sensors */
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);

        accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gravity = manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        orientation = manager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        magnetic = manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);

        sensors = manager.getSensorList(Sensor.TYPE_ALL);

        /** getting views and setting click listeners */
        sens = findViewById(R.id.sensors);
        errors = findViewById(R.id.errors);
        accelData = findViewById(R.id.accelData);
        clearData = findViewById(R.id.clearData);
        orienData = findViewById(R.id.orientationData);
        gravityData = findViewById(R.id.gravityData);
        geomagData = findViewById(R.id.geomagData);
        velData = findViewById(R.id.velData);
        calibData = findViewById(R.id.calibData);
        accelActual = findViewById(R.id.accelActual);

        accelStop = findViewById(R.id.accelStop);
        clearStop = findViewById(R.id.clearStop);
        calibrateButton = findViewById(R.id.calibrate);

        accelStop.setOnClickListener((e) -> {
            moveAccel = !moveAccel;
            accelStop.setText(moveAccel ? "Стоп" : "Двигать");
        });

        clearStop.setOnClickListener((e) -> {
            moveClear = !moveClear;
            clearStop.setText(moveClear ? "Стоп" : "Двигать");
        });

        calibrateButton.setOnClickListener((e) -> {
            startCalibrate();
        });

        accelX = findViewById(R.id.accelX);
        clearAccel = findViewById(R.id.clearAccel);

        value = findViewById(R.id.kvalue);
        listenEditText(this);

        /** getting string resources **/
        debug_info[0] = getResources().getString(R.string.accel);
        debug_info[1] = getResources().getString(R.string.clear);
        debug_info[2] = getResources().getString(R.string.gravity);
        debug_info[3] = getResources().getString(R.string.orientation);
        debug_info[4] = getResources().getString(R.string.geomag);
        debug_info[5] = getResources().getString(R.string.velocity);
        debug_info[6] = getResources().getString(R.string.calibration);
        debug_info[7] = getResources().getString(R.string.accelActual);

        /** settings */
        showSensorList();
        registerListeners(SensorManager.SENSOR_DELAY_NORMAL);

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
                        long tn = System.currentTimeMillis();
                        time = tn - lastTime;
                        lastTime = tn;

                        setActualAccelData();
                        calcClearAcceleration(0.01f);

                        updateCharts();
                        showValues();
                    }
                });
            }
        };
        timer.schedule(task, 0, updateTime);
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
        float[] v = e.values.clone();
        switch (e.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                for (int i=0; i<3; i++) {
                    this.accel_data[i] = new BigDecimal(Float.toString(v[i])).setScale(3, BigDecimal.ROUND_HALF_UP);
                    if (!started) {
                        this.opt_accel_data[i] = this.accel_data[i];
                    } else
                        this.opt_accel_data[i] = this.accel_data[i].multiply(KA).add(
                                                    opt_accel_data[i].multiply(
                                                        ONE.subtract(KA)));
                }
                break;
            case Sensor.TYPE_GRAVITY:
                for (int i=0; i<3; i++){
                    this.gravity_data[i] = new BigDecimal(Float.toString(v[i])).setScale(3, BigDecimal.ROUND_HALF_UP);
                    /*if (!started)
                        this.gravity_data[i] = v[i];
                    else
                        this.gravity_data[i] = KG * v[i] + (1 - KG) * gravity_data[i];*/
                }
                break;
            case Sensor.TYPE_ORIENTATION:
                for (int i=0; i<3; i++)
                    this.orientation_data[i] = v[i];
                break;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                for (int i=0; i<3; i++)
                    this.geomag_data[i] = v[i];
                break;
        }
        if (!started) started = true;
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }



    private void startCalibrate(){
        BigDecimal[] ac = accel_data.clone();
        calib_data[0] = ac[0].negate();
        calib_data[1] = ac[1].negate();
        calib_data[2] = GRAVITY_EARTH.subtract(ac[2]);
        Toast.makeText(this, "Откалибровано", Toast.LENGTH_SHORT).show();
    }



    private void setActualAccelData(){
        BigDecimal[] ac = accel_data.clone();
        BigDecimal[] cal = calib_data.clone();
        for (int i=0; i<3; i++)
            actual_accel[i] = ac[i].add(cal[i]);
    }



    private void calcClearAcceleration(float a)
    {
        BigDecimal alpha = new BigDecimal(Float.toString(a));
        BigDecimal bufA[] = accel_data.clone();
        BigDecimal bufG[] = gravity_data.clone();
        for (int i=0; i<3; i++){
            BigDecimal g = bufG[i].multiply(alpha)
                                .add(
                                    bufA[i].multiply(
                                        ONE.subtract(alpha)
                                    )
                                );
            linear_data[i] = bufA[i].subtract(g);
            if (!started) {
                this.opt_linear_data[i] = linear_data[i];
            } else
                this.opt_linear_data[i] = linear_data[i].multiply(KC) // KC * linear_data[i] + (1 - KC) * opt_linear_data[i]
                                            .add(
                                                    opt_linear_data[i].multiply(
                                                            ONE.subtract(KC)
                                                    )
                                            );
        }
        calcVelocity();
    }



    private void calcVelocity(){
        for (int i=0; i<3; i++) {
            //if (velocity[i].abs().compareTo(VELOCITY_THRESHOLD) == 1){
                velocity[i] = velocity[i].add( //velocity[i] + (opt_)linear_data[i] * ((float)(time) / 1000f)
                        opt_linear_data[i].multiply(
                                getTimeInSec()
                        )
                );
            //}
        }
        setSensorData(velData, velocity, 5);
    }



    private void listenEditText(MainActivity a){
        value.setText(KC+"");
        value.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                KC = new BigDecimal(s.toString());
                Toast.makeText(a, "KC = "+KC.floatValue(), Toast.LENGTH_LONG).show();
            }
        });
    }



    private void registerListeners(int delay)
    {
        if (accel != null)
            manager.registerListener(this, accel, delay);
        else
            errors.append("No accelerometer found.\n");

        if (gravity != null)
            manager.registerListener(this, gravity, delay);
        else
            errors.append("No gravity sensor found.\n");

        if (orientation != null)
            manager.registerListener(this, orientation, delay);
        else
            errors.append("No orientation sensor found.\n");

        if (magnetic != null)
            manager.registerListener(this, magnetic, delay);
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



    private void setSensorData(TextView t, BigDecimal[] f, int ind)
    {
        BigDecimal buf[] = f.clone();
        t.setText(String.format(
                debug_info[ind], buf[0].toPlainString(), buf[1].toPlainString(), buf[2].toPlainString()
        ));
    }



    private void showValues()
    {
        setSensorData(accelData, accel_data, 0);
        setSensorData(clearData, linear_data, 1);
        setSensorData(gravityData, gravity_data, 2);
        setSensorData(orienData, orientation_data, 3);
        setSensorData(geomagData, geomag_data, 4);
        setSensorData(calibData, calib_data, 6);
        setSensorData(accelActual, actual_accel, 7);
    }



    private BigDecimal getTimeInSec(){
        float val = ((float)(time))/1000f;
        return new BigDecimal(Float.toString(val));
    }



    private void initCharts(){
        // ------ init acceleration chart ------
        accelX.setBorderColor(0x18473Aff);
        accelX.setBorderWidth(3);
        accelX.setDragEnabled(true);
        accelX.setScaleEnabled(true);
        accelX.getDescription().setEnabled(false);

        XAxis x = accelX.getXAxis();
        x.enableGridDashedLine(10, 10, 0);

        YAxis y = accelX.getAxisLeft();
        y.enableGridDashedLine(10, 10, 0);
//        y.setAxisMaximum(11f);
//        y.setAxisMinimum(-11f);

        ArrayList<ILineDataSet> sets = initDataSets(
                new String[]{"X", "Y", "Z", "kX", "kY", "kZ"},
                new int[]{COLOR_PINK, COLOR_CYAN, Color.GREEN, Color.RED, Color.BLUE, COLOR_GREEN}
        );

        LineData data = new LineData(sets);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value+"";
            }
        });
        accelX.setData(data);

        // ------ init clear acceleration chart ------
        clearAccel.setBorderColor(0x18473Aff);
        clearAccel.setBorderWidth(3);
        clearAccel.setTouchEnabled(true);
        clearAccel.setDragEnabled(true);
        clearAccel.getDescription().setEnabled(false);

        x = null;
        x = clearAccel.getXAxis();
        x.enableGridDashedLine(10, 10, 0);

        y = null;
        y = clearAccel.getAxisLeft();
        y.enableGridDashedLine(10, 10, 0);
//        y.setAxisMaximum(11f);
//        y.setAxisMinimum(-11f);

        sets = null;
        sets = initDataSets(
                new String[]{"X", "Y", "Z", "cX", "cY", "cZ"},
                new int[]{COLOR_PINK, COLOR_CYAN, Color.GREEN, Color.RED, Color.BLUE, COLOR_GREEN}
        );

        data = null;
        data = new LineData(sets);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value+"";
            }
        });
        clearAccel.setData(data);
    }



    private ArrayList<ILineDataSet> initDataSets(String[] s, int[] c){

        ArrayList<ILineDataSet> set = new ArrayList<>();

        for (int i = 0; i < s.length; i++){
            LineDataSet dataX = new LineDataSet(new ArrayList<Entry>(), s[i]);

            dataX.setDrawCircles(false);
            dataX.setColor(c[i]);

            set.add(dataX);
        }

        return set;
    }



    private void updateCharts()
    {
        BigDecimal xd, yd, zd;

        // ------ update acceleration chart ------
        BigDecimal buf[] = accel_data.clone();

        LineData data = accelX.getLineData();
        ArrayList<Entry> setX = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(0))).getValues();
        ArrayList<Entry> setY = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(1))).getValues();
        ArrayList<Entry> setZ = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(2))).getValues();
        ArrayList<Entry> setkX = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(3))).getValues();
        ArrayList<Entry> setkY = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(4))).getValues();
        ArrayList<Entry> setkZ = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(5))).getValues();

        xd = buf[0];
        yd = buf[1];
        zd = buf[2];

        setX.add(new Entry((setX.size()-1)/10f, xd.floatValue()));
        setY.add(new Entry((setY.size()-1)/10f, yd.floatValue()));
        setZ.add(new Entry((setZ.size()-1)/10f, zd.floatValue()));

        buf = opt_accel_data.clone();
        xd = buf[0];
        yd = buf[1];
        zd = buf[2];

        setkX.add(new Entry((setX.size()-1)/10f, xd.floatValue()));
        setkY.add(new Entry((setY.size()-1)/10f, yd.floatValue()));
        setkZ.add(new Entry((setZ.size()-1)/10f, zd.floatValue()));

        /*if (setX.size() > 500){
            setX.remove(0);
            setY.remove(0);
            setZ.remove(0);

            setkX.remove(0);
            setkY.remove(0);
            setkZ.remove(0);
        }*/

        ((LineDataSet) (data.getDataSetByIndex(0))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(1))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(2))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(3))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(4))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(5))).notifyDataSetChanged();

        accelX.getData().notifyDataChanged();
        accelX.notifyDataSetChanged();
        if (moveAccel)
            accelX.moveViewToX((float)(setX.size()-1));

        // ------ update clear acceleration chart ------
        buf = linear_data.clone();
        xd = buf[0];
        yd = buf[1];
        zd = buf[2];

        data = clearAccel.getLineData();
        setX = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(0))).getValues();
        setY = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(1))).getValues();
        setZ = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(2))).getValues();
        setkX = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(3))).getValues();
        setkY = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(4))).getValues();
        setkZ = (ArrayList<Entry>) ((LineDataSet) (data.getDataSetByIndex(5))).getValues();

        setX.add(new Entry((setX.size()-1)/10f, xd.floatValue()));
        setY.add(new Entry((setY.size()-1)/10f, yd.floatValue()));
        setZ.add(new Entry((setZ.size()-1)/10f, zd.floatValue()));

        buf = opt_linear_data.clone();
        xd = buf[0];
        yd = buf[1];
        zd = buf[2];

        setkX.add(new Entry((setX.size()-1)/10f, xd.floatValue()));
        setkY.add(new Entry((setY.size()-1)/10f, yd.floatValue()));
        setkZ.add(new Entry((setZ.size()-1)/10f, zd.floatValue()));

        /*if (setX.size() > 500){
            setX.remove(0);
            setY.remove(0);
            setZ.remove(0);

            setkX.remove(0);
            setkY.remove(0);
            setkZ.remove(0);
        }*/

        Log.i("stag", setX.size()+"");

        ((LineDataSet) (data.getDataSetByIndex(0))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(1))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(2))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(3))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(4))).notifyDataSetChanged();
        ((LineDataSet) (data.getDataSetByIndex(5))).notifyDataSetChanged();

        clearAccel.getData().notifyDataChanged();
        clearAccel.notifyDataSetChanged();
        if (moveClear)
            clearAccel.moveViewToX((float)(setX.size()-1));

    }



}
