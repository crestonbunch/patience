//#!GameRules klondike

var properties = {
    name: "Klondike",
    description: "The classic game of solitaire.",
    version: 1,
    rules:
    "<b>Foundations:</b> The foundations are built up by suit starting with an ace.<br><br>"
  + "<b>Tableaux:</b> The tableaux are built down by alternating colors starting with a king.<br><br>"
  + "<b>Stock:</b> Cards may be drawn from the stock and placed in the waste.<br><br>"
  + "<b>Waste:</b> The top card in the waste may be placed on the tableaux or the foundations."
};

LAYOUTS.TRIPLEDRAW = function(rect, width, height, pile) {
    var num = pile.cards.length;
    var result = [];
    if (num == 0) { return result; }
    var delta = width / 2; // at most one half of the card will show

    // stack bottom cards neatly underneath
    for (var i = 0; i < num - 3; i++) {
        result.push({x: rect.left, y: rect.top});
    }
    // top 3 cards are fanned to the right
    for (var i = 0; i < Math.min(num, 3); i++) {
        result.push({x: rect.left + i * delta, y: rect.top});
    }

    return result;
}

/**
 * Initialize a basic klondike game.
 */
function init() {

    // TODO: provide seed to deck
    var deck = new Deck();
    deck.shuffle();

    var game = {
        name: properties.name,
        board: {cols: 7, rows: 4},
        score: 0,
        won: false,
        lost: false,
        undo: true,
        options: {
            tripleDraw: {
                display: "Deal 3 cards",
                value: false
            },
            instantFlip: {
                display: "Flip tableaux cards automatically",
                value: true
            }
        },
        piles: {
            stock: {
                name: 'stock',
                col: 0, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: deck.draw(24),
                tap: stockTap,
                split: noSplit,
                merge: noMerge
            },

            waste: {
                name: 'waste',
                col: 1, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: wasteSplit,
                merge: noMerge
            },

            tableaux1: {
                name: 'tableaux',
                col: 0, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(1),
                tap: tableauxTap('tableaux1'),
                split: tableauxSplit,
                merge: tableauxMerge,
                back: 'king' // show an empty king slot
            },

            tableaux2: {
                name: 'tableaux',
                col: 1, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(2),
                tap: tableauxTap('tableaux2'),
                split: tableauxSplit,
                merge: tableauxMerge,
                back: 'king' // show an empty king slot
            },

            tableaux3: {
                name: 'tableaux',
                col: 2, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(3),
                tap: tableauxTap('tableaux3'),
                split: tableauxSplit,
                merge: tableauxMerge,
                back: 'king' // show an empty king slot
            },

            tableaux4: {
                name: 'tableaux',
                col: 3, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(4),
                tap: tableauxTap('tableaux4'),
                split: tableauxSplit,
                merge: tableauxMerge,
                back: 'king' // show an empty king slot
            },

            tableaux5: {
                name: 'tableaux',
                col: 4, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(5),
                tap: tableauxTap('tableaux5'),
                split: tableauxSplit,
                merge: tableauxMerge,
                back: 'king' // show an empty king slot
            },

            tableaux6: {
                name: 'tableaux',
                col: 5, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(6),
                tap: tableauxTap('tableaux6'),
                split: tableauxSplit,
                merge: tableauxMerge,
                back: 'king' // show an empty king slot
            },

            tableaux7: {
                name: 'tableaux',
                col: 6, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(7),
                tap: tableauxTap('tableaux7'),
                split: tableauxSplit,
                merge: tableauxMerge,
                back: 'king' // show an empty king slot
            },
            foundation1: {
                name: 'foundation',
                col: 3, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: foundationSplit,
                merge: foundationMerge,
                back: 'ace' // show an empty ace slot
            },
            foundation2: {
                name: 'foundation',
                col: 4, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: foundationSplit,
                merge: foundationMerge,
                back: 'ace'
            },
            foundation3: {
                name: 'foundation',
                col: 5, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: foundationSplit,
                merge: foundationMerge,
                back: 'ace'
            },
            foundation4: {
                name: 'foundation',
                col: 6, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: foundationSplit,
                merge: foundationMerge,
                back: 'ace'
            }
        }
    };

    // flip the top card in each tableaux
    flip(top(game.piles['tableaux1']));
    flip(top(game.piles['tableaux2']));
    flip(top(game.piles['tableaux3']));
    flip(top(game.piles['tableaux4']));
    flip(top(game.piles['tableaux5']));
    flip(top(game.piles['tableaux6']));
    flip(top(game.piles['tableaux7']));

    return game;
};

/**
 * Called whenever options get updated.
 */
function updateOptions(game) {
    // set the layout for the waste to tripleDraw
    if (game.options.tripleDraw.value) {
        game.piles.waste.layout = LAYOUTS.TRIPLEDRAW;
    }
}

/**
 * Change the layout based on screen dimensions.
 */
function resize(game, width, height) {
    if (height > width) {
        // portrait
        game.board.rows = 4;
        game.board.cols = 7;
        game.piles.stock.row = 0;
        game.piles.stock.col = 0;
        game.piles.waste.row = 0;
        game.piles.waste.col = 1;
        game.piles.tableaux1.row = 1.5;
        game.piles.tableaux1.col = 0;
        game.piles.tableaux2.row = 1.5;
        game.piles.tableaux2.col = 1;
        game.piles.tableaux3.row = 1.5;
        game.piles.tableaux3.col = 2;
        game.piles.tableaux4.row = 1.5;
        game.piles.tableaux4.col = 3;
        game.piles.tableaux5.row = 1.5;
        game.piles.tableaux5.col = 4;
        game.piles.tableaux6.row = 1.5;
        game.piles.tableaux6.col = 5;
        game.piles.tableaux7.row = 1.5;
        game.piles.tableaux7.col = 6;
        game.piles.foundation1.row = 0;
        game.piles.foundation1.col = 3;
        game.piles.foundation2.row = 0;
        game.piles.foundation2.col = 4;
        game.piles.foundation3.row = 0;
        game.piles.foundation3.col = 5;
        game.piles.foundation4.row = 0;
        game.piles.foundation4.col = 6;
    } else {
        // landscape
        game.board.rows = 4;
        game.board.cols = 12;
        game.piles.stock.row = 0;
        game.piles.stock.col = 11;
        game.piles.waste.row = 0;
        game.piles.waste.col = 9;
        game.piles.tableaux1.row = 0;
        game.piles.tableaux1.col = 1.5;
        game.piles.tableaux2.row = 0;
        game.piles.tableaux2.col = 2.5;
        game.piles.tableaux3.row = 0;
        game.piles.tableaux3.col = 3.5;
        game.piles.tableaux4.row = 0;
        game.piles.tableaux4.col = 4.5;
        game.piles.tableaux5.row = 0;
        game.piles.tableaux5.col = 5.5;
        game.piles.tableaux6.row = 0;
        game.piles.tableaux6.col = 6.5;
        game.piles.tableaux7.row = 0;
        game.piles.tableaux7.col = 7.5;
        game.piles.foundation1.row = 0;
        game.piles.foundation1.col = 0;
        game.piles.foundation2.row = 1;
        game.piles.foundation2.col = 0;
        game.piles.foundation3.row = 2;
        game.piles.foundation3.col = 0;
        game.piles.foundation4.row = 3;
        game.piles.foundation4.col = 0;
    }
}

function stockTap(game) {
    var stock = game.piles.stock;
    var waste = game.piles.waste;

    if (stock.cards.length > 0) {
        if (!game.options.tripleDraw.value) {
            // move top card from stock to waste
            var card = pop(stock);
            waste.cards.push(card);
            flip(card);
        } else {
            // move top 3 cards from stock to waste
            var cards = [pop(stock), pop(stock), pop(stock)];
            for (var i = 0; i < cards.length; i++) {
                var card = cards[i];
                if (card !== null) {
                    waste.cards.push(card);
                    flip(card);
                }
            }
        }
    } else {
        // recycle waste
        move(waste, stock, false);

        // recycling waste scores -100 only when in single draw mode
        if (!game.options.tripleDraw.value) {
            score(-100);
        }
    }

    history();
    updateStatus(game);
    save();
}

function wasteSplit(game, pile, target) {
    var index = pile.cards.indexOf(target);
    // can only split at the top card in the waste
    if (index == pile.cards.length-1) {
        return pile.cards.splice(index, pile.cards.length);
    }
}

function tableauxTap(tableaux) {
    return function(game) {
        var target = game.piles[tableaux];
        if (top(target) !== null && top(target).visible === false) {
            flip(top(target));
            // turning over a tableaux card scores 5 points
            score(5);
        }
    }
}

function tableauxSplit(game, pile, card) {
    var index = pile.cards.indexOf(card);
    // only split a tableaux at a visible card
    if (index > -1 && card.visible) {
        return pile.cards.splice(index, pile.cards.length);
    }
}

function tableauxMerge(game, pile, addon) {

    var merged = false;

    if (pile.cards.length > 0 && addon.cards.length > 0) {
        var a = top(pile);
        var b = bottom(addon);

        // only merge when b has 1 lower rank than a and is opposite color
        if (RANKS.indexOf(a.rank) - RANKS.indexOf(b.rank) === 1) {
            var valid = false;
            if (a.suit == 'SPADES' || a.suit == 'CLUBS') {
                valid = (b.suit == 'HEARTS' || b.suit == 'DIAMONDS');
            } else {
                valid = (b.suit == 'SPADES' || b.suit == 'CLUBS');
            }
            if (valid) {
                merged = true;
            }
        }
    } else if (pile.cards.length == 0 && addon.cards.length > 0) {
        // if a pile is empty, you can only place a king
        if (bottom(addon).rank == 'KING') {
            merged = true;
        }
    }

    if (merged) {
        move(reverse(addon), pile, true);

        if (game.options.instantFlip.value) {
            tableauxTap('tableaux1')(game);
            tableauxTap('tableaux2')(game);
            tableauxTap('tableaux3')(game);
            tableauxTap('tableaux4')(game);
            tableauxTap('tableaux5')(game);
            tableauxTap('tableaux6')(game);
            tableauxTap('tableaux7')(game);
        }

        if (addon.name == 'foundation') {
            score(-15); // moving a card from the foundation scores -15
        } else if (addon.name == 'waste') {
            score(5) // moving a card from the waste scores 5
        }

        history();
        updateStatus(game);
        save();
    }

    return merged;
}

function foundationSplit(game, pile, card) {
    var index = pile.cards.indexOf(card);
    if (index > -1) {
        return pile.cards.splice(index, pile.cards.length);
    }
}

function foundationMerge(game, pile, addon) {
    if (addon.cards.length > 1 ) { return false; }
    var a = top(pile);
    var b = bottom(addon);
    var merged = false;
    if (a != null
        && b != null
        && RANKS.indexOf(b.rank) - RANKS.indexOf(a.rank) === 1
        && a.suit == b.suit)
    {
        // can only merge a pile onto the foundation if it is the same suit and one higher rank
        merged = true;
    } else if (a === null && b !== null && b.rank == 'ACE') {
        // foundations must start with an ace
        merged = true;
    }

    if (merged) {
        move(addon, pile, true);

        if (game.options.instantFlip.value) {
            tableauxTap('tableaux1')(game);
            tableauxTap('tableaux2')(game);
            tableauxTap('tableaux3')(game);
            tableauxTap('tableaux4')(game);
            tableauxTap('tableaux5')(game);
            tableauxTap('tableaux6')(game);
            tableauxTap('tableaux7')(game);
        }

        score(10); // moving a card to the foundation scores 10 points

        history();
        updateStatus(game);
        save();
    }

    return merged;
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
    game.piles.stock.split = noSplit;
    game.piles.stock.merge = noMerge;
    game.piles.stock.layout = LAYOUTS.SQUARED;
    game.piles.waste.tap = noTap;
    game.piles.waste.split = wasteSplit;
    game.piles.waste.merge = noMerge;
    if (!game.options.tripleDraw.value) {
        game.piles.waste.layout = LAYOUTS.SQUARED;
    } else {
        game.piles.waste.layout = LAYOUTS.TRIPLEDRAW;
    }
    game.piles.tableaux1.tap = tableauxTap('tableaux1');
    game.piles.tableaux2.tap = tableauxTap('tableaux2');
    game.piles.tableaux3.tap = tableauxTap('tableaux3');
    game.piles.tableaux4.tap = tableauxTap('tableaux4');
    game.piles.tableaux5.tap = tableauxTap('tableaux5');
    game.piles.tableaux6.tap = tableauxTap('tableaux6');
    game.piles.tableaux7.tap = tableauxTap('tableaux7');

    for (var key in game.piles) {
        var pile = game.piles[key];

        if (pile.name == 'tableaux') {
            pile.merge = tableauxMerge;
            pile.split = tableauxSplit;
            pile.layout = LAYOUTS.FANNED;
        } else if (pile.name == 'foundation') {
            pile.tap = noTap;
            pile.split = foundationSplit;
            pile.merge = foundationMerge;
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
    var waste = game.piles.waste;
    var tableaux = [
        game.piles.tableaux1, game.piles.tableaux2, game.piles.tableaux3,
        game.piles.tableaux4, game.piles.tableaux5, game.piles.tableaux6,
        game.piles.tableaux7
    ]
    var foundations = [
        game.piles.foundation1, game.piles.foundation2,
        game.piles.foundation3, game.piles.foundation4
    ]

    // the game is won if every tableaux is empty or every card in every tableaux is faceup
    if (empty(stock) && empty(waste)) {
        var e = true;
        for (var i in tableaux) {
            if (!empty(tableaux[i])) { e = false; };
        }

        var victory;
        // all piles are empty except for the foundations, check victor
        if (e) {
            victory = true;
            for (var i in foundations) {
                var foundation = foundations[i];
                if (foundation.cards.length < 13) {
                    victory = false;
                }
            }
        // check that every card is faceup
        } else {
            victory = true;
            for (var i in tableaux) {
                var t = tableaux[i];
                for (var j in t.cards) {
                    var card = t.cards[j];
                    if (!card.visible) { victory = false;}
                }
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
    var secs = time / 1000;

    if (secs > 30) {
        return Math.round(35000 / secs);
    }

    return 0;
}