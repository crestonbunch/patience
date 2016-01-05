package im.bunch.patience.game;

import java.util.List;

import im.bunch.patience.geometry.Point;

/**
 * Interface for game components tracked by a ComponentManager.
 *
 * @author Creston Bunch
 */
public interface Component {

    void onDragStart(List<Component> targets, Point p);
    void onDrag(List<Component> targets, Point p);
    void onDragEnd(List<Component> targets, Point p);
    void onTap(List<Component> targets, Point p);
    boolean onCancelDrag();

    void onRegister(ComponentManager componentManager);
    void onUnregister(ComponentManager componentManager);

    void onUpdate();

    void onSurfaceChange(int width, int height);

    /**
     * Return a number that determines the position of this component in the component list.
     */
    int getOrder();

    /**
     * Check if this component collides with a point in 3d space.
     * @param p
     * @return
     */
    boolean collidesWithPoint(Point p);

}
