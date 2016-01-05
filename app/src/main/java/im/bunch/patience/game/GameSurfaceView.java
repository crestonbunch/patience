package im.bunch.patience.game;

import android.content.Context;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import java.util.List;

import im.bunch.patience.model.JavascriptModel;

/**
 * This is a hardware accelerated surface view for drawing game components.
 *
 * @author Creston
 */
public class GameSurfaceView extends GLSurfaceView {

    private final GameRenderer renderer;
    private Context context;
    private ComponentManager componentManager;
    private InputManager inputManager;

    public GameSurfaceView(Context context, ComponentManager componentManager) {
        super(context);
        this.context = context;
        this.componentManager = componentManager;
        this.inputManager = new InputManager(this.componentManager);

        setEGLContextClientVersion(2);

        this.renderer = new GameRenderer(componentManager);
        setRenderer(this.renderer);

        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        this.inputManager.registerTouch(e);
        requestRender();
        return true;
    }



}
