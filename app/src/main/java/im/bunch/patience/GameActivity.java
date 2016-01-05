package im.bunch.patience;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import im.bunch.patience.game.Component;
import im.bunch.patience.game.ComponentManager;
import im.bunch.patience.game.GameSurfaceView;
import im.bunch.patience.game.RectangleComponent;
import im.bunch.patience.model.JavascriptModel;
import im.bunch.patience.storage.SavedGameStorage;
import im.bunch.patience.view.Pile;


public class GameActivity extends AppCompatActivity
        implements OptionsDialogFragment.OptionsDialogListener {

    public static final String LOG_TAG = "GameActivity";
    public static final String BUNDLE_KEY_SCRIPT = "script";
    public static final String BUNDLE_KEY_STATE = "state";
    public static final String BUNDLE_KEY_TIME = "time";
    public static final String BUNDLE_KEY_HISTORY = "history";

    private boolean mUnsaved;
    private long mStorageId;
    private String mFilename;
    private String mScript;
    private JavascriptModel mModel;
    private ComponentManager mComponentManager;
    private long mTime;
    private Chronometer mChronometer;
    private Handler mHandler;
    private SQLiteDatabase mDatabase;
    private SavedGameStorage mGameStorage;
    private boolean mDirty; // flag set when history is modified

    private GLSurfaceView mSurfaceView;

    private Runnable mScoreListener = new Runnable() {
        @Override
        public void run() {
            // this score listener will be executed in a different thread (probably), so we
            // will execute a new runnable on this thread. Otherwise we cannot update this thread's
            // views.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateScore();
                }
            });
        }
    };

    private Runnable mEndListener = new Runnable() {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mModel.bonus(getGameTime());
                    notifyEnd();
                }
            });
        }
    };

    // save the game when JavaScript tells us to
    private Runnable mSaveListener = new Runnable() {
        @Override
        public void run() {
            // TODO: saving here causes weird bugs when you undo rapidly. Until I figure out a
            // proper fix, saving will only happen during the onPause() method.
            mDirty = true; // something changed
            //new SaveAsyncTask().execute();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        final Intent intent = getIntent();

        mGameStorage = new SavedGameStorage(this);
        mDatabase = mGameStorage.getWritableDatabase();

        try {
            if (savedInstanceState == null) {
                mFilename = intent.getStringExtra(GamesList.EXTRA_SCRIPT_FILENAME);
                mScript = GamesList.loadScript(this, mFilename);
                mModel = new JavascriptModel(this, mScript);
                mHandler = new Handler();

                if (intent.hasExtra(GamesList.EXTRA_SAVE_ID)) {
                    // starting a saved game
                    // load the data from the database
                    long id = intent.getLongExtra(GamesList.EXTRA_SAVE_ID, 0);
                    Cursor cursor = mGameStorage.getSavedGame(mDatabase, id);
                    cursor.moveToFirst();
                    String filename = cursor.getString(2);
                    String state = cursor.getString(3);
                    String historyJson = cursor.getString(5);
                    long time = cursor.getLong(7);

                    Gson gson = new Gson();
                    Type collectionType = new TypeToken<List<String>>(){}.getType();
                    List<String> history = gson.fromJson(historyJson, collectionType);

                    // load the state and history
                    mModel.deserialize(state, history);
                    mUnsaved = false;
                    mStorageId = id;
                    mFilename = filename;
                    mTime = time;
                    mScoreListener.run();

                } else {
                    Log.i(LOG_TAG, "Starting new game.");
                    // starting a new game
                    mModel.initialize();
                    mTime = 0;
                    mUnsaved = true;

                    // show the options dialog
                    if (mModel.hasOptions()) {
                        Log.i(LOG_TAG, "Options found, showing them.");
                        OptionsDialogFragment optionsDialog;
                        optionsDialog = new OptionsDialogFragment();
                        optionsDialog.setModel(mModel);
                        optionsDialog.show(getSupportFragmentManager(), "options");
                    } else {
                        Log.i(LOG_TAG, "No options found.");
                    }
                }

            } else {
                Log.i(LOG_TAG, "Resuming from saved instance state.");
                // resume game
                mScript = savedInstanceState.getString(BUNDLE_KEY_SCRIPT);
                String state = savedInstanceState.getString(BUNDLE_KEY_STATE);
                mModel = new JavascriptModel(this, mScript);
                mModel.deserialize(state);
                mTime = savedInstanceState.getLong(BUNDLE_KEY_TIME);
            }

            Log.i(LOG_TAG, "Initialized game.");

            FloatingActionButton undoButton = (FloatingActionButton) findViewById(R.id.undo_button);
            if (mModel.canUndo()) {
                undoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        undo();
                    }
                });
            } else {
                undoButton.hide();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onResume() {
        Log.i(LOG_TAG, "Resuming activity.");
        super.onResume();
        // TODO: so this is really bad, but since I can't figure out a convenient way to reload
        // GLES textures, I just create an entirely new GLSurfaceView when the activity resumes
        this.setup();
        //surfaceView.onResume();
    }
    @Override
    public void onPause() {
        Log.i(LOG_TAG, "Pausing activity");
        mSurfaceView.onPause();
        mTime = getGameTime();

        // TODO: move this to an onPause() component manager method
        for (Component c : new ArrayList<>(mComponentManager.getComponents())) {
            if (c instanceof RectangleComponent) {
                ((RectangleComponent) c).clearRectangle();
            }
        }

        // cancel any ongoing drags
        boolean dragged = mComponentManager.cancelDrag();

        // save the game if something was being/has been dragged or the game is old
        if (dragged || mDirty || !mUnsaved) {
            new SaveAsyncTask().execute();
        }

        super.onPause();
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        Log.i(LOG_TAG, "Restoring instance state.");
        super.onRestoreInstanceState(state);
        try {
            mScript = state.getString(BUNDLE_KEY_SCRIPT);
            String oldState = state.getString(BUNDLE_KEY_STATE);
            mModel = new JavascriptModel(this, mScript);
            mModel.deserialize(oldState);
            mTime = state.getLong(BUNDLE_KEY_TIME);

            this.setup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        Log.i(LOG_TAG, "Saving instance state.");
        state.putString(BUNDLE_KEY_SCRIPT, mScript);
        synchronized (mModel) {
            mModel.acquireLock();
            state.putString(BUNDLE_KEY_STATE, mModel.serialize());
            state.putString(BUNDLE_KEY_SCRIPT, mScript);
            mModel.releaseLock();
        }
        state.putLong(BUNDLE_KEY_TIME, getGameTime());
        super.onSaveInstanceState(state);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setup() {
        mDirty = false; // nothing has changed yet

        mComponentManager = new ComponentManager(this);

        // register view components from the model
        mModel.buildView(mComponentManager);

        ViewGroup layout = (ViewGroup) findViewById(R.id.layout);
        // remove the old surface view
        if (mSurfaceView != null) {
            layout.removeView(mSurfaceView);
        }

        mSurfaceView = new GameSurfaceView(this, mComponentManager);
        mSurfaceView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        layout.addView(mSurfaceView);

        // Use a chronometer to track time
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mChronometer.setBase(SystemClock.elapsedRealtime() - mTime);
        mChronometer.start();

        mModel.registerScoreListener(mScoreListener);
        mModel.registerSaveListener(mSaveListener);
        mModel.registerEndListener(mEndListener);

        // check if the game is already won
        notifyEnd();
    }

    /**
     * Get the total duration of the game from the chronometer.
     */
    public long getGameTime() {
        return SystemClock.elapsedRealtime() - mChronometer.getBase();
    }

    private void undo() {
        mComponentManager.cancelDrag();
        // restart the chronometer if the game was finished
        mChronometer.start();
        mModel.back(mComponentManager);
        // update the new  components to their environment
        mComponentManager.onSurfaceChanged(
                mSurfaceView.getWidth(), mSurfaceView.getHeight()
        );
        mComponentManager.update();
        mScoreListener.run();
        mSurfaceView.requestRender();

        // update saved game
        mSaveListener.run();
    }

    /**
     * Begin the game after a positive options dialog click.
     *
     * @param dialog
     */
    @Override
    public void onDialogPositiveClick(OptionsDialogFragment dialog) {
        Map<String, Boolean> optionsMap = dialog.getOptions();
        // set new options
        for (Map.Entry<String, Boolean> entry : optionsMap.entrySet()) {
            mModel.setOption(entry.getKey(), entry.getValue());
        }
        // restart the timer at 0
        mTime = 0;
        mChronometer.setBase(SystemClock.elapsedRealtime() - mTime);
    }

    /**
     * Return to the previous activity after a negative options dialog click.
     *
     * @param dialog
     */
    @Override
    public void onDialogNegativeClick(OptionsDialogFragment dialog) {
        finish();
    }

    private void updateScore() {
        TextView scoreView = (TextView) findViewById(R.id.score);
        scoreView.setText(String.format(Locale.getDefault(), "%d", mModel.getScore()));
    }

    /**
     * Called when a user clicks a dialog button to restart the game.
     */
    private class RestartGameListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = new Intent(GameActivity.this, GameActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(GamesList.EXTRA_SCRIPT_FILENAME, mFilename);
            startActivity(intent);
        }
    }

    /**
     * Called when a user click a dialog option to go back.
     */
    private class GoBackListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }
    }

    /**
     * Called when a user clicks a dialog option to undo.
     */
    private class UndoListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            undo();
        }
    }

    /**
     * Called to save the game asynchronously.
     */
    private class SaveAsyncTask extends AsyncTask<Object, Object, Object> {
        @Override
        protected Object doInBackground(Object... params) {
            mTime = getGameTime();
            if (mUnsaved) {
                // create a new saved game
                mStorageId = mGameStorage.saveGame(mDatabase, mFilename, mModel, mTime);
                mUnsaved = false;
            } else {
                // update an old saved game
                mGameStorage.saveGame(mDatabase, mStorageId, mFilename, mModel, mTime);
            }

            return null;
        }
    };

    private void notifyEnd() {
        if (mModel.hasWon()) {
            new AlertDialog.Builder(GameActivity.this)
                    .setTitle(getString(R.string.victory))
                    .setMessage(getString(R.string.congrats))
                    //.setIcon(R.drawable.ic_warning_black_24dp)
                    .setPositiveButton(R.string.play_again, new RestartGameListener())
                    .setNegativeButton(R.string.go_back, new GoBackListener())
                    .setCancelable(false).show();
            mChronometer.stop();
        } else if (mModel.hasLost()) {
            mChronometer.stop();

            if (mModel.canUndo()) {
                new AlertDialog.Builder(GameActivity.this)
                        .setTitle(getString(R.string.out_of_move))
                        .setMessage(getString(R.string.no_more_moves))
                        //.setIcon(R.drawable.ic_warning_black_24dp)
                        .setPositiveButton(R.string.play_again, new RestartGameListener())
                        .setNegativeButton(R.string.undo_last, new UndoListener())
                        .setCancelable(false).show();
            } else {
                new AlertDialog.Builder(GameActivity.this)
                        .setTitle(getString(R.string.out_of_move))
                        .setMessage(getString(R.string.no_more_moves))
                        //.setIcon(R.drawable.ic_warning_black_24dp)
                        .setPositiveButton(R.string.play_again, new RestartGameListener())
                        .setNegativeButton(R.string.go_back, new GoBackListener())
                        .setCancelable(false).show();
            }

        }
    }
}