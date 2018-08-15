package com.refrugby.watch;

import android.view.View;
import android.widget.TextView;
import android.view.ViewGroup;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.SimpleViewHolder> {
    private String[] dataSource;
    public interface AdapterCallback{
        void onItemClicked(String player);
    }
    private AdapterCallback callback;



    public RecyclerViewAdapter(AdapterCallback callback, String[] dataArgs){
        this.dataSource = dataArgs;
        this.callback = callback;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = new TextView(parent.getContext());
        return new SimpleViewHolder(view);
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder{
        public TextView textView;
        public SimpleViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }

    @Override
    public void onBindViewHolder(final SimpleViewHolder holder, final int position) {
        holder.textView.setTextSize(30);
        holder.textView.setGravity(Gravity.CENTER);
        holder.textView.setText(dataSource[position]);
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                // Define setting holder
                //
                if(callback != null) {
                    callback.onItemClicked(dataSource[position]);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataSource.length;
    }
}