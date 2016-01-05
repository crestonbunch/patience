package im.bunch.patience.game;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import im.bunch.patience.geometry.Point;

/**
 * Tracks game components. These are components that are views and can be interacted with. Each
 * component must be registered with a component manager to receive events. Additionally, a
 * component manager must be registered to an InputManager to receive touch events.
 *
 * @author Creston Bunch
 */
public class ComponentManager {

    public static final String LOG_TAG = "CmptMgr";

    private List<Component> mComponentList;
    private Context mContext;

    /**
     * Initialize the ComponentManager with an empty list of components.
     */
    public ComponentManager(Context context) {
        mComponentList = new ArrayList<>();
        mContext = context;
    }

    /**
     * Register a component with the ComponentManager.
     *
     * @param c
     */
    public synchronized void registerComponent(Component c) {
        mComponentList.add(c);
        c.onRegister(this);
    }

    public synchronized void unregisterComponent(Component c) {
        boolean result = mComponentList.remove(c);
        c.onUnregister(this);
    }

    // TODO: components should be in some kind of priority queue
    public synchronized List<Component> getComponents() {
        Collections.sort(mComponentList, new Comparator<Component>() {
            @Override
            public int compare(Component lhs, Component rhs) {
                if (lhs != null && rhs == null) {
                    return lhs.getOrder();
                } else if (rhs != null && lhs == null) {
                    return 0 - rhs.getOrder();
                } else if (lhs == null && rhs == null) {
                    return 0;
                }
                return lhs.getOrder() - rhs.getOrder();
            }
        });
        return mComponentList;
    }

    /**
     * Runs the update function on all components.
     */
    public synchronized void update() {
        Long start = System.currentTimeMillis();
        for (Component c : new ArrayList<>(this.getComponents())) {
            c.onUpdate();
        }
        Long end = System.currentTimeMillis();
        Log.d(LOG_TAG, "Update cycle: " + Double.toString((end - start) / 1000.0) + "s");
    }

    public Context getContext() {
        return this.mContext;
    }

    /**
     * Activate drag start events for components at the given point.
     *
     * @param p
     */
    public synchronized void onDragStart(Point p) {
        // gather all components at this point
        List<Component> targets = new ArrayList<>();
        for (Component c : new ArrayList<>(this.getComponents())) {
            if (c.collidesWithPoint(p)) {
                targets.add(c);
            }
        }

        for (Component c : new ArrayList<>(this.getComponents())) {
            c.onDragStart(targets, p);
        }
        update();
    }

    /**
     * Activate drag events for components at the given point.
     *
     * @param p
     */
    public synchronized void onDrag(Point p) {
        // gather all components at this point
        List<Component> targets = new ArrayList<>();
        for (Component c : new ArrayList<>(this.getComponents())) {
            if (c.collidesWithPoint(p)) {
                targets.add(c);
            }
        }

        for (Component c : new ArrayList<>(this.getComponents())) {
                c.onDrag(targets, p);
        }
        update();
    }

    /**
     * Activate drag end events for components at the given point.
     *
     * @param p
     */
    public synchronized void onDragEnd(Point p) {
        // gather all components at this point
        List<Component> targets = new ArrayList<>();
        for (Component c : new ArrayList<>(this.getComponents())) {
            if (c.collidesWithPoint(p)) {
                targets.add(c);
            }
        }

        // each component gets a list of all targets
        for (Component c : new ArrayList<>(this.getComponents())) {
            c.onDragEnd(targets, p);
        }
        update();
    }

    /**
     * Activate tap events for components at the given point.
     *
     * @param p
     */
    public synchronized void onTap(Point p) {
        // gather all components at this point
        List<Component> targets = new ArrayList<>();
        for (Component c : new ArrayList<>(this.getComponents())) {
            if (c.collidesWithPoint(p)) {
                targets.add(c);
            }
        }

        for (Component c : new ArrayList<>(this.getComponents())) {
            c.onTap(targets, p);
        }
        update();
    }

    /**
     * Activate events when the surface shape changes.
     */
    public synchronized void onSurfaceChanged(int width, int height) {
        for (Component c : new ArrayList<>(this.getComponents())) {
            c.onSurfaceChange(width, height);
        }
        update();
    }

    /**
     * Cancels the drag event for components.
     */
    public synchronized boolean cancelDrag() {
        // each component gets canceled
        boolean changed = false;
        for (Component c : new ArrayList<>(this.getComponents())) {
            if (c.onCancelDrag()) {
                changed = true;
            }
        }
        update();
        return changed;
    }
}
