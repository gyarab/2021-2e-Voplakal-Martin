package cz.voplakal.recepty;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;

/**
 *
 * Třída pro práci s vestavěnou SQL databází Androidu
 * {@inheritDoc}
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    Context context;
    public static final int DATABASE_VERSION = 11;
    public static final String DATABASE_NAME = "Recepty.db";
    public static final String TABLE_RECEPTY = "recepty";
    public static final String ID_KEY = "id";
    public static final String NAZEV_KEY = "nazev";
    public static final String INGREDIENCE_KEY = "ingredience";
    public static final String POSTUP_KEY = "postup";
    public static final String PICTURE_KEY = "picture";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    // využitý TUTORIÁL: https://www.javatpoint.com/android-sqlite-tutorial
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_RECEPTY + "("
                + ID_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT,"   //AUTOINCREMENT
                + NAZEV_KEY + " TEXT,"
                + INGREDIENCE_KEY + " TEXT,"
                + POSTUP_KEY + " TEXT,"
                + PICTURE_KEY + " TEXT"
                + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }


    void addRecept(Recept recept) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        recept.insertIn(values, false);
        db.insert(TABLE_RECEPTY, null, values);
        db.close();
    }

    Recept getRecept(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RECEPTY, new String[]{ID_KEY,
                        NAZEV_KEY, INGREDIENCE_KEY, POSTUP_KEY, PICTURE_KEY}, ID_KEY + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        return new Recept(cursor, false);
    }

    /**
     * @param smaller if include pictures and "postup"
     * @return
     */
    public ArrayList<Recept> getAllRecepty(boolean smaller) {
        ArrayList<Recept> list = new ArrayList<>();
        String selectQuery;
        if (smaller)
            selectQuery = "SELECT " + ID_KEY + ", " + NAZEV_KEY + ", " + INGREDIENCE_KEY + " FROM " + TABLE_RECEPTY;
        else selectQuery = "SELECT  * FROM " + TABLE_RECEPTY;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(new Recept(cursor, smaller));
            } while (cursor.moveToNext());
        }
        return list;
    }

    public int updateRecept(Recept recept) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        recept.insertIn(values, true);
        // updating row
        return db.update(TABLE_RECEPTY, values, ID_KEY + " = ?",
                new String[]{String.valueOf(recept.getId())});
    }

    public void deleteRecept(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECEPTY, ID_KEY + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }
    public boolean checkIsExist(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String Query = "Select * from " + TABLE_RECEPTY + " where " + ID_KEY + " = " + id;
        Cursor cursor = db.rawQuery(Query, null);
        if(cursor.getCount() <= 0){
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }

    //pouze pro potřeby vývoje, ale je poviné použít
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECEPTY);
        onCreate(db);

        //smazat obrázky, potom celé odebrat
        File fileOrDirectory = new File("/storage/emulated/0/Android/data/cz.voplakal.recepty/files/Pictures");
        deleteRecursive(fileOrDirectory);
    }
    //delete pictures tohle taky odebrat
    public void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
}
