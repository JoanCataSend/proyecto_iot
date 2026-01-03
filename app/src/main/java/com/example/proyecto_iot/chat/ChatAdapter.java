package com.example.proyecto_iot.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iot.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_ASSISTANT = 2;

    private final List<ChatMessage> items;

    // ✅ Constructor vacío para que tu ChatbotActivity pueda hacer: new ChatAdapter()
    public ChatAdapter() {
        this.items = new ArrayList<>();
    }

    // ✅ Si alguna vez quieres pasar lista desde fuera
    public ChatAdapter(@NonNull List<ChatMessage> items) {
        this.items = items;
    }

    // ✅ Lo que tu Activity está intentando usar
    public void add(@NonNull ChatMessage msg) {
        items.add(msg);
        notifyItemInserted(items.size() - 1);
    }

    // ✅ Lo que tu OpenAIService necesita: el historial
    public List<ChatMessage> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = items.get(position);
        return msg.isUser() ? TYPE_USER : TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_USER) {
            View v = inflater.inflate(R.layout.item_message_user, parent, false);
            return new MsgVH(v);
        } else {
            View v = inflater.inflate(R.layout.item_message_assistant, parent, false);
            return new MsgVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = items.get(position);
        ((MsgVH) holder).tvMessage.setText(msg.getText());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MsgVH extends RecyclerView.ViewHolder {
        TextView tvMessage;

        MsgVH(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}
