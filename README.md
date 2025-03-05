# TacoTrainer
Android app to help with interval training

TODO

General
 * Make strings resources

Number entry fields:
  * improve decorations
  * fix deletion of ":". If the cursor is to the right of the ":" and you hit backspace, it
    will delete the ":", which has no effect other than moving the cursor.
  * automatically select whole thing on entry (Partially works: There is a bug that sometimes
    the text is deselected immediately in onValueChange)

Edit page:
 * Animate item selection
 * Allow reordering

List page:
 * implement delete

Workout run page:
 * Make timer a service so that it can run offscreen


