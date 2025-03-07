# TacoTrainer
Android app to help with interval training

TODO

General
 * Light and dark themes (everywhere except execute page)
 * Back button in header
 * Style navigation bar
 * Style status bar

Entry fields:
  * improve decorations
  * fix deletion of ":". If the cursor is to the right of the ":" and you hit backspace, it
    will delete the ":", which has no effect other than moving the cursor.
  * automatically select whole thing on entry (Partially works: There is a bug that sometimes
    the text is deselected immediately in onValueChange)

Edit page:
 * Animate item selection
 * Allow reordering
 * Only allow one item selected at a time
 * Clean up layout, spacing, colors
 * Support colors
 * Support "skip in last rep"
 * Support additional data

List page:
 * improve UI

Workout run page:
 * Animate transitions between periods

Workout timer
 * Make timer a service so that it can run offscreen
 * Confirm dialog before end workout
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
 * Free form

