# TacoTrainer
Android app to help with interval training

TODO

General
 * Back button in header
 * Style status bar
 * Style navigation bar

Entry fields:
  * improve decorations
  * fix deletion of ":". If the cursor is to the right of the ":" and you hit backspace, it
    will delete the ":", which has no effect other than moving the cursor.
  * automatically select whole thing on entry (Partially works: There is a bug that sometimes
    the text is deselected immediately in onValueChange)

Edit page:
 * Confirm before delete
 * Animate item selection
 * Allow reordering
 * Clean up layout, spacing, colors
 * Support colors for periods
 * Support "skip in last rep"
 * Support additional data

List page:
 * improve UI

Workout run page:
 * Animate transitions between periods

Workout lifecycle 
 * If page is launched while a timer is in progress, either just keep displaying that, or ask if they want to cancel that workout and start a new one.
 * Confirm dialog before end workout
 * Persist paused workout state and reload on app restart

Workout timer
 * Post notifications when running
 * Hold wake lock during timer run
 * Make sound on transitions
 * Period timer moves after finishing, some layout problem.
 * If launched with a different workout, prompt to ask if we should change.

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

