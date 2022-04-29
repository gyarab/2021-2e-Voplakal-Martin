package cz.voplakal.recepty;

import android.content.Context;

import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Třída k uchovávání seznamu receptů a vyhledávání v nich
 */
public class ArrayListRenewable<Type extends Recept> extends ArrayList<Type> {
    private MyItemRecyclerViewAdapter adapter;
    private Context context;
    private ArrayList removedItemsList = new ArrayList<Type>();    //pro obnovu původně načteného listu z databáze, pro ušetření času práce s databází



    @Deprecated
    public ArrayListRenewable() {     //nepodporuje prázdný konstruktor
        throw new UnsupportedOperationException("Use other constructor");
    }

    public ArrayListRenewable(Context context) {
        super();
        this.context = context;
    }

    public void setAdapter(MyItemRecyclerViewAdapter adapter) {
        this.adapter = adapter;
    }
    /**
     * zavolá metodu na odebrání předka a odebraný prvek si uloží pro pozdější obnovení
     * <br>
     * {@inheritDoc}
     */
    @Override
    public boolean remove(@Nullable Object o) {
        removedItemsList.add(o);
        boolean r = super.remove(o);
        return r;
    }

    /**
     * Upozorní RecyclerView aby aktualizovalo zobrazený sernam
     */
    public synchronized void notifyDataSetChanged() {
        if (adapter == null)
            throw new UnsupportedOperationException("Adapter is null. First set it setAdapter(...)");
        adapter.notifyDataSetChanged();
    }

    /**
     * vyřadí z z listu všechny recepty, které neodpovídají vyhledávací frázi
     * @param filter zadanáý text do vyhledávání
     */
    public synchronized void filter(String filter) {
        //odebrání diakritiky
        filter = normalize(filter);
        //rozdělení slov
        String[] filters = filter.split(" ");   //
        Iterator<Recept> it = (Iterator<Recept>) iterator();
        while (it.hasNext()) {
            Recept recept = it.next();
            if (Thread.currentThread().isInterrupted()) {
       //        System.out.println("interrupted");
                return;
            }
            for (String filter1 : filters) {
                if (normalize(recept.getNazev()).contains(filter1)
                        || normalize(recept.getIngredience()).contains(filter1));
                else {
                    removedItemsList.add(recept);
                    it.remove();
                    break;
                }
            }
        }
    }
    /**
     * odstranit diakritiku, toLowerCase
     */
    private String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
    }

    /**
     * obnovit dříve odebraná data
     */
    public synchronized void restoreDatabse() {
        this.addAll(removedItemsList);
        removedItemsList.clear();
    }

    //TODO odebrat_______________________________________________________________
    public void clear(boolean notify) {
        int size = size();
        super.clear();
        if (adapter != null && notify)
            adapter.notifyItemRangeRemoved(0, size);
        //adapter.notifyDataSetChanged();
    }

    /**
     * Synchronized
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean add(Type recept) {
        return super.add(recept);
    }

    /**
     * Synchronized
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear() {
        super.clear();
        removedItemsList.clear();
    }


}