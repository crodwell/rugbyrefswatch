package com.refrugby.watch;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.ViewGroup;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;
import java.util.ArrayList;


public class MainMenuAdapter extends RecyclerView.Adapter<MainMenuAdapter.RecyclerViewHolder> {

    private ArrayList<MenuItem> dataSource;
    public interface AdapterCallback{
        void onItemClicked(String menuId);
    }
    private AdapterCallback callback;

    public MainMenuAdapter(ArrayList<MenuItem> dataArgs, AdapterCallback callback){
        this.dataSource = dataArgs;
        this.callback = callback;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_menu_item,parent,false);
        return new RecyclerViewHolder(view);
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder
    {
        RelativeLayout menuContainer;
        TextView menuItem;
        ImageView menuIcon;

        public RecyclerViewHolder(View view) {
            super(view);
            menuContainer = view.findViewById(R.id.menu_container);
            menuItem = view.findViewById(R.id.menu_item);
            menuIcon = view.findViewById(R.id.menu_icon);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, final int position) {
        final MenuItem data_provider = dataSource.get(position);

        holder.menuItem.setText(data_provider.getText());
        holder.menuIcon.setImageResource(data_provider.getImage());
        holder.menuContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                if(callback != null) {
                    callback.onItemClicked(data_provider.getId());
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return dataSource.size();
    }
}



class MenuItem
{
    private String id;
    private String text;
    private int image;

    public MenuItem(String id, int image, String text) {
        this.id = id;
        this.image = image;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public int getImage() {
        return image;
    }

}