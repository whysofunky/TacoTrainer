package com.luckyzero.tacotrainer.viewModels

import com.luckyzero.tacotrainer.models.FlatSegmentInterface
import com.luckyzero.tacotrainer.models.SegmentInterface

object SegmentFlattener {

    fun flatten(root: SegmentInterface?) : List<FlatSegmentInterface> {
        return flattenSegment(root, 0, null)
    }

    private fun flattenSegment(
        segment: SegmentInterface?,
        depth: Int,
        parent: FlatSegmentInterface.Set?,
    ) : List<FlatSegmentInterface> {
        return when (segment) {
            null -> emptyList()
            is SegmentInterface.Set -> {
                val set = FlatSegmentModel.Set(segment, depth, parent)
                val footer = FlatSegmentModel.SetFooter(segment, depth, parent, set)
                mutableListOf<FlatSegmentInterface>().apply {
                    add(set)
                    addAll(segment.children.map { flattenSegment(it, depth + 1, set) }.flatten())
                    add(footer)
                }
            }
            is SegmentInterface.RootSet -> {
                val set = FlatSegmentModel.Set(segment, depth, null)
                val footer = FlatSegmentModel.SetFooter(segment, depth, parent, set)
                mutableListOf<FlatSegmentInterface>().apply {
                    add(set)
                    addAll(segment.children.map { flattenSegment(it, depth + 1, set) }.flatten())
                    add(footer)
                }
            }
            is SegmentInterface.Period -> {
                listOf(FlatSegmentModel.Period(segment, depth, parent))
            }
            else -> { throw IllegalStateException("unexpected segment ${segment.javaClass.name}")}
        }
    }

    sealed class FlatSegmentModel(override val depth: Int) : FlatSegmentInterface {
        abstract val model: SegmentInterface

        class Set(
            override val model: SegmentInterface.RootSet,
            depth: Int,
            override val parent: FlatSegmentInterface.Set?
        ) : FlatSegmentModel(depth),
            FlatSegmentInterface.Set {
            override val id: Long? get() = model.segmentId
            override val repeatCount: Int get() = model.repeatCount
        }

        class Period(
            override val model: SegmentInterface.Period,
            depth: Int,
            override val parent: FlatSegmentInterface.Set?
        ) : FlatSegmentModel(depth), FlatSegmentInterface.Period {
            override val id: Long = model.segmentId
            override val name: String = model.name
            override val duration: Int = model.duration
        }

        class SetFooter(
            override val model: SegmentInterface.RootSet,
            depth: Int,
            override val parent: FlatSegmentInterface.Set?,
            override val set: FlatSegmentInterface.Set,
        ) : FlatSegmentModel(depth), FlatSegmentInterface.SetFooter {
            override val id: Long? = model.segmentId
        }
    }
}