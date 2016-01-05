package im.bunch.patience.game;

import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import im.bunch.patience.game.graphics.Rectangle;
import im.bunch.patience.model.JavascriptModel;
import im.bunch.patience.view.Card;

/**
 * This is a hardware accelerated renderer for game components.
 *
 * @author Creston
 */
public class GameRenderer implements GLSurfaceView.Renderer {

    private ComponentManager componentManager;
    private int width;
    private int height;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    public GameRenderer(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //GLES20.glClearColor(0.0f, 0.415f, 0.051f, 1.0f);
        GLES20.glClearColor(0.38f, 0.49f, 0.55f, 1.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        GLES20.glViewport(0,0, width, height);

        float ratio = (float) height / width;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, 0, 1, ratio, 0, 1, 3);

        // update components
        componentManager.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // update logic
        //this.componentManager.update();

        Long start = System.currentTimeMillis();
        // draw
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 1, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        for (Component c : new ArrayList<>(this.componentManager.getComponents())) {
            if (c instanceof RectangleComponent) {

                Rect r = ((RectangleComponent) c).getBounds();
                if (r != null) {
                    float scaleX = (float) r.width() / this.width;
                    float scaleY = (float) r.height() / r.width() * scaleX;
                    float translateX = (float) r.left / this.width;
                    float translateY = (float) r.top / this.width;

                    // calculate transformation matrix
                    float[] transform = new float[16];
                    float[] translate = new float[16];
                    float[] scale = new float[16];

                    Matrix.setIdentityM(transform, 0);
                    Matrix.setIdentityM(translate, 0);
                    Matrix.setIdentityM(scale, 0);
                    // build scale and translation matrices
                    Matrix.scaleM(scale, 0, scaleX, scaleY, 1.0f);
                    Matrix.translateM(translate, 0, translateX, translateY, 0.0f);
                    // build transformation matrix
                    Matrix.multiplyMM(transform, 0, translate, 0, scale, 0);

                    // combine transformation matrix with the projection and camera view
                    Matrix.multiplyMM(transform, 0, mMVPMatrix, 0, transform, 0);

                    Rectangle rect = ((RectangleComponent) c).getRectangle();
                    if (rect != null) {
                        rect.draw(transform);
                    }
                }
            }
        }
        Long end = System.currentTimeMillis();

        Log.d("Renderer", "Render cycle: " + Double.toString((end - start) / 1000.0) + "s");
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

}
