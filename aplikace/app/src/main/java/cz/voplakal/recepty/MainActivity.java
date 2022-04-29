package cz.voplakal.recepty;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;


/**
 * Main activity obsahuje všechny klíčové metody aplikace a funkcionalitu dále distribuuje do dalších class
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static String TAG = "MYTAG";
    private DrawerLayout drawer;
    public static NavigationView navigationView;
    public static ArrayListRenewable<Recept> receptyMainList;
    DatabaseHandler db;

    public MainActivity() {
        receptyMainList = new ArrayListRenewable<Recept>(this);
    }

    public void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DatabaseHandler(this);
        loadAndApplyData();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().setReorderingAllowed(true).disallowAddToBackStack()
                    .replace(R.id.fragment_container, Recepty.class, null).commit();
            navigationView.setCheckedItem(R.id.nav_recepty);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (notifyFragmentExit()) return false;
        hideKeyboard();
        switch (item.getItemId()) {
            case R.id.nav_recepty:
                getSupportFragmentManager().beginTransaction().setReorderingAllowed(true).addToBackStack(null)
                        .replace(R.id.fragment_container, Recepty.class, null).commit();
                break;
            case R.id.nav_novyRecept:
                getSupportFragmentManager().beginTransaction().setReorderingAllowed(true).addToBackStack(null)
                        .replace(R.id.fragment_container, NovyRecept.class, null).commit();
                break;
            case R.id.nav_settings:
                getSupportFragmentManager().beginTransaction().setReorderingAllowed(true).addToBackStack(null)
                        .replace(R.id.fragment_container, SettingsFragment.class, null).commit();
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true; //return true to select the clicked item
    }

    /**
     * vyresetuje list receptů a načte ho znovu
     */
    public void loadAndApplyData() {
        receptyMainList.clear();
        receptyMainList.addAll(db.getAllRecepty(true));
        if (receptyMainList.isEmpty()) receptyMainList.add(
                new Recept(this.getResources().getString(R.string.emptyName), new String[]{
                        this.getResources().getString(R.string.emptyIngredience)}, null, null, -1)); //id menší než jedna, následně se deaktivuje onClick a nastaví:
    }                                                                                    // "Zatím nejsou žádné recepty, vytvořte..."

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (!notifyFragmentExit()) {
                super.onBackPressed();
            }
        }
    }

    /**
     * @return true if children handled the event
     */
    public boolean notifyFragmentExit() {
        List fragmentList = getSupportFragmentManager().getFragments();
        boolean handled = false;
        Object f = fragmentList.get(0);
        if (f instanceof NovyRecept) {
            handled = ((NovyRecept) f).onExit();
        }
        return handled;
    }

    /**
     * Využíváno při focení obrázku
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        NovyRecept activeFragment = (NovyRecept) getSupportFragmentManager().getFragments().get(0);
        activeFragment.loadPicture(activeFragment.actualPicturePaths.get(activeFragment.actualPicturePaths.size() - 1));
        if (!activeFragment.novyRecept) activeFragment.saveRecept();
        // https://developer.android.com/training/camera/photobasics
    }

    /**
     * onclick z nastavení pro zahájení obnovy z backupu
     */
    public void backup(View v) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String clientkey = sharedPreferences.getString("clientkeyvalue", "");
        Log.d(TAG, "syncNow: " + clientkey);
        if (clientkey.equals("")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Nejprve si zkopírujte/vložte identifikační klíč");
            builder.setPositiveButton("Ok", null);
            builder.create().show();
            return;
        }
        //sync
        runOnUiThread(() ->
                Toast.makeText(this, "Zálohování zahájeno. Toto může chvíli trvat.", Toast.LENGTH_LONG).show());
        APIClient api = new APIClient(this);
        ArrayList<Recept> dbRecepty = db.getAllRecepty(false);
        api.backup(dbRecepty, clientkey);
    }

    /**
     * onclick z nastavení pro zahájení obnovy z backupu
     */
    public void restoreBackup(View v) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String clientkey = sharedPreferences.getString("clientkeyvalue", "");

        if (clientkey.equals("")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Nejprve si zkopírujte/vložte identifikační klíč");
            builder.setPositiveButton("Ok", null);
            builder.create().show();
            return;
        }
        //restbackup
        runOnUiThread(() ->
                Toast.makeText(this, "Obnova ze zálohy", Toast.LENGTH_LONG).show());
        APIClient api = new APIClient(this);
        api.restoreBackup(db, clientkey);
    }
}

