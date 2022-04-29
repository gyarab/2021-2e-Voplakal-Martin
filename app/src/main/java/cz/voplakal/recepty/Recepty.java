package cz.voplakal.recepty;

import static cz.voplakal.recepty.MainActivity.navigationView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * fragment s vyhledávacím polem receptů a jeho výsledky
 */
public class Recepty extends Fragment {

    private final String TAG = MainActivity.TAG;
    EditText searchView;

    public Recepty() {
        super(R.layout.fragment_recepty);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Recepty fragment onCreate");
        Log.d(TAG, "pocet receptu: " + MainActivity.receptyMainList.size());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navigationView.setCheckedItem(R.id.nav_recepty);
        navigationView.getCheckedItem().setChecked(true);
        Log.d(TAG, "Recepty fragment onViewCreated");
        searchView = getView().findViewById(R.id.inputTextSearch);
        // searchView.setImeActionLabel("Custom text", KeyEvent.KEYCODE_NAVIGATE_NEXT);
        searchView.addTextChangedListener(new TextWatcher() {
            long lastCheck = System.currentTimeMillis();
            String lastFilter = "";
            Thread lastThread;

            //povinné metody
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            /**
             * filtrování výsledků vyhledávání při změně textu
             */
            @Override
            public void afterTextChanged(Editable s) {
                if (System.currentTimeMillis() - lastCheck > 80) {   // pro zmenšení zátěže systému se při rychlém psaní metoda znovu nevoĺá
                    lastCheck = System.currentTimeMillis();
                //pokud ještě běží dřívější vláko zastavit
                    if (lastThread != null) lastThread.interrupt();
                    lastThread = new Thread(() -> {
                        String newFilter = searchView.getText().toString().toLowerCase().trim();

                        if (lastFilter.length() < newFilter.length() && newFilter.contains(lastFilter)) {
                            MainActivity.receptyMainList.filter(newFilter);
                        } else {
                            //obnova dat
                            MainActivity.receptyMainList.restoreDatabse();
                            MainActivity.receptyMainList.filter(newFilter);
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        } else lastFilter = newFilter;
                        //notify dataSetChanged() musí být voláno v Ui vlákně jinak:
                        //      android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
                        getActivity().runOnUiThread(() -> {
                            MainActivity.receptyMainList.notifyDataSetChanged();
                        });

                    });
                    lastThread.start();
                }
            }
        });
    }
}