//#!GameRule calculation

var properties = {
    name: "Calculation",
    description: "A challenging patience game involving skill.",
    version: 1,
    rules:
    "<b>Foundations:</b> Each foundation is built by rank as follows:<br>"
  + "<br>"
  + "<i>Ones</i>: A, 2, 3, 4, 5, 6, 7, 8, 9, 10, J, Q, K<br>"
  + "<i>Twos</i>: 2, 4, 6, 8, 10, Q, A, 3, 5, 7, 9, J, K<br>"
  + "<i>Threes</i>: 3, 6, 9, Q, 2, 5, 8, J, A, 4, 7, 10, K<br>"
  + "<i>Fours</i>: 4, 8, Q, 3, 7, J, 2, 6, 10, A, 5, 9, K<br>"
  + "<br>"
  + "Suit does not matter.<br><br>"
  + "<b>Tableaux:</b> Any cards can go anywhere in the tableaux, but once placed they cannot be "
  + "moved except by placing the top card in the foundation.<br><br>"
  + "<b>Stock:</b> One card may be drawn at a time and must be placed immediately."
};

var ONES_PATH = ['ACE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN',
                'EIGHT', 'NINE', 'TEN', 'JACK', 'QUEEN', 'KING'];
var TWOS_PATH = ['TWO', 'FOUR', 'SIX', 'EIGHT', 'TEN', 'QUEEN', 'ACE', 'THREE', 'FIVE', 'SEVEN',
                'NINE', 'JACK', 'KING'];
var THREES_PATH = ['THREE', 'SIX', 'NINE', 'QUEEN', 'TWO', 'FIVE', 'EIGHT', 'JACK', 'ACE', 'FOUR',
                  'SEVEN', 'TEN', 'KING'];
var FOURS_PATH = ['FOUR', 'EIGHT', 'QUEEN', 'THREE', 'SEVEN', 'JACK', 'TWO', 'SIX', 'TEN', 'ACE',
                 'FIVE', 'NINE', 'KING'];

/**
 * Initialize a basic calculation game.
 */
function init() {

    // TODO: provide seed to deck
    var deck = new Deck();
    deck.shuffle();

    var game = {
        name: properties.name,
        board: {cols:5, rows: 4},
        score: 0,
        won: false,
        lost: false,
        options: {},
        undo: false,
        piles: {
            tableaux1: {
                name: 'tableaux',
                col: 0, row: 1,
                layout: LAYOUTS.FANNED,
                cards: [],
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },
            tableaux2: {
                name: 'tableaux',
                col: 1, row: 1,
                layout: LAYOUTS.FANNED,
                cards: [],
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },
            tableaux3: {
                name: 'tableaux',
                col: 2, row: 1,
                layout: LAYOUTS.FANNED,
                cards: [],
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },
            tableaux4: {
                name: 'tableaux',
                col: 3, row: 1,
                layout: LAYOUTS.FANNED,
                cards: [],
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },
            ones: {
                name: 'ones',
                col: 0, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: deck.find('ACE'),
                tap: noTap,
                split: noSplit,
                merge: onesMerge
            },
            twos: {
                name: 'twos',
                col: 1, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: deck.find('TWO'),
                tap: noTap,
                split: noSplit,
                merge: twosMerge
            },
            threes: {
                name: 'threes',
                col: 2, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: deck.find('THREE'),
                tap: noTap,
                split: noSplit,
                merge: threesMerge
            },
            fours: {
                name: 'fours',
                col: 3, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: deck.find('FOUR'),
                tap: noTap,
                split: noSplit,
                merge: foursMerge
            },
            stock: {
                name: 'stock',
                col: 4, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: deck.draw(48),
                tap: stockTap,
                split: stockSplit,
                merge: noMerge
            }
        }
    };

    // flip the top card in each foundation
    flip(top(game.piles.ones))
    flip(top(game.piles.twos));
    flip(top(game.piles.threes));
    flip(top(game.piles.fours));

    return game;
};


/**
 * Called whenever options get updated.
 */
function updateOptions(game) {
    // no options for calculation
}

/**
 * Change the layout based on screen dimensions.
 */
function resize(game, width, height) {

    if (height > width) {
        // portrait
        game.board.rows = 4;
        game.board.cols = 5;
        game.piles.tableaux1.row = 1;
        game.piles.tableaux1.col = 0;
        game.piles.tableaux1.layout = LAYOUTS.FANNED;
        game.piles.tableaux2.row = 1;
        game.piles.tableaux2.col = 1;
        game.piles.tableaux2.layout = LAYOUTS.FANNED;
        game.piles.tableaux3.row = 1;
        game.piles.tableaux3.col = 2;
        game.piles.tableaux3.layout = LAYOUTS.FANNED;
        game.piles.tableaux4.row = 1;
        game.piles.tableaux4.col = 3;
        game.piles.tableaux4.layout = LAYOUTS.FANNED;
        game.piles.ones.row = 0;
        game.piles.ones.col = 0;
        game.piles.twos.row = 0;
        game.piles.twos.col = 1;
        game.piles.threes.row = 0;
        game.piles.threes.col = 2;
        game.piles.fours.row = 0;
        game.piles.fours.col = 3;
        game.piles.stock.row = 0;
        game.piles.stock.col = 4;
    } else {
        // landscape
        game.board.rows = 5;
        game.board.cols = 7;
        game.piles.tableaux1.row = 0;
        game.piles.tableaux1.col = 1.5;
        game.piles.tableaux1.layout = LAYOUTS.FANNED_RIGHT;
        game.piles.tableaux2.row = 1;
        game.piles.tableaux2.col = 1.5;
        game.piles.tableaux2.layout = LAYOUTS.FANNED_RIGHT;
        game.piles.tableaux3.row = 2;
        game.piles.tableaux3.col = 1.5;
        game.piles.tableaux3.layout = LAYOUTS.FANNED_RIGHT;
        game.piles.tableaux4.row = 3;
        game.piles.tableaux4.col = 1.5;
        game.piles.tableaux4.layout = LAYOUTS.FANNED_RIGHT;
        game.piles.ones.row = 0;
        game.piles.ones.col = 0;
        game.piles.twos.row = 1;
        game.piles.twos.col = 0;
        game.piles.threes.row = 2;
        game.piles.threes.col = 0;
        game.piles.fours.row = 3;
        game.piles.fours.col = 0;
        game.piles.stock.row = 4;
        game.piles.stock.col = 0;
    }
}

function stockTap(game) {
    flip(top(game.piles.stock));
    updateStatus(game);
}

function stockSplit(game, pile, target) {
    var index = pile.cards.indexOf(target);
    // can only split at the top card in the stock and it must be visible
    if (index > -1) {
        if (index == pile.cards.length-1 && top(pile).visible) {
            return pile.cards.splice(index, pile.cards.length);
        }
    }
}

function tableauxSplit(game, pile, card) {
    var stock = game.piles.stock;

    // can only split tableaux if there is no visible card on the stock
    if (top(stock) == null || !top(stock).visible) {
        var index = pile.cards.indexOf(card);
        // only split a tableaux at the top card
        if (index == pile.cards.length-1) {
            return pile.cards.splice(index, pile.cards.length);
        }
    }
}


function tableauxMerge(game, pile, addon) {

    if (addon.cards.length == 1 && addon.name == 'stock') {
        move(addon, pile, true);
        history();
        updateStatus(game);
        save();
        return true;
    }

    return false;
}

function onesMerge(game, pile, addon) {
    var a = top(pile);
    var b = bottom(addon);

    if (addon.cards.length == 1 && ONES_PATH.indexOf(b.rank) - ONES_PATH.indexOf(a.rank) == 1) {
        move(addon, pile, true);
        score(ONES_PATH.indexOf(b.rank) + 1);
        history();
        updateStatus(game);
        save();
        return true;
    }

    return false;
}

function twosMerge(game, pile, addon) {
    var a = top(pile);
    var b = bottom(addon);

    if (addon.cards.length == 1 && TWOS_PATH.indexOf(b.rank) - TWOS_PATH.indexOf(a.rank) == 1) {
        move(addon, pile, true);
        score(TWOS_PATH.indexOf(b.rank) + 1);
        history();
        updateStatus(game);
        save();
        return true;
    }

    return false;
}

function threesMerge(game, pile, addon) {
    var a = top(pile);
    var b = bottom(addon);

    if (addon.cards.length == 1 && THREES_PATH.indexOf(b.rank) - THREES_PATH.indexOf(a.rank) == 1) {
        move(addon, pile, true);
        score(THREES_PATH.indexOf(b.rank) + 1);
        history();
        updateStatus(game);
        save();
        return true;
    }

    return false;
}

function foursMerge(game, pile, addon) {
    var a = top(pile);
    var b = bottom(addon);

    if (addon.cards.length == 1 && FOURS_PATH.indexOf(b.rank) - FOURS_PATH.indexOf(a.rank) == 1) {
        move(addon, pile, true);
        score(FOURS_PATH.indexOf(b.rank) + 1);
        history();
        updateStatus(game);
        save();
        return true;
    }

    return false;
}


/**
 * Serialize a game for storage.
 */
function serialize(game) {
    return JSON.stringify(game);
}

/**
 * Deserialize a game by reattaching callback functions.
 */
function deserialize(json) {
    var game = JSON.parse(json);
    // reattach functions
    game.piles.stock.tap = stockTap;
    game.piles.stock.split = stockSplit;
    game.piles.stock.merge = noMerge;
    game.piles.stock.layout = LAYOUTS.SQUARED;

    for (var key in game.piles) {
        var pile = game.piles[key];

        if (pile.name == 'tableaux') {
            pile.merge = tableauxMerge;
            pile.split = tableauxSplit;
            pile.tap = noTap;
            pile.layout = LAYOUTS.FANNED;
        } else if (pile.name == 'ones') {
            pile.tap = noTap;
            pile.merge = onesMerge;
            pile.split = noSplit;
            pile.layout = LAYOUTS.SQUARED;
        } else if (pile.name == 'twos') {
            pile.tap = noTap;
            pile.merge = twosMerge;
            pile.split = noSplit;
            pile.layout = LAYOUTS.SQUARED;
        } else if (pile.name == 'threes') {
            pile.tap = noTap;
            pile.merge = threesMerge;
            pile.split = noSplit;
            pile.layout = LAYOUTS.SQUARED;
        } else if (pile.name == 'fours') {
            pile.tap = noTap;
            pile.merge = foursMerge;
            pile.split = noSplit;
            pile.layout = LAYOUTS.SQUARED;
        }
    }

    return game;
}

/**
 * Check the win condition of the game.
 */
function updateStatus(game) {

    var ones = game.piles.ones;
    var twos = game.piles.twos;
    var threes = game.piles.threes;
    var fours = game.piles.fours;
    var stock = game.piles.stock;
    var tableaux = [game.piles.tableaux1, game.piles.tableaux2,
                    game.piles.tableaux3, game.piles.tableaux4];

    if (game.piles.ones.cards.length == 13 &&
        game.piles.twos.cards.length == 13 &&
        game.piles.threes.cards.length == 13 &&
        game.piles.fours.cards.length == 13 &&
        !game.won) {

        game.won = true;
        win();

    } else if (empty(stock)) {
        // check for moves left
        var lost = true;

        // check tableaux
        for (var i in tableaux) {
            var t = tableaux[i];

            var a;
            var b = top(t);

            a = top(ones);
            if (a != null && b != null &&
            ONES_PATH.indexOf(b.rank) - ONES_PATH.indexOf(a.rank) == 1) {
                lost = false;
            }

            a = top(twos);
            if (a != null && b != null &&
            TWOS_PATH.indexOf(b.rank) - TWOS_PATH.indexOf(a.rank) == 1) {
                lost = false;
            }

            a = top(threes);
            if (a != null && b != null &&
            THREES_PATH.indexOf(b.rank) - THREES_PATH.indexOf(a.rank) == 1) {
                lost = false;
            }

            a = top(fours);
            if (a != null && b != null &&
            FOURS_PATH.indexOf(b.rank) - FOURS_PATH.indexOf(a.rank) == 1) {
                lost = false;
            }
        }

        if (lost && !game.lost) {
            game.lost = lost;
            lose();
        }
    }
}

/**
 * Called when a game is won, provides a bonus score.
 */
function bonus(time) {
    var secs = time / 1000;

    return Math.round(70000 / secs);
}