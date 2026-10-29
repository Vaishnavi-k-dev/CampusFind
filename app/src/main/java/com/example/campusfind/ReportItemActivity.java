package com.example.campusfind;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportItemActivity extends AppCompatActivity {

    Spinner spinnerReportType;
    EditText etItemName, etDescription, etLocation, etDate;
    Button btnSubmitReport;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    // Android emulator connects to your computer using 10.0.2.2
    private static final String API_URL =
            "http://10.0.2.2:5000/api/items";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_item);

        spinnerReportType = findViewById(R.id.spinnerReportType);
        etItemName = findViewById(R.id.etItemName);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etDate = findViewById(R.id.etDate);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);

        String[] reportTypes = {"Lost Item", "Found Item"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                reportTypes
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerReportType.setAdapter(adapter);

        String selectedType = getIntent().getStringExtra("reportType");

        if ("Found".equals(selectedType)) {
            spinnerReportType.setSelection(1);
        } else {
            spinnerReportType.setSelection(0);
        }

        btnSubmitReport.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {

        String reportType = spinnerReportType.getSelectedItem().toString();
        String itemName = etItemName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        // Validate required fields
        if (itemName.isEmpty()
                || description.isEmpty()
                || location.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please fill in all required fields",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnSubmitReport.setEnabled(false);
        btnSubmitReport.setText("Submitting...");

        executor.execute(() -> {

            HttpURLConnection connection = null;

            try {
                URL url = new URL(API_URL);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                // Prepare report data as JSON
                JSONObject report = new JSONObject();

                report.put("reportType", reportType);
                report.put("itemName", itemName);
                report.put("description", description);
                report.put("location", location);
                report.put("date", date);

                byte[] requestData = report.toString()
                        .getBytes(StandardCharsets.UTF_8);

                // Send report to Express backend
                try (OutputStream outputStream =
                             connection.getOutputStream()) {

                    outputStream.write(requestData);
                }

                int responseCode = connection.getResponseCode();

                InputStream inputStream;

                if (responseCode >= 200 && responseCode < 300) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }

                StringBuilder response = new StringBuilder();

                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    StandardCharsets.UTF_8))) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                JSONObject responseJson =
                        new JSONObject(response.toString());

                String message = responseJson.optString(
                        "message",
                        "Unexpected server response"
                );

                boolean success =
                        responseCode >= 200 && responseCode < 300;

                runOnUiThread(() -> {

                    btnSubmitReport.setEnabled(true);
                    btnSubmitReport.setText("Submit Report");

                    if (success) {
                        Toast.makeText(
                                ReportItemActivity.this,
                                "Report saved successfully!",
                                Toast.LENGTH_LONG
                        ).show();

                        // Clear the form after successful submission
                        etItemName.setText("");
                        etDescription.setText("");
                        etLocation.setText("");
                        etDate.setText("");

                    } else {
                        Toast.makeText(
                                ReportItemActivity.this,
                                "Error: " + message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    btnSubmitReport.setEnabled(true);
                    btnSubmitReport.setText("Submit Report");

                    Toast.makeText(
                            ReportItemActivity.this,
                            "Connection failed. Check your backend. "
                                    + e.getMessage(),
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}