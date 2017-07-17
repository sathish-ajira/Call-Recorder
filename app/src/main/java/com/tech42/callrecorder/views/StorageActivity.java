package com.tech42.callrecorder.views;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.tech42.callrecorder.R;

import java.util.ArrayList;

public class StorageActivity extends AppCompatActivity {

    PieChart storagePieChart;
    public static  final int[] MY_COLORS = {
            Color.rgb(153,255,153), Color.rgb(255,204,153)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        storagePieChart = (PieChart) findViewById(R.id.piechart);

        storagePieChart.setUsePercentValues(true);

        createDataSet();
    }

    private void createDataSet(){
        ArrayList<Entry> yvalues = new ArrayList<Entry>();
        yvalues.add(new Entry(60f, 0));
        yvalues.add(new Entry(40f, 1));
        PieDataSet dataSet = new PieDataSet(yvalues, "");

        ArrayList<String> xVals = new ArrayList<String>();

        xVals.add("Cloud Storage");
        xVals.add("Local Storage");

        PieData data = new PieData(xVals, dataSet);

        data.setValueFormatter(new PercentFormatter());
        storagePieChart.setData(data);

        data.setValueTextSize(15f);
        data.setValueTextColor(Color.DKGRAY);

        storagePieChart.setDrawHoleEnabled(true);
        storagePieChart.setTransparentCircleRadius(40f);
        storagePieChart.setHoleRadius(40f);

        // adding colors
        ArrayList<Integer> colors = new ArrayList<Integer>();

        // Added My Own colors
        for (int c : MY_COLORS)
            colors.add(c);
        dataSet.setColors(colors);

        dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

    }
}
