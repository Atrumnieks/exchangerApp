package com.rudolfs.exchangerapp;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;

import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * A placeholder fragment containing a simple view.
 */
public class ExchangerFragment extends Fragment {

    private static final String LOG_TAG = ExchangerFragment.class.getSimpleName();

    public static final String EXTRA_CURRENCY_FROM = "currency_from";
    public static final String EXTRA_CURRENCY_TO = "currency_to";

    private EditText editFrom;
    private EditText editTo;

    private boolean changeNeeded = true;
    private BigDecimal exchangeRate = BigDecimal.ONE;

    public ExchangerFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_exchanger, container, false);

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(EXTRA_CURRENCY_FROM) && intent.hasExtra(EXTRA_CURRENCY_TO)) {
            final String currencyFrom = intent.getStringExtra(EXTRA_CURRENCY_FROM);
            final String currencyTo = intent.getStringExtra(EXTRA_CURRENCY_TO);

            TextView currencyTitleFrom = (TextView) rootView.findViewById(R.id.currency_title_from);
            currencyTitleFrom.setText(String.format(getResources().getString(R.string.exchanger_input_title), currencyFrom));

            TextView currencyTitleTo = (TextView) rootView.findViewById(R.id.currency_title_to);
            currencyTitleTo.setText(String.format(getResources().getString(R.string.exchanger_input_title), currencyTo));

            // Get currency exchange rate
            Subscription ratioSub = Single.create(new Single.OnSubscribe<String>() {
                @Override
                public void call(SingleSubscriber<? super String> singleSubscriber) {
                    singleSubscriber.onSuccess(getCurrencyRatioJson(currencyFrom, currencyTo));
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            exchangeRate = getCurrencyRatio(s, currencyTo);
                        }
                     }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.e(LOG_TAG, throwable.getMessage(), throwable);
                    }
            });
        }

        editFrom = (EditText) rootView.findViewById(R.id.currency_input_from);
        editFrom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (changeNeeded) {
                    changeNeeded = false;
                    BigDecimal valueFrom = new BigDecimal(String.valueOf(s));
                    editTo.setText(String.valueOf(valueFrom.multiply(exchangeRate).setScale(4, BigDecimal.ROUND_DOWN)));
                    changeNeeded = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        editTo = (EditText) rootView.findViewById(R.id.currency_input_to);
        editTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (changeNeeded) {
                    changeNeeded = false;
                    BigDecimal valueTo = new BigDecimal(String.valueOf(s));
                    editFrom.setText(String.valueOf(valueTo.divide(exchangeRate, 4, BigDecimal.ROUND_DOWN)));
                    changeNeeded = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return rootView;
    }

    private String getCurrencyRatioJson(String currencyFrom, String currencyTo) {
        final String BASE_URL = "http://api.fixer.io/latest?";
        final String BASE_PARAM = "base";
        final String SYMBOLS_PARAM = "symbols";

        try {
            Uri exchangerUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(BASE_PARAM, currencyFrom)
                    .appendQueryParameter(SYMBOLS_PARAM, currencyTo)
                    .build();

            URL exchangeUrl = new URL(exchangerUri.toString());
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
            Double rateDouble = (Double) ratesObject.getDouble(currencyTo);
            BigDecimal rate = new BigDecimal(rateDouble);
            return rate;
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return null;
        }
    }
}
