//#!GameRules spider

var properties = {
    name: "Spider",
    description: "Stop the rampaging horde of cards.",
    version: 1,
    rules: "<b>Tableaux:</b> The tableaux are built down by rank in with any suit."
  + " However, you can only move groups of cards of the same suite.<br><br>"
  + "<b>Stock:</b> Drawing from the stock will play another card on top of each tableaux.<br><br>"
  + "To win, build stacks of cards of the same suite from ace to king."
};


/**
 * Initialize a spider game
 */
function init() {

    // TODO: provide seed to deck

    // spider uses 2 decks
    var deck = new Deck();
    var deck2 = new Deck();
    deck.cards = deck.cards.concat(deck2.cards);
    deck.shuffle();

    var game = {
        name: properties.name,
        board: {cols: 12, rows: 4},
        score: 0,
        won: false,
        lost: false,
        undo: true,
        options: {
            instantFlip: {
                display: "Flip tableaux cards automatically",
                value: true
            }
        },
        piles: {

            tableaux1: {
                name: 'tableaux',
                col: 1.5, row: 0,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(6),
                tap: tableauxTap('tableaux1'),
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux2: {
                name: 'tableaux',
                col: 2.5, row: 0,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(6),
                tap: tableauxTap('tableaux2'),
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux3: {
                name: 'tableaux',
                col: 3.5, row: 0,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(6),
                tap: tableauxTap('tableaux3'),
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux4: {
                name: 'tableaux',
                col: 4.5, row: 0,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(6),
                tap: tableauxTap('tableaux4'),
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux5: {
                name: 'tableaux',
                col: 5.5, row: 0,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(5),
                tap: tableauxTap('tableaux5'),
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux6: {
                name: 'tableaux',
                col: 6.5, row: 0,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(5),
                tap: tableauxTap('tableaux6'),
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux7: {
                name: 'tableaux',
                col: 7.5, row: 0,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(5),
                tap: tableauxTap('tableaux7'),
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux8: {
                name: 'tableaux',
                col: 8.5, row: 0,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(5),
                tap: tableauxTap('tableaux8'),
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux9: {
                name: 'tableaux',
                col: 9.5, row: 0,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(5),
                tap: tableauxTap('tableaux9'),
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux10: {
                name: 'tableaux',
                col: 10.5, row: 0,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(5),
                tap: tableauxTap('tableaux10'),
                split: tableauxSplit,
                merge: tableauxMerge
            },

            stock: {
                name: 'stock',
                col: 0, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: deck.draw(50),
                tap: stockTap,
                split: noSplit,
                merge: noMerge
            }
        }
    };

    // flip the top card of every tableaux
    flip(top(game.piles.tableaux1));
    flip(top(game.piles.tableaux2));
    flip(top(game.piles.tableaux3));
    flip(top(game.piles.tableaux4));
    flip(top(game.piles.tableaux5));
    flip(top(game.piles.tableaux6));
    flip(top(game.piles.tableaux7));
    flip(top(game.piles.tableaux8));
    flip(top(game.piles.tableaux9));
    flip(top(game.piles.tableaux10));

    return game;
};


/**
 * Called whenever options get updated.
 */
function updateOptions(game) {
    // do nothing
}

/**
 * Change the layout based on screen dimensions.
 */
function resize(game, width, height) {

    // spider solitaire does not change layouts
    if (height > width) {
        // portrait
    } else {
        // portrait
    }
}

function tableauxTap(tableaux) {
    return function(game) {
        var target = game.piles[tableaux];
        if (top(target) !== null && top(target).visible === false) {

            flip(top(target));
            // turning over a tableaux card scores 5 points
            score(5);

            // remove sequences of kings to aces
            clearPiles(game);
        }
    }
}

function tableauxSplit(game, pile, target) {

    var index = pile.cards.indexOf(target);
    var cards = pile.cards.slice(index, pile.cards.length);
    var valid = true; // assume we can move it

    for (var i = 0; i < cards.length-1; i++) {
        var card = cards[i];
        var next = cards[i+1];

        if (!card.visible) {
            valid = false; // can't move facedown cards
        }
        // cards must be the same suit and in rank order
        if (card.suit !== next.suit || RANKS.indexOf(card.rank) - RANKS.indexOf(next.rank) !== 1) {
            valid = false;
        }
    }

    if (valid) {
        return pile.cards.splice(index, pile.cards.length);
    }
}

function tableauxMerge(game, pile, addon) {

    var merged = false;
    if (size(pile) > 0 && size(addon) > 0) {
        var a = top(pile);
        var b = bottom(addon);

        // only merge when b has 1 lower rank than a
        if (RANKS.indexOf(a.rank) - RANKS.indexOf(b.rank) === 1) {
            merged = true;
        }
    } else if (size(pile) == 0 && size(addon) > 0) {
        // if a pile is empty, you can place anything
        merged = true;
    }

    if (merged) {
        move(reverse(addon), pile, true);

        // remove sequences of kings to aces
        clearPiles(game);

        // flip over every top card
        if (game.options.instantFlip.value) {
            tableauxTap('tableaux1')(game);
            tableauxTap('tableaux2')(game);
            tableauxTap('tableaux3')(game);
            tableauxTap('tableaux4')(game);
            tableauxTap('tableaux5')(game);
            tableauxTap('tableaux6')(game);
            tableauxTap('tableaux7')(game);
            tableauxTap('tableaux8')(game);
            tableauxTap('tableaux9')(game);
            tableauxTap('tableaux10')(game);
        }

        // moving scores -1 points
        score(-1);

        history();
        updateStatus(game);
        save();
    }

    return merged;
}

function clearPiles(game) {
    // clear piles of kings -> aces if they exist

    var tableaux = [game.piles.tableaux1, game.piles.tableaux2, game.piles.tableaux3,
    game.piles.tableaux4, game.piles.tableaux5, game.piles.tableaux6, game.piles.tableaux7,
    game.piles.tableaux8, game.piles.tableaux9, game.piles.tableaux10];

    for (var i in tableaux) {
        var pile = tableaux[i];

        // remove dead cards and facedown cards from the pile
        var cards = [];
        for (var j = 0; j < pile.cards.length; j++) {
            if (pile.cards[j].alive && pile.cards[j].visible) {
                cards.push(pile.cards[j]);
            }
        }

        // pop off cards from the bottom of the list that aren't kings
        while (cards.length > 0 && cards[0].rank != 'KING') {
            cards = cards.splice(1, cards.length);
        }
        // pop off cards from the top that aren't aces
        while (cards.length > 0 && cards[cards.length-1].rank != 'ACE') {
            cards = cards.splice(0, cards.length-1);
        }
        // check if each card is one rank below the previous one and the same suit
        var valid = true;
        for (var j = 1; j < cards.length; j++) {
            var a = cards[j-1];
            var b = cards[j];

            if (a.suit != b.suit && RANKS.indexOf(a.rank) - RANKS.indexOf(b.rank) !== 1) {
                valid = false;
            }
        }
        // check if there are 13 cards in the pile
        if (valid && cards.length == 13) {
            // remove cards
            for (var j = 0; j < cards.length; j++) {
                cards[j].alive = false;
                var index = pile.cards.indexOf(cards[j]);
                pile.cards.splice(index, index + 1);
            }
            // clearing a pile scores 100 points
            score(100);
        }
    }
}

function stockTap(game) {

    var stock = game.piles.stock;

    var tableaux = [game.piles.tableaux1, game.piles.tableaux2, game.piles.tableaux3,
    game.piles.tableaux4, game.piles.tableaux5, game.piles.tableaux6, game.piles.tableaux7,
    game.piles.tableaux8, game.piles.tableaux9, game.piles.tableaux10];

    // can't draw cards if there are empty piles
    for (var i in tableaux) {
        if (empty(tableaux[i])) {
            return;
        }
    }

    if (!empty(stock)) {
        for (var i in tableaux) {
            tableaux[i].cards.push(pop(stock));
        }

        // flip over every top card
        if (game.options.instantFlip.value) {
            tableauxTap('tableaux1')(game);
            tableauxTap('tableaux2')(game);
            tableauxTap('tableaux3')(game);
            tableauxTap('tableaux4')(game);
            tableauxTap('tableaux5')(game);
            tableauxTap('tableaux6')(game);
            tableauxTap('tableaux7')(game);
            tableauxTap('tableaux8')(game);
            tableauxTap('tableaux9')(game);
            tableauxTap('tableaux10')(game);
        }

        // remove sequences of kings to aces
        clearPiles(game);

        history();
        updateStatus(game);
        save();
    }
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

    game.piles.tableaux1.tap = tableauxTap('tableaux1');
    game.piles.tableaux2.tap = tableauxTap('tableaux2');
    game.piles.tableaux3.tap = tableauxTap('tableaux3');
    game.piles.tableaux4.tap = tableauxTap('tableaux4');
    game.piles.tableaux5.tap = tableauxTap('tableaux5');
    game.piles.tableaux6.tap = tableauxTap('tableaux6');
    game.piles.tableaux7.tap = tableauxTap('tableaux7');
    game.piles.tableaux8.tap = tableauxTap('tableaux8');
    game.piles.tableaux9.tap = tableauxTap('tableaux9');
    game.piles.tableaux10.tap = tableauxTap('tableaux10');

    for (var key in game.piles) {
        var pile = game.piles[key];

        if (pile.name == 'tableaux') {
            pile.merge = tableauxMerge;
            pile.split = tableauxSplit;
            pile.layout = LAYOUTS.FANNED;
        } else if (pile.name == 'stock') {
            pile.tap = stockTap;
            pile.split = noSplit;
            pile.merge = noMerge;
            pile.layout = LAYOUTS.SQUARED;
        }
    }

    return game;
}

/**
 * Check the win condition of the game.
 */
function updateStatus(game) {

    var stock = game.piles.stock;
    var tableaux = [game.piles.tableaux1, game.piles.tableaux2, game.piles.tableaux3,
    game.piles.tableaux4, game.piles.tableaux5, game.piles.tableaux6, game.piles.tableaux7,
    game.piles.tableaux8, game.piles.tableaux9, game.piles.tableaux10];

    // the game is won if every pile is empty
    if (empty(stock)) {
        var victory = true;

        for (var i in tableaux) {
            if (!empty(tableaux[i])) {
                victory = false;
            }
        }

        if (victory && !game.won) {
            game.won = true;
            win();
            return;
        }
    }

    // TODO: figure out if there are no moves left
}

/**
 * Called when a game is won, provides a bonus score.
 */
function bonus(time) {
    return 0;
}