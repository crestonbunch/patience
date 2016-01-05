package im.bunch.patience.game;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import im.bunch.patience.game.graphics.Rectangle;

/**
 * Compenent views are objects that contain drawables that can be drawn to a canvas.
 *
 * @author Creston Bunch
 */
public interface RectangleComponent extends Component {

    Rectangle getRectangle();

    void clearRectangle();

    Rect getBounds();

}
