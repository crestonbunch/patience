Patience
========

This is an Android solitaire app. You can download it from
the play store: https://play.google.com/store/apps/details?id=im.bunch.patience

If you are interested in helping maintain an ad-free, solitaire suite
than please contribute!

How it works
============

Games of solitaire are defined under assets/scripts using JavaScript.
Please feel free to see existing games to learn how the rules are written.

The rules are parsed using the V8 runtime in realtime while the user plays.
Standard actions are tracked by the game such as tapping or dragging, and 
the rules are then executed on the V8 engine to validate any user action to
confirm that that action is acceptable.
