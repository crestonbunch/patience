/**
 * This file contains common assets for game scripts to use like JSON card representations, etc.
 *
 * @author Creston
 * @version 1.0.0
 */

var SUITS = ['CLUBS', 'HEARTS', 'DIAMONDS', 'SPADES'];
var RANKS = ['ACE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE',
            'TEN', 'JACK', 'QUEEN', 'KING'];

/**
 * Layouts are functions that take in a given rectangular space, card width, card height, and
 * pile object and output a list of c(x, y) coordinates for each card.
 *
 * Rectangles have a left, top, bottom, and right property.
 */
var LAYOUTS = {

    SQUARED: function(rect, width, height, pile) {
        var num = pile.cards.length;
        var result = [];
        for (var i = 0; i < num; i++) {
            result.push({x: rect.left, y: rect.top});
        }
        return result;
    },

    FANNED: function(rect, width, height, pile) {
        var num = pile.cards.length;
        var result = [];
        if (num == 0) { return result; }
        var maxDelta = height / 3; // at most one third of the card will show
        var minDelta = height / 8; // at least one eigth of the card will show
        var totalPileSpace = (rect.bottom - rect.top - height);
        var spacePerCard = totalPileSpace / num;

        var delta = Math.max(Math.min(spacePerCard, maxDelta), minDelta);

        var dx = 0;
        for (var i = 0; i < num; i++) {
            if (!pile.cards[i].visible) {
                // turned-over cards don't get as much space
                result.push({x: rect.left, y: rect.top + dx});
                dx += minDelta;
            } else {
                result.push({x: rect.left, y: rect.top + dx});
                dx += delta;
            }
        }

        return result;
    },

    FANNED_RIGHT: function(rect, width, height, pile) {
        var num = pile.cards.length;
        var result = [];
        if (num == 0) { return result; }
        var maxDelta = width / 3; // at most one third of the card will show
        var minDelta = width / 8; // at least one eigth of the card will show
        var totalPileSpace = (rect.right - rect.left - width);
        var spacePerCard = totalPileSpace / num;

        var delta = Math.max(Math.min(spacePerCard, maxDelta), minDelta);

        for (var i = 0; i < num; i++) {
            result.push({x: rect.left + i*delta, y: rect.top});
        }

        return result;
    },

    PYRAMID: function(rect, width, height, pile) {
        var num = pile.cards.length;
        var result = [];
        // count how many cards will be in the bottom row
        var count = 0;
        var next = 1;
        for (var i = 0; i < num-1; i++) {
            count++;
            // move to the next row
            if (count == next) {
                count = 0;
                next++;
            }
        }
        count++;
        // calculate the width of the pyramid box
        var w = count*width;
        // this is the root of the pyramid
        var x1 = rect.left + w/2 - width/2;
        var y = rect.top;
        var dx = width / 2;
        var dy = height / 2;
        var count = 0;
        var next = 1;
        for (var i = 0; i < num; i++) {
            result.push({x: x1 + count*width, y: y});
            count++;
            // move to the next row
            if (count == next) {
                count = 0;
                next++;
                x1 = x1-dx;
                y = y + dy;
            }
        }

        return result;
    }
}


var Card = function(suit, rank) {
    this.suit = suit;
    this.rank = rank;
    this.visible = false;
    this.selected = false;
    // default position and size for each card
    this.x = 0;
    this.y = 0;
    this.z = 0;
    this.width = 25;
    this.height = 35;
    this.offsetX = 0; // add an offset to the card position, as a percent of the card width/height
    this.offsetY = 0;
    this.alive = true; // set to false to remove this card from play
}

var Deck = function() {

    // initialize a standard deck
    this.cards = [];

    // create all 52 cards
    for (var i = 0; i < SUITS.length; i++) {
        for (var j = 0; j < RANKS.length; j++) {
            this.cards.push(new Card(SUITS[i], RANKS[j]));
        }
    }

    this.shuffle = function() {
        this.cards = shuffle(this.cards);
    }

    /**
     * Draw num cards, or just the first one if not given.
     */
    this.draw = function(num) {
        if (typeof num !== "undefined") {
            var out = [];
            while (out.length < num) {
                out.push(this.cards.pop())
            }
            return out;
        } else {
            return this.cards.pop();
        }
    }

    /**
     * Draw a specific card from the deck. Suit is optional.
     */
    this.find = function(rank, suit) {
        if (suit === undefined) {
            var i = 0;
            for (; i < this.cards.length; i++) {
                if (this.cards[i].rank == rank) {
                    break;
                }
            }
            return this.cards.splice(i, 1);
        } else {
            var i = 0;
            for (; i < this.cards.length; i++) {
                if (this.cards[i].rank == rank && this.cards[i].suit == suit) {
                    break;
                }
            }
            return this.cards.splice(i, 1);
        }
        return null;
    }
}

/* source: http://stackoverflow.com/questions/2450954/how-to-randomize-shuffle-a-javascript-array */
function shuffle(array) {
    var currentIndex = array.length, temporaryValue, randomIndex;

    /* While there remain elements to shuffle...*/
    while (0 !== currentIndex) {
        /* Pick a remaining element...*/
        randomIndex = Math.floor(Math.random() * currentIndex);
        currentIndex -= 1;

        /* And swap it with the current element.*/
        temporaryValue = array[currentIndex];
        array[currentIndex] = array[randomIndex];
        array[randomIndex] = temporaryValue;
    }

    return array;
}

/**
 * Get the bottom (living) card from a pile.
 */
function bottom(pile) {
    if (pile.cards.length > 0) {
        var i = 0;
        while (i < pile.cards.length+1 && !pile.cards[i].alive) {
            i++;
        }
        if (i < pile.cards.length) {
            return pile.cards[i];
        } else {
            return null;
        }
    } else {
        return null;
    }
}

/**
 * Get the top card (living) card from a pile.
 */
function top(pile) {
    if (pile.cards.length > 0) {
        var i = pile.cards.length - 1;
        while (i > -1 && !pile.cards[i].alive) {
            i--;
        }
        if (i > -1) {
            return pile.cards[i];
        } else {
            return null;
        }
    } else {
        return null;
    }
}

/**
 * Flip a card over.
 */
function flip(card) {
    if (card !== null && card !== undefined) {
        card.visible = true;
    }
}

/**
 * Check if a pile is empty.
 */
function empty(pile) {
    var i = 0;
    var count = 0;
    while (count == 0 && i < pile.cards.length) {
        if (pile.cards[i].alive) { count++; }
        i++;
    }
    return count == 0;
}

/**
 * Move cards from pile a to b with visibility
 * Reverses the order of the cards (like a stack), use reverse() on pile A before calling move
 * if you want to retain the same order.
 */
function move(a, b, visibility) {
    while (a.cards.length > 0) {
        var c = a.cards.pop();
        c.visible = visibility;
        b.cards.push(c);
    }
}

/**
 * Pop a card off a pile.
 */
function pop(pile) {
    var card = top(pile);
    var i = pile.cards.indexOf(card);
    pile.cards.splice(i, i+1);
    return card;
}

/**
 * Reverse the cards in a pile.
 */
function reverse(pile) {
    pile.cards.reverse();
    return pile;
}

/**
 * Count the number of living cards in a pile.
 */
function size(pile) {
    var count = 0;
    for (var i = 0; i < pile.cards.length; i++) {
        var card = pile.cards[i];
        if (card.alive) {
            count++;
        }
    }
    return count;
}

/**
 * Helper function to disallow tapping.
 */
function noTap(game) {

}

/**
 * Helper function to disallow splitting.
 */
function noSplit(game, pile, card) {
    return [];
}

/**
 * Helper function to disallow merging.
 */
function noMerge(game, pile, addon) {
    return false;
}