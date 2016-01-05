package im.bunch.patience.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import im.bunch.patience.R;
import im.bunch.patience.game.Component;
import im.bunch.patience.game.ComponentManager;
import im.bunch.patience.game.RectangleComponent;
import im.bunch.patience.game.graphics.Rectangle;
import im.bunch.patience.geometry.Point;
import im.bunch.patience.model.JavascriptModel;

/**
 * This is a Java pile view that is backed by a Javascript pile model.
 *
 * @author Creston
 */
public class Pile implements RectangleComponent {

    private final JavascriptModel mJSModel;
    private final V8Object mJSPile;
    private ComponentManager mComponentManager;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mCardWidth;
    private int mCardHeight;
    private int mRows;
    private int mCols;
    private int mPosX;
    private int mPosY;
    private int mNumCards;
    private int mBack;
    private Rect mBounds;
    private Pile mParent;
    private boolean mDragging;
    private Point mAnchor;
    private boolean mDraw;

    private Rectangle mRectangle;
    public static final String LOG_TAG = "Pile";
    public static final int CARD_MARGIN = 5; //px
    public static final int PILE_ORDER = -1;

    /**
     * Construct the pile from a given Javascript pile.
     *
     * @param jsModel The JavaScript model that created this pile.
     * @param jsPile The JavaScript pile model.
     */
    public Pile(JavascriptModel jsModel, V8Object jsPile) {
        mJSModel = jsModel;
        mJSPile = jsPile;
    }

    /**
     * Construct the pile from a given Javascript pile with dragging enabled.
     *
     * @param jsModel The JavaScript model that created this pile.
     * @param jsPile The JavaScript pile model.
     * @param parent The pile this one is being dragged from.
     * @param dragging Make this pile draggable.
     * @param anchor The grabbed (x,y) position relative to the pile's (x,y) position.
     */
    public Pile(
            JavascriptModel jsModel, V8Object jsPile,
            Pile parent, boolean dragging, Point pos, Point anchor,
            int surfaceWidth, int surfaceHeight, int cardWidth, int cardHeight
    ) {
        mJSModel = jsModel;
        mJSPile = jsPile;
        mParent = parent;
        mDragging = dragging;
        mPosX = (int) pos.x;
        mPosY = (int) pos.y;
        mAnchor = anchor;
        mSurfaceWidth = surfaceWidth;
        mSurfaceHeight = surfaceHeight;
        mCardWidth = cardWidth;
        mCardHeight = cardHeight;
    }

    /**
     * Get the JavaScript model associated with this pile.
     */
    public V8Object getJSPile() {
        return mJSPile;
    }

    /**
     * Get the parent pile that spawned this pile when it was split.
     */
    public Pile getParent() {
        return mParent;
    }

    /**
     * Calculate the intersection with another pile.
     */
    public double intersection(Pile other) {
        Rect intersection = new Rect(this.getBounds());
        boolean i = intersection.intersect(other.getBounds());
        if (i) {
            return intersection.width() * intersection.height();
        } else {
            return 0.0;
        }
    }

    /**
     * The rectangle is an object used by the GameRenderer to render this pile in GLES. The pile
     * only returns a rectangle if it is empty, in which case the rectangle is the empty pile
     * sprite.
     *
     * @return
     */
    @Override
    public Rectangle getRectangle() {
        // can set the draw flag to false to disable drawing a pile placeholder
        if (!mDraw) { return null; }
        if (mRectangle == null) {
            int res = mBack; // populated in onSurfaceChanged

            Context context = mComponentManager.getContext();
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(
                    context.getResources(), res, options
            );

            Rectangle rectangle = new Rectangle(bitmap);
            mRectangle = rectangle;

            return rectangle;
        } else {
            return mRectangle;
        }
    }

    /**
     * Clear the rectangle map to force it to be recreated next draw cycle.
     */
    @Override
    public void clearRectangle() {
        if (mRectangle != null) {
            mRectangle.destroy();
            mRectangle = null;
        }
    }

    /**
     * Calculate the x position of this pile based on its column and card width.
     */
    public int getX() {
        return mPosX;
    }

    /**
     * Calculate the y position of this pile based on its row and card height.
     */
    public int getY() {
        return mPosY;
    }

    /**
     * Get a rectangle object that describes where this pile is on the GameSurface. The bounds of
     * this rectangle changes depending on the layout and number of cards.
     *
     * @return
     */
    @Override
    public Rect getBounds() {
        // calculate margins
        return new Rect(
                mBounds.left + CARD_MARGIN,
                mBounds.top + CARD_MARGIN,
                mBounds.right - CARD_MARGIN,
                mBounds.bottom - CARD_MARGIN
        );
    }

    /**
     * When dragging over a pile, we split the pile at the card being dragged. A new pile gets
     * registered with the component manager and is flagged for dragging.
     *
     * @param targets
     * @param p
     */
    @Override
    public void onDragStart(List<Component> targets, Point p) {
        synchronized (mJSModel) {
            mJSModel.acquireLock();

            if (targets.contains(this)) {
                Log.i(LOG_TAG, "Targeting a pile.");

                V8Object target = null;

                for (Component c : targets) {
                    if (c instanceof Card) {
                        V8Object card = ((Card) c).getJSCard();

                        if (target == null || card.getInteger("z") > target.getInteger("z")) {
                            target = card;
                        }
                    }
                }

                // found a target card, ask JavaScript if we can split this pile at the given card
                if (target != null) {

                    Log.i(LOG_TAG, "Found a card.");
                    V8Array parameters = new V8Array(mJSPile.getRuntime());
                    parameters.push(mJSModel.getGame()).push(mJSPile).push(target);
                    // JavaScript will split the pile and return the part we can drag, or it will
                    // return an empty array if there is nothing to drag.
                    V8Array split = mJSPile.executeArrayFunction("split", parameters);
                    // only drag if there is something to drag
                    if (!split.isUndefined() && split.length() > 0) {
                        Point anchor = new Point(p.x - target.getInteger("x"),
                                p.y - target.getInteger("y"));

                        V8Object newPile = new V8Object(mJSPile.getRuntime());
                        newPile.add("name", mJSPile.getString("name"));
                        newPile.add("row", mJSPile.getDouble("row"));
                        newPile.add("col", mJSPile.getDouble("col"));
                        newPile.add("layout", mJSPile.getObject("layout"));
                        newPile.add("cards", split);
                        // disallow splitting and tapping of this pile
                        newPile.add("split", mJSPile.getRuntime().getObject("noSplit"));
                        newPile.add("tap", mJSPile.getRuntime().getObject("noTap"));
                        newPile.add("merge", mJSPile.getRuntime().getObject("noMerge"));
                        // keep track of the source pile
                        newPile.add("source", mJSPile);

                        // create a new draggable pile
                        Point pos = new Point(target.getInteger("x"), target.getInteger("y"));
                        Pile newComponent = new Pile(
                                mJSModel, newPile, this, true, pos,
                                anchor, mSurfaceWidth, mSurfaceHeight,
                                mCardWidth, mCardHeight
                        );
                        // temporarily update the bounds so we don't see a stretched-out
                        // empty pile underneath everything
                        mBounds = new Rect(
                                getX(), getY(), getX() + mCardWidth, getY() + mCardHeight
                        );
                        mComponentManager.registerComponent(newComponent);
                    }
                }
            }
            mJSModel.releaseLock();
        }
    }

    /**
     * If this pile is being dragged, move its position (row and column) to wherever the pointer
     * is dragging it to.
     *
     * @param targets
     * @param p
     */
    @Override
    public void onDrag(List<Component> targets, Point p) {
        if (mDragging) {
            mPosX = (int) (p.x - mAnchor.x);
            mPosY = (int) (p.y - mAnchor.y);
        }
    }

    /**
     * Dragging is over, try to merge it with a target pile or otherwise revert back to where it
     * used to be.
     *
     * @param targets
     * @param p
     */
    @Override
    public void onDragEnd(List<Component> targets, Point p) {

        synchronized (mJSModel) {
            mJSModel.acquireLock();

            Pile target = null;
            // find the pile in the list of targets with the maximum overlap
            for (Component c : new ArrayList<>(mComponentManager.getComponents())) {
                if (c != this && c instanceof Pile) {
                    Pile d = (Pile) c;
                    if (d.intersection(this) > 0) {
                        if (target == null || d.intersection(this) > target.intersection(this)) {
                            target = d;
                        }
                    }
                }
            }
            if (mDragging) {

                boolean merged = false;
                if (target != null) {
                    V8Object targetJsPile = target.getJSPile();
                    V8Array parameters = new V8Array(mJSPile.getRuntime());
                    parameters.push(mJSModel.getGame()).push(target.getJSPile()).push(mJSPile);

                    // JavaScript will merge the piles if possible and tell us whether it was successful
                    // or not.
                    if (!targetJsPile.isUndefined() && targetJsPile.contains("merge")) {
                        merged = targetJsPile.executeBooleanFunction("merge", parameters);
                    }
                    parameters.release();
                    Log.i(LOG_TAG, "Merged attempted.");
                } else {
                    Log.i(LOG_TAG, "Target is null.");
                }

                // couldn't merge, so let's put the cards back where they came from
                if (!merged) {
                    Log.i(LOG_TAG, "Couldn't merge.");
                    resetPile();
                } else  {
                    Log.i(LOG_TAG, "Merged!");
                }

                mComponentManager.unregisterComponent(this);
                mDragging = false;
            }

            mJSModel.releaseLock();
        }
    }

    /**
     * Called when the game surface is tapped. Checks if this pile is a target and calls the
     * appropriate JavaScript model function.
     *
     * @param targets
     * @param p
     */
    @Override
    public void onTap(List<Component> targets, Point p) {
        if (targets.contains(this)) {
            synchronized (mJSModel) {
                mJSModel.acquireLock();
                V8Array parameters = new V8Array(mJSPile.getRuntime());
                parameters.push(mJSModel.getGame());
                mJSPile.executeVoidFunction("tap", parameters);
                parameters.release();
                mJSModel.releaseLock();
            }
        }
    }

    /**
     * Resets the pile in case the drag gets canceled to prevent erroneous errors.
     */
    @Override
    public boolean onCancelDrag() {
        if (mDragging) {
            synchronized (mJSModel) {
                mJSModel.acquireLock();
                resetPile();
                mDragging = false;
                mComponentManager.unregisterComponent(this);
                mJSModel.releaseLock();
                return true;
            }
        }
        return false;
    }

    /**
     * Called when a ComponentManager registers this pile. We will track the component manager so
     * we can get access to other components and global variables in this pile. Should be done
     * every time a new pile is created (or it won't get shown by the GameRenderer).
     *
     * Note: you probably shouldn't try to register a pile with more than one ComponentManager.
     *
     * @param componentManager
     */
    @Override
    public void onRegister(ComponentManager componentManager) {
        mComponentManager = componentManager;
    }

    /**
     * Called when this pile is unregistered from a ComponentManager. Happens when a pile is merged
     * into another pile and should no longer be tracked by the component manager.
     *
     * @param componentManager
     */
    @Override
    public void onUnregister(ComponentManager componentManager) {
        if (mRectangle != null) {
            mRectangle.destroy();
        }
        mRectangle = null;
        mJSPile.release();
    }

    /**
     * Perform logic business before each draw cycle. For each pile we update the position of cards
     * in the pile based on the pile layout and how much space is available to draw the pile. We
     * also update the bounding rect of this pile based on the position of cards.
     */
    @Override
    public void onUpdate() {
        // if we're dragging a component, don't update any other components
        for (Component c : new ArrayList<>(mComponentManager.getComponents())) {
            if (c instanceof Pile) {
                Pile p = (Pile) c;
                if (p.isDragging() && p != this) {
                    return;
                }
            }
        }

        // create a rect object for JavaScript. This is how much space the pile has to draw.
        synchronized (mJSModel) {
            mJSModel.acquireLock();

            // sometimes piles get released in the middle of an update cycle before the update
            // cycle gets a chance to update them, so in case that happens we'll do a quick check
            // to prevent any errors
            if (!mJSPile.isReleased()) {
                updateBounds();
            }

            if (mJSPile.contains("draw") && !mJSPile.getBoolean("draw")) {
                mDraw = false;
            } else {
                mDraw = true;
            }

            // get the card back to draw for empty piles
            if (mJSPile.contains("back")) {
                if (mJSPile.getString("back").equals("ace")) {
                    mBack = R.drawable.ace_blank;
                } else if (mJSPile.getString("back").equals("king")) {
                    mBack = R.drawable.king_blank;
                }
            } else {
                mBack = R.drawable.card_blank;
            }

            mJSModel.releaseLock();
        }

    }

    /**
     * Called whenever the surface changes shape, we keep track of the surface width and height
     * so we know what size to draw each pile.
     *
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChange(int width, int height) {
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

        synchronized (mJSModel) {
            mJSModel.acquireLock();
            if (!mDragging) {
                mPosX = (int) (mJSPile.getDouble("col") * mCardWidth);
                mPosY = (int) (mJSPile.getDouble("row") * mCardHeight);
            }

            mJSModel.releaseLock();
        }



        // redraw textures
        if (mRectangle != null) {
            mRectangle.destroy();
        }
        mRectangle = null;
    }

    /**
     * The order that the pile gets updated / drawn in the list of components. We want piles
     * to update before the cards they contain, so we return a negative number (lower than any
     * card).
     * @return
     */
    @Override
    public int getOrder() {
        return PILE_ORDER;
    }

    /**
     * Used to find targets for touch events in the component manager. We return true if the point
     * is inside the pile rectangle.
     *
     * @param p
     * @return
     */
    @Override
    public boolean collidesWithPoint(Point p) {
        Rect bounds = getBounds();
        if (bounds == null) {
            return false;
        }
        return getBounds().contains((int) p.x, (int) p.y);
    }

    /**
     * Return true if we're dragging this pile.
     */
    public boolean isDragging() {
        return mDragging;
    }

    /**
     * Set the dragging state
     */
    public void setDragging(boolean b) {
        mDragging = b;
    }

    /**
     * Return a pile to its parent.
     */
    public void resetPile() {
        if (mParent != null) {
            V8Object parentJsPile = mParent.getJSPile();
            V8Array parentJsCards = parentJsPile.getArray("cards");
            for (String key : mJSPile.getArray("cards").getKeys()) {
                parentJsCards.push(mJSPile.getArray("cards").getObject(key));
            }
        } else {
            Log.e(LOG_TAG, "Oh god this should never happen.");
        }
    }


    private void updateBounds() {
        int x = getX();
        int y = getY();
        int maxX = x;
        int maxY = y;

        V8Object rect = new V8Object(mJSPile.getRuntime());
        rect.add("left", x);
        rect.add("top", y);
        rect.add("right", mSurfaceWidth - x);
        rect.add("bottom", mSurfaceHeight - y);

        V8Array cards = mJSPile.getArray("cards");

        int num = cards.length();
        // Ask JavaScript to calculate the position of each card for us
        V8Array params = new V8Array(mJSPile.getRuntime());
        params.push(rect).push(mCardWidth).push(mCardHeight).push(mJSPile);
        V8Array positions = mJSPile.executeArrayFunction("layout", params);
        params.release();

        mNumCards = cards.length();

        // update card positions from list
        for (String index : cards.getKeys()) {
            V8Object card = cards.getObject(index);
            V8Object pos = positions.getObject(index);

            // skip 'dead' cards
            if (card.contains("alive") && !card.getBoolean("alive")) {
                continue;
            }

            x = pos.getInteger("x");
            y = pos.getInteger("y");
            card.add("x", x);
            card.add("y", y);
            if (mDragging) {
                // draw cards being dragged above everything else
                card.add("z", 1000 + Integer.parseInt(index));
            } else {
                card.add("z", Integer.parseInt(index));
            }
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        mBounds = new Rect(getX(), getY(), maxX + mCardWidth, maxY + mCardHeight);
    }
}
