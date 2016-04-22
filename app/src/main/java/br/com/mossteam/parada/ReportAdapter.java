package br.com.mossteam.parada;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private Context context;
    private JSONArray reports;

    public JSONArray getReports() {
        return reports;
    }

    public void setReports(JSONArray reports) {
        this.reports = reports;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View view;
        public TextView mTextView;
        public TextView mTextView1;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            mTextView = (TextView) view.findViewById(R.id.bus_code);
            mTextView1 = (TextView) view.findViewById(R.id.date);
        }
    }

    public ReportAdapter(Context context, JSONArray reports) {
        this.context = context;
        this.reports = reports;
    }

    @Override
    public ReportAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.component_report_list, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        try {
            holder.mTextView.setText(reports.getJSONObject(position).getString("bus_code"));
            Locale.setDefault(new Locale("pt", "BR"));
            final SimpleDateFormat dateFormat = new SimpleDateFormat
                    ("dd/MM hh:mm", Locale.getDefault());
            Date date = new Date(Long.valueOf(reports
                    .getJSONObject(position).getString("date")));
            holder.mTextView1.setText(dateFormat.format(date));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        holder.mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ReportActivity.class);
                try {
                    intent.putExtra("_id", reports.getJSONObject(position)
                            .getString("_id"));
                    Log.d("json", reports.getJSONObject(position).getString
                            ("_, id"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reports.length();
    }
}
