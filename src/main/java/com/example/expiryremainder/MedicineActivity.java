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

public class MedicineActivity extends AppCompatActivity {

    private EditText editTextName, editTextDate;
    private Button addButton;
    private ListView listView;
    private ArrayList<String> medicineList;
    private ArrayAdapter<String> adapter;
    private static final String CHANNEL_ID = "expiry_reminder_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine);

        editTextName = findViewById(R.id.editTextTextPersonName);
        editTextDate = findViewById(R.id.editTextTextPersonName3);
        addButton = findViewById(R.id.mbutton1);
        listView = findViewById(R.id.list);

        medicineList = loadMedicineList();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, medicineList);
        listView.setAdapter(adapter);

        createNotificationChannel();

        addButton.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String date = editTextDate.getText().toString().trim();

            if (!name.isEmpty() && !date.isEmpty()) {
                medicineList.add(name + " - " + date);
                adapter.notifyDataSetChanged();
                saveMedicineList(medicineList);

                showNotification(name); // immediate notification
                scheduleNotification(name, date); // 3-day before expiry notification

                editTextName.setText("");
                editTextDate.setText("");
            } else {
                Toast.makeText(MedicineActivity.this, "Please enter both Name and Expiry Date", Toast.LENGTH_SHORT).show();
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            medicineList.remove(position);
            adapter.notifyDataSetChanged();
            saveMedicineList(medicineList);
            Toast.makeText(MedicineActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void saveMedicineList(ArrayList<String> list) {
        SharedPreferences prefs = getSharedPreferences("MedicinePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> set = new HashSet<>(list);
        editor.putStringSet("medicine_list", set);
        editor.apply();
    }

    private ArrayList<String> loadMedicineList() {
        SharedPreferences prefs = getSharedPreferences("MedicinePrefs", MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("medicine_list", new HashSet<>());
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
                .setContentTitle("Medicine Item Added")
                .setContentText(itemName + " has been added to Medicine products.")
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
