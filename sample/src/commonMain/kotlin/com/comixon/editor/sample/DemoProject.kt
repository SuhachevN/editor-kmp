package com.comixon.editor.sample

import com.comixon.editor.core.BubbleFamily
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorFrame
import com.comixon.editor.core.EditorPoint
import com.comixon.editor.core.EditorProject
import com.comixon.editor.core.EditorRect
import com.comixon.editor.core.FontSize

internal fun demoProject(): EditorProject =
    EditorProject(
        frames =
            listOf(
                EditorFrame(
                    id = "frame_0",
                    originalImageUrl = demoImage(),
                    layers =
                        listOf(
                            EditorBubbleLayer(
                                "bubble_0",
                                EditorRect(90.0, 120.0, 560.0, 350.0),
                                BubbleFamily.CLASSIC,
                                0,
                                "Every story starts with a bubble.",
                                FontSize.L,
                                EditorPoint(730.0, 650.0),
                                true,
                            ),
                            EditorBubbleLayer(
                                "bubble_1",
                                EditorRect(490.0, 700.0, 480.0, 330.0),
                                BubbleFamily.ROCKS,
                                1,
                                "Make it yours!",
                                FontSize.XL,
                                EditorPoint(330.0, 1140.0),
                                true,
                            ),
                        ),
                ),
            ),
    )
