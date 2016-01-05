package im.bunch.patience;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SplashScreen extends AppCompatActivity {

    public static final String LOG_TAG = "SplashScreen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        Resources res = getResources();
        final AssetManager am = res.getAssets();
        // directory to place scripts in local storage
        final File dir = getApplicationContext().getDir("scripts", Context.MODE_PRIVATE);
        try {
            // go through each script in the scripts directory and put them in local storage
            final String fileList[] = am.list("scripts");
            Log.i("Scripts", Arrays.toString(fileList));
            if (fileList != null) {
                for (int i = 0; i < fileList.length; i++) {
                    // read the game asset
                    final String file = fileList[i];
                    Log.d(LOG_TAG, "Reading game asset from " + file);
                    // copy the game asset
                    File out = new File(dir, file);
                    Log.d(LOG_TAG, "Writing game asset to " + out);
                    FileOutputStream fos = new FileOutputStream(out);
                    IOUtils.copy(am.open("scripts/" +file), fos);
                    fos.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent i = new Intent(SplashScreen.this, GamesList.class);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash_screen, menu);
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
}
