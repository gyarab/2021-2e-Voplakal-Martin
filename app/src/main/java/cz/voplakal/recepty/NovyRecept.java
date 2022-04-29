package cz.voplakal.recepty;

import static cz.voplakal.recepty.MainActivity.TAG;
import static cz.voplakal.recepty.MainActivity.navigationView;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Umožňuje načítat a upravovat a uložit, popřípadě vytvořit recept
 */
public class NovyRecept extends Fragment {
    private Menu topMenu;
    EditText nazevReceptuView;
    ArrayList<EditText> ingredienceViews = new ArrayList<>();
    EditText postupView;
    LinearLayout linearLayout;
    ArrayList<ImageView> pictureViews = new ArrayList<>();
    ImageButton addIngred;


    ArrayList<String> picturePaths; //bylo načteno z db
    ArrayList<String> actualPicturePaths; // je aktuálně zobrazeno
    String nazev = "";  //pro check isedited
    ArrayList<String> ingredience;
    String postup = "";

    ArrayList<String> newIngred = new ArrayList<>();
    Recept recept;

    boolean editability = true;
    boolean novyRecept;
    long idReceptu;

    public NovyRecept() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_novy_recept, container, false);
    }

    /**
     * přidá do seznamu ingrediencí další
     *
     * @param text název konkrétní ingredience
     * @return
     */
    private EditText addIngrToGroup(String text) {
        LinearLayout l = getView().findViewById(R.id.ingrLyout);
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.ingredience_view, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view1.setBackgroundTintList(getContext().getColorStateList(R.color.underlineEditText));
        }
        l.addView(view1);
        EditText editText = (EditText) view1;
        ingredienceViews.add(editText);
        if (text != null) editText.setText(text);
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //You can identify which key pressed buy checking keyCode value with KeyEvent.KEYCODE_
                if (keyCode == KeyEvent.KEYCODE_DEL && editText.getText().toString().equals("") && l.getChildCount() > 1) {
                    l.removeView(editText);
                    ingredienceViews.remove(editText);
                }
                return false;
            }
        });
        return editText;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nazevReceptuView = getView().findViewById(R.id.nazev_receptu);

        postupView = getView().findViewById(R.id.postup_na_pripravu_text);
        linearLayout = getView().findViewById(R.id.linearLayout);
        Button addPicture = getView().findViewById(R.id.takePictureBtn);
        addPicture.setOnClickListener(v -> dispatchTakePictureIntentFull());
        addIngred = getView().findViewById(R.id.addIngr);
        addIngred.setOnClickListener(v -> {
            addIngrToGroup(null);
        });


        if (getArguments() != null) {
            editability = false;
            novyRecept = false;
            navigationView.getCheckedItem().setChecked(false);

            idReceptu = getArguments().getLong(Recept.ID);

            //načíst data z db
            Recept receptDB = ((MainActivity) getActivity()).db.getRecept(idReceptu);
            nazev = receptDB.getNazev();
            ingredience = new ArrayList<String>(Arrays.asList(receptDB.getIngredienceArr()));
            postup = receptDB.getPostup();
            picturePaths = new ArrayList<>(Arrays.asList(receptDB.getPicturePaths()));

            nazevReceptuView.setText(nazev);
            for (String s : ingredience)
                addIngrToGroup(s);
            postupView.setText(postup);
            actualPicturePaths = new ArrayList<>(picturePaths);
            for (String p : actualPicturePaths) {
                loadPicture(p);
            }
        } else {
            editability = true;
            novyRecept = true;
            picturePaths = new ArrayList<>();
            actualPicturePaths = new ArrayList<>();
            ingredience = new ArrayList<>();
            addIngrToGroup(null);
        }
        Log.d(TAG, "onViewCreated: id: " + idReceptu);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.edit, menu);
    }

    /**
     * voláno z vlastní metody v MainActivity.notifyFragmentExit()
     *
     * @return
     */
    public boolean onExit() {
        if (isChanged() && editability) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.changesNotSaved);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                saveRecept();
                setEditability(false);
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> {
                getParentFragmentManager().beginTransaction().setReorderingAllowed(true)
                        .replace(R.id.fragment_container, Recepty.class, null).commit();
            });
            builder.create().show();
            return true;
        } else return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //TODO pokud ještě není "delete image if not saved");

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_done:
            case R.id.menu_done_text:
                if (!saveRecept()) break;
                //hide keyboard
                ((MainActivity) getActivity()).hideKeyboard();

                FragmentTransaction transaction = getParentFragmentManager().beginTransaction().setReorderingAllowed(true)
                        .setCustomAnimations(
                                R.anim.slide_in,  // enter
                                R.anim.fade_out,  // exit
                                R.anim.fade_in,   // popEnter
                                R.anim.slide_out  // popExit
                        ).replace(R.id.fragment_container, Recepty.class, null);   //TODO nepřecházet domů ale jen obnovit fragmetn pomocí
                if (!novyRecept)
                    transaction.addToBackStack(null);                  //TODO Bundle bundle = new Bundle(); bundle.putLong(Recept.ID, id);
                else getParentFragmentManager().popBackStackImmediate();
                transaction.commit();

                navigationView.setCheckedItem(R.id.nav_recepty);
                break;
            case R.id.menu_edit:
            case R.id.menu_edit_text:
                setEditability(true);
                break;
            case R.id.menu_trash:
                if (!novyRecept) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.deleteRecept);
                    builder.setPositiveButton("Ok", (dialog, which) -> {
                        deleteRecept();
                        getParentFragmentManager().beginTransaction().setReorderingAllowed(true)
                                .setCustomAnimations(
                                        R.anim.slide_in,  // enter
                                        R.anim.fade_out,  // exit
                                        R.anim.fade_in,   // popEnter
                                        R.anim.slide_out  // popExit
                                ).replace(R.id.fragment_container, Recepty.class, null).commit();
                        navigationView.setCheckedItem(R.id.nav_recepty);
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.create().show();
                } else {
                    getParentFragmentManager().beginTransaction().setReorderingAllowed(true)
                            .setCustomAnimations(
                                    R.anim.slide_in,  // enter
                                    R.anim.fade_out,  // exit
                                    R.anim.fade_in,   // popEnter
                                    R.anim.slide_out  // popExit
                            ).replace(R.id.fragment_container, Recepty.class, null).commit();
                    navigationView.setCheckedItem(R.id.nav_recepty);
                }
                break;
            case R.id.share:
                if (editability) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.saveFirst);
                    builder.setPositiveButton("Ok", null);
                    builder.create().show();
                } else {
                    APIClient client = new APIClient(getActivity());
                    if (!actualPicturePaths.isEmpty())
                        Toast.makeText(getActivity(), "Sdílení zahájeno, pokud jsou součástí i obrázky, může trvat déle.", Toast.LENGTH_LONG).show();
                    client.share(new Recept(nazev, ingredience.toArray(new String[0]), postup, actualPicturePaths.toArray(new String[0])));
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        topMenu = menu;
        super.onPrepareOptionsMenu(menu);
        setEditability(editability);
    }

    private void deleteRecept() {
        if (!actualPicturePaths.isEmpty())
            try {
                for (String path : actualPicturePaths) {
                    new File(path).delete();
                }
            } catch (SecurityException e) {
                Toast.makeText(getContext(), "ERR při mazání obrázků receptu", Toast.LENGTH_SHORT).show();

            }
        DatabaseHandler db = ((MainActivity) getActivity()).db;
        db.deleteRecept(idReceptu);
        ((MainActivity) getActivity()).loadAndApplyData();
    }

    /**
     * @return if changed were saved
     */
    boolean saveRecept() {
        if (nazevReceptuView.getText().toString().trim().length() < 3) {
            //https://www.javatpoint.com/android-alert-dialog-example
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.dialogPrazdnyRecept);
            builder.setPositiveButton("Ok", null);
            builder.create().show();
            return false;
        }
        DatabaseHandler db = ((MainActivity) getActivity()).db;
        ArrayList<String> ingrs = new ArrayList<>();
        for (EditText i : ingredienceViews) {
            ingrs.add(i.getText().toString().trim());
        }
        if (novyRecept) {
            db.addRecept(new Recept(nazevReceptuView.getText().toString().trim(), ingrs.toArray(new String[0]),
                    postupView.getText().toString().trim(), actualPicturePaths.toArray(new String[0])));
            novyRecept = false;
        } else
            db.updateRecept(new Recept(nazevReceptuView.getText().toString(), ingrs.toArray(new String[0]),
                    postupView.getText().toString(), actualPicturePaths.toArray(new String[0]), idReceptu));

        Toast.makeText(getContext(), R.string.dataSaved, Toast.LENGTH_SHORT).show();
        ((MainActivity) getActivity()).loadAndApplyData();
        return true;
    }

    private void setViewEditability(@NonNull EditText editText, boolean editability) {
        //editText.setEnabled(editability);
        if (!editability) {
            editText.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            editText.setFocusable(false);
        } else {
            editText.setFocusableInTouchMode(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                editText.setBackgroundTintList(getContext().getColorStateList(R.color.underlineEditText));
            }
        }

    }

    /**
     * naství, zda se dají data ve fragmentu upravovat
     *
     * @param editability true/false
     */
    private void setEditability(boolean editability) {
        this.editability = editability;
        topMenu.findItem(R.id.menu_done).setVisible(editability);
        topMenu.findItem(R.id.menu_done_text).setVisible(editability);
        topMenu.findItem(R.id.menu_edit).setVisible(!editability);
        topMenu.findItem(R.id.menu_edit_text).setVisible(!editability);
        addIngred.setVisibility(editability ? View.VISIBLE : View.GONE);
        setViewEditability(nazevReceptuView, editability);
        for (EditText e : ingredienceViews)
            setViewEditability(e, editability);

        setViewEditability(postupView, editability);
        updateImageForeground();
    }

    /**
     * nastaví na obrázek ikonu koše pokud je v režimu editace
     */
    private void updateImageForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Drawable dw = getResources().getDrawable(R.drawable.foreground_trash, null);
            for (ImageView image : pictureViews) {
                if (editability) {
                    image.setForeground(dw);
                    image.setForegroundGravity(Gravity.CENTER);
                } else image.setForeground(null);
            }
        }
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    /**
     * více v MainActivtiy onActivityResult()
     */
    private void dispatchTakePictureIntentFull() {              // https://developer.android.com/training/camera/photobasics
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(getContext(), "Error when tying to create a file", Toast.LENGTH_SHORT).show();
        }
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(getContext(),
                    "com.example.android.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_11_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        actualPicturePaths.add(image.getAbsolutePath());
        Log.d(TAG, "createImageFile: path of the new picture: " + actualPicturePaths.get(actualPicturePaths.size() - 1));
        return image;
    }

    void loadPicture(String path) {
        if (path.trim().equals("")) return;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try {
            File file = new File(path);
            Bitmap imageBitmap = BitmapFactory.decodeFile(file.toString());
            ImageView imageView = (ImageView) LayoutInflater.from(getContext()).inflate(R.layout.image_layout, null);
            //    imageView.setPadding(0, 20, 0, 0);
            imageView.setImageBitmap(imageBitmap);
            imageView.setOnClickListener(v -> {
                Log.d(TAG, "loadPicture: " + picturePaths.size());
                Log.d(TAG, "loadPicture: " + actualPicturePaths.size());
                if (editability) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.deletePicture);
                    builder.setPositiveButton("Ok", (dialog, which) -> deletePicture(imageView, path));
                    builder.setNegativeButton("Cancel", null);
                    builder.create().show();
                }
            });
            linearLayout.addView(imageView);
            pictureViews.add(imageView);
        } catch (Exception e) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "ERR při načítání obr", Toast.LENGTH_SHORT).show());
        }
        updateImageForeground();
    }

    private void deletePicture(View v, String path) {
        try {
            actualPicturePaths.remove(path);
            linearLayout.removeView(v);
            File file = new File(path);
            file.delete();
        } catch (Exception e) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "ERR při mazání obr", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * @return if data were changed
     */
    boolean isChanged() {
        ArrayList<String> ingrs = new ArrayList<>();
        String s;
        for (EditText i : ingredienceViews) {
            s = i.getText().toString().trim();
            if (!s.isEmpty()) ingrs.add(s);
        }
        Log.d(TAG, "isChanged: " + (!ingrs.equals(ingredience)));
        Log.d(TAG, "isChanged: " + ingrs.size());
        Log.d(TAG, "isChanged: " + ingredience.size());

        return !nazevReceptuView.getText().toString().trim().equals(nazev.trim())
                || !ingrs.equals(ingredience)
                || !postupView.getText().toString().trim().equals(postup)
                || !picturePaths.equals(actualPicturePaths);
    }

}