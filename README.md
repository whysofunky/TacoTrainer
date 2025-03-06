# TacoTrainer
Android app to help with interval training

TODO

General
 * Make strings resources

Entry fields:
  * improve decorations
  * fix deletion of ":". If the cursor is to the right of the ":" and you hit backspace, it
    will delete the ":", which has no effect other than moving the cursor.
  * automatically select whole thing on entry (Partially works: There is a bug that sometimes
    the text is deselected immediately in onValueChange)

Edit page:
 * Animate item selection
 * Allow reordering
 * Clean up layout, spacing, colors
 * Support colors
 * Support "skip in last rep"
 * Support additional data

List page:
 * implement delete
 * improve UI

Workout run page:
 * Animate transitions between periods

Workout timer
 * Make timer a service so that it can run offscreen
 * Post notifications when running
 * Make sound on transitions

Settings page
 * Max HR
 * Resting HR
 * FTP
 * Custom HR zones
 * Custom FTP zones

Period note types
 * HR zone
 * HR range (manual)
 * FTP zone
 * FTP range (manual)
 * Target pace
 * Target speed
 * 

