package im.bunch.patience.model;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8Executor;
import com.eclipsesource.v8.utils.V8Runnable;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import im.bunch.patience.game.Component;
import im.bunch.patience.game.ComponentManager;
import im.bunch.patience.view.Board;
import im.bunch.patience.view.Card;
import im.bunch.patience.view.Game;
import im.bunch.patience.view.Pile;

/**
 * A javascript model holds the game state in a V8 runtime. This class exposes some APIs for the
 * Java view to get information and update the state.
 *
 * @author Creston Bunch
 */
public class JavascriptModel {

    private V8Object mGame;
    private String mScript;
    private Context mContext;
    private V8 mRuntime;
    private List<Runnable> mScoreListeners;
    private List<Runnable> mSaveListeners;
    private List<Runnable> mEndListeners;
    private List<String> mHistory;

    /**
     * Construct a Javascript model from a given application context and a script.
     *
     * @param ctx    The application context that is loading the model.
     * @param script The script to use for creating the model.
     * @throws IOException if common.js cannot be loaded.
     */
    public JavascriptModel(Context ctx, String script) throws IOException {
        mScript = script;
        mContext = ctx;
        mScoreListeners = new ArrayList<>();
        mSaveListeners = new ArrayList<>();
        mEndListeners = new ArrayList<>();
        mRuntime = V8.createV8Runtime("", "/tmp");
        mHistory = new ArrayList<>();

        final AssetManager am = ctx.getAssets();
        String common = IOUtils.toString(am.open("common.js"));

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
        mRuntime.registerJavaMethod(callback, "print");

        callback = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
                if (parameters.length() > 0) {
                    try {
                        int arg1 = parameters.getInteger(0);
                        mGame.add("score", Math.max(mGame.getInteger("score") + arg1, 0));
                        // run handlers listening for score changes
                        for (Runnable r : mScoreListeners) {
                            r.run();
                        }
                    } catch (Exception e) {
                        Log.e("Model", e.toString());
                    }
                }
                parameters.release();
            }
        };

        // function for adding score
        mRuntime.registerJavaMethod(callback, "score");

        callback = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
                try {
                    for (Runnable r : mEndListeners) {
                        r.run();
                    }
                } catch (Exception e) {
                    Log.e("Model", e.toString());
                }
            }
        };

        // function for executing win/lose listeners
        mRuntime.registerJavaMethod(callback, "win");
        mRuntime.registerJavaMethod(callback, "lose");

        callback = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
                try {
                    mHistory.add(serialize());
                    for (Runnable r : mSaveListeners) {
                        r.run();
                    }
                } catch (Exception e) {
                    Log.e("Model", e.toString());
                }
                parameters.release();
            }
        };

        // function to update history
        mRuntime.registerJavaMethod(callback, "history");

        callback = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
                try {
                    for (Runnable r : mSaveListeners) {
                        r.run();
                    }
                } catch (Exception e) {
                    Log.e("Model", e.toString());
                }
                parameters.release();
            }
        };

        // function to save the game
        mRuntime.registerJavaMethod(callback, "save");


        synchronized (this) {
            acquireLock();
            mRuntime.executeScript(common);
            mRuntime.executeScript(script);
            releaseLock();
        }
    }

    /**
     * Initialize an empty game from the model.
     */
    public synchronized void initialize() {
        acquireLock();
        V8Array parameters = new V8Array(mRuntime);
        mGame = mRuntime.executeObjectFunction("init", parameters);
        mHistory.add(serialize());
        parameters.release();
        releaseLock();
    }

    /**
     * Calls the serialize function defined in JavaScript on the current game state so that it can
     * be safely preserved.
     *
     * @return
     */
    public synchronized String serialize() {
        V8Array parameters = new V8Array(mRuntime);
        parameters.push(mGame);
        String result = mRuntime.executeStringFunction("serialize", parameters);
        return result;
    }

    /**
     * Calls the deserialize function defined in JavaScript on a JSON string so that the game state
     * can be rebuilt from a previously serialized state.
     *
     * @param json The serialized json from serialize
     */
    public synchronized void deserialize(String json) {
        acquireLock();
        V8Array parameters = new V8Array(mRuntime);
        parameters.push(json);
        mGame = mRuntime.executeObjectFunction("deserialize", parameters);
        parameters.release();
        releaseLock();
    }

    /**
     * Calls the deserialize function defined in JavaScript on a JSON string so that the game state
     * can be rebuilt from a previously serialized state. Also updates the history from a given
     * history list.
     *
     * @param json    The serialized json from serialize
     * @param history The history list to load.
     */
    public synchronized void deserialize(String json, List<String> history) {
        acquireLock();
        V8Array parameters = new V8Array(mRuntime);
        parameters.push(json);
        mGame = mRuntime.executeObjectFunction("deserialize", parameters);
        parameters.release();
        mHistory = history;
        releaseLock();
    }

    /**
     * Get the script that the V8 runtime is using.
     */
    public String getScript() {
        return mScript;
    }

    /**
     * Get the name of the game from the script.
     */
    public synchronized String getName() {
        acquireLock();
        String name = mRuntime.getObject("properties").getString("name");
        releaseLock();
        return name;
    }

    /**
     * Get the game description from the script.
     */
    public synchronized String getDescription() {
        acquireLock();
        String name = mRuntime.getObject("properties").getString("description");
        releaseLock();
        return name;
    }

    /**
     * Get the game rules from the script.
     */
    public synchronized String getRules() {
        acquireLock();
        String name = mRuntime.getObject("properties").getString("rules");
        releaseLock();
        return name;
    }

    /**
     * Get the JavaScript game model.
     */
    public synchronized V8Object getGame() {
        return mGame;
    }

    /**
     * Get the current score.
     */
    public synchronized int getScore() {
        acquireLock();
        int score = mGame.getInteger("score");
        releaseLock();
        return score;
    }

    /**
     * Check if the game is won
     */
    public synchronized boolean hasWon() {
        acquireLock();
        boolean won = mGame.getBoolean("won");
        releaseLock();
        return won;
    }

    /**
     * Check if the game is lost.
     */
    public synchronized boolean hasLost() {
        acquireLock();
        boolean lost = mGame.getBoolean("lost");
        releaseLock();
        return lost;
    }

    /**
     * Get options.
     */
    public synchronized V8Object getOptions() {
        acquireLock();
        V8Object options = mGame.getObject("options");
        releaseLock();
        return options;
    }

    /**
     * Check if there are any customizations for this game.
     */
    public synchronized boolean hasOptions() {
        acquireLock();
        V8Object options = mGame.getObject("options");
        boolean result = options.getKeys().length > 0;
        releaseLock();
        return result;
    }

    /**
     * Set a game option.
     */
    public synchronized void setOption(String key, boolean value) {
        acquireLock();
        V8Object options = mGame.getObject("options");
        V8Object option = options.getObject(key);
        option.add("value", value);
        V8Array parameters = new V8Array(mRuntime);
        parameters.push(mGame);
        mRuntime.executeVoidFunction("updateOptions", parameters);
        parameters.release();
        releaseLock();
    }

    /**
     * Check if this game allows undos
     */
    public synchronized boolean canUndo() {
        acquireLock();
        boolean result = mGame.getBoolean("undo");
        releaseLock();
        return result;
    }

    /**
     * Get the model history.
     */
    public synchronized  List<String> getHistory() {
        return mHistory;
    }

    /**
     * Pop off the history last history state and rebuild the view.
     */
    public void back(ComponentManager componentManager) {
        Log.d("Model", Integer.toString(mHistory.size()));
        if (mHistory.size() > 0) {
            synchronized (this) {
                acquireLock();
                for (Component c : new ArrayList<>(componentManager.getComponents())) {
                    componentManager.unregisterComponent(c);
                }
                releaseLock();
            }
            // remove last state
            if (mHistory.size() > 1) {
                mHistory.remove(mHistory.size() - 1);
            }
            // return to old state
            deserialize(mHistory.get(mHistory.size() - 1));

            buildView(componentManager);
        }
    }

    /**
     * Calls a function whenever a card gets tapped.
     */
    public synchronized void tapCard(V8Object card) {
        acquireLock();
        if (mRuntime.contains("tapCard")) {
            V8Array parameters = new V8Array(mRuntime);
            parameters.push(mGame).push(card);
            mRuntime.executeVoidFunction("tapCard", parameters);
            parameters.release();
        } else {
            // do nothing
        }
        releaseLock();
    }

    /**
     * Calls the bonus score calculation.
     */
    public synchronized void bonus(long time) {
        acquireLock();
        if (mRuntime.contains("bonus")) {
            V8Array parameters = new V8Array(mRuntime);
            parameters.push(time);
            int score = mGame.getInteger("score");
            mGame.add("score", score + mRuntime.executeIntegerFunction("bonus", parameters));
            parameters.release();
        } else {
            Log.i("Model", "No bonus function defined.");
        }
        releaseLock();

        for (Runnable r: mScoreListeners) {
            r.run();
        }
    }

    /**
     * Called when the screen size changes.
     */
    public synchronized void resize(int width, int height) {
        acquireLock();
        if (mRuntime.contains("resize")) {
            V8Array parameters = new V8Array(mRuntime);
            parameters.push(mGame).push(width).push(height);
            mRuntime.executeVoidFunction("resize", parameters);
            parameters.release();
        }
        releaseLock();
    }

    /**
     * Attach a runnable that gets run each time the score is updated.
     */
    public void registerScoreListener(Runnable r) {
        mScoreListeners.add(r);
    }

    /**
     * Attach a runnable that gets run each time the history changes.
     */
    public void registerSaveListener(Runnable r) {
        mSaveListeners.add(r);
    }

    /**
     * Attach a runnable that gets run at the end of the game (win or lose).
     */
    public void registerEndListener(Runnable r) {
        mEndListeners.add(r);
    }

    /**
     * Get the column width of the board. This function is not thread safe.
     */
    public int getColumns() {
        V8Object board = mGame.getObject("board");
        int columns = board.getInteger("cols");
        return columns;
    }

    /**
     * Get the row height of the board. This function is not thread safe.
     */
    public int getRows() {
        acquireLock();
        V8Object board = mGame.getObject("board");
        int rows = board.getInteger("rows");
        return rows;
    }

    /**
     * Builds a view from this model. Constructs a Board object, Piles, and Cards and registers
     * them all with a component manager. Returns a Game object.
     *
     * @param componentManager The componentManager that will manage view components.
     * @return
     */
    public synchronized Game buildView(ComponentManager componentManager) {
        
        Game game = new Game(this, componentManager);

        acquireLock();

        V8Object jsBoard = mGame.getObject("board");
        Board board = new Board(this, jsBoard);
        componentManager.registerComponent(board);

        V8Object jsPilesMap = mGame.getObject("piles");

        for (String key : jsPilesMap.getKeys()) {
            V8Object jsPile = jsPilesMap.getObject(key);

            V8Array cards = jsPile.getArray("cards");

            for (String index : cards.getKeys()) {
                V8Object jsCard = cards.getObject(index);

                Card card = new Card(this, jsCard);
                componentManager.registerComponent(card);
            }

            Pile pile = new Pile(this, jsPile);
            componentManager.registerComponent(pile);
        }
        releaseLock();
        
        return game;
    }

    /**
     * Acquires the V8 runtime thread lock. You'll notice that we acquire and release this lock
     * A LOT. It is also necessary to synchronize the JavaScript model in addition to acquiring
     * and releasing the lock so that we do not attempt to acquire a lock in use.
     */
    public synchronized void acquireLock() {
        mRuntime.getLocker().acquire();
    }

    /**
     * Releases the V8 runtime thread lock.
     */
    public synchronized void releaseLock() {
        mRuntime.getLocker().release();
    }

    /**
     * Check if this thread has the lock.
     */
    public synchronized boolean hasLock() {
        return mRuntime.getLocker().hasLock();
    }

}
