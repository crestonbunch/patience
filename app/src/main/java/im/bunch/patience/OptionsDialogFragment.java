package im.bunch.patience;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.VoiceInteractor;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.eclipsesource.v8.V8Object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.bunch.patience.model.JavascriptModel;


/**
 * An options dialog that provides configuration settings for patience games.
 *
 * @author Creston
 */
public class OptionsDialogFragment extends DialogFragment {

    public interface OptionsDialogListener {
        void onDialogPositiveClick(OptionsDialogFragment dialog);
        void onDialogNegativeClick(OptionsDialogFragment dialog);
    }

    OptionsDialogListener mListener;
    String[] mOptionKeys;
    CharSequence[] mOptionNames;
    boolean[] mOptionDefaults;
    Map<String, Boolean> mOptions;

    public OptionsDialogFragment() {
        super();
    }

    // TODO: use setArguments() poperly
    public void setModel(JavascriptModel model) {
        V8Object options = model.getOptions();
        synchronized (model) {
            model.acquireLock();
            mOptions = new HashMap<>();

            // build the option arrays
            List<String> keys = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<Boolean> defaults = new ArrayList<>();
            for (String key : options.getKeys()) {
                keys.add(key);
                V8Object option = options.getObject(key);
                names.add(option.getString("display"));
                boolean d = option.getBoolean("value");
                defaults.add(d);
                mOptions.put(key, d);
            }
            boolean[] temp = new boolean[defaults.size()];
            mOptionKeys = keys.toArray(new String[keys.size()]);
            mOptionNames = names.toArray(new String[names.size()]);
            for (int i = 0; i < defaults.size(); i++) {
                temp[i] = defaults.get(i);
            }
            mOptionDefaults = temp;
            model.releaseLock();
        }
    }

    /**
     * Called when an activity attaches itself to this options dialog, so we can keep track of
     * the activity.
     *
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (OptionsDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // TODO: put these in strings resource
        builder.setTitle("Options")
            .setMultiChoiceItems(mOptionNames, mOptionDefaults,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        mOptions.put(mOptionKeys[which], isChecked);
                    }
                })
            .setPositiveButton("Start Game", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mListener.onDialogPositiveClick(OptionsDialogFragment.this);
                }
            })
            .setNegativeButton("Go Back", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    mListener.onDialogNegativeClick(OptionsDialogFragment.this);
                }
            });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    /**
     * Get the map of chosen options.
     *
     * @return
     */
    public Map<String, Boolean> getOptions() {
        return mOptions;
    }

}
