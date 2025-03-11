# TacoTrainer
Android app to help with interval training

TODO

General
 * Back button in header
 *   https://github.com/pgatti86/toolbar-demo/blob/main/app/src/main/java/dev/gatti/toolbardemo/MainActivity.kt
 * Style status bar
 * Style navigation bar
 * Set up app theming better
 *   https://developer.android.com/develop/ui/compose/compositionlocal
 *   https://developer.android.com/develop/ui/compose/designsystems/material3
 *   https://developer.android.com/codelabs/jetpack-compose-theming#3

Entry fields:
  * consider using normal TextField all the time (readonly = true when not selected)
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
 * Confirm before delete

Workout run page:
 * Animate transitions between periods

Workout lifecycle 
 * If page is launched while a timer is in progress, either just keep displaying that, or ask if they want to cancel that workout and start a new one.
 * Confirm dialog before end workout
 * Persist paused workout state and reload on app restart

Workout timer
 * Post notifications when running
 * Hold wake lock during timer run
 * Make sound on transitions (use SoundPool https://developer.android.com/reference/android/media/SoundPool, https://stackoverflow.com/questions/9656853/the-correct-way-to-play-short-sounds-android)
 * Period timer moves after finishing, some layout problem.
 * If launched with a different workout, prompt to ask if we should change.
 * If launched for same workout, but it's already finished, automatically restart.

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

