package com.example.chat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter {

    private static final int msg_enviada = 0;
    private static final int msg_recebida = 1;
    private static final int img_enviada = 2;
    private static final int img_recebida = 3;
    private static final int new_user = 4;

    private String nomeUltimaMensagem = "";

    private LayoutInflater infalter;
    private List<JSONObject> msgs = new ArrayList<>();

    public ChatAdapter (LayoutInflater pInflater){
        this.infalter = pInflater;
    }

    private class MensagemEnviadaCell extends RecyclerView.ViewHolder{

        private TextView lblMsg;

        public MensagemEnviadaCell(@NonNull View itemView) {
            super(itemView);

            lblMsg = itemView.findViewById(R.id.lblMsg);
        }
    }
    private class MensagemRecebidaCell extends RecyclerView.ViewHolder{

        private TextView lblMsg;
        private TextView lblNome;

        public MensagemRecebidaCell(@NonNull View itemView) {
            super(itemView);

            lblMsg = itemView.findViewById(R.id.lblMsg);
            lblNome = itemView.findViewById(R.id.lblNome);
        }
    }
    private class ImagemEnviadaCell extends RecyclerView.ViewHolder{

        private ImageView img;

        public ImagemEnviadaCell(@NonNull View itemView) {
            super(itemView);

            img = itemView.findViewById(R.id.img);
        }
    }
    private class ImagemRecebidaCell extends RecyclerView.ViewHolder{

        private ImageView img;
        private TextView lblNome;

        public ImagemRecebidaCell(@NonNull View itemView) {
            super(itemView);

            img = itemView.findViewById(R.id.img);
            lblNome = itemView.findViewById(R.id.lblNome);
        }
    }
    private class NovoUsuarioCell extends RecyclerView.ViewHolder{

        private TextView lblMsg;

        public NovoUsuarioCell(@NonNull View itemView) {
            super(itemView);

            lblMsg = itemView.findViewById(R.id.lblMsg);
        }
    }


    @Override
    public int getItemViewType(int position) {
        JSONObject msg = msgs.get(position);
        try{
            if (msg.has("entrou")){
                return new_user;
            }
            if (msg.getBoolean("enviado")){
                if (msg.has("msg")){
                    return msg_enviada;
                }
                else{
                    return img_enviada;
                }
            }
            else{
                if (msg.has("msg")){
                    return msg_recebida;
                }
                else{
                    return img_recebida;
                }
            }
        }
        catch (Exception e){

        }
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        switch (viewType){
            case msg_enviada:
                view = infalter.inflate(R.layout.item_sent_message, parent, false);
                return new MensagemEnviadaCell(view);
            case msg_recebida:
                view = infalter.inflate(R.layout.item_recive_message, parent, false);
                return new MensagemRecebidaCell(view);
            case img_enviada:
                view = infalter.inflate(R.layout.item_sent_image, parent, false);
                return new ImagemEnviadaCell(view);
            case img_recebida:
                view = infalter.inflate(R.layout.item_recive_image, parent, false);
                return new ImagemRecebidaCell(view);
            case new_user:
                view = infalter.inflate(R.layout.item_new_user, parent, false);
                return new NovoUsuarioCell(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        JSONObject msg = msgs.get(position);
        try{
            if (msg.has("entrou")){
                NovoUsuarioCell cell = (NovoUsuarioCell) holder;
                if (msg.getBoolean("entrou")){
                    cell.lblMsg.setText(msg.getString("nome") + " Entrou");
                }
                else{
                    cell.lblMsg.setText(msg.getString("nome") + " Saiu");
                }

            }
            else{
                if (msg.getBoolean("enviado")){
                    if (msg.has("msg")){
                        MensagemEnviadaCell cell = (MensagemEnviadaCell) holder;
                        cell.lblMsg.setText(msg.getString("msg"));
                    }
                    else{
                        ImagemEnviadaCell cell = (ImagemEnviadaCell) holder;
                        Bitmap imagem = getBitmapFromString(msg.getString("img"));
                        cell.img.setImageBitmap(imagem);
                    }
                }
                else{
                    if (msg.has("msg")){
                        MensagemRecebidaCell cell = (MensagemRecebidaCell) holder;
                        cell.lblMsg.setText(msg.getString("msg"));
                        cell.lblNome.setText(msg.getString("nome"));
                        if (nomeUltimaMensagem == msg.getString("nome")){
                            cell.lblNome.setVisibility(View.GONE);
                        }
                    }
                    else{
                        ImagemRecebidaCell cell = (ImagemRecebidaCell) holder;
                        Bitmap imagem = getBitmapFromString(msg.getString("img"));
                        cell.img.setImageBitmap(imagem);
                        cell.lblNome.setText(msg.getString("nome"));
                        if (nomeUltimaMensagem == msg.getString("nome")){
                            cell.lblNome.setVisibility(View.GONE);
                        }
                    }
                }
                nomeUltimaMensagem = msg.getString("nome");
            }

        }
        catch (Exception e){
            Log.i("erro msgm", e.toString());
        }
    }

    @Override
    public int getItemCount() {
        return msgs.size();
    }

    private Bitmap getBitmapFromString(String base64){
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public void addItem(JSONObject objeto){
        msgs.add(objeto);
        notifyDataSetChanged();
    }
}
