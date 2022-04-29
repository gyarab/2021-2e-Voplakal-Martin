package cz.voplakal.recepty;

import static cz.voplakal.recepty.DatabaseHandler.ID_KEY;
import static cz.voplakal.recepty.DatabaseHandler.INGREDIENCE_KEY;
import static cz.voplakal.recepty.DatabaseHandler.NAZEV_KEY;
import static cz.voplakal.recepty.DatabaseHandler.PICTURE_KEY;
import static cz.voplakal.recepty.DatabaseHandler.POSTUP_KEY;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.Arrays;

/**
 * Třída reprezentuje recept jako takový a jeho data,
 * byla vytvořena pro zjednodušení předávání dat ve zbytku aplikace
 */
public class Recept {
    //bundle keys tohle bych nejradši sloučil s kláči DB
    public static final String ID = "recept_ID";
    public static final String NAZEV = "recept_NAZEV";
    public static final String INGREDIENCE = "recept_INGREDIENCE";
    public static final String POSTUP = "recept_POPIS";
    public static final String PICTURE = "recept_PICTURE";

    private long id; //optional
    private String nazev;
    private String[] ingredience;
    private String postup;
    private String[] picturePaths; //optional

    public Recept(String nazev, String[] ingredience, String postup, String[] picturePath) {
        this.nazev = nazev;
        this.ingredience = ingredience;
        this.postup = postup;
        this.picturePaths = picturePath;
    }

    public Recept(String nazev, String[] ingredience, String postup, String[] picturePath, long id) {
        this(nazev, ingredience, postup, picturePath);
        this.id = id;
    }


    /**
     * Usnadňuje vytvoření tohoto objektu přímo při čtení z databáze
     *
     * @param cursor
     * @param small  pokdy je true vloží pouze natev a ingredience
     */
    public Recept(Cursor cursor, boolean small) {
        this.id = cursor.getInt(0);
        this.nazev = cursor.getString(1);
        this.ingredience = derialize(cursor.getString(2));
        if (small) return;
        this.postup = cursor.getString(3);
        String paths = cursor.getString(4);
        if (paths != null)
            this.picturePaths = derialize(paths);
    }

    /**
     * @param values objekt do kterého data vložit
     * @param withId jestli použít i ID receptu
     * @return ContentValues připravená data do objektu pro vložení do databáze
     */
    public ContentValues insertIn(ContentValues values, boolean withId) {
        values.put(NAZEV_KEY, getNazev());
        values.put(INGREDIENCE_KEY, serialize(getIngredienceArr()));
        values.put(POSTUP_KEY, getPostup());
        if (withId)
            values.put(ID_KEY, getId());
        if (picturePaths.length > 0)
            values.put(PICTURE_KEY, serialize(picturePaths));
        return values;
    }


    String ARRAY_DIVIDER = "#gyarab684as";

    /**
     * zde bylo potřeba vyřešit ukádání pole stringů do databáze, což SQL nepodporuje a je tak nutné
     * je převést na String, Jako oddělovač je použit ARRAY_DIVIDER
     */
    private String serialize(String content[]) {
        return TextUtils.join(ARRAY_DIVIDER, content);
    }

    private String[] derialize(String content) {
        return content.split(ARRAY_DIVIDER);
    }

    //getters
    public long getId() {
        return id;
    }

    public String getNazev() {
        return nazev;
    }

    /**
     * @return ingredients.toString striped form {} on the start and end
     */
    public String getIngredience() {
        String s = Arrays.toString(ingredience);
        return s.substring(1, s.length() - 1);
    }

    public String[] getIngredienceArr() {
        return ingredience;
    }


    public String getPostup() {
        return postup;
    }

    public String[] getPicturePaths() {
        return picturePaths == null || picturePaths.length == 0 ? new String[0] : picturePaths;
    }

    @Override
    public String toString() {
        return nazev;
    }
}
