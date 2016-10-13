package com.rudolfs.exchangerapp;

import android.util.Log;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Rudolfs on 2016.10.11..
 */

public class CurrencyData {

    private static final String LOG_TAG = CurrencyData.class.getSimpleName();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    private List<Date> yearlyDates;
    private List<Date> monthlyDates;
    private List<Date> dailyDates;

    private Map<Date, BigDecimal> yearlyData;
    private Map<Date, BigDecimal> monthlyData;
    private Map<Date, BigDecimal> dailyData;
    private BigDecimal latesRatio;

    private LineData yearlyLineData;
    private LineData monthlyLineData;
    private LineData dailyLineData;

    public CurrencyData() {
        yearlyData = new HashMap<Date, BigDecimal>();
        monthlyData = new HashMap<Date, BigDecimal>();
        dailyData = new HashMap<Date, BigDecimal>();
        latesRatio = BigDecimal.ONE;

        yearlyLineData = new LineData();
        monthlyLineData = new LineData();
        dailyLineData = new LineData();

        // Generate chart dates
        yearlyDates = new ArrayList<Date>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        for (int i = 0; i < 13; i++) {
            yearlyDates.add(cal.getTime());
//                Log.d(LOG_TAG, "WTF: " + cal.getTime() + " : " + i);
            cal.add(Calendar.MONTH, 1);
        }

        monthlyDates = new ArrayList<Date>();
        cal = Calendar.getInstance();
        Calendar thisMonth = (Calendar) cal.clone();
        cal.add(Calendar.MONTH, -1);
        cal.add(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek() - cal.get(Calendar.DAY_OF_WEEK));
        while (!cal.after(thisMonth)) {
            monthlyDates.add(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 4);
            if (cal.after(thisMonth)) break;
            monthlyDates.add(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 3);
        }

        dailyDates = new ArrayList<Date>();
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        for (int i = 0; i < 8; i++) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                dailyDates.add(cal.getTime());
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    public void clearData() {
        yearlyData.clear();
        monthlyData.clear();
        dailyData.clear();
        yearlyLineData.clearValues();
        monthlyLineData.clearValues();
        dailyLineData.clearValues();
    }

    public void addYearlyData(Date date, BigDecimal ratio) {
        yearlyData.put(date, ratio);
    }

    public void addMonthlyData(Date date, BigDecimal ratio) {
        monthlyData.put(date, ratio);
    }

    public void addDailyData(Date date, BigDecimal ratio) {
        dailyData.put(date, ratio);
    }

    public void setLatesRatio(BigDecimal latesRatio) {
        this.latesRatio = latesRatio;
    }

    public Map<Date, BigDecimal> getYearlyData() {
        return yearlyData;
    }

    public Map<Date, BigDecimal> getDailyData() {
        return dailyData;
    }

    public Map<Date, BigDecimal> getMonthlyData() {
        return monthlyData;
    }

    public BigDecimal getLatesRatio() {
        return this.latesRatio;
    }

    public LineData getYearlyLineData() {
        return yearlyLineData;
    }

    public LineData getDailyLineData() {
        return dailyLineData;
    }

    public LineData getMonthlyLineData() {
        return monthlyLineData;
    }

    public void generateLineData() {
        List<Entry> yearlyValues = new ArrayList<Entry>();
        List<Entry> monthlyValues = new ArrayList<Entry>();
        List<Entry> dailyValues = new ArrayList<Entry>();

        for (Map.Entry<Date, BigDecimal> data : yearlyData.entrySet()) {
            Log.d(LOG_TAG, "Yearly data: X: " + DATE_FORMAT.format(data.getKey()) + " Y: " + data.getValue().stripTrailingZeros().floatValue());
            yearlyValues.add(new Entry(Integer.valueOf(DATE_FORMAT.format(data.getKey())), data.getValue().stripTrailingZeros().floatValue()));
        }

//        for (Map.Entry<Date, BigDecimal> data : monthlyData.entrySet()) {
//            monthlyValues.add(new Entry(data.getKey().getTime(), data.getValue().floatValue()));
//        }
//
//        for (Map.Entry<Date, BigDecimal> data : monthlyData.entrySet()) {
//            dailyValues.add(new Entry(data.getKey().getTime(), data.getValue().floatValue()));
//        }

        Collections.sort(yearlyValues, new EntryXComparator());
        LineDataSet yearlyDataSet = new LineDataSet(yearlyValues, "");
        yearlyDataSet.setColor(ColorTemplate.COLORFUL_COLORS[2]);
        yearlyDataSet.setLineWidth(2.0f);
        yearlyLineData = new LineData(yearlyDataSet);
//        LineDataSet monthlyDataSet = new LineDataSet(monthlyValues, "");
//        monthlyLineData = new LineData(monthlyDataSet);
//        LineDataSet dailyDataSet = new LineDataSet(dailyValues, "");
//        dailyLineData = new LineData(dailyDataSet);
    }

    public List<Date> getYearlyDates() {
        return yearlyDates;
    }

    public List<Date> getMonthlyDates() {
        return monthlyDates;
    }

    public List<Date> getDailyDates() {
        return dailyDates;
    }
}
