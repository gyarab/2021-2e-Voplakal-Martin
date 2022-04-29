package cz.voplakal.recepty;

import static cz.voplakal.recepty.MainActivity.TAG;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

/**
 * Fragment s nastaveními aplikace
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        Preference copykey = findPreference("clientkey");
        copykey.setOnPreferenceClickListener(preference -> {
            Thread thread = new Thread(() -> {
                String value = sharedPreferences.getString("clientkeyvalue", "");
                if (value.equals("")) {
                    value  = createKey();
                }
                if(value == null || value.isEmpty()) return;
                Looper.prepare();
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Client ID", value);
                clipboard.setPrimaryClip(clip);
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Zkopírováno", Toast.LENGTH_SHORT).show());
                Log.d(TAG, "onCreatePreferences: zkopírováno " + value);
            });
            thread.start();
            return true;
        });
        Preference resetKey = findPreference("resetkey");
        resetKey.setOnPreferenceClickListener(preference -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Klíč bude z aplikace odstraněn.");
            builder.setPositiveButton("Ok", (dialog, which) -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("clientkeyvalue");
                editor.commit();
                Toast.makeText(getContext(), "Klíč smazán, zkopírujte si nový", Toast.LENGTH_LONG).show();
            });
            builder.setCancelable(true);
            builder.setNegativeButton("Cancel", null);
            builder.create().show();
            return true;
        });
        Preference insertKey = findPreference("insertKey");             //source: https://mkyong.com/android/android-prompt-user-input-dialog-example/
        insertKey.setOnPreferenceClickListener(preference -> {
            // get prompts.xml view
            LayoutInflater li = LayoutInflater.from(getContext());
            View promptsView = li.inflate(R.layout.insert_dialog, null);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    getContext());
            alertDialogBuilder.setTitle("Vložte klíč");

            // set prompts.xml to alertdialog builder
            alertDialogBuilder.setView(promptsView);

            final EditText userInput = (EditText) promptsView
                    .findViewById(R.id.editTextDialogUserInput);

            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        String newKey = userInput.getText().toString();
                        editor.putString("clientkeyvalue", newKey);
                        editor.commit();
                        Toast.makeText(getContext(), "Klíč uložen", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            // show it
            alertDialog.show();

            return true;
        });
    }

    /**
     * požádat server o identifikační klíč
     * @return
     */
    public String createKey() {
        APIClient api = new APIClient(getActivity());
        Log.d(TAG, "syncNow: try");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //String newKey = api.getClientID();
        String newKey = api.get("/backupCode", null, null);
        if (newKey == null) {
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Chyba při žádosti o nový klíč", Toast.LENGTH_LONG).show());
            return null;
        }
        newKey = newKey.substring(1, newKey.length() - 1);
        editor.putString("clientkeyvalue", newKey);
        editor.commit();
        Log.d(TAG, "syncNow: " + sharedPreferences.getString("clientkeyvalue", ""));
        return newKey;
    }

}