package com.example.tunispromo.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tunispromo.R;
import com.example.tunispromo.activities.PromoDetailActivity;
import com.example.tunispromo.models.FavoritePromo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter RecyclerView pour afficher les favoris lus depuis Firestore (TP6).
 */
public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private final Context context;
    private final List<FavoritePromo> favorites;

    public FavoritesAdapter(Context context) {
        this.context = context;
        this.favorites = new ArrayList<>();
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite_card, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        FavoritePromo fav = favorites.get(position);

        holder.tvTitre.setText(safeText(fav.getTitre()));
        holder.tvSite.setText(safeText(fav.getSiteSource()));
        holder.tvPrixPromo.setText(String.format(Locale.FRANCE, "%.2f TND", fav.getPrixPromo()));

        Glide.with(context)
                .load(fav.getImageUrl())
                .placeholder(R.drawable.ic_promo_placeholder)
                .error(R.drawable.ic_broken_image)
                .centerCrop()
                .into(holder.ivPromo);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent explicite vers le détail — voir TP2
                Intent intent = new Intent(context, PromoDetailActivity.class);
                intent.putExtra("id", fav.getId());
                intent.putExtra("titre", fav.getTitre());
                intent.putExtra("prix_promo", fav.getPrixPromo());
                intent.putExtra("prix_original", 0.0);
                intent.putExtra("reduction", 0.0);
                intent.putExtra("image_url", fav.getImageUrl());
                intent.putExtra("site_source", fav.getSiteSource());
                intent.putExtra("lien_promo", "");
                intent.putExtra("categorie", "");
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    public void setFavorites(List<FavoritePromo> list) {
        favorites.clear();
        if (list != null) {
            favorites.addAll(list);
        }
        notifyDataSetChanged();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPromo;
        TextView tvTitre;
        TextView tvSite;
        TextView tvPrixPromo;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPromo = itemView.findViewById(R.id.iv_promo);
            tvTitre = itemView.findViewById(R.id.tv_titre);
            tvSite = itemView.findViewById(R.id.tv_site);
            tvPrixPromo = itemView.findViewById(R.id.tv_prix_promo);
        }
    }
}
