package com.rudolfs.exchangerapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.math.BigDecimal;

/**
 * A placeholder fragment containing a simple view.
 */
public class ExchangerFragment extends Fragment {

    private static final String LOG_TAG = ExchangerFragment.class.getSimpleName();

    public static final String EXTRA_CURRENCY_FROM = "currency_from";
    public static final String EXTRA_CURRENCY_TO = "currency_to";
    public static final String EXTRA_CURRENCY_RATIO = "currency_ratio";

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
            final String currencyRatio = intent.getStringExtra(EXTRA_CURRENCY_RATIO);

            TextView currencyTitleFrom = (TextView) rootView.findViewById(R.id.exchanger_title_from);
            currencyTitleFrom.setText(String.format(getResources().getString(R.string.exchanger_input_title), currencyFrom));

            TextView currencyTitleTo = (TextView) rootView.findViewById(R.id.exchanger_title_to);
            currencyTitleTo.setText(String.format(getResources().getString(R.string.exchanger_input_title), currencyTo));

            exchangeRate = new BigDecimal(currencyRatio);
        }

        editFrom = (EditText) rootView.findViewById(R.id.exchanger_input_from);
        editFrom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (changeNeeded) {
                    changeNeeded = false;
                    if (s.length() <= 0) {
                        editTo.setText("");
                    } else {
                        BigDecimal valueFrom = new BigDecimal(String.valueOf(s));
                        editTo.setText(String.valueOf(valueFrom.multiply(exchangeRate).setScale(4, BigDecimal.ROUND_DOWN).stripTrailingZeros()));
                    }
                    changeNeeded = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        editTo = (EditText) rootView.findViewById(R.id.exchanger_input_to);
        editTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (changeNeeded) {
                    changeNeeded = false;
                    if (s.length() <= 0) {
                        editFrom.setText("");
                    } else {
                        BigDecimal valueTo = new BigDecimal(String.valueOf(s));
                        editFrom.setText(String.valueOf(valueTo.divide(exchangeRate, 4, BigDecimal.ROUND_DOWN).stripTrailingZeros()));
                    }
                    changeNeeded = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return rootView;
    }
}
