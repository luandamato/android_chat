package com.example.chat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.WebSocket;

public class ChatIoActivity extends AppCompatActivity implements TextWatcher {

    private String nome = "";
    //private String server_path = "http://18.230.130.206:3000";
    private String server_path = "http://192.168.0.18:3000";
    private EditText txtMensagem;
    private TextView lblEnviar;
    private ImageView img;
    private RecyclerView recyclerView;
    private int image_request_id = 1;
    private ChatAdapter adapter;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(server_path);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_io);

        mSocket.on("mensagemRecebida", onNewMessage);
        mSocket.on("mensagensAnteriores", historicoMensagens);
        mSocket.on("novaConexao", onNewConnection);
        mSocket.on(Socket.EVENT_CONNECT, conectar);
        mSocket.connect();

        txtMensagem = findViewById(R.id.txtMensagem);
        lblEnviar = findViewById(R.id.lblEnviar);
        img = findViewById(R.id.imgChose);
        recyclerView = findViewById(R.id.recyclerView);

        nome = getIntent().getStringExtra("nome");adapter = new ChatAdapter(getLayoutInflater());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        txtMensagem.addTextChangedListener(this);

        lblEnviar.setOnClickListener(v -> {
            JSONObject o = new JSONObject();
            try{
                o.put("nome", nome);
                o.put("msg", txtMensagem.getText().toString().trim());
                o.put("id", mSocket.id());
                mSocket.emit("enviarMensagem", o);

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

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    JSONObject o = new JSONObject();
                    try{
                        o.put("nome", data.getString("nome"));
                        o.put("msg", data.getString("msg"));
                        o.put("id", data.getString("id"));
                        o.put("enviado", false);

                        adapter.addItem(o);
                        recyclerView.smoothScrollToPosition(adapter.getItemCount());
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private Emitter.Listener onNewConnection = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    JSONObject o = new JSONObject();
                    try{
                        o.put("nome", data.getString("nome"));
                        o.put("entrou", true);

                        adapter.addItem(o);
                        recyclerView.smoothScrollToPosition(adapter.getItemCount());
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private Emitter.Listener historicoMensagens = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONArray data = (JSONArray) args[0];


                    for (int i = 0; i < data.length(); i++){
                        JSONObject o = new JSONObject();
                        try{
                            o.put("nome", data.getJSONObject(i).getString("nome"));
                            o.put("msg", data.getJSONObject(i).getString("msg"));
                            o.put("id", data.getJSONObject(i).getString("id"));
                            o.put("enviado", false);

                            adapter.addItem(o);
                            recyclerView.smoothScrollToPosition(adapter.getItemCount());
                        }
                        catch (JSONException e){
                            e.printStackTrace();
                        }
                    }

                }
            });
        }
    };

    private Emitter.Listener conectar = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //JSONObject data = (JSONObject) args[0];
                    String token = ChatIoActivity.this.getSharedPreferences("_", MODE_PRIVATE).getString("fb", "empty");;
                    Log.i("socketConectado", token);
                    JSONObject o = new JSONObject();
                    try{
                        o.put("nome", nome);
                        o.put("id", mSocket.id());
                        o.put("pushTokenId", token);
                        mSocket.emit("conectar", o);

                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }

                }
            });
        }
    };

    private void sendImage(Bitmap image){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 50, output);
        String base64 = Base64.encodeToString(output.toByteArray(), Base64.DEFAULT);

        JSONObject o = new JSONObject();
        try{
            o.put("nome", nome);
            o.put("img", base64);
            o.put("id", mSocket.id());
            mSocket.emit("enviarMensagem", o);

            o.put("enviado", true);
            adapter.addItem(o);
            recyclerView.smoothScrollToPosition(adapter.getItemCount());
        }
        catch (JSONException e){
            e.printStackTrace();
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off("mensagemRecebida", onNewMessage);
        mSocket.off("mensagensAnteriores", historicoMensagens);
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