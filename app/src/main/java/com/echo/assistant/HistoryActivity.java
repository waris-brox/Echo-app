package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends Activity {

    protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            setContentView(
                R.layout.activity_history);

            if (findViewById(R.id.btn_back)
                != null)
                findViewById(R.id.btn_back)
                    .setOnClickListener(
                    v -> finish());

            RecyclerView rv =
                findViewById(R.id.rv_history);
            if (rv == null) return;

            rv.setLayoutManager(
                new LinearLayoutManager(this));

            JSONArray history =
                ChatHistory.load(this);
            List<String[]> items =
                new ArrayList<>();

            for (int i = 0;
                i < history.length(); i++) {
                try {
                    JSONObject m =
                        history.getJSONObject(i);
                    if (m.getString("role")
                        .equals("user")) {
                        items.add(new String[]{
                            m.getString("content"),
                            "Tap to continue"
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Collections.reverse(items);

            if (items.isEmpty()) {
                TextView empty =
                    new TextView(this);
                empty.setText(
                    "No history yet Boss."
                    + " Start chatting!");
                empty.setTextColor(
                    getResources().getColor(
                    R.color.text_dim));
                empty.setPadding(
                    40, 60, 40, 60);
                empty.setTextSize(15);
                empty.setGravity(
                    android.view.Gravity.CENTER);
                RecyclerView.LayoutParams lp =
                    new RecyclerView
                    .LayoutParams(
                    ViewGroup.LayoutParams
                    .MATCH_PARENT,
                    ViewGroup.LayoutParams
                    .WRAP_CONTENT);
                empty.setLayoutParams(lp);
                rv.setAdapter(
                    new RecyclerView.Adapter() {
                    public RecyclerView
                        .ViewHolder
                        onCreateViewHolder(
                        ViewGroup p, int t) {
                        return new RecyclerView
                            .ViewHolder(empty){};
                    }
                    public void
                        onBindViewHolder(
                        RecyclerView.ViewHolder h,
                        int pos) {}
                    public int getItemCount() {
                        return 1;
                    }
                });
                return;
            }

            List<String[]> finalItems = items;
            rv.setAdapter(
                new RecyclerView.Adapter
                <RecyclerView.ViewHolder>() {

                public RecyclerView.ViewHolder
                    onCreateViewHolder(
                    ViewGroup p, int t) {
                    View v =
                        getLayoutInflater()
                        .inflate(
                        R.layout.item_history,
                        p, false);
                    return new RecyclerView
                        .ViewHolder(v) {};
                }

                public void onBindViewHolder(
                    RecyclerView.ViewHolder h,
                    int pos) {
                    try {
                        TextView preview =
                            h.itemView
                            .findViewById(
                            R.id.tv_preview);
                        TextView date =
                            h.itemView
                            .findViewById(
                            R.id.tv_date);
                        if (preview != null)
                            preview.setText(
                            finalItems
                            .get(pos)[0]);
                        if (date != null)
                            date.setText(
                            finalItems
                            .get(pos)[1]);
                        h.itemView
                            .setOnClickListener(
                            v -> {
                            try {
                                Intent i =
                                    new Intent(
                                    HistoryActivity
                                    .this,
                                    ChatActivity
                                    .class);
                                i.putExtra(
                                    "message",
                                    finalItems
                                    .get(pos)[0]);
                                startActivity(i);
                            } catch (
                                Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public int getItemCount() {
                    return finalItems.size();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
