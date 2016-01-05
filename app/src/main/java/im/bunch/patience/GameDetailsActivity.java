package im.bunch.patience;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import im.bunch.patience.model.JavascriptModel;
import im.bunch.patience.storage.SavedGameStorage;

public class GameDetailsActivity extends AppCompatActivity {

    String mFilename;
    String mScript;
    JavascriptModel mModel;
    SavedGameStorage mGameStorage;
    SQLiteDatabase mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_details);

        final Intent intent = getIntent();
        mFilename = intent.getStringExtra(GamesList.EXTRA_SCRIPT_FILENAME);
        mGameStorage = new SavedGameStorage(this);
        mDatabase = mGameStorage.getReadableDatabase();

        try {
            mScript = GamesList.loadScript(this, mFilename);
            mModel = new JavascriptModel(this, mScript);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // fill in stats
        new UpdateBestTimeTask().execute();
        new UpdateGamesPlayedTask().execute();
        new UpdateGamesWon().execute();
        new UpdateHighScoreTask().execute();
        new UpdateWonPercentage().execute();
        new UpdateTitle().execute();
        new UpdateRules().execute();

        Button resetButton = (Button) findViewById(R.id.reset_stats);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(GameDetailsActivity.this)
                    .setTitle("Reset stats")
                    .setMessage("This will permanently delete your record!")
                    .setIcon(R.drawable.ic_warning_black_24dp)
                    .setPositiveButton(R.string.okay, new ResetAcceptListener())
                    .setNegativeButton(R.string.nevermind, null).show();
            }
        });
    }

    /**
     * This task asynchronously updates the high score.
     */
    private class UpdateHighScoreTask extends AsyncTask<Object, Object, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            return mGameStorage.highScore(mDatabase, mFilename);
        }

        @Override
        protected void onProgressUpdate(Object... progress) {
            // update progress here
        }

        @Override
        protected void onPostExecute(Integer result) {
            TextView highScoreView = (TextView) findViewById(R.id.high_score);
            if (result > -1) {
                highScoreView.setText(String.format("%d", result));
            } else {
                highScoreView.setText("--");
            }
        }
    }

    /**
     * This task asynchronously updates the best time.
     */
    private class UpdateBestTimeTask extends AsyncTask<Object, Object, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            return mGameStorage.bestTime(mDatabase, mFilename);
        }

        @Override
        protected void onProgressUpdate(Object... progress) {
            // update progress here
        }

        @Override
        protected void onPostExecute(Integer result) {
            TextView bestTimeView = (TextView) findViewById(R.id.best_time);
            if (result > -1) {
                TimeZone tz = TimeZone.getTimeZone("UTC");
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                df.setTimeZone(tz);
                String time = df.format(new Date(result));
                bestTimeView.setText(String.format("%s", time));
            } else {
                bestTimeView.setText("--:--");
            }
        }
    }

    /**
     * This task asynchronously updates the games played.
     */
    private class UpdateGamesPlayedTask extends AsyncTask<Object, Object, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            return mGameStorage.gamesPlayed(mDatabase, mFilename);
        }

        @Override
        protected void onProgressUpdate(Object... progress) {
            // update progress here
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result > -1) {
                TextView bestTimeView = (TextView) findViewById(R.id.games_played);
                bestTimeView.setText(String.format("%d", result));
            }
        }
    }

    /**
     * This task asynchronously updates the games won.
     */
    private class UpdateGamesWon extends AsyncTask<Object, Object, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            return mGameStorage.gamesWon(mDatabase, mFilename);
        }

        @Override
        protected void onProgressUpdate(Object... progress) {
            // update progress here
        }

        @Override
        protected void onPostExecute(Integer result) {
            TextView bestTimeView = (TextView) findViewById(R.id.games_won);
            if (result > -1) {
                bestTimeView.setText(String.format("%d", result));
            } else {
                bestTimeView.setText("--");
            }
        }
    }

    /**
     * This task asynchronously updates the win percentage.
     */
    private class UpdateWonPercentage extends AsyncTask<Object, Object, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            int gamesPlayed = mGameStorage.gamesPlayed(mDatabase, mFilename);
            int gamesWon = mGameStorage.gamesWon(mDatabase, mFilename);
            if (gamesPlayed > 0) {
                return (int) (((float) gamesWon / gamesPlayed) * 100);
            } else {
                return -1;
            }
        }

        @Override
        protected void onProgressUpdate(Object... progress) {
            // update progress here
        }

        @Override
        protected void onPostExecute(Integer result) {
            TextView bestTimeView = (TextView) findViewById(R.id.percent_won);
            if (result > -1) {
                bestTimeView.setText(String.format("(%d%%)", result));
            } else {
                bestTimeView.setText("(--%)");
            }
        }
    }

    /**
     * This task asynchronously updates the details title.
     */
    private class UpdateTitle extends AsyncTask<Object, Object, String> {

        @Override
        protected String doInBackground(Object... params) {
             return mModel.getName();
        }

        @Override
        protected void onProgressUpdate(Object... progress) {
            // update progress here
        }

        @Override
        protected void onPostExecute(String result) {
            GameDetailsActivity.this.setTitle(result);
        }
    }


    /**
     * This task asynchronously updates the rules webview.
     */
    private class UpdateRules extends AsyncTask<Object, Object, String> {

        @Override
        protected String doInBackground(Object... params) {
            return mModel.getRules();
        }

        @Override
        protected void onProgressUpdate(Object... progress) {
            // update progress here
        }

        @Override
        protected void onPostExecute(String result) {
            TextView rulesView = (TextView) findViewById(R.id.rules);
            rulesView.setText(Html.fromHtml(result));
        }
    }

    /**
     * This class asynchronously resets game stats
     */
    private class ResetStatsTask extends AsyncTask<Object, Object, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {
            mGameStorage.reset(mDatabase, mFilename);
            return true;
        }

        @Override
        protected void onProgressUpdate(Object... progress) {
            // update progress here
        }

        @Override
        protected void onPostExecute(Boolean result) {
            new UpdateBestTimeTask().execute();
            new UpdateGamesPlayedTask().execute();
            new UpdateGamesWon().execute();
            new UpdateHighScoreTask().execute();
            new UpdateWonPercentage().execute();
        }
    }

    private class ResetAcceptListener implements DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            new ResetStatsTask().execute();
        }
    }

}
