package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import org.json.*;
import java.util.*;

public class HistoryActivity extends Activity {

    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_history);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(this));

        JSONArray history = ChatHistory.load(this);
        List<String> items = new ArrayList<>();

        for (int i = 0; i < history.length(); i++) {
            try {
                JSONObject m = history.getJSONObject(i);
                if (m.getString("role").equals("user")) {
                    items.add(m.getString("content"));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        Collections.reverse(items);

        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                View v = getLayoutInflater().inflate(R.layout.item_history, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                TextView preview = h.itemView.findViewById(R.id.tv_preview);
                TextView date = h.itemView.findViewById(R.id.tv_date);
                preview.setText(items.get(pos));
                date.setText("Tap to continue");
                h.itemView.setOnClickListener(v ->
                    startActivity(new Intent(HistoryActivity.this, ChatActivity.class)
                        .putExtra("message", items.get(pos))));
            }
            public int getItemCount() { return items.size(); }
        });
    }
}
