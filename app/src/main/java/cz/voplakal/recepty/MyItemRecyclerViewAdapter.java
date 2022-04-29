package cz.voplakal.recepty;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cz.voplakal.recepty.databinding.FragmentItemBinding;

/**
 * Adapter pro RecyclerView
 * stará se o vykreslování výsledků vyhledávání pod vyhledávacím políčkem
 */
public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {

    private final List<Recept> mValues;
    FragmentManager fragmentManager;

    public MyItemRecyclerViewAdapter(List list, FragmentManager fragmentManager) {
        mValues = list;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        //Pokud je text příliš dlouhý
        String ingr = mValues.get(position).getIngredience();
        if (ingr.length() > 60)
            ingr = ingr.substring(0, 60) + "...";
        holder.nazevView.setText(mValues.get(position).getNazev());
        holder.ingredienceView.setText(ingr);
        holder.id = mValues.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    /**
     * třída reprezentujícíc jeden kronkrétní recept v RecyclerView
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView nazevView;
        public final TextView ingredienceView;
        public long id;

        public ViewHolder(FragmentItemBinding binding) {
            super(binding.getRoot());
            nazevView = binding.itemNazevReceptu;
            ingredienceView = binding.itemIngredience;
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (id >= 0) {  //id < 0 je pokud má být prvek pouze placeholder "Nic tu není atd...
                        Bundle bundle = new Bundle();
                        bundle.putLong(Recept.ID, id);
                        fragmentManager.beginTransaction().setReorderingAllowed(true).addToBackStack(null)
                                .replace(R.id.fragment_container, NovyRecept.class, bundle).commit();
                        //shovat klávesnici
                        Activity activity = fragmentManager.getFragments().get(0).getActivity();
                        ((MainActivity) activity).hideKeyboard();
                    } else
                        //je to placeholder item po kliknutí vytvořit recept
                        fragmentManager.beginTransaction().setReorderingAllowed(true).addToBackStack(null)
                                .replace(R.id.fragment_container, NovyRecept.class, null).commit();
                }
            });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + nazevView.getText() + "'";
        }
    }
}