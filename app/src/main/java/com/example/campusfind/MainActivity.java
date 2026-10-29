package com.example.campusfind;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the buttons from activity_main.xml
        Button btnReportLost = findViewById(R.id.btnReportLost);
        Button btnReportFound = findViewById(R.id.btnReportFound);
        Button btnViewItems = findViewById(R.id.btnViewItems);

        // Open the Lost Item form
        btnReportLost.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    ReportItemActivity.class
            );

            intent.putExtra("reportType", "Lost");
            startActivity(intent);
        });

        // Open the Found Item form
        btnReportFound.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    ReportItemActivity.class
            );

            intent.putExtra("reportType", "Found");
            startActivity(intent);
        });

        // Open the list of reported items
        btnViewItems.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    activity_view_items.class
            );

            startActivity(intent);
        });
    }
}