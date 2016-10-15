package com.rudolfs.exchangerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.trello.rxlifecycle.components.support.RxFragment;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Fragment that allows user to choose two currencies to see their exchange rate
 * and shows 3 charts in different time periods.
 */
public class CurrencyFragment extends RxFragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private static final String LOG_TAG = CurrencyFragment.class.getSimpleName();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private AppCompatSpinner currencyFromSpinner;
    private AppCompatSpinner currencyToSpinner;
    private LineChart currencyChart;
    private Button btnDaily, btnMonthly, btnYearly;

    private CurrencyData currencyData;
    private Subscription currencySub;
    private Subscription ratioSub;

    public CurrencyFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_currency, container, false);
        currencyData = new CurrencyData();

        currencyFromSpinner = (AppCompatSpinner) rootView.findViewById(R.id.currency_from);
        currencyToSpinner = (AppCompatSpinner) rootView.findViewById(R.id.currency_to);

        currencyChart = (LineChart) rootView.findViewById(R.id.currency_chart_view);
        currencyChart.getXAxis().setValueFormatter(currencyData);
        currencyChart.setDescription(null);
        currencyChart.setTouchEnabled(true);
        currencyChart.setDragEnabled(true);
        currencyChart.setScaleEnabled(true);
        currencyChart.setPinchZoom(true);

        btnDaily = (Button) rootView.findViewById(R.id.btn_chart_daily);
        btnDaily.setOnClickListener(this);
        btnMonthly = (Button) rootView.findViewById(R.id.btn_chart_monthly);
        btnMonthly.setOnClickListener(this);
        btnYearly = (Button) rootView.findViewById(R.id.btn_chart_yearly);
        btnYearly.setOnClickListener(this);

        // Populate from - to currency choosers
        ArrayAdapter<CharSequence> currencyFromToAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.currency_from_to_values, android.R.layout.simple_spinner_item);
        currencyFromToAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencyFromSpinner.setAdapter(currencyFromToAdapter);
        currencyFromSpinner.setOnItemSelectedListener(this);
        currencyFromSpinner.setSelection(currencyFromToAdapter.getPosition(getString(R.string.currency_value_EUR)));
        currencyToSpinner.setAdapter(currencyFromToAdapter);
        currencyToSpinner.setOnItemSelectedListener(this);
        currencyToSpinner.setSelection(currencyFromToAdapter.getPosition(getString(R.string.currency_value_USD)));

        Button exchangerBtn = (Button) rootView.findViewById(R.id.currency_btn_exchange);
        exchangerBtn.setOnClickListener(click -> {
            Intent exchangeIntent = new Intent(getActivity(), ExchangerActivity.class);
            exchangeIntent.putExtra(ExchangerFragment.EXTRA_CURRENCY_FROM, (String) currencyFromSpinner.getSelectedItem());
            exchangeIntent.putExtra(ExchangerFragment.EXTRA_CURRENCY_TO, (String) currencyToSpinner.getSelectedItem());
            exchangeIntent.putExtra(ExchangerFragment.EXTRA_CURRENCY_RATIO, String.valueOf(currencyData.getLatestRatio()));
            startActivity(exchangeIntent);
        });

        getData();
        return rootView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        getData();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onClick(View v) {
        int id = v.getId();
        currencyChart.clear();
        switch (id) {
            case R.id.btn_chart_daily:
                currencyChart.setData(currencyData.getDailyLineData());
            case R.id.btn_chart_monthly:
                currencyChart.setData(currencyData.getMonthlyLineData());
            case R.id.btn_chart_yearly:
                currencyChart.setData(currencyData.getYearlyLineData());
        }
        currencyChart.invalidate();
        btnDaily.setEnabled(id == R.id.btn_chart_daily ? false : true);
        btnMonthly.setEnabled(id == R.id.btn_chart_monthly ? false : true);
        btnYearly.setEnabled(id == R.id.btn_chart_yearly ? false : true);
    }

    private void getData() {
        currencyData.clearData();
        String currencyFrom = (String) currencyFromSpinner.getSelectedItem();
        String currencyTo = (String) currencyToSpinner.getSelectedItem();

        // Get currency exchange rate
        if (ratioSub == null || ratioSub.isUnsubscribed()) {
            ratioSub = Single.create(sub -> {
                String ratioJsonStr = getCurrencyRatioJson(currencyFrom, currencyTo, Calendar.getInstance().getTime());
                currencyData.setLatestRatio(getCurrencyRatio(ratioJsonStr, currencyTo));

                sub.onSuccess(null);
            })
                    .compose(bindToLifecycle().forSingle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(success -> {

                    }, error -> {

                    });
        }

        // Get data for charts
        if (currencySub == null || currencySub.isUnsubscribed()) {
            currencySub = Single.create(sub -> {
                for (Date yearlyDate : currencyData.getYearlyDates()) {
                    String currencyJsonStr = getCurrencyRatioJson(currencyFrom, currencyTo, yearlyDate);
                    currencyData.addYearlyData(getCurrencyRatio(currencyJsonStr, currencyTo));
                }

                for (Date monthlyDate : currencyData.getMonthlyDates()) {
                    String currencyJsonStr = getCurrencyRatioJson(currencyFrom, currencyTo, monthlyDate);
                    currencyData.addMonthlyData(getCurrencyRatio(currencyJsonStr, currencyTo));
                }

                for (Date dailyDate : currencyData.getDailyDates()) {
                    String currencyJsonStr = getCurrencyRatioJson(currencyFrom, currencyTo, dailyDate);
                    currencyData.addDailyData(getCurrencyRatio(currencyJsonStr, currencyTo));
                }

                sub.onSuccess(null);
            })
                    .compose(bindToLifecycle().forSingle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(success -> {
                        Log.d(LOG_TAG, "data sizes: Y: " + currencyData.getYearlyData().size() +
                                " M: " + currencyData.getMonthlyData().size() + " D: " + currencyData.getDailyData().size());

                        // After data is gathered, generate chart
                        currencyData.generateLineData();
                        currencyChart.setData(currencyData.getDailyLineData());
                        currencyChart.invalidate();
                        btnMonthly.setEnabled(true);
                        btnYearly.setEnabled(true);
                    }, error -> {
                        Log.e(LOG_TAG, error.getMessage(), error);
                    });
        }
    }

    private String getCurrencyRatioJson(String currencyFrom, String currencyTo, Date date) {
        final String BASE_URL = "http://api.fixer.io/";
        final String BASE_PARAM = "base";
        final String SYMBOLS_PARAM = "symbols";

        try {
            Uri currencyUri = Uri.parse(BASE_URL).buildUpon()
                    .appendPath(DATE_FORMAT.format(date))
                    .appendQueryParameter(BASE_PARAM, currencyFrom)
                    .appendQueryParameter(SYMBOLS_PARAM, currencyTo)
                    .build();

//            Log.d(LOG_TAG, "URL: " + currencyUri.toString());
            URL exchangeUrl = new URL(currencyUri.toString());
            HttpURLConnection urlConnection = (HttpURLConnection) exchangeUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
//            Log.d(LOG_TAG, "Recieved json: " + buffer.toString());

            return buffer.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

    private BigDecimal getCurrencyRatio(String jsonString, String currencyTo) {
        final String RATIO_RATES = "rates";

        try {
            JSONObject ratioJson = new JSONObject(jsonString);
            JSONObject ratesObject = ratioJson.getJSONObject(RATIO_RATES);
            Double rateDouble = ratesObject.getDouble(currencyTo);
            BigDecimal rate = new BigDecimal(rateDouble);
            return rate;
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }
}
