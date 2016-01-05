package im.bunch.patience.files;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.v8.V8;

import im.bunch.patience.GameActivity;

/**
 * A class designed to mediate between Android storage and program logic so that activities are not
 * tightly coupled to file loading logic.
 *
 * @author Creston
 * @version 1.0.0
 */
public class GameRules {

    public static final String LOG_TAG = "GameRules";
    public static final String SHEBANG = "//#!GameRules";

    private V8 runtime;
    private String filename;
    private String script;

    private GameActivity activity;

    /**
     * Initialize a game rule with a context and script.
     *
     * @param runtime the V8 runtime used to run the script.
     * @param script The script used.
     * @throws IOException
     */
    public GameRules(V8 runtime, String filename, String script)
            throws IOException {
        this.runtime = runtime;
        this.filename = filename;
        this.script = script;
    }

    /**
     * Get the script used to create this game rule.
     */
    public String getScript() {
        return this.script;
    }

    /**
     * Get the human friendly name of this game rule.
     *
     * @return The name.
     */
    public String getName() {
        return runtime.getObject("properties").getString("name");
    }

    /**
     * Get the file name of the script.
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * Return a list of game rules available.
     *
     * @param ctx The application context to load the files from.
     * @param path The directory the files are kept in.
     * @return The list of game rules.
     */
    public static List<GameRules> list(Context ctx, String path) throws IOException {
        // load the list of game rules from local storage
        Log.d(LOG_TAG, "Loading game rules from local storage.");
        final File dir = ctx.getDir(path, Context.MODE_PRIVATE);
        final List<GameRules> result = new ArrayList<>();

        for (final File f : dir.listFiles()) {
            Log.d(LOG_TAG, "Reading game asset from " + f.getAbsolutePath());
            final FileInputStream fin = new FileInputStream(f);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(fin));

            // only scripts that begin with the right shebang should be included
            if (reader.readLine().startsWith(SHEBANG)) {
                final String script = IOUtils.toString(reader);

                GameRules rule = GameRules.load(ctx, f.getName(), script);

                result.add(rule);
            }
        }

        return result;
    }

    /**
     * Load a game rule from a script and an Android application context.
     *
     * @param ctx The application context to load common.js from.
     * @param script The script to read
     * @return The game rule.
     */
    public static GameRules load(Context ctx, String name, String script) throws IOException {
        final AssetManager am = ctx.getAssets();
        // load common.js
        String common = IOUtils.toString(am.open("common.js"));

        V8 runtime = V8.createV8Runtime("","/tmp");
        final GameRules rules = new GameRules(runtime, name, script);

        JavaVoidCallback callback = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
                if (parameters.length() > 0) {
                    Object arg1 = parameters.get(0);
                    System.out.println(arg1);
                    if (arg1 instanceof Releasable) {
                        ((Releasable) arg1).release();
                    }
                }
            }
        };
        // allow printing
        runtime.registerJavaMethod(callback, "print");

        runtime.executeScript(common);
        runtime.executeScript(script);

        return rules;
    }

    public void destroy() {
        this.runtime.release();
    }
}
