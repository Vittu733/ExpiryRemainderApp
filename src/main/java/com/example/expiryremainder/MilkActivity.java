package com.example.expiryremainder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MilkActivity extends AppCompatActivity {

    private EditText editTextName, editTextDate;
    private Button addButton;
    private ListView listView;
    private ArrayList<String> milkList;
    private ArrayAdapter<String> adapter;
    private static final String CHANNEL_ID = "expiry_reminder_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_milk);

        editTextName = findViewById(R.id.editTextTextPersonName);
        editTextDate = findViewById(R.id.editTextTextPersonName3);
        addButton = findViewById(R.id.mbutton1);
        listView = findViewById(R.id.list);

        milkList = loadMilkList();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, milkList);
        listView.setAdapter(adapter);

        createNotificationChannel();

        addButton.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String date = editTextDate.getText().toString().trim();

            if (!name.isEmpty() && !date.isEmpty()) {
                milkList.add(name + " - " + date);
                adapter.notifyDataSetChanged();
                saveMilkList(milkList);

                showNotification(name);
                scheduleNotification(name, date);

                editTextName.setText("");
                editTextDate.setText("");
            } else {
                Toast.makeText(MilkActivity.this, "Please enter both Name and Expiry Date", Toast.LENGTH_SHORT).show();
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            milkList.remove(position);
            adapter.notifyDataSetChanged();
            saveMilkList(milkList);
            Toast.makeText(MilkActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void saveMilkList(ArrayList<String> list) {
        SharedPreferences prefs = getSharedPreferences("MilkPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> set = new HashSet<>(list);
        editor.putStringSet("milk_list", set);
        editor.apply();
    }

    private ArrayList<String> loadMilkList() {
        SharedPreferences prefs = getSharedPreferences("MilkPrefs", MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("milk_list", new HashSet<>());
        return new ArrayList<>(set);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Expiry Reminder Channel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Channel for expiry reminders");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String itemName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Milk Item Added")
                .setContentText(itemName + " has been added to Milk products.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void scheduleNotification(String itemName, String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        try {
            Date expiryDate = sdf.parse(dateString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(expiryDate);
            cal.add(Calendar.DAY_OF_YEAR, -3); // 3 days before expiry

            Intent intent = new Intent(this, NotificationReceiver.class);
            intent.putExtra("itemName", itemName);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}