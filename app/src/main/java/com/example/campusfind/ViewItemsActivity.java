package com.example.campusfind;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewItemsActivity extends AppCompatActivity {

    private ListView listViewItems;
    private TextView tvEmpty;
    private ArrayList<String> itemList;
    private ArrayAdapter<String> adapter;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final String API_URL =
            "http://10.0.2.2:5000/api/items";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        listViewItems = findViewById(R.id.listViewItems);
        tvEmpty = findViewById(R.id.tvEmpty);

        itemList = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                itemList
        );

        listViewItems.setAdapter(adapter);
        listViewItems.setEmptyView(tvEmpty);

        loadItems();
    }

    private void loadItems() {

        executor.execute(() -> {

            HttpURLConnection connection = null;

            try {
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();

                if (responseCode < 200 || responseCode >= 300) {
                    throw new Exception(
                            "Server returned HTTP " + responseCode
                    );
                }

                StringBuilder response = new StringBuilder();

                try (InputStream inputStream =
                             connection.getInputStream();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(
                                     inputStream,
                                     StandardCharsets.UTF_8))) {

                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                JSONArray items = new JSONArray(response.toString());

                ArrayList<String> fetchedItems = new ArrayList<>();

                for (int i = 0; i < items.length(); i++) {

                    JSONObject item = items.getJSONObject(i);

                    String reportType =
                            item.optString("reportType", "Unknown");

                    String itemName =
                            item.optString("itemName", "Unnamed Item");

                    String description =
                            item.optString("description", "");

                    String location =
                            item.optString("location", "Unknown");

                    String date =
                            item.optString("date", "");

                    String status =
                            item.optString("status", "Open");

                    String details =
                            reportType + "\n"
                                    + "Item: " + itemName + "\n"
                                    + "Description: " + description + "\n"
                                    + "Location: " + location + "\n"
                                    + "Date: " + (date.isEmpty()
                                    ? "Not provided" : date) + "\n"
                                    + "Status: " + status;

                    fetchedItems.add(details);
                }

                runOnUiThread(() -> {
                    itemList.clear();
                    itemList.addAll(fetchedItems);
                    adapter.notifyDataSetChanged();

                    if (fetchedItems.isEmpty()) {
                        tvEmpty.setText("No items reported yet.");
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> Toast.makeText(
                        ViewItemsActivity.this,
                        "Unable to load items. Check your connection.",
                        Toast.LENGTH_LONG
                ).show());

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}