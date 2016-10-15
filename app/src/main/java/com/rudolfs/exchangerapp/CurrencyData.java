package com.rudolfs.exchangerapp;

import android.util.Log;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Rudolfs on 2016.10.11..
 */

public class CurrencyData implements AxisValueFormatter {

    private static final String LOG_TAG = CurrencyData.class.getSimpleName();
    private static final SimpleDateFormat YEARLY_FROMAT = new SimpleDateFormat("yyyy/MM");
    private static final SimpleDateFormat MONTHLY_DAILY_FORMAT = new SimpleDateFormat("MM/dd");

    // Constants for axis formatter to know
    // from which date array to take values
    private static final int LINE_YEARLY = 0;
    private static final int LINE_MONTHLY = 1;
    private static final int LINE_DAILY= 2;

    private List<Date> yearlyDates;
    private List<Date> monthlyDates;
    private List<Date> dailyDates;

    private List<BigDecimal> yearlyData;
    private List<BigDecimal> monthlyData;
    private List<BigDecimal> dailyData;
    private BigDecimal latestRatio;

    private LineData yearlyLineData;
    private LineData monthlyLineData;
    private LineData dailyLineData;
    private int selectedLine = LINE_YEARLY;

    public CurrencyData() {
        yearlyData = new ArrayList<BigDecimal>();
        monthlyData = new ArrayList<BigDecimal>();
        dailyData = new ArrayList<BigDecimal>();
        latestRatio = BigDecimal.ONE;

        yearlyLineData = new LineData();
        monthlyLineData = new LineData();
        dailyLineData = new LineData();

        // Generate chart dates
        yearlyDates = new ArrayList<Date>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        for (int i = 0; i < 13; i++) {
            yearlyDates.add(cal.getTime());
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
        latestRatio = null;
        yearlyData.clear();
        monthlyData.clear();
        dailyData.clear();
        yearlyLineData.clearValues();
        monthlyLineData.clearValues();
        dailyLineData.clearValues();
    }

    public void addYearlyData(BigDecimal ratio) {
        yearlyData.add(ratio);
    }

    public void addMonthlyData(BigDecimal ratio) {
        monthlyData.add(ratio);
    }

    public void addDailyData(BigDecimal ratio) {
        dailyData.add(ratio);
    }

    public void setLatestRatio(BigDecimal latestRatio) {
        this.latestRatio = latestRatio;
    }

    public List<BigDecimal> getYearlyData() {
        return yearlyData;
    }

    public List<BigDecimal> getDailyData() {
        return dailyData;
    }

    public List<BigDecimal> getMonthlyData() {
        return monthlyData;
    }

    public BigDecimal getLatestRatio() {
        return this.latestRatio;
    }

    public LineData getYearlyLineData() {
        selectedLine = LINE_YEARLY;
        return yearlyLineData;
    }

    public LineData getMonthlyLineData() {
        selectedLine = LINE_MONTHLY;
        return monthlyLineData;
    }

    public LineData getDailyLineData() {
        selectedLine = LINE_DAILY;
        return dailyLineData;
    }

    // Generates line data for chart to use for all time periods.
    // To use this, period data lists must be populated.
    public void generateLineData() {
        List<Entry> yearlyValues = new ArrayList<Entry>();
        List<Entry> monthlyValues = new ArrayList<Entry>();
        List<Entry> dailyValues = new ArrayList<Entry>();

        for (int i = 0; i < yearlyData.size(); i++) {
            BigDecimal ratio = yearlyData.get(i);
            Float x = (float) i;
//            Log.d(LOG_TAG, "Index: " + i + " float: " + x);
            yearlyValues.add(new Entry(x, ratio.floatValue()));
        }

        for (int i = 0; i < monthlyData.size(); i++) {
            BigDecimal ratio = yearlyData.get(i);
            Float x = (float) i;
//            Log.d(LOG_TAG, "Index: " + i + " float: " + x);
            monthlyValues.add(new Entry(x, ratio.floatValue()));
        }

        for (int i = 0; i < dailyData.size(); i++) {
            BigDecimal ratio = dailyData.get(i);
            Float x = (float) i;
            Log.d(LOG_TAG, "Index: " + i + " float: " + x);
            dailyValues.add(new Entry(x, ratio.floatValue()));
        }

        Collections.sort(yearlyValues, new EntryXComparator());
        LineDataSet yearlyDataSet = new LineDataSet(yearlyValues, "");
        yearlyDataSet.setColor(ColorTemplate.COLORFUL_COLORS[2]);
        yearlyDataSet.setLineWidth(2.0f);
        yearlyLineData = new LineData(yearlyDataSet);
        Collections.sort(monthlyValues, new EntryXComparator());
        LineDataSet monthlyDataSet = new LineDataSet(monthlyValues, "");
        monthlyDataSet.setColor(ColorTemplate.COLORFUL_COLORS[2]);
        monthlyDataSet.setLineWidth(2.0f);
        monthlyLineData = new LineData(monthlyDataSet);
        Collections.sort(dailyValues, new EntryXComparator());
        for (Entry e : dailyValues) Log.d(LOG_TAG, "Daily values: " + e.toString());
        LineDataSet dailyDataSet = new LineDataSet(dailyValues, "");
        dailyDataSet.setColor(ColorTemplate.COLORFUL_COLORS[2]);
        dailyDataSet.setLineWidth(2.0f);
        dailyLineData = new LineData(dailyDataSet);
    }

    /*
     * Generates correct dates for all time periods,
     * using today as start point.
     */

    public List<Date> getYearlyDates() {
        return yearlyDates;
    }

    public List<Date> getMonthlyDates() {
        return monthlyDates;
    }

    public List<Date> getDailyDates() {
        return dailyDates;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        // The formatter asks not only the values which are supplied entries on
        // but also values between them and before/after, whom we do not have
        // formatted values.
//        Log.d(LOG_TAG, "Value: " + value);
        if (value % 1 == 0) {
            Date date;
            int index = Float.valueOf(value).intValue();
            if (selectedLine == LINE_YEARLY && index < yearlyData.size()) {
                date = yearlyDates.get(index);
                return YEARLY_FROMAT.format(date);
            } else if (selectedLine == LINE_MONTHLY && index < monthlyData.size()) {
                date = monthlyDates.get(index);
                return MONTHLY_DAILY_FORMAT.format(date);
            } else if (selectedLine == LINE_DAILY && index < dailyData.size()) {
                date = dailyDates.get(index);
                return MONTHLY_DAILY_FORMAT.format(date);
            }
        }
        return "";
    }

    @Override
    public int getDecimalDigits() {
        return 0;
    }
}
