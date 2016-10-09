package com.rudolfs.exchangerapp;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

/**
 * A placeholder fragment containing a simple view.
 */
public class CurrencyFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private AppCompatSpinner currencyFromSpinner;
    private AppCompatSpinner currencyToSpinner;

    public CurrencyFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_currency, container, false);

        currencyFromSpinner = (AppCompatSpinner) rootView.findViewById(R.id.currency_from);
        currencyToSpinner = (AppCompatSpinner) rootView.findViewById(R.id.currency_to);

        ArrayAdapter<CharSequence> currencyFromToAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.currency_from_to_values, android.R.layout.simple_spinner_item);
        currencyFromToAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencyFromSpinner.setAdapter(currencyFromToAdapter);
        currencyFromSpinner.setOnItemSelectedListener(this);
        currencyFromSpinner.setSelection(currencyFromToAdapter.getPosition(getString(R.string.currency_value_EUR)));
        currencyToSpinner.setAdapter(currencyFromToAdapter);
        currencyToSpinner.setOnItemSelectedListener(this);
        currencyToSpinner.setSelection(currencyFromToAdapter.getPosition(getString(R.string.currency_value_USD)));

        Button exchangerBtn = (Button) rootView.findViewById(R.id.currency_btn_exchange);
        exchangerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent exchangeIntent = new Intent(getActivity(), ExchangerActivity.class);
                exchangeIntent.putExtra(ExchangerFragment.EXTRA_CURRENCY_FROM, (String) currencyFromSpinner.getSelectedItem());
                exchangeIntent.putExtra(ExchangerFragment.EXTRA_CURRENCY_TO, (String) currencyToSpinner.getSelectedItem());
                startActivity(exchangeIntent);
            }
        });

        return rootView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
