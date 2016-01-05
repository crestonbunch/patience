//#!GameRule pyramid

var properties = {
    "name": "Pyramid",
    "description": "Clear the pyramid.",
    version: 1,
    "rules":
    "<b>Pyramid:</b> Clear the pyramid by selecting pairs of cards that sum to 13.<br><br>"
  + "<b>Stock:</b> Cards may be drawn from the stock and placed in the waste.<br><br>"
  + "<b>Waste:</b> The top card in the waste may be selected."
};

var PARENTS = {
    '00':['10','11'], '10':['20','21'], '11':['21','22'], '20':['30','31'], '21':['31','32'],
    '22':['32','33'], '30':['40','41'], '31':['41','42'], '32':['42','43'], '33':['43','44'],
    '40':['50','51'], '41':['51','52'], '42':['52','53'], '43':['53','54'], '44':['54','55'],
    '50':['60','61'], '51':['61','62'], '52':['62','63'], '53':['63','64'], '54':['64','65'],
    '55':['65','66'], '60':[], '61':[], '62':[], '63':[], '64':[], '65':[], '66':[]
}

var RANK_MAP = {
    'ACE':1,'TWO':2,'THREE':3,'FOUR':4,'FIVE':5,'SIX':6,'SEVEN':7,'EIGHT':8,'NINE':9,'TEN':10,
    'JACK':11,'QUEEN':12,'KING':13
}

var SELECTION_OFFSET = 0.3;

/**
 * Initialize a basic klondike game.
 */
function init() {
    // TODO: provide seed to deck
    var deck = new Deck();
    deck.shuffle();

    var game = {
        name: properties.name,
        board: {cols: 7, rows: 6},
        score: 0,
        won: false,
        lost: false,
        undo: false,
        options: {},
        piles: {
            'pyramid': {
                name: 'pyramid',
                col: 0, row: 0,
                layout: LAYOUTS.PYRAMID,
                cards: deck.draw(28),
                tap: noTap,
                split: noSplit,
                merge: noMerge,
                draw: false // don't show the rectangle background
            },

            'stock': {
                name: 'stock',
                col: 0, row: 4.5,
                layout: LAYOUTS.SQUARED,
                cards: deck.draw(24),
                tap: stockTap,
                split: noSplit,
                merge: noMerge
            },

            'temp': {
                name: 'temp',
                col: 1, row: 4.5,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: noSplit,
                merge: noMerge
            },

            'waste': {
                name: 'waste',
                col: 6, row: 4.5,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: noSplit,
                merge: noMerge
            }
        }
    };

    for (var i in game.piles.pyramid.cards) {
        flip(game.piles.pyramid.cards[i]);
    }

    return game;
}

/**
 * Calculate the row of the pyramid from an index.
 *
 * TODO: can we do this for arbitray pyramids with a math function?
 */
function pyramidRow(index) {
    if (index == 0) { return 0; }
    else if (index >= 1 && index <= 2) { return 1; }
    else if (index >= 3 && index <= 5) { return 2; }
    else if (index >= 6 && index <= 9) { return 3; }
    else if (index >= 10 && index <= 14) { return 4; }
    else if (index >= 15 && index <= 20) { return 5; }
    else if (index >= 21 && index <= 27) { return 6; }
    else { return -1; }
}

/**
 * Count the living cards in each row of the pyramid.
 */
function countRows(pyramid) {
    var result = [0,0,0,0,0,0,0];

    for (var i = 0; i < pyramid.cards.length; i++) {
        var card = pyramid.cards[i];
        if (card.alive) {
            result[pyramidRow(i)]++;
        }
    }

    return result;
}


/**
 * Change the layout based on screen dimensions.
 */
function resize(game, width, height) {

    if (height > width) {
        // portrait
        game.board.rows = 6;
        game.board.cols = 7;
        game.piles.pyramid.col = 0;
        game.piles.pyramid.row = 0;
        game.piles.stock.col = 0;
        game.piles.stock.row = 4.5;
        game.piles.temp.col = 1;
        game.piles.temp.row = 4.5;
        game.piles.waste.col = 6;
        game.piles.waste.row = 4.5;
    } else {
        // landscape
        game.board.rows = 4;
        game.board.cols = 10;
        game.piles.pyramid.col = 2;
        game.piles.pyramid.row = 0;
        game.piles.stock.col = 0;
        game.piles.stock.row = 0;
        game.piles.temp.col = 1;
        game.piles.temp.row = 0;
        game.piles.waste.col = 9;
        game.piles.waste.row = 0;
    }
}

/**
 * Called whenever a card gets tapped.
 */
function tapCard(game, target) {

    var pyramid = game.piles.pyramid;
    var stock = game.piles.stock;
    var waste = game.piles.waste;
    var temp = game.piles.temp;

    // this card was just moved from the stock, don't select it
    if (target.moved !== undefined && target.moved) {
        target.moved = false;
        return;
    }

    if (target.selected) {
        // reset selected cards
        target.selected = false;
        target.offsetY = 0;
    } else {
        // select a new card

        // select cards in the pyramid
        var pyramidIndex = pyramid.cards.indexOf(target);

        if (pyramidIndex > -1) {
            var row = pyramidRow(pyramidIndex);
            var left = pyramid.cards[pyramidIndex + row + 1];
            var right = pyramid.cards[pyramidIndex + row + 2];
            // check if children cards have been removed
            if (row == 6 || ((left == undefined || !left.alive)
                         && (right == undefined || !right.alive))
            ) {
                target.selected = true;
                target.offsetY = SELECTION_OFFSET;
            }
        }
        // can only select the top card
        if (target != null && target == top(waste)) {
            target.selected = true;
            target.offsetY = SELECTION_OFFSET;
        }

        // can only select the top card (should be the only card, actually)
        if (target != null && target == top(temp)) {
            target.selected = true;
            target.offsetY = SELECTION_OFFSET;
        }

        // check for selected cards
        var selected = [];
        var cards = pyramid.cards.concat(waste.cards, temp.cards);
        for (var i in cards) {
            var c = cards[i];
            if (c.selected) {
                selected.push(c);
            }
        }

        var before = countRows(pyramid);

        // check if two cards add to 13
        if (selected.length == 2) {
            var sum = RANK_MAP[selected[0].rank] + RANK_MAP[selected[1].rank];
                selected[0].selected = false;
                selected[1].selected = false;
                selected[0].offsetY = 0;
                selected[1].offsetY = 0;
            if (sum == 13) {
                selected[0].alive = false;
                selected[1].alive = false;
                score(10); // matching two cards scores 10 points
                history();
                updateStatus(game);
                save();
            }
        // check if we selected a king
        } else if (selected.length == 1) {
            var sum = RANK_MAP[selected[0].rank];
            if (sum == 13) {
                selected[0].selected = false;
                selected[0].alive = false;
                selected[0].offsetY = 0;
                score(10); // selecting a king scores 10 points
                history();
                updateStatus(game);
                save();
            }
        }

        var after = countRows(pyramid);
        for (var i in before) {
            // cleared a row, score based on the row cleared
            if (after[i] == 0 && before[i] > 0) {
                score((7-i) * 50);
            }
        }
    }
}

/**
 * When you tap the stock, move the top card faceup to the temp slot
 */
function stockTap(game) {
    var stock = game.piles.stock;
    var temp = game.piles.temp;
    var waste = game.piles.waste;

    if (size(stock) > 0) {
        // move cards from temp to waste
        move(temp, waste, true);
        // move top card from stock to temp
        var card = pop(stock);
        temp.cards.push(card);
        card.moved = true; // don't automatically select this card when onTap() gets called
        flip(card);
    } else {
        // recycle waste
        move(temp, waste, true);
        move(waste, stock, false);
        // recycling the waste scores -100
        score(-100);
    }

    history();
    updateStatus(game);
    save();
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

    for (var key in game.piles) {
        var pile = game.piles[key];

        if (pile.name == 'stock') {
            pile.tap = stockTap;
            pile.merge = noMerge;
            pile.split = noSplit;
            pile.layout = LAYOUTS.SQUARED;
        } else if (pile.name == 'waste') {
            pile.tap = noTap;
            pile.split = noSplit;
            pile.merge = noMerge;
            pile.layout = LAYOUTS.SQUARED;
        } else if (pile.name == 'temp') {
            pile.tap = noTap;
            pile.split = noSplit;
            pile.merge = noMerge;
            pile.layout = LAYOUTS.SQUARED;
        } else if (pile.name == 'pyramid') {
            pile.tap = noTap;
            pile.split = noSplit;
            pile.merge = noMerge;
            pile.layout = LAYOUTS.PYRAMID;
        }
    }

    return game;
}

/**
 * Check the win condition of the game.
 */
function updateStatus(game) {

    var pyramid = game.piles.pyramid;
    var stock = game.piles.stock;
    var waste = game.piles.waste;
    var temp = game.piles.temp;

    if (size(pyramid) == 0 && !game.won) {
        // game is won
        game.won = true;
        win();
    } else {
        // check if game is lost
        var lost = true;

        // count exposed cards on the pyramid
        var exposed = [];

        for (var i = 0; i < pyramid.cards.length; i++) {
            var row = pyramidRow(i);
            // children
            var left = pyramid.cards[i + row + 1];
            var right = pyramid.cards[i + row + 2];
            // check if children cards have been removed
            if (row == 6 || ((left == undefined || !left.alive)
                         && (right == undefined || !right.alive))
            ) {
                if (pyramid.cards[i].alive) {
                    exposed.push(pyramid.cards[i]);
                }
            }
        }

        // check if any exposed cards sum to 13
        for (var i in exposed) {
            var a = exposed[i];
            for (var j in exposed) {
                var b = exposed[j];

                if (a.rank == 'KING' || b.rank == 'KING') {
                    lost = false;
                } else if (a != b) {
                    var sum = RANK_MAP[a.rank] + RANK_MAP[b.rank];

                    if (sum == 13) {
                        lost = false;
                    }
                }
            }
        }

        // check if any exposed cards + cards in the waste, stock, or temp sum to 13
        var cards = waste.cards.concat(stock.cards, temp.cards);

        for (var i in cards) {
            var a = cards[i];
            for (var j in exposed) {
                var b = exposed[j];

                var sum = RANK_MAP[a.rank] + RANK_MAP[b.rank];
                if (a.alive && b.alive && sum == 13) {
                    lost = false;
                }
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

    return Math.round(60000 / secs);
}