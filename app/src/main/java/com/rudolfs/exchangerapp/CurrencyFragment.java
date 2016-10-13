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
 * A placeholder fragment containing a simple view.
 */
public class CurrencyFragment extends RxFragment implements AdapterView.OnItemSelectedListener {

    private static final String LOG_TAG = CurrencyFragment.class.getSimpleName();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private AppCompatSpinner currencyFromSpinner;
    private AppCompatSpinner currencyToSpinner;
    private LineChart currencyChart;
    private CurrencyData currencyData;
    private Subscription currencySub;

    public CurrencyFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_currency, container, false);
        currencyData = new CurrencyData();

        // Currency selectors from - to and chart
        currencyFromSpinner = (AppCompatSpinner) rootView.findViewById(R.id.currency_from);
        currencyToSpinner = (AppCompatSpinner) rootView.findViewById(R.id.currency_to);

        currencyChart = (LineChart) rootView.findViewById(R.id.currency_chart_view);
        currencyChart.setDescription(null);
        currencyChart.setTouchEnabled(true);
        currencyChart.setDragEnabled(true);
        currencyChart.setScaleEnabled(true);
        currencyChart.setPinchZoom(true);

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
            exchangeIntent.putExtra(ExchangerFragment.EXTRA_CURRENCY_RATIO, String.valueOf(currencyData.getLatesRatio()));
            startActivity(exchangeIntent);
        });

        // TODO - Get currency data
        getData();

        return rootView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        getData();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    private void getData() {
        currencyData.clearData();
        String currencyFrom = (String) currencyFromSpinner.getSelectedItem();
        String currencyTo = (String) currencyToSpinner.getSelectedItem();

        // Get currency exchange rate
        if (currencySub == null || currencySub.isUnsubscribed()) {
            currencySub = Single.create(sub -> {
                for (Date yearlyDate : currencyData.getYearlyDates()) {
                    String currencyJsonStr = getCurrencyRatioJson(currencyFrom, currencyTo, yearlyDate);
                    currencyData.addYearlyData(yearlyDate, getCurrencyRatio(currencyJsonStr, currencyTo));
                }

                for (Date monthlyDate : currencyData.getMonthlyDates()) {
                    String currencyJsonStr = getCurrencyRatioJson(currencyFrom, currencyTo, monthlyDate);
                    currencyData.addMonthlyData(monthlyDate, getCurrencyRatio(currencyJsonStr, currencyTo));
                }

                for (Date dailyDate : currencyData.getDailyDates()) {
                    String currencyJsonStr = getCurrencyRatioJson(currencyFrom, currencyTo, dailyDate);
                    currencyData.addDailyData(dailyDate, getCurrencyRatio(currencyJsonStr, currencyTo));
                }

                String latestRatio = getCurrencyRatioJson(currencyFrom, currencyTo, Calendar.getInstance().getTime());
                currencyData.setLatesRatio(getCurrencyRatio(latestRatio, currencyTo));

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
                        currencyChart.setData(currencyData.getYearlyLineData());
                        currencyChart.getXAxis().setValueFormatter(new AxisValueFormatter() {
                            @Override
                            public String getFormattedValue(float value, AxisBase axis) {
                                String dateFloat = String.valueOf(value);
//                                Log.d(LOG_TAG, "Formating x axis: " + dateFloat);
                                return dateFloat.substring(0, 4) + "-" + dateFloat.substring(4, 6) + "-" + dateFloat.substring(6, 8);
                            }

                            @Override
                            public int getDecimalDigits() {
                                return 0;
                            }
                        });
                        currencyChart.invalidate();
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
                // Nothing to do.
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
