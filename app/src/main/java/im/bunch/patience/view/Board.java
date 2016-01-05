package im.bunch.patience.view;

import com.eclipsesource.v8.V8Object;

import java.util.List;

import im.bunch.patience.game.Component;
import im.bunch.patience.game.ComponentManager;
import im.bunch.patience.geometry.Point;
import im.bunch.patience.model.JavascriptModel;

/**
 * This is a Java view that represents the game board that solitaire is played on.
 *
 * @author Creston
 */
public class Board implements Component {

    private JavascriptModel mJSModel;
    private V8Object mJSBoard;

    public static final int BOARD_ORDER = -2;

    /**
     * Construct a board from a Javascript model.
     *
     * @param jsModel
     * @param jsBoard
     */
    public Board(JavascriptModel jsModel, V8Object jsBoard) {
        mJSModel = jsModel;
        mJSBoard = jsBoard;
    }

    public int getRows() {
        return mJSBoard.getInteger("rows");
    }

    public int getColumns() {
        return mJSBoard.getInteger("cols");
    }

    @Override
    public void onDragStart(List<Component> targets, Point p) {

    }

    @Override
    public void onDrag(List<Component> targets, Point p) {

    }

    @Override
    public void onDragEnd(List<Component> targets, Point p) {

    }

    @Override
    public void onTap(List<Component> targets, Point p) {

    }

    @Override
    public boolean onCancelDrag() {
        return false;
    }

    @Override
    public void onRegister(ComponentManager componentManager) {

    }

    @Override
    public void onUnregister(ComponentManager componentManager) {

    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onSurfaceChange(int width, int height) {
        mJSModel.resize(width, height);
    }

    @Override
    public int getOrder() {
        return BOARD_ORDER;
    }

    @Override
    public boolean collidesWithPoint(Point p) {
        return false;
    }
}
