package im.bunch.patience.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.bunch.patience.R;
import im.bunch.patience.game.Component;
import im.bunch.patience.game.ComponentManager;
import im.bunch.patience.game.RectangleComponent;
import im.bunch.patience.game.graphics.Rectangle;
import im.bunch.patience.geometry.Point;
import im.bunch.patience.model.JavascriptModel;

/**
 * This is a Java card view that corresponds to a Javascript card model.
 *
 * @author Creston Bunch
 */
public class Card implements RectangleComponent {

    private String mSuit;
    private String mRank;
    private JavascriptModel mJSModel;
    private V8Object mJSCard;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mCardWidth;
    private int mCardHeight;
    private int mRows;
    private int mCols;
    private ComponentManager mComponentManager;
    private Rectangle mFace;
    private Rectangle mBack;
    private int mOrder;
    private Rect mBounds;
    private boolean mVisible;

    // standard playing cards are 2.5in by 3.5in
    public static final double HEIGHT_WIDTH_RATIO = 3.5 / 2.5;

    private static Map<String, Rectangle> rectangleMap = new HashMap<>();

    /**
     * Costruct a card from a JavaScript model.
     * 
     * @param jsModel The JavaScript model that created this card.
     * @param jsCard The JavaScript card model representing this card.
     */
    public Card(JavascriptModel jsModel, V8Object jsCard) {
        mJSModel = jsModel;
        mJSCard = jsCard;
        mSuit = mJSCard.getString("suit");
        mRank = mJSCard.getString("rank");
    }

    /**
     * Get the JavaScript card model.
     */
    public V8Object getJSCard() {
        return mJSCard;
    }

    /**
     * Gets a Rectangle used by GLES for drawing this card to the screen.
     *
     * @return
     */
    @Override
    public Rectangle getRectangle() {
        if (mVisible) {
            if (!rectangleMap.containsKey(getResId())) {
                int res = mComponentManager.getContext().getResources().getIdentifier(
                        getResId(), "drawable", mComponentManager.getContext().getPackageName()
                );

                Context context = mComponentManager.getContext();
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;   // No pre-scaling

                // Read in the resource
                final Bitmap bitmap = BitmapFactory.decodeResource(
                        context.getResources(), res, options
                );

                Rectangle rectangle = new Rectangle(bitmap);
                rectangleMap.put(getResId(), rectangle);

                return rectangle;
            } else {
                return rectangleMap.get(getResId());
            }
        } else {
            if (!rectangleMap.containsKey("card_back")) {
                int res = R.drawable.card_back;

                Context context = mComponentManager.getContext();
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;   // No pre-scaling

                // Read in the resource
                final Bitmap bitmap = BitmapFactory.decodeResource(
                        context.getResources(), res, options
                );

                Rectangle rectangle = new Rectangle(bitmap);

                rectangleMap.put("card_back", rectangle);

                return rectangle;
            } else {
                return rectangleMap.get("card_back");
            }
        }
    }

    /**
     * Clear the rectangle map to force it to be recreated next draw cycle.
     */
    @Override
    public void clearRectangle() {
        for (Rectangle rect : rectangleMap.values()) {
            rect.destroy();
        }
        rectangleMap = new HashMap<>();
    }

    /**
     * Gets the bounds of the card that describes where it is on the surface and how big it is.
     * 
     * @return
     */
    @Override
    public Rect getBounds() {
        return new Rect(
                mBounds.left + Pile.CARD_MARGIN,
                mBounds.top + Pile.CARD_MARGIN,
                mBounds.right - Pile.CARD_MARGIN,
                mBounds.bottom - Pile.CARD_MARGIN
        );
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
        if (targets.contains(this)) {
            mJSModel.tapCard(mJSCard);
        }
    }

    @Override
    public boolean onCancelDrag() {
        return false;
    }

    /**
     * Called when this card gets registered with a component manager. We will keep track of the
     * component manager it is registered to so we can have access to other components.
     * 
     * @param componentManager
     */
    @Override
    public void onRegister(ComponentManager componentManager) {
        mComponentManager = componentManager;
    }

    /**
     * Called when this card gets unregistered with a component manager. We will destroy the
     * OpenGL objects that are used to draw this card since we no longer need them and they will
     * consume video memory. We also release the JavaScript card model in the V8 runtime.
     * 
     * @param componentManager
     */
    @Override
    public void onUnregister(ComponentManager componentManager) {
        if (mFace != null) {
            mFace.destroy();
        }
        if (mBack != null) {
            mBack.destroy();
        }
        mFace = null;
        mBack = null;
        mJSCard.release();
    }

    /**
     * Called periodically before drawing, we update the card bounds to describe where it should
     * be drawn on the surface.
     */
    @Override
    public void onUpdate() {
        synchronized (mJSModel) {
            mJSModel.acquireLock();
            int order = mJSCard.getInteger("z");

            if (!mJSCard.contains("alive") || mJSCard.getBoolean("alive")) {
                int x = mJSCard.getInteger("x");
                int y = mJSCard.getInteger("y");

                // each card can add a unique position offset
                x += mCardWidth * mJSCard.getDouble("offsetX");
                y += mCardHeight * mJSCard.getDouble("offsetY");

                mVisible = mJSCard.getBoolean("visible");
                mOrder = order;
                mBounds = new Rect(x, y, x + mCardWidth, y + mCardHeight);
            } else {
                // unregister cards that have been 'killed'
                if (mComponentManager.getComponents().contains(this)) {
                    mComponentManager.unregisterComponent(this);
                }
            }

            mJSModel.releaseLock();
        }
    }

    /**
     * Called whenever the shape of the GameSurfaceView changes. We use this information to update
     * the size of each card.
     * 
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChange(int width, int height) {
        synchronized (mJSModel) {
            mJSModel.acquireLock();
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            mRows = mJSModel.getRows();
            mCols = mJSModel.getColumns();

            // optimize card size for the screen dimensions

            double maxWidth = mSurfaceWidth / (double) mCols;
            double maxHeight = mSurfaceHeight / (double) mRows;
            double testHeight = maxWidth * Card.HEIGHT_WIDTH_RATIO;
            double scaleFactor = Math.min(maxHeight / testHeight, 1.0);

            mCardWidth = (int) (maxWidth * scaleFactor);
            mCardHeight = (int) (mCardWidth * Card.HEIGHT_WIDTH_RATIO);

            mJSModel.releaseLock();
        }

        // redraw textures
        if (mFace != null) {
            mFace.destroy();
        }
        if (mBack != null) {
            mBack.destroy();
        }
        mFace = null;
        mBack = null;
    }

    /**
     * This determines the order in which components are drawn. Cards are given a z value based on
     * their position in a pile.
     * 
     * @return
     */
    @Override
    public int getOrder() {
        return mOrder;
    }

    /**
     * Called by the component manager to determine which components are the target of a touch
     * event. Checks if the point is within the card bounds.
     * 
     * @param p
     * @return
     */
    @Override
    public boolean collidesWithPoint(Point p) {
        return getBounds().contains((int) p.x, (int) p.y);
    }

    /**
     * Get the resource id corresponding to the drawable to use for this card.
     *
     * @return
     */
    private String getResId() {
        String resId = "";

        if (mSuit.equals("CLUBS")) {
            resId += "c";
        } else if (mSuit.equals("DIAMONDS")) {
            resId += "d";
        } else if (mSuit.equals("HEARTS")) {
            resId += "h";
        } else if (mSuit.equals("SPADES")) {
            resId += "s";
        }

        if (mRank.equals("ACE")) {
            resId += "a";
        } else if (mRank.equals("TWO")) {
            resId += "2";
        } else if (mRank.equals("THREE")) {
            resId += "3";
        } else if (mRank.equals("FOUR")) {
            resId += "4";
        } else if (mRank.equals("FIVE")) {
            resId += "5";
        } else if (mRank.equals("SIX")) {
            resId += "6";
        } else if (mRank.equals("SEVEN")) {
            resId += "7";
        } else if (mRank.equals("EIGHT")) {
            resId += "8";
        } else if (mRank.equals("NINE")) {
            resId += "9";
        } else if (mRank.equals("TEN")) {
            resId += "10";
        } else if (mRank.equals("JACK")) {
            resId += "j";
        } else if (mRank.equals("QUEEN")) {
            resId += "q";
        } else if (mRank.equals("KING")) {
            resId += "k";
        }

        return resId;
    }

    @Override
    public String toString() {
        return getResId();
    }
}
