package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MainActivity extends AppCompatActivity {

    private Button btnEntrar;
    private EditText txtNome;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnEntrar = findViewById(R.id.btnEntrar);
        txtNome = findViewById(R.id.txtNome);

        String token = FirebaseMessaging.getInstance().getToken().toString();

        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                proxTela();
            }
        });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {

                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.i("TOKEN", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        String msg =  token;
                        Log.d("TOKEN", msg);
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fb", token).apply();
                    }
                });



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 10);
        }

    }

    private void proxTela(){
        if (txtNome.getText().toString().isEmpty()){
            Toast.makeText(MainActivity.this, "Preencha seu nome para entrar na conversa", Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(this, ChatIoActivity.class);
        i.putExtra("nome", txtNome.getText().toString());
        startActivity(i);
    }


}

//class MyFirebaseMessagingService extends FirebaseMessagingService {
//
//    @Override
//    public void onNewToken(String s) {
//        super.onNewToken(s);
//        Log.e("newToken", s);
//        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fb", s).apply();
//    }
//
//    @Override
//    public void onMessageReceived(RemoteMessage remoteMessage) {
//        super.onMessageReceived(remoteMessage);
//    }
//
//    public static String getToken(Context context) {
//        return context.getSharedPreferences("_", MODE_PRIVATE).getString("fb", "empty");
//    }
//}
