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
        parent: FlatSegmentInterface.RootSet?,
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
                val set = FlatSegmentModel.RootSet(segment, depth)
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

        open class RootSet(
            override val model: SegmentInterface.RootSet,
            depth: Int,
        ): FlatSegmentModel(depth), FlatSegmentInterface.RootSet {
            override val id: Long?  get() = null
            override val repeatCount: Int get() = model.repeatCount
            override val parent: FlatSegmentInterface.RootSet?  get() = null
        }

        class Set(
            override val model: SegmentInterface.Set,
            depth: Int,
            override val parent: FlatSegmentInterface.RootSet?
        ): FlatSegmentModel.RootSet(model, depth), FlatSegmentInterface.Set {
            override val id: Long get() = model.segmentId
            override val repeatCount: Int get() = model.repeatCount
        }

        class Period(
            override val model: SegmentInterface.Period,
            depth: Int,
            override val parent: FlatSegmentInterface.RootSet?
        ) : FlatSegmentModel(depth), FlatSegmentInterface.Period {
            override val id: Long = model.segmentId
            override val name: String = model.name
            override val duration: Int = model.duration
        }

        class SetFooter(
            override val model: SegmentInterface.RootSet,
            depth: Int,
            override val parent: FlatSegmentInterface.RootSet?,
            override val set: FlatSegmentInterface.RootSet,
        ) : FlatSegmentModel(depth), FlatSegmentInterface.SetFooter {
            override val id: Long? = model.segmentId
        }
    }
}