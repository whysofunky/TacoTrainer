package com.luckyzero.tacotrainer.ui.pages

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.ui.navigation.WorkoutExecute
import com.luckyzero.tacotrainer.viewModels.WorkoutExecuteViewModel

private data class ColorTheme(
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val contrast: Color,
    val colorless: Color,
    val colorlessInverse: Color,
)

private val redTheme = ColorTheme(
    background = Color(0xFFD08080),
    primary = Color(0xFFC03030),
    secondary = Color(0xFFC05050),
    highlight = Color(0xFF600000),
    contrast = Color(0xFFC0C000),
    colorless = Color(0xFF101010),
    colorlessInverse = Color(0xFFF0F0F0)
)

private val greenTheme = ColorTheme(
    background = Color(0xFF80D080),
    primary = Color(0xFF30C030),
    secondary = Color(0xFF50C050),
    highlight = Color(0xFF006000),
    contrast = Color(0xFFC0C000),
    colorless = Color(0xFF101010),
    colorlessInverse = Color(0xFFF0F0F0)
)

private class SecondTicker(
    private val treeWalker: TreeWalker
) {
    private var startTimeMs: Long = 0
    private var pauseStartTimeMs: Long? = null
    private var pausedDurationMs: Long = 0

    fun start(timeMs: Long) {
        startTimeMs = timeMs
        pausedDurationMs = 0
        pauseStartTimeMs = null
        treeWalker.firstPeriod()
    }

    fun onTimeUpdate(timeMs: Long) {
        // This is now active duration since the start, regardless of when we started or how
        // much we paused.
        val normalizedTimeMs = (timeMs - startTimeMs - pausedDurationMs)

        // PUBLISH
        val totalSecondsElapsed = normalizedTimeMs / 1000
        val msToNextSecond = 1000 - normalizedTimeMs % 1000

        treeWalker.onTimeUpdate(normalizedTimeMs)

        val periodEndTimeMs = treeWalker.periodStartTimeMs +
                (treeWalker.currentPeriod?.durationMs ?: 0)

        // PUBLISH
        val currentPeriod = treeWalker.currentPeriod
        val periodElapsedMs = normalizedTimeMs - treeWalker.periodStartTimeMs
        val periodRemainMs = periodEndTimeMs - normalizedTimeMs
    }

    fun pause(timeMs: Long) {
        if (pauseStartTimeMs == null) {
            pauseStartTimeMs = timeMs
        }
    }

    fun resume(timeMs: Long) {
        pauseStartTimeMs?.let {
            pausedDurationMs += (timeMs - it)
        }
        pauseStartTimeMs = null
    }
}


class TreeWalker(
    private val rootSet: SegmentInterface.Set
) {
    private val stack = ArrayDeque<Node>()
    var currentPeriod: SegmentInterface.Period? = null
        private set
    var periodStartTimeMs: Long = 0
        private set

    fun firstPeriod() {
        val node = Node(rootSet, 0, 0)
        currentPeriod = findFirstPeriod(node)
    }

    // Normalized time means "active time since the start" of the workout.
    fun onTimeUpdate(normalizedTimeMs: Long) {
        var period: SegmentInterface.Period? = currentPeriod
        var periodStartTime = periodStartTimeMs

        while (period != null && period.durationMs > (normalizedTimeMs - periodStartTime)) {
            // This period has run out. Go to the next one, and
            // check to see if it has run out as well.
            periodStartTime += period.durationMs
            period = nextPeriod()
        }
    }

    private fun nextPeriod(): SegmentInterface.Period? {
        val node = getIncrementedNode()
        return findFirstPeriod(node)
    }

    private fun findFirstPeriod(node: Node?): SegmentInterface.Period? {
        while (node != null) {
            when (val child = node.set.children[node.currentChild]) {
                is SegmentInterface.Period -> {
                    return child
                }

                is SegmentInterface.Set -> {
                    stack.add(Node(child, 0, 0))
                }
            }
        }
        return null
    }

    private fun getIncrementedNode(): Node? {
        while (stack.isNotEmpty()) {
            val newNode = incNode(stack.removeLast())
            if (newNode != null) {
                stack.add(newNode)
                return newNode
            }
        }
        return null
    }

    private fun incNode(node: Node): Node? {
        return if (node.currentChild < node.set.children.count()) {
            Node(node.set, node.currentChild + 1, node.currentRep)
        } else if (node.currentRep < node.set.repeatCount) {
            Node(node.set, 0, node.currentRep + 1)
        } else {
            null
        }
    }

    data class Node(
        val set: SegmentInterface.Set,
        val currentChild: Int,
        val currentRep: Int,
    )
}


@Composable
fun WorkoutExecutePage(args: WorkoutExecute,
                       navHostController: NavHostController,
                       modifier: Modifier
) {
    val dbAccess = DbAccess(LocalContext.current)
    val viewModel = viewModel { WorkoutExecuteViewModel(args.workoutId, dbAccess) }
    val workout = viewModel.workoutFlow.collectAsStateWithLifecycle(null).value
    val rootSet = viewModel.setFlow.collectAsStateWithLifecycle(null).value


    // workout name
    // total duration remain / total duration
    // segment name
    // repetition / of repetition
    // remain duration
    // details ( HR range, watt range, gear range, etc.)
    // pause, end workout buttons
    Text(text = "Foo")
}










class TreeWalkerOld(
    private val rootSet: SegmentInterface.Set
) {
    private val stack = ArrayDeque<Node>()

    data class State(
        val currentPeriod: SegmentInterface.Period,
        val periodStartTimeMs: Long,
        val pausedDurationMs: Long
    )

    fun getUpdatedPeriod(
        timeMs: Long,
        state: State,
    ): State? {
        var period: SegmentInterface.Period? = state.currentPeriod
        var periodStartTime = state.periodStartTimeMs
        var pausedDurationMs = state.pausedDurationMs

        while (period != null && period.durationMs > (timeMs - periodStartTime - pausedDurationMs)) {
            periodStartTime += (period.durationMs + pausedDurationMs)
            pausedDurationMs = 0
            period = nextPeriod()
        }
        return period?.let { State(period, periodStartTime, pausedDurationMs) }
    }

    fun firstPeriod(): SegmentInterface.Period? {
        val node = Node(rootSet, 0, 0)
        return findFirstPeriod(node)
    }

    private fun nextPeriod(): SegmentInterface.Period? {
        val node = getIncrementedNode()
        return findFirstPeriod(node)
    }

    private fun findFirstPeriod(node: Node?): SegmentInterface.Period? {
        while (node != null) {
            when (val child = node.set.children[node.currentChild]) {
                is SegmentInterface.Period -> {
                    return child
                }

                is SegmentInterface.Set -> {
                    stack.add(Node(child, 0, 0))
                }
            }
        }
        return null
    }

    private fun getIncrementedNode(): Node? {
        while (stack.isNotEmpty()) {
            val newNode = incNode(stack.removeLast())
            if (newNode != null) {
                stack.add(newNode)
                return newNode
            }
        }
        return null
    }

    private fun incNode(node: Node): Node? {
        return if (node.currentChild < node.set.children.count()) {
            Node(node.set, node.currentChild + 1, node.currentRep)
        } else if (node.currentRep < node.set.repeatCount) {
            Node(node.set, 0, node.currentRep + 1)
        } else {
            null
        }
    }

    data class Node(
        val set: SegmentInterface.Set,
        val currentChild: Int,
        val currentRep: Int,
    )
}

