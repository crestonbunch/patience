package im.bunch.patience.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import im.bunch.patience.model.JavascriptModel;

/**
 * This class wraps an SQLite database in a convenient API for storing and retrieving saved games.
 *
 * @author Creston Bunch
 */
public class SavedGameStorage extends SQLiteOpenHelper {

    private Context mContext;

    public static final String LOG_TAG = "SavedGameStorage";

    public static final String DATABASE_NAME = "patience";
    public static final int DATABASE_VERSION = 1;

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + SavedGameEntry.TABLE_NAME + " (" +
                    SavedGameEntry._ID + INT_TYPE + " PRIMARY KEY," +
                    SavedGameEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    SavedGameEntry.COLUMN_NAME_SCRIPT + TEXT_TYPE + COMMA_SEP +
                    SavedGameEntry.COLUMN_NAME_STATE + TEXT_TYPE + COMMA_SEP +
                    SavedGameEntry.COLUMN_NAME_TIMESTAMP + INT_TYPE + COMMA_SEP +
                    SavedGameEntry.COLUMN_NAME_HISTORY + TEXT_TYPE + COMMA_SEP +
                    SavedGameEntry.COLUMN_NAME_SCORE + TEXT_TYPE + COMMA_SEP +
                    SavedGameEntry.COLUMN_NAME_PLAY_TIME + INT_TYPE + COMMA_SEP +
                    SavedGameEntry.COLUMN_NAME_WON + INT_TYPE + COMMA_SEP +
                    SavedGameEntry.COLUMN_NAME_ARCHIVED + INT_TYPE + COMMA_SEP +
                    SavedGameEntry.COLUMN_NAME_DELETED + INT_TYPE +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SavedGameEntry.TABLE_NAME;

    public static class SavedGameEntry implements BaseColumns {
        public static final String TABLE_NAME = "saved_games";

        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_SCRIPT = "script_filename";
        public static final String COLUMN_NAME_STATE = "game_state";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_HISTORY = "history";
        public static final String COLUMN_NAME_SCORE = "score";
        public static final String COLUMN_NAME_PLAY_TIME = "play_time";
        public static final String COLUMN_NAME_WON = "won";
        public static final String COLUMN_NAME_ARCHIVED = "archived";
        public static final String COLUMN_NAME_DELETED = "deleted";
    }

    public SavedGameStorage(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * Saves the state of a javascript model to the database.
     *
     * @param db The database handle to use for inserting data.
     * @param model The javascript model describing the game state.
     * @param gameTime How long the game has been played.
     */
    public synchronized long saveGame(
            SQLiteDatabase db, String filename, final JavascriptModel model, long gameTime
    ) {
        String name = model.getName();
        // TODO: can we do this without Gson?
        Gson gson = new Gson();
        String history = gson.toJson(new ArrayList<>(model.getHistory()));
        int score = model.getScore();
        int won = model.hasWon() ? 1 : 0;
        long timestamp = System.currentTimeMillis() / 1000;

        synchronized (model) {
            model.acquireLock();
            String json = model.serialize();
            model.releaseLock();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(SavedGameEntry.COLUMN_NAME_NAME, name);
            values.put(SavedGameEntry.COLUMN_NAME_SCRIPT, filename);
            values.put(SavedGameEntry.COLUMN_NAME_STATE, json);
            values.put(SavedGameEntry.COLUMN_NAME_TIMESTAMP, timestamp);
            values.put(SavedGameEntry.COLUMN_NAME_HISTORY, history);
            values.put(SavedGameEntry.COLUMN_NAME_SCORE, score);
            values.put(SavedGameEntry.COLUMN_NAME_PLAY_TIME, gameTime);
            values.put(SavedGameEntry.COLUMN_NAME_WON, won);
            values.put(SavedGameEntry.COLUMN_NAME_ARCHIVED, 0);
            values.put(SavedGameEntry.COLUMN_NAME_DELETED, 0);

            Log.i(LOG_TAG, "Saved game!");

            // Insert the new row, returning the primary key value of the new row
            return db.insert(SavedGameEntry.TABLE_NAME, null, values);
        }
    }

    /**
     * Saves the state of a javascript model to the database.
     *
     * @param db The database handle to use for inserting data.
     * @param model The javascript model describing the game state.
     * @param gameTime How long the game has been played.
     */
    public synchronized long saveGame(
            SQLiteDatabase db, long id, String filename, final JavascriptModel model, long gameTime
    ) {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();

        // TODO: can we do this without Gson?
        // Gson is a slow pile of shit
        Gson gson = new Gson();
        // copy the array list to avoid ConcurrentModification exceptions
        String history = gson.toJson(new ArrayList<>(model.getHistory()));

        synchronized (model) {
            String name = model.getName();
            long timestamp = System.currentTimeMillis() / 1000;
            int score = model.getScore();
            int won = model.hasWon() ? 1 : 0;

            model.acquireLock();
            String json = model.serialize();
            model.releaseLock();

            values.put(SavedGameEntry.COLUMN_NAME_NAME, name);
            values.put(SavedGameEntry.COLUMN_NAME_SCRIPT, filename);
            values.put(SavedGameEntry.COLUMN_NAME_STATE, json);
            values.put(SavedGameEntry.COLUMN_NAME_TIMESTAMP, timestamp);
            values.put(SavedGameEntry.COLUMN_NAME_HISTORY, history);
            values.put(SavedGameEntry.COLUMN_NAME_SCORE, score);
            values.put(SavedGameEntry.COLUMN_NAME_PLAY_TIME, gameTime);
            values.put(SavedGameEntry.COLUMN_NAME_WON, won);
        }

        // Which row to update, based on the ID
        String selection = SavedGameEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        db.update(
            SavedGameEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        );

        Log.i(LOG_TAG, "Updated saved game!");

        return id;
    }

    /**
     * Get a cursor for traversing over the list of saved games.
     *
     * @param db
     */
    public Cursor getCursor(SQLiteDatabase db) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                SavedGameEntry._ID,
                SavedGameEntry.COLUMN_NAME_NAME,
                SavedGameEntry.COLUMN_NAME_SCRIPT,
                SavedGameEntry.COLUMN_NAME_STATE,
                SavedGameEntry.COLUMN_NAME_TIMESTAMP,
                SavedGameEntry.COLUMN_NAME_HISTORY,
                SavedGameEntry.COLUMN_NAME_SCORE,
                SavedGameEntry.COLUMN_NAME_PLAY_TIME,
                SavedGameEntry.COLUMN_NAME_WON
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = SavedGameEntry.COLUMN_NAME_TIMESTAMP + " DESC";

        // only select games that are not archived
        String selection = SavedGameEntry.COLUMN_NAME_ARCHIVED + " = ?";
        String[] selectionArgs = {"0"};

        Cursor c = db.query(
                SavedGameEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        Log.i(LOG_TAG, "Fetched saved game cursror!");

        return c;
    }

    /**
     * Get a specific saved game from a primary key.
     */
    public Cursor getSavedGame(SQLiteDatabase db, long id) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                SavedGameEntry._ID,
                SavedGameEntry.COLUMN_NAME_NAME,
                SavedGameEntry.COLUMN_NAME_SCRIPT,
                SavedGameEntry.COLUMN_NAME_STATE,
                SavedGameEntry.COLUMN_NAME_TIMESTAMP,
                SavedGameEntry.COLUMN_NAME_HISTORY,
                SavedGameEntry.COLUMN_NAME_SCORE,
                SavedGameEntry.COLUMN_NAME_PLAY_TIME,
                SavedGameEntry.COLUMN_NAME_WON
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = SavedGameEntry.COLUMN_NAME_TIMESTAMP + " DESC";

        String selection = SavedGameEntry._ID + " = ?";
        String[] selectionArgs = {Long.toString(id)};

        Cursor c = db.query(
                SavedGameEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        Log.i(LOG_TAG, "Fetched saved game from id!");

        return c;
    }

    /**
     * Set the archive flag to true for a given game id.
     *
     * @param db
     * @param id
     * @return
     */
    public boolean archiveGame(SQLiteDatabase db, long id) {

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SavedGameEntry.COLUMN_NAME_ARCHIVED, 1);

        // Which row to update, based on the ID
        String selection = SavedGameEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        int result = db.update(
                SavedGameEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
        );

        Log.i(LOG_TAG, "Archived game!");

        return result > 0;
    }

    /**
     * Set the archive flag to false for a given game id.
     *
     * @param db
     * @param id
     * @return
     */
    public boolean unarchiveGame(SQLiteDatabase db, long id) {

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SavedGameEntry.COLUMN_NAME_ARCHIVED, 0);

        // Which row to update, based on the ID
        String selection = SavedGameEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        int result = db.update(
                SavedGameEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
        );

        Log.i(LOG_TAG, "Unarchived game!");

        return result > 0;
    }

    /**
     * Get the number of games played for a certain script filename.
     */
    public int gamesPlayed(SQLiteDatabase db, String filename) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                SavedGameEntry._ID,
        };

        String selection = SavedGameEntry.COLUMN_NAME_SCRIPT + " = ?";
        String[] selectionArgs = {filename};

        Cursor c = db.query(
                SavedGameEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        int num = c.getCount();
        Log.i(LOG_TAG, String.format("Counted %d games played for %s", num, filename));

        return num;
    }

    /**
     * Get the number of games won for a certain script filename.
     */
    public int gamesWon(SQLiteDatabase db, String filename) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                SavedGameEntry._ID,
        };

        // How you want the results sorted in the resulting Cursor
        //String sortOrder = SavedGameEntry.COLUMN_NAME_TIMESTAMP + " DESC";

        String selection = SavedGameEntry.COLUMN_NAME_SCRIPT + " = ? AND "
                + SavedGameEntry.COLUMN_NAME_WON + " = ?";
        String[] selectionArgs = {filename, Integer.toString(1)};

        Cursor c = db.query(
                SavedGameEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        int num = c.getCount();
        Log.i(LOG_TAG, String.format("Counted %d games won for %s", num, filename));

        return num;
    }

    /**
     * Get the high score of a game.
     */
    public int highScore(SQLiteDatabase db, String filename) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                SavedGameEntry.COLUMN_NAME_SCORE,
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = SavedGameEntry.COLUMN_NAME_SCORE + " DESC";
        String selection = SavedGameEntry.COLUMN_NAME_SCRIPT + " = ? AND "
                + SavedGameEntry.COLUMN_NAME_WON + " = 1";
        String[] selectionArgs = {filename};

        Cursor c = db.query(
                SavedGameEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        if (c.getCount() > 0) {
            c.moveToFirst();
            int num = c.getInt(0);
            Log.i(LOG_TAG, String.format("Got high score %d for %s", num, filename));

            return num;
        } else {
            return -1;
        }
    }


    /**
     * Get the best time of a game.
     */
    public int bestTime(SQLiteDatabase db, String filename) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                SavedGameEntry.COLUMN_NAME_PLAY_TIME,
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = SavedGameEntry.COLUMN_NAME_PLAY_TIME + " ASC";
        String selection = SavedGameEntry.COLUMN_NAME_SCRIPT + " = ? AND "
                + SavedGameEntry.COLUMN_NAME_WON + " = 1";
        String[] selectionArgs = {filename};

        Cursor c = db.query(
                SavedGameEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        if (c.getCount() > 0) {
            c.moveToFirst();
            int num = c.getInt(0);
            Log.i(LOG_TAG, String.format("Got best time %d for %s", num, filename));

            return num;
        } else {
            return -1;
        }
    }

    /**
     * Delets all records of played games.
     */
    public void reset(SQLiteDatabase db, String filename) {
        // Define 'where' part of query.
        String selection = SavedGameEntry.COLUMN_NAME_SCRIPT + " = ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { filename };
        // Issue SQL statement.
        db.delete(SavedGameEntry.TABLE_NAME, selection, selectionArgs);
    }

}
