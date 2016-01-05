package im.bunch.patience.game;

import android.util.Log;
import android.view.MotionEvent;

import java.util.Vector;

import im.bunch.patience.geometry.Point;

/**
 * Tracks user input during the game execution. Connects to a ComponentManager and executes events.
 *
 * @author Creston Bunch
 */
public class InputManager {

    public static final String LOG_TAG = "Input Manager";
    public static final double TAP_DISTANCE_THRESHOLD = 10.0;

    private ComponentManager componentManager;

    private Point starting;
    private Point ending;
    private boolean dragging;

    public InputManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    /**
     * Call each time a motion event happens. Tracks motion events to determine if they are simple
     * taps or drag events and calls the necessary event handlers.
     *
     * @param event
     */
    public void registerTouch(MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_MOVE) {

            // calculate previous distance
            double prevDistance = this.starting.distanceTo(this.ending);
            // calculate current distance
            this.ending = new Point(event.getX(), event.getY());
            double currDistance = this.starting.distanceTo(this.ending);

            if (currDistance > TAP_DISTANCE_THRESHOLD && prevDistance <= TAP_DISTANCE_THRESHOLD) {
                // current distance dragged will be registered as a drag event
                Log.i(LOG_TAG, "Tap threshold reached.");
                this.onDragStart(event);

            } else if (currDistance > TAP_DISTANCE_THRESHOLD) {
                // current distance dragged is already registered as a drag event
                this.onDrag(event);
            }

        } else if (action == MotionEvent.ACTION_DOWN) {
            Log.i(LOG_TAG, "Down action begun.");

            this.starting = new Point(event.getX(), event.getY());
            this.ending = new Point(event.getX(), event.getY());

        } else if (action == MotionEvent.ACTION_UP) {

            double currDistance = this.starting.distanceTo(this.ending);
            Log.d(LOG_TAG, "Traveled distance: " + Double.toString(currDistance));

            if (currDistance > TAP_DISTANCE_THRESHOLD) {
                this.onDragEnd(event);
            } else {
                this.onTap(event);
            }
        }
    }

    private void onDragStart(MotionEvent event) {
        Log.i(LOG_TAG, "Starting drag.");

        Point p = new Point(event.getX(), event.getY());
        this.componentManager.onDragStart(p);
    }

    private void onDrag(MotionEvent event) {
        Point p = new Point(event.getX(), event.getY());
        this.componentManager.onDrag(p);
    }

    private void onDragEnd(MotionEvent event) {
        Log.i(LOG_TAG, "Ending drag.");

        Point p = new Point(event.getX(), event.getY());
        this.componentManager.onDragEnd(p);
    }

    private void onTap(MotionEvent event) {
        Log.i(LOG_TAG, "Tapped.");

        Point p = new Point(event.getX(), event.getY());
        this.componentManager.onTap(p);
    }

}
