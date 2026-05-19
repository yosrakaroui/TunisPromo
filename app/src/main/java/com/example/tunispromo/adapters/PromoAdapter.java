package com.example.tunispromo.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tunispromo.R;
import com.example.tunispromo.activities.MainActivity;
import com.example.tunispromo.activities.PromoDetailActivity;
import com.example.tunispromo.models.Promotion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter RecyclerView avec ViewHolder pour afficher les promotions sous forme de cartes.
 *
 * <p>Cette classe est l'application directe du TP6: le RecyclerView demande à l'adapter
 * de créer des lignes, de lier les données aux vues, puis de recycler les anciennes vues
 * pour garder une interface fluide même avec beaucoup de promotions.</p>
 */
public class PromoAdapter extends RecyclerView.Adapter<PromoAdapter.PromoViewHolder> {

    /** Contexte Android utilisé pour démarrer l'activité de détail et charger les images. */
    private final Context context;

    /** Liste actuellement affichée après recherche ou chargement. */
    private final List<Promotion> promotions;

    /** Liste complète gardée en mémoire pour pouvoir réinitialiser le filtre. */
    private final List<Promotion> allPromotions;

    /**
     * Construit l'adapter avec une liste initiale de promotions.
     *
     * @param context Contexte de l'activité qui possède le RecyclerView.
     * @param promotions Liste initiale à afficher.
     * @return Rien, le constructeur prépare les listes internes.
     */
    public PromoAdapter(Context context, List<Promotion> promotions) {
        this.context = context;
        this.promotions = new ArrayList<>(promotions);
        this.allPromotions = new ArrayList<>(promotions);
    }

    /**
     * Crée une nouvelle vue de carte quand le RecyclerView en a besoin.
     *
     * <p>Inflate signifie "gonfler le layout XML": Android transforme item_promo_card.xml
     * en vrais objets View Java manipulables dans le code.</p>
     *
     * @param parent Parent RecyclerView qui contiendra la carte.
     * @param viewType Type de vue, non utilisé ici car toutes les lignes ont le même layout.
     * @return ViewHolder contenant les références vers les vues de la carte.
     */
    @NonNull
    @Override
    public PromoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_promo_card, parent, false);
        return new PromoViewHolder(view);
    }

    /**
     * Lie les données d'une promotion à une carte visible à l'écran.
     *
     * <p>Lier les données à la vue signifie remplir les TextView et ImageView avec les valeurs
     * du modèle Promotion. Cette méthode est appelée souvent, donc elle reste simple.</p>
     *
     * @param holder ViewHolder qui contient les vues déjà trouvées.
     * @param position Position de la promotion dans la liste affichée.
     * @return Rien, la méthode modifie directement les vues du ViewHolder.
     */
    @Override
    public void onBindViewHolder(@NonNull PromoViewHolder holder, int position) {
        Promotion promo = promotions.get(position);

        holder.tvTitre.setText(safeText(promo.getTitre()));
        holder.tvSite.setText(safeText(promo.getSiteSource()));
        holder.tvPrixPromo.setText(String.format(Locale.FRANCE, "%.2f TND", promo.getPrixPromo()));
        holder.tvPrixOriginal.setText(String.format(Locale.FRANCE, "%.2f TND", promo.getPrixOriginal()));
        holder.tvReduction.setText(String.format(Locale.FRANCE, "-%.0f%%", promo.getReduction()));

        // Le texte barré montre visuellement que le prix original n'est plus le prix actuel.
        holder.tvPrixOriginal.setPaintFlags(holder.tvPrixOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // Glide charge l'image réseau sans bloquer le fil principal de l'interface.
        Glide.with(context)
                .load(promo.getImageUrl())
                .placeholder(R.drawable.ic_promo_placeholder)
                .error(R.drawable.ic_broken_image)
                .centerCrop()
                .into(holder.ivPromo);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PromoDetailActivity.class);
            intent.putExtra("id", promo.getIdAsString());
            intent.putExtra("titre", promo.getTitre());
            intent.putExtra("prix_original", promo.getPrixOriginal());
            intent.putExtra("prix_promo", promo.getPrixPromo());
            intent.putExtra("reduction", promo.getReduction());
            intent.putExtra("image_url", promo.getImageUrl());
            intent.putExtra("site_source", promo.getSiteSource());
            intent.putExtra("lien_promo", promo.getLienPromo());
            intent.putExtra("categorie", promo.getCategorie());

            // Si le contexte est MainActivity, on utilise startActivityForResult pour couvrir le TP5.
            if (context instanceof MainActivity) {
                ((MainActivity) context).openPromotionDetail(intent);
            } else {
                context.startActivity(intent);
            }
        });
    }

    /**
     * Retourne le nombre de cartes actuellement affichées.
     *
     * @return Taille de la liste filtrée ou complète.
     */
    @Override
    public int getItemCount() {
        return promotions.size();
    }

    /**
     * Filtre les promotions par titre, site source ou catégorie.
     *
     * @param query Texte saisi dans le SearchView de la Toolbar.
     * @return Rien, la liste affichée est mise à jour puis le RecyclerView est rafraîchi.
     */
    public void filter(String query) {
        promotions.clear();

        if (query == null || query.trim().isEmpty()) {
            promotions.addAll(allPromotions);
        } else {
            String lowerQuery = query.toLowerCase(Locale.ROOT).trim();
            for (Promotion promotion : allPromotions) {
                if (containsIgnoreCase(promotion.getTitre(), lowerQuery)
                        || containsIgnoreCase(promotion.getSiteSource(), lowerQuery)
                        || containsIgnoreCase(promotion.getCategorie(), lowerQuery)) {
                    promotions.add(promotion);
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Remplace les promotions après un chargement Retrofit ou un rafraîchissement.
     *
     * @param list Nouvelle liste reçue du backend Flask.
     * @return Rien, les listes internes sont remplacées puis l'affichage est actualisé.
     */
    public void setPromotions(List<Promotion> list) {
        promotions.clear();
        allPromotions.clear();

        if (list != null) {
            promotions.addAll(list);
            allPromotions.addAll(list);
        }

        notifyDataSetChanged();
    }

    /**
     * Protège l'interface contre les valeurs nulles venant du backend.
     *
     * @param value Texte potentiellement nul.
     * @return Texte non nul prêt à être affiché.
     */
    private String safeText(String value) {
        return value == null ? "" : value;
    }

    /**
     * Vérifie si un texte contient la recherche déjà passée en minuscules.
     *
     * @param value Texte source potentiellement nul.
     * @param lowerQuery Recherche déjà normalisée en minuscules.
     * @return true si le texte contient la recherche, false sinon.
     */
    private boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    /**
     * ViewHolder contenant les vues d'une carte promotion.
     *
     * <p>Le ViewHolder évite les appels répétés à findViewById(): les références sont trouvées
     * une seule fois dans le constructeur, puis réutilisées pendant le recyclage des lignes.</p>
     */
    public static class PromoViewHolder extends RecyclerView.ViewHolder {

        /** Image du produit ou placeholder si l'image réseau échoue. */
        ImageView ivPromo;

        /** Titre de la promotion. */
        TextView tvTitre;

        /** Site source de la promotion. */
        TextView tvSite;

        /** Prix final après réduction. */
        TextView tvPrixPromo;

        /** Prix original affiché barré. */
        TextView tvPrixOriginal;

        /** Pourcentage de réduction. */
        TextView tvReduction;

        /**
         * Lie les vues XML de la carte aux attributs Java du ViewHolder.
         *
         * @param itemView Vue racine de item_promo_card.xml.
         * @return Rien, les références sont stockées dans l'objet.
         */
        public PromoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPromo = itemView.findViewById(R.id.iv_promo);
            tvTitre = itemView.findViewById(R.id.tv_titre);
            tvSite = itemView.findViewById(R.id.tv_site);
            tvPrixPromo = itemView.findViewById(R.id.tv_prix_promo);
            tvPrixOriginal = itemView.findViewById(R.id.tv_prix_original);
            tvReduction = itemView.findViewById(R.id.tv_reduction);
        }
    }
}
