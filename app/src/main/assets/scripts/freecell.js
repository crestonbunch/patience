//#!GameRules freecell

var properties = {
    name: "Freecell",
    description: "Each game is a new puzzle.",
    version: 1,
    rules:
    "<b>Foundations:</b> The foundations are built up by suit starting with an ace.<br><br>"
  + "<b>Tableaux:</b> The tableaux are built down by alternating colors.<br><br>"
  + "<b>Cells:</b> Each cell may hold any card.<br><br>"
  + "You may only move groups of cards if there is enough room to move each card individually."
};


/**
 * Initialize a freecell game
 */
function init() {

    // TODO: provide seed to deck
    var deck = new Deck();
    deck.shuffle();

    var game = {
        name: properties.name,
        board: {cols: 8, rows: 4},
        score: 0,
        won: false,
        lost: false,
        undo: true,
        options: {
            autoFoundation: {
                display: "Automatically fill the foundation.",
                value: true
            },
        },
        piles: {

            tableaux1: {
                name: 'tableaux',
                col: 0, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(7),
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux2: {
                name: 'tableaux',
                col: 1, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(7),
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux3: {
                name: 'tableaux',
                col: 2, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(7),
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux4: {
                name: 'tableaux',
                col: 3, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(7),
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux5: {
                name: 'tableaux',
                col: 4, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(6),
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux6: {
                name: 'tableaux',
                col: 5, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(6),
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux7: {
                name: 'tableaux',
                col: 6, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(6),
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },

            tableaux8: {
                name: 'tableaux',
                col: 7, row: 1.5,
                layout: LAYOUTS.FANNED,
                cards: deck.draw(6),
                tap: noTap,
                split: tableauxSplit,
                merge: tableauxMerge
            },

            foundation1: {
                name: 'foundation',
                col: 4, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: noSplit,
                merge: foundationMerge,
                back: 'ace'
            },
            foundation2: {
                name: 'foundation',
                col: 5, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: noSplit,
                merge: foundationMerge,
                back: 'ace'
            },
            foundation3: {
                name: 'foundation',
                col: 6, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: noSplit,
                merge: foundationMerge,
                back: 'ace'
            },
            foundation4: {
                name: 'foundation',
                col: 7, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: noSplit,
                merge: foundationMerge,
                back: 'ace'
            },
            cell1: {
                name: 'cell',
                col: 0, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: cellSplit,
                merge: cellMerge
            },
            cell2: {
                name: 'cell',
                col: 1, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: cellSplit,
                merge: cellMerge
            },
            cell3: {
                name: 'cell',
                col: 2, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: cellSplit,
                merge: cellMerge
            },
            cell4: {
                name: 'cell',
                col: 3, row: 0,
                layout: LAYOUTS.SQUARED,
                cards: [],
                tap: noTap,
                split: cellSplit,
                merge: cellMerge
            }
        }
    };

    // flip every card in the game
    for (var i = 0; i < 7; i++) {
        flip(game.piles.tableaux1.cards[i]);
        flip(game.piles.tableaux2.cards[i]);
        flip(game.piles.tableaux3.cards[i]);
        flip(game.piles.tableaux4.cards[i]);
    }
    for (var i = 0; i < 6; i++) {
        flip(game.piles.tableaux5.cards[i]);
        flip(game.piles.tableaux6.cards[i]);
        flip(game.piles.tableaux7.cards[i]);
        flip(game.piles.tableaux8.cards[i]);
    }

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

    if (height > width) {
        // portrait
        game.board.cols = 8;
        game.board.rows = 4;
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
        game.piles.tableaux8.row = 1.5;
        game.piles.tableaux8.col = 7;
        game.piles.foundation1.row = 0;
        game.piles.foundation1.col = 4;
        game.piles.foundation2.row = 0;
        game.piles.foundation2.col = 5;
        game.piles.foundation3.row = 0;
        game.piles.foundation3.col = 6;
        game.piles.foundation4.row = 0;
        game.piles.foundation4.col = 7;
        game.piles.cell1.row = 0;
        game.piles.cell1.col = 0;
        game.piles.cell2.row = 0;
        game.piles.cell2.col = 1;
        game.piles.cell3.row = 0;
        game.piles.cell3.col = 2;
        game.piles.cell4.row = 0;
        game.piles.cell4.col = 3;
    } else {
        // portrait
        game.board.cols = 11;
        game.board.rows = 4;
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
        game.piles.tableaux8.row = 0;
        game.piles.tableaux8.col = 8.5;
        game.piles.foundation1.row = 0;
        game.piles.foundation1.col = 0;
        game.piles.foundation2.row = 1;
        game.piles.foundation2.col = 0;
        game.piles.foundation3.row = 2;
        game.piles.foundation3.col = 0;
        game.piles.foundation4.row = 3;
        game.piles.foundation4.col = 0;
        game.piles.cell1.row = 0;
        game.piles.cell1.col = 10;
        game.piles.cell2.row = 1;
        game.piles.cell2.col = 10;
        game.piles.cell3.row = 2;
        game.piles.cell3.col = 10;
        game.piles.cell4.row = 3;
        game.piles.cell4.col = 10;
    }
}

function tableauxSplit(game, pile, target) {

    var index = pile.cards.indexOf(target);
    // cards must be alternating color and decscending in order
    var cards = pile.cards.slice(index, pile.cards.length);
    var valid = true; // assume we can move it
    for (var i = 1; i < cards.length; i++) {
        var b = cards[i];
        var a = cards[i-1];

        if (RANKS.indexOf(a.rank) - RANKS.indexOf(b.rank) === 1) {
            if (a.suit == 'SPADES' || a.suit == 'CLUBS') {
                if (!(b.suit == 'HEARTS' || b.suit == 'DIAMONDS')) {
                    valid = false;
                }
            } else {
                if (!(b.suit == 'SPADES' || b.suit == 'CLUBS')) {
                    valid = false;
                }
            }
        } else {
            valid = false; // can't move
        }
    }
    if (valid) {
        return pile.cards.splice(index, pile.cards.length);
    }
}

function tableauxMerge(game, pile, addon) {

    var merged = false;

    var tableaux = [game.piles.tableaux1, game.piles.tableaux2, game.piles.tableaux3,
                     game.piles.tableaux4, game.piles.tableaux5, game.piles.tableaux6,
                     game.piles.tableaux7, game.piles.tableaux8]
    var cells = [game.piles.cell1, game.piles.cell2, game.piles.cell3, game.piles.cell4];

    // the target cannot be included
    if (tableaux.indexOf(pile) > -1) {
        tableaux.splice(tableaux.indexOf(pile), 1);
    }
    if (cells.indexOf(pile) > -1) {
        cells.splice(cells.indexOf(pile), 1);
    }
    // the source cannot be included
    if (tableaux.indexOf(addon.source) > -1) {
        tableaux.splice(tableaux.indexOf(addon.source), 1);
    }
    if (cells.indexOf(addon.source) > -1) {
        cells.splice(cells.indexOf(addon.source), 1);
    }

    // count the number of free tableaux
    var tableauxCount = 0;
    for (var i in tableaux) {
        if (tableaux[i].cards.length == 0) {
            tableauxCount++;
        }
    }
    // count the number of free cells
    var cellCount = 0;
    for (var i in cells) {
        if (cells[i].cards.length == 0) {
            cellCount++;
        }
    }
    // number of cards being moved
    var numMoving = addon.cards.length;
    // simulate filling up empty tableaux
    for (var i = 0; i < tableauxCount; i++) {
        numMoving -= (cellCount + 1)
    }
    // simulate filling up cells
    for (var i = 0; i < cellCount; i++) {
        numMoving--;
    }
    // always move at least one card
    numMoving--;

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
        // if a pile is empty, you can place anything
        merged = true;
    }

    // make sure there are enough slots to move
    if (numMoving > 0) {
        merged = false;
    }

    if (merged) {
        move(reverse(addon), pile, true);

        if (game.options.autoFoundation != undefined &&
            game.options.autoFoundation.value) {
            autoFoundation(game)
        }

        history();
        updateStatus(game);
        save();
    }

    return merged;
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

        if (game.options.autoFoundation != undefined &&
            game.options.autoFoundation.value) {
            autoFoundation(game)
        }

        history();
        updateStatus(game);
        save();
    }

    return merged;
}

function cellSplit(game, pile, target) {

    if (pile.cards.length > 0) {
        return pile.cards.splice(0, pile.cards.length);
    }

}

function cellMerge(game, pile, addon) {

    // can move any card so long as the cell is empty
    if (pile.cards.length == 0) {
        move(addon, pile, true);

        if (game.options.autoFoundation != undefined &&
            game.options.autoFoundation.value) {
            autoFoundation(game)
        }

        history();
        updateStatus(game);
        save();
        return true;
    }

    return false;
}

/**
 * Automatically move cards to the foundation.
 */
function autoFoundation(game) {
    var piles = [game.piles.tableaux1, game.piles.tableaux2, game.piles.tableaux3,
                    game.piles.tableaux4, game.piles.tableaux5, game.piles.tableaux6,
                    game.piles.tableaux7, game.piles.tableaux8, game.piles.cell1, game.piles.cell2,
                    game.piles.cell3, game.piles.cell4];

    var foundations = [game.piles.foundation1, game.piles.foundation2,
                       game.piles.foundation3, game.piles.foundation4];

    var moveMade = true;

    while (moveMade) {
        moveMade = false;
        for (var i in piles) {
            var pile = piles[i];

            for (var j in foundations) {
                var foundation = foundations[j];

                var a = top(foundation);
                var b = top(pile);

                var merged = false;
                if (a != null
                    && b != null
                    && RANKS.indexOf(b.rank) - RANKS.indexOf(a.rank) === 1
                    && a.suit == b.suit)
                {
                    merged = true;
                } else if (a === null && b !== null && b.rank == 'ACE') {
                    merged = true;
                }

                if (merged) {
                    var card = pile.cards.splice(pile.cards.length-1, pile.cards.length);
                    foundation.cards.push(card[0]);
                    moveMade = true;
                }
            }
        }
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

    // reattach functions
    for (var key in game.piles) {
        var pile = game.piles[key];

        if (pile.name == 'tableaux') {
            pile.tap = noTap;
            pile.merge = tableauxMerge;
            pile.split = tableauxSplit;
            pile.layout = LAYOUTS.FANNED;
        } else if (pile.name == 'foundation') {
            pile.tap = noTap;
            pile.split = noSplit;
            pile.merge = foundationMerge;
            pile.layout = LAYOUTS.SQUARED;
        } else if (pile.name == 'cell') {
            pile.tap = noTap;
            pile.split = cellSplit;
            pile.merge = cellMerge;
            pile.layout = LAYOUTS.SQUARED;
        }
    }

    return game;
}

/**
 * Check the win condition of the game.
 */
function updateStatus(game) {

    var tableaux = [
        game.piles.tableaux1, game.piles.tableaux2, game.piles.tableaux3,
        game.piles.tableaux4, game.piles.tableaux5, game.piles.tableaux6,
        game.piles.tableaux7, game.piles.tableaux8, game.piles.cell1,
        game.piles.cell2, game.piles.cell3, game.piles.cell4
    ]
    var foundations = [
        game.piles.foundation1, game.piles.foundation2,
        game.piles.foundation3, game.piles.foundation4
    ]

    // the game is won if every tableaux is empty or every card in every tableaux is faceup
    var e = true;
    for (var i in tableaux) {
        if (!empty(tableaux[i])) { e = false; };
    }

    var victory = false;
    // all piles are empty except for the foundations, check victor
    if (e) {
        victory = true;
        for (var i in foundations) {
            var foundation = foundations[i];
            if (foundation.cards.length < 13) {
                victory = false;
            }
        }
    }
    if (victory && !game.won) {
        game.won = true;
        win();
        return;
    }

    // TODO: figure out if there are no moves left
}

/**
 * Called when a game is won, provides a bonus score.
 */
function bonus(time) {
    return 0;
}