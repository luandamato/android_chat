package com.example.chat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.WebSocket;

public class ChatIoActivity extends AppCompatActivity implements TextWatcher {

    private String nome = "";
    private String server_path = "http://18.230.130.206:3000";
//    private String server_path = "http://192.168.0.18:3000";
    private EditText txtMensagem;
    private TextView lblEnviar;
    private TextView lblNomeConversa;
    private TextView lblIntegrantes;
    private ImageView img;
    private RecyclerView recyclerView;
    private int image_request_id = 1;
    private ChatAdapter adapter;

    private Boolean dig = false;
    private List<String> digitando = new ArrayList<>();
    private List<JSONObject> conectados = new ArrayList<>();

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
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getSupportActionBar().hide();

        setContentView(R.layout.activity_chat_io);



        txtMensagem = findViewById(R.id.txtMensagem);
        lblEnviar = findViewById(R.id.lblEnviar);
        lblNomeConversa = findViewById(R.id.lblNomeConversa);
        lblIntegrantes = findViewById(R.id.lblIntegrantes);
        img = findViewById(R.id.imgChose);
        recyclerView = findViewById(R.id.recyclerView);

        nome = getIntent().getStringExtra("nome");adapter = new ChatAdapter(getLayoutInflater());

        iniciarSocket();
        configurarControles();

    }

    private void iniciarSocket(){
        mSocket.on("mensagemRecebida", onNewMessage);
        mSocket.on("mensagensAnteriores", historicoMensagens);
        mSocket.on("conexoesAnteriores", historicoConexoes);
        mSocket.on("novaConexao", onNewConnection);
        mSocket.on("desconectado", onCloseConnection);
        mSocket.on("digitando", onDigitando);
        mSocket.on("parouDigitar", onParouDigitar);
        mSocket.on(Socket.EVENT_CONNECT, conectar);
        mSocket.connect();
    }

    private void configurarControles(){
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
//            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//            intent.setType("image/*");
//            startActivityForResult(Intent.createChooser(intent, "Imagem"), image_request_id);

            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
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
                        if (data.has("msg")) o.put("msg", data.getString("msg"));
                        if (data.has("img")) o.put("img", data.getString("img"));
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

    private Emitter.Listener onDigitando = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    try{
                       digitando.add(data.getString("nome"));
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    configurarLblDigitando();

                }
            });
        }
    };

    private Emitter.Listener onParouDigitar = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    try{
                        digitando.remove(data.getString("nome"));
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    configurarLblDigitando();

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

                    try{
                        conectados.add(data);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    configurarLblDigitando();
                }
            });
        }
    };

    private Emitter.Listener onCloseConnection = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    JSONObject o = new JSONObject();
                    try{
                        o.put("nome", data.getString("nome"));
                        o.put("entrou", false);

                        adapter.addItem(o);
                        recyclerView.smoothScrollToPosition(adapter.getItemCount());
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }

                    try{
                        int i = 0;
                        for (JSONObject item: conectados){
                            if (item.getString("nome").equals(data.getString("nome"))){
                                conectados.remove(item);
                                break;
                            }
                            i++;
                        }
                        //conectados.remove(i);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    configurarLblDigitando();
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
                            if (data.getJSONObject(i).has("msg")) o.put("msg", data.getJSONObject(i).getString("msg"));
                            if (data.getJSONObject(i).has("img")) o.put("img", data.getJSONObject(i).getString("img"));
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

    private Emitter.Listener historicoConexoes = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONArray data = (JSONArray) args[0];


                    for (int i = 0; i < data.length(); i++){
                        JSONObject o = new JSONObject();
                        try{
                            o = data.getJSONObject(i);
                            conectados.add(o);
                        }
                        catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                    configurarLblDigitando();

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
                        //o.put("pushTokenId", token);
                        mSocket.emit("conectar", o);

                        conectados.add(o);

                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                    configurarLblDigitando();

                }
            });
        }
    };

    private void configurarLblDigitando(){
        String str = "";
        if (digitando.isEmpty()){
            if (!conectados.isEmpty()){

                for (JSONObject item: conectados) {
                    try{
                        if (!str.isEmpty()){
                            str += ", ";
                        }
                        if (item.getString("id").equals(mSocket.id())){
                            str += "você";
                        }
                        else{
                            str += item.getString("nome");
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }
        }
        else{
            if (digitando.size() > 3){
                str = "Diversas pessoas estão digitando...";
            }
            else{
                for (String item: digitando) {
                    try{
                        if (!str.isEmpty()){
                            str += ",";
                        }
                        str += item;
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                }
                if(digitando.size() == 1){
                    str += " está digitando...";
                }
                else{
                    str += " estão digitando...";
                }

            }
        }
        lblIntegrantes.setText(str);
    }

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

        if (!dig){
            JSONObject o = new JSONObject();
            try{
                o.put("nome", nome);
                o.put("id", mSocket.id());
                mSocket.emit("digitando", o);

            }
            catch (JSONException e){
                e.printStackTrace();
            }
            dig = true;
        }

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

        JSONObject o = new JSONObject();
        dig = false;
        try{
            o.put("nome", nome);
            o.put("id", mSocket.id());
            mSocket.emit("parouDigitar", o);

        }
        catch (JSONException e){
            e.printStackTrace();
        }
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
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
//                Uri frontUri = result.getUri();
//                binding.imageFront.setImageURI(frontUri);
//                String frontpathString = getRealPathFromURI(frontUri);
//                frontfile = new File(frontpathString);
//                frontfile = new File(frontpathString);des
//                binding.tvFrontfileName.setText("File Name : "+ frontfile.getName());
                try{
//                    InputStream is = getContentResolver().openInputStream(data.getData());
//                    Bitmap image = BitmapFactory.decodeStream(is);
                    Uri selectedfile = result.getUri();
                    if (selectedfile != null) {
                       Bitmap image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedfile);
                        sendImage(image);
                    }


                }
                catch (Exception e){
                    e.printStackTrace();
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }


//        if(requestCode == image_request_id && resultCode == RESULT_OK){
//            try{
//                InputStream is = getContentResolver().openInputStream(data.getData());
//                Bitmap image = BitmapFactory.decodeStream(is);
//
//                sendImage(image);
//            }
//            catch (Exception e){
//                e.printStackTrace();
//            }
//        }
    }
}