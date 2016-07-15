package im.bunch.patience;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.wdullaer.swipeactionadapter.SwipeActionAdapter;
import com.wdullaer.swipeactionadapter.SwipeDirection;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.bunch.patience.model.JavascriptModel;
import im.bunch.patience.storage.SavedGameStorage;

public class GamesList extends AppCompatActivity {

    public static final String EXTRA_SCRIPT_FILENAME = "im.bunch.patience.SCRIPT_FILENAME";
    public static final String EXTRA_SAVE_ID = "im.bunch.patience.SAVE_ID";

    public static final String SCRIPTS_DIR = "scripts";

    private SQLiteDatabase mDatabase;
    private SavedGameStorage mGameStorage;
    private Cursor mCursor;
    private ListView mGamesListView;
    private SwipeActionAdapter mSavesSwipeAdapter;
    private SimpleCursorAdapter mSavesCursorAdapter;
    private Snackbar mArchiveSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_games_list);

        mGamesListView = (ListView) findViewById(R.id.gamesList);

        // initialize views
        final FloatingActionButton button = (FloatingActionButton)
                findViewById(R.id.new_game_button);
        final BottomSheetLayout bottomsheet = (BottomSheetLayout) findViewById(R.id.bottomsheet);

        button.hide();

        final List<Map<String, String>> scripts = new ArrayList<>();
        final Map<String, String> namesMap = new HashMap<>();
        final Map<String, String> descriptionsMap = new HashMap<>();

        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.scripts_list, bottomsheet, false);
        final ListView scriptsList = (ListView) view.findViewById(R.id.scripts_list);
        String[] from = {"filename", "filename"};
        int[] to = {R.id.script_name, R.id.info_button};
        final SimpleAdapter adapter = new SimpleAdapter(
                this, scripts, R.layout.scripts_list_item, from, to
        );
        scriptsList.setAdapter(adapter);

        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                final String filename = (String) data;
                if (view.getId() == R.id.script_name) {
                    TextView textView = (TextView) view;

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(GamesList.this, GameActivity.class);
                            intent.putExtra(EXTRA_SCRIPT_FILENAME, filename);
                            bottomsheet.dismissSheet();
                            startActivity(intent);
                        }
                    });
                    textView.setText(namesMap.get(filename));

                    return true;
                } else if (view.getId() == R.id.info_button) {
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(GamesList.this, GameDetailsActivity.class);
                            intent.putExtra(EXTRA_SCRIPT_FILENAME, filename);
                            bottomsheet.dismissSheet();
                            startActivity(intent);
                        }
                    });

                    return true;
                }

                return false;
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bottomsheet.showWithSheetView(view);
            }
        });

        // asynchronously load the list of scripts and show the floating action button when we're
        // ready
        new AsyncTask<Object, Object, Boolean>() {

            @Override
            protected Boolean doInBackground(Object... params) {
                for (String filename : getScripts()) {
                    try {
                        String script = loadScript(GamesList.this, filename);
                        JavascriptModel model = new JavascriptModel(GamesList.this, script);

                        Map<String, String> entry = new HashMap<>();
                        entry.put("filename", filename);
                        scripts.add(entry);

                        namesMap.put(filename, model.getName());
                        descriptionsMap.put(filename, model.getDescription());

                        adapter.notifyDataSetChanged();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                return true;
            }

            protected void onProgressUpdate(Integer... progress) {
                // update progress here
            }

            protected void onPostExecute(Boolean result) {
                button.show();
            }


        }.execute();
    }

    @Override
    public void onResume() {
        super.onResume();

        PopulateSavesTask populateSavesTask = new PopulateSavesTask();
        populateSavesTask.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_games_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.about) {
            Intent intent = new Intent(GamesList.this, AboutActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Enumerate a list of script file names.
     *
     * @return
     */
    private List<String> getScripts() {

        File dir = this.getDir(SCRIPTS_DIR, Context.MODE_PRIVATE);
        List<String> result = new ArrayList<>();

        for (final File f : dir.listFiles()) {
            result.add(f.getName());
        }

        return result;
    }

    /**
     * Load a script from a given filename.
     *
     * @param context
     * @param filename
     * @return
     * @throws IOException
     */
    public static String loadScript(Context context, String filename) throws IOException {

        File dir = context.getDir(SCRIPTS_DIR, Context.MODE_PRIVATE);
        File f = new File(dir.getPath(), filename);

        final FileInputStream fin = new FileInputStream(f);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
        final String script = IOUtils.toString(reader);

        return script;
    }

    /**
     * This asynchronous task populates the saved games list. It builds a cursor adapter that
     * fills the list from the database and a swipe adapter that handles swipe actions for
     * archiving games.
     */
    private class PopulateSavesTask extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object... params) {
            mGameStorage = new SavedGameStorage(GamesList.this);
            mDatabase = mGameStorage.getReadableDatabase();
            mCursor = mGameStorage.getCursor(mDatabase);

            String[] fromColumns = {SavedGameStorage.SavedGameEntry.COLUMN_NAME_NAME,
                    SavedGameStorage.SavedGameEntry.COLUMN_NAME_TIMESTAMP};
            int[] toViews = {R.id.game_name, R.id.timestamp};
            mSavesCursorAdapter = new SimpleCursorAdapter(GamesList.this,
                    R.layout.games_list_item, mCursor, fromColumns, toViews, 0);

            mSavesSwipeAdapter = new SwipeActionAdapter(mSavesCursorAdapter);
            // Pass a reference of your ListView to the SwipeActionAdapter
            mSavesSwipeAdapter.setListView(mGamesListView);

            // Set backgrounds for the swipe directions
            mSavesSwipeAdapter
                    .addBackground(SwipeDirection.DIRECTION_NORMAL_LEFT,R.layout.list_item_swipe)
                    .addBackground(SwipeDirection.DIRECTION_NORMAL_RIGHT,R.layout.list_item_swipe);

            mSavesSwipeAdapter.setFixedBackgrounds(true);
            mSavesSwipeAdapter.setNormalSwipeFraction(0.4f);
            mSavesSwipeAdapter.setFarSwipeFraction(1.0f);

            // Listen to swipes
            mSavesSwipeAdapter.setSwipeActionListener(new SavesSwipeListener());

            return null;
        }

        @Override
        protected void onProgressUpdate(Object... progress) {
            // update progress bar here
        }

        @Override
        protected void onPostExecute(Object result) {

            // add a view binder that changes data to better fit the views
            mSavesCursorAdapter.setViewBinder(new SavedItemViewBinder());

            // set the list view adapter to the swipe adapter
            mGamesListView.setAdapter(mSavesSwipeAdapter);

            // add a click listener that opens the saved game
            mGamesListView.setOnItemClickListener(new SavesClickListener());
        }
    }

    /**
     * This asynchronous task archives saved games.
     */
    private class ArchiveTask extends AsyncTask<Integer, Integer, Cursor> {

        Long[] mTargets;

        @Override
        protected Cursor doInBackground(Integer... positions) {
            int count = positions.length;
            int i = 0;
            mTargets = new Long[count];

            // get a new cursor to avoid modifying the old one
            Cursor temp = mGameStorage.getCursor(mDatabase);

            for (int pos : positions) {
                if (temp.moveToPosition(pos)) {
                    long id = temp.getLong(0);
                    mGameStorage.archiveGame(mDatabase, id);
                    publishProgress((int) ((float) i / count) * 100);
                    mTargets[i] = id;
                    i++;
                }
            }
            temp.close();

            return mGameStorage.getCursor(mDatabase);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // update progress bar here
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            // TODO: synchronize the cursor?
            mSavesCursorAdapter.swapCursor(cursor);
            //mSavesCursorAdapter.notifyDataSetChanged();
            // TODO: Testing with this line
            mSavesSwipeAdapter.notifyDataSetChanged();
            mCursor.close();
            mCursor = cursor;
            Log.i("List", "Updated cursor.");

            //CoordinatorLayout coordinatorLayout = new CoordinatorLayout(GamesList.this);
            View coordinatorLayout = findViewById(R.id.coordinator_layout);

            mArchiveSnackbar = Snackbar.make(
                    coordinatorLayout, R.string.game_archived, Snackbar.LENGTH_LONG
            );
            mArchiveSnackbar.setAction(R.string.undo, new UnarchiveClickListener(mTargets));
            mArchiveSnackbar.show();
        }

    };

    /**
     * This asynchronous unarchives saved games.
     */
    private class UnarchiveTask extends AsyncTask<Long, Integer, Cursor> {

        @Override
        protected Cursor doInBackground(Long... ids) {
            int count = ids.length;
            int i = 0;

            for (long id : ids) {
                mGameStorage.unarchiveGame(mDatabase, id);
                publishProgress((int) ((float) i / count) * 100);
                i++;
            }

            return mGameStorage.getCursor(mDatabase);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // update progress here
        }

        @Override
        protected void onPostExecute(Cursor result) {
            mSavesCursorAdapter.swapCursor(result);
            //mSavesCursorAdapter.notifyDataSetChanged();
            mCursor.close();
            mCursor = result;
            mSavesSwipeAdapter.notifyDataSetChanged();
        }
    }

    /**
     * This class handles swipe actions on saved game list items. When you swipe right on an item
     * it will archive the game by calling an ArchiveTask.
     */
    private class SavesSwipeListener implements SwipeActionAdapter.SwipeActionListener {

        @Override
        public boolean hasActions(int position, SwipeDirection direction) {
            if(direction.isLeft()) return false; // disable left swipes
            return direction.isRight();
        }

        @Override
        public boolean shouldDismiss(int position, SwipeDirection direction) {
            // Only dismiss an item when swiping normal right
            if (direction == SwipeDirection.DIRECTION_NORMAL_RIGHT) {
                ArchiveTask archiveTask = new ArchiveTask();
                archiveTask.execute(position);
                return true;
            }
            return false;
        }

        @Override
        public void onSwipe(int[] positionList, SwipeDirection[] directionList) {
            for(int i = 0; i < positionList.length; i++) {
                final SwipeDirection direction = directionList[i];
                final int position = positionList[i];

                switch (direction) {
                    case DIRECTION_FAR_LEFT:
                        break;
                    case DIRECTION_NORMAL_LEFT:
                        break;
                    case DIRECTION_FAR_RIGHT:
                        break;
                    case DIRECTION_NORMAL_RIGHT:
                        break;
                }
            }

            //mSavesSwipeAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Open a game activity when you click on a saved game item.
     */
    private class SavesClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Use a temporary cursor to avoid crashes? Does this work?
            Cursor temp = mGameStorage.getCursor(mDatabase);
            if (temp.moveToPosition(position)) {
                if (temp.getColumnCount() > 0) {
                    long index = temp.getLong(0);
                    String filename = temp.getString(2);
                    Intent intent = new Intent(GamesList.this, GameActivity.class);
                    intent.putExtra(EXTRA_SAVE_ID, index);
                    intent.putExtra(EXTRA_SCRIPT_FILENAME, filename);
                    temp.close();
                    startActivity(intent);
                } else {
                    Log.w("List", "No columns?");
                }
            }
        }

    }

    /**
     * This maps the unix timestamp from the database to a relative time string.
     */
    private class SavedItemViewBinder implements SimpleCursorAdapter.ViewBinder {

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (columnIndex == 4) {
                TextView timeView = (TextView) view;
                long timestamp = cursor.getLong(columnIndex);

                String formattedTime = (String) DateUtils
                        .getRelativeTimeSpanString(timestamp * 1000);
                // TODO: put this in strings resource
                String prefix = getString(R.string.last_played);
                timeView.setText(prefix + formattedTime);
                return true;
            }
            return false;
        }
    }

    /**
     * A click listener that calls the unarchive task.
     */
    private class UnarchiveClickListener implements View.OnClickListener {

        Long[] mTargets;

        public UnarchiveClickListener(Long[] targets) {
            super();
            mTargets = targets;
        }

        @Override
        public void onClick(View v) {
            new UnarchiveTask().execute(mTargets);
        }
    }

}
