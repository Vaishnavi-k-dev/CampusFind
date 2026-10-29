package com.example.campusfind;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class activity_view_items extends AppCompatActivity {

    private EditText etSearchItems;
    private Spinner spinnerFilterType;
    private ListView listViewItems;
    private TextView tvEmpty;

    private final ArrayList<JSONObject> allReports =
            new ArrayList<>();

    private final ArrayList<String> displayedReports =
            new ArrayList<>();

    private ArrayAdapter<String> listAdapter;

    private ExecutorService executorService;

    private boolean reportsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        etSearchItems = findViewById(R.id.etSearchItems);
        spinnerFilterType = findViewById(R.id.spinnerFilterType);
        listViewItems = findViewById(R.id.listViewItems);
        tvEmpty = findViewById(R.id.tvEmpty);

        String[] filterOptions = {
                "All Items",
                "Lost Items",
                "Found Items"
        };

        ArrayAdapter<String> filterAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        filterOptions
                );

        filterAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerFilterType.setAdapter(filterAdapter);

        listAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayedReports
        );

        listViewItems.setAdapter(listAdapter);

        etSearchItems.addTextChangedListener(
                new android.text.TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        filterReports();
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {
                    }
                }
        );

        spinnerFilterType.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        filterReports();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );

        executorService = Executors.newSingleThreadExecutor();

        loadReports();
    }

    private void loadReports() {

        tvEmpty.setText("Loading reported items...");
        tvEmpty.setVisibility(View.VISIBLE);
        listViewItems.setVisibility(View.GONE);

        executorService.execute(() -> {

            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                        "http://10.0.2.2:5000/api/items"
                );

                connection = (HttpURLConnection)
                        url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception(
                            "Server returned error " + responseCode
                    );
                }

                InputStream inputStream =
                        connection.getInputStream();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(inputStream)
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONArray jsonArray =
                        new JSONArray(response.toString());

                ArrayList<JSONObject> fetchedReports =
                        new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    fetchedReports.add(
                            jsonArray.getJSONObject(i)
                    );
                }

                runOnUiThread(() -> {

                    allReports.clear();
                    allReports.addAll(fetchedReports);

                    reportsLoaded = true;

                    filterReports();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    tvEmpty.setText(
                            "Unable to load reports. " +
                                    "Check that the backend is running."
                    );

                    tvEmpty.setVisibility(View.VISIBLE);
                    listViewItems.setVisibility(View.GONE);

                    Toast.makeText(
                            activity_view_items.this,
                            "Could not connect to the server.",
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void filterReports() {

        if (!reportsLoaded) {
            return;
        }

        String searchText = etSearchItems.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        String selectedFilter =
                spinnerFilterType.getSelectedItem()
                        .toString();

        displayedReports.clear();

        for (JSONObject report : allReports) {

            String itemName =
                    report.optString("itemName", "");

            String description =
                    report.optString("description", "");

            String location =
                    report.optString("location", "");

            String date =
                    report.optString("date", "");

            String reportType =
                    report.optString("reportType", "");

            String status =
                    report.optString("status", "");

            String searchableText = (
                    itemName + " " +
                            description + " " +
                            location
            ).toLowerCase(Locale.ROOT);

            boolean matchesSearch =
                    searchableText.contains(searchText);

            boolean matchesFilter = true;

            if (selectedFilter.equals("Lost Items")) {

                matchesFilter =
                        reportType.toLowerCase(Locale.ROOT)
                                .contains("lost");

            } else if (selectedFilter.equals("Found Items")) {

                matchesFilter =
                        reportType.toLowerCase(Locale.ROOT)
                                .contains("found");
            }

            if (matchesSearch && matchesFilter) {

                String itemDetails =
                        "Type: " + reportType +
                                "\nItem: " + itemName +
                                "\nDescription: " + description +
                                "\nLocation: " + location +
                                "\nDate: " + date +
                                "\nStatus: " + status;

                displayedReports.add(itemDetails);
            }
        }

        listAdapter.notifyDataSetChanged();

        if (displayedReports.isEmpty()) {

            tvEmpty.setText(
                    allReports.isEmpty()
                            ? "No reports available yet."
                            : "No matching items found."
            );

            tvEmpty.setVisibility(View.VISIBLE);
            listViewItems.setVisibility(View.GONE);

        } else {

            tvEmpty.setVisibility(View.GONE);
            listViewItems.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}