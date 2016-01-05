package im.bunch.patience.view;

import im.bunch.patience.game.ComponentManager;
import im.bunch.patience.model.JavascriptModel;

/**
 * A Game manages Java view objects that represent objects from the JavaScript model.
 *
 * @author Creston Bunch
 */
public class Game {

    private JavascriptModel mJSModel;
    private ComponentManager mComponentManager;

    /**
     * Construct a JavaView with a backing JavaScript model and component manager.
     *
     * @param jsModel The javascript model to communicate with.
     * @param componentManager The component manager to track view objects with.
     */
    public Game(JavascriptModel jsModel, ComponentManager componentManager) {
        this.mJSModel = jsModel;
        this.mComponentManager = componentManager;
    }
}
