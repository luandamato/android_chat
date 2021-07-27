package com.example.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatActivity extends AppCompatActivity implements TextWatcher {

    private String nome = "";
    private WebSocket webSocket;
    private String server_path = "ws://192.168.0.18:3000";
    private EditText txtMensagem;
    private TextView lblEnviar;
    private ImageView img;
    private RecyclerView recyclerView;
    private int image_request_id = 1;
    private ChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        txtMensagem = findViewById(R.id.txtMensagem);
        lblEnviar = findViewById(R.id.lblEnviar);
        img = findViewById(R.id.imgChose);
        recyclerView = findViewById(R.id.recyclerView);

        nome = getIntent().getStringExtra("nome");
        initSocket();
    }

    private void initSocket(){
        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder().url(server_path).build();
        webSocket = client.newWebSocket(req, new socketListener());
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        String texto = editable.toString();
        if (texto.trim().isEmpty()){
            resetTxt();
        }
        else{
            lblEnviar.setVisibility(View.VISIBLE);
            img.setVisibility(View.GONE);
        }
    }

    private void resetTxt(){
        txtMensagem.removeTextChangedListener(this);
        txtMensagem.setText("");
        lblEnviar.setVisibility(View.GONE);
        img.setVisibility(View.VISIBLE);
        txtMensagem.addTextChangedListener(this);
    }

    private class socketListener extends WebSocketListener{

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            runOnUiThread(() ->{
                Toast.makeText(ChatActivity.this, "conectado", Toast.LENGTH_LONG).show();
                initView();
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            runOnUiThread(() -> {
                try{
                    JSONObject o = new JSONObject(text);
                    o.put("enviado", false);

                    adapter.addItem(o);
                    recyclerView.smoothScrollToPosition(adapter.getItemCount());

                }
                catch (JSONException e){
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
        }
    }

    private void initView(){
        adapter = new ChatAdapter(getLayoutInflater());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        txtMensagem.addTextChangedListener(this);

        lblEnviar.setOnClickListener(v -> {
            JSONObject o = new JSONObject();
            try{
                o.put("nome", nome);
                o.put("msg", txtMensagem.getText().toString().trim());

                webSocket.send(o.toString());

                o.put("enviado", true);
                adapter.addItem(o);
                recyclerView.smoothScrollToPosition(adapter.getItemCount());
                resetTxt();
            }
            catch (JSONException e){
                e.printStackTrace();
            }
        });

        img.setOnClickListener(v ->{
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

            startActivityForResult(Intent.createChooser(intent, "Imagem"), image_request_id);
        });
    }

    private void sendImage(Bitmap image){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 50, output);
        String base64 = Base64.encodeToString(output.toByteArray(), Base64.DEFAULT);

        JSONObject o = new JSONObject();
        try{
            o.put("nome", nome);
            o.put("img", base64);

            webSocket.send(o.toString());

            o.put("enviado", true);
            adapter.addItem(o);
            recyclerView.smoothScrollToPosition(adapter.getItemCount());
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == image_request_id && resultCode == RESULT_OK){
            try{
                InputStream is = getContentResolver().openInputStream(data.getData());
                Bitmap image = BitmapFactory.decodeStream(is);

                sendImage(image);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}