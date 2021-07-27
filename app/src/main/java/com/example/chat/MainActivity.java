package com.example.chat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Button btnEntrar;
    private EditText txtNome;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnEntrar = findViewById(R.id.btnEntrar);
        txtNome = findViewById(R.id.txtNome);

        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                proxTela();
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