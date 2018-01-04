package com.example.hogehoge.accsensor_plot;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.System.currentTimeMillis;

public class AccsensorPlotActivity extends AppCompatActivity implements SensorEventListener{

    private LineChart mChart;
    private LineDataSet dataset;

    private TextView text;

    private SensorManager manager;

    private Runnable runnable;
    private Handler handler = new Handler();

    private float accValue[] = new float[3];
    private float timecnt = 0;
    private List<Float> ydata;
    private List<Float> xdata;

    private long timeStart;
    private float actSamplingRate;

    private final static float LOGGINGRATE = 20; //ms
    private final static int REFRESHRATE = 20; // Hz
    private final static float TIMESPAN_X = 3; // Sec

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accsensor_plot);

        // Chartの初期化
        initChart();
        // TextViewの初期化
        initTextView();
        // SensorManagerを取得
        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        // センサ値取得のタイマースタート
        timerSet();
        // サンプリングレート計算のための開始時間取得
        timeStart = System.currentTimeMillis();
    }

    // Chartの初期化
    private void initChart(){
        mChart = (LineChart)findViewById(R.id.chart);

        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setPinchZoom(true);

        int nData = (int)(TIMESPAN_X / (LOGGINGRATE / 1000));

        // X軸は0～500のデータをセット
        xdata = new ArrayList();
        for(int i=0; i<nData; i++){
            xdata.add((float)i);
        }
        // Y軸は0データをセット
        Float[] arr = new Float[nData];
        ydata = new ArrayList(Arrays.asList(arr));
        Collections.fill(ydata, 0f);

        // Entry型のListへ代入
        List<Entry> entries = new ArrayList<>();
        for(int i = 0; i < xdata.size(); i++){
            entries.add(new Entry(xdata.get(i), ydata.get(i)));
        }

        // グラフのラインになるLineDataSetを作成
        dataset = new LineDataSet(entries, "Xaxis");
        dataset.setColor(Color.BLUE);

        // LineDataSsetを使ってLineDataを初期化
        LineData lineData = new LineData(dataset);

        // LineChartにLineDataをセット
        mChart.setData(lineData);

        // Yaxis
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setAxisMinimum(-20f);
        leftAxis.setAxisMaximum(20f);
        // Yaxis(右側)
        YAxis rightAxis  =mChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Xaxis
        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        // グラフの更新
        mChart.invalidate();
    }

    // TextViewの初期化
    private void initTextView(){
        text = (TextView)findViewById(R.id.textView);
    }

    // グラフの更新
    private void updateChart(){

        LineData lineData = mChart.getData();
        lineData.removeDataSet(dataset);

        // ArrayListの先頭を一つ削除して末尾に新しいデータを追加
        ydata.remove(0);
        ydata.add(accValue[0]);

        // Entry型のListへ代入
        List<Entry> entries = new ArrayList<>();
        for(int i = 0; i < xdata.size(); i++){
            entries.add(new Entry(xdata.get(i), ydata.get(i)));
        }
        // Test

        dataset = new LineDataSet(entries, "Xaxis");
        dataset.setDrawCircles(false);
        dataset.setDrawCircleHole(false);
        lineData.addDataSet(dataset);

        //  データを追加したら必ずよばないといけない
        mChart.notifyDataSetChanged();
        mChart.invalidate();
    }

    // TextViewの更新
    private void updateTextView(){
        String str = "Acc values:"
                + "\n X axis: " + String.format("%1$.2f mm/s^2", accValue[0])
                + "\n Y axis: " + String.format("%1$.2f mm/s^2", accValue[1])
                + "\n Z axis: " + String.format("%1$.2f mm/s^2", accValue[2])
                + "\n Sampling: " + String.format("%1$.0f Hz" ,actSamplingRate);

        text.setText(str);
    }


    @Override
    protected void onStop(){
        super.onStop();
        manager.unregisterListener(this);
        // スクリーンONをキャンセル
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume(){
        super.onResume();

        // スクリーンONを維持
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ACCELEROMETER);

        if(sensors.size() > 0){
            Sensor s = sensors.get(0);
            manager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        }
    }

    // センサの精度が変わったときに呼ばれる？ いつ？
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){
        // pass
    }

    // センサの値が変わったときに呼ばれる
    @Override
    public void onSensorChanged(SensorEvent event){
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            timecnt++;
            accValue[0] = event.values[0];
            accValue[1] = event.values[1];
            accValue[2] = event.values[2];
        }
    }

    // Timer
        private void timerSet(){
        runnable = new Runnable() {
            @Override
            public void run(){

                // 実サンプリングレートの計算
                long actSamplingtime = System.currentTimeMillis() - timeStart;
                actSamplingRate = 1 / ((float)actSamplingtime / 1000);
                timeStart = System.currentTimeMillis();

                // グラフの更新
                updateChart();
                // TextViewの更新
                updateTextView();
                handler.postDelayed(this, (int)LOGGINGRATE);
            }
        };
        handler.post(runnable);
    }
}