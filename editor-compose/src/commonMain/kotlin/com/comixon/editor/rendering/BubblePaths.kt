package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

internal object BubblePaths {
    fun inReferenceSpace(
        rect: Rect,
        make: (Rect) -> Path,
    ): Path {
        val p = BubbleReferenceMetrics.POINT_TO_REFERENCE
        return make(Rect(rect.left / p, rect.top / p, rect.right / p, rect.bottom / p)).apply {
            transform(scaleMatrix(p, p))
        }
    }

    fun smoothCornersRectangle(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.14520f * r.width, r.top + 0.02500f * r.height)
            lineTo(r.left + 0.87521f * r.width, r.top + 0.02500f * r.height)
            cubicTo(
                r.left + 0.91114f * r.width,
                r.top + 0.02500f * r.height,
                r.left + 0.92911f * r.width,
                r.top + 0.02500f * r.height,
                r.left + 0.94531f * r.width,
                r.top + 0.03155f * r.height,
            )
            lineTo(r.left + 0.94845f * r.width, r.top + 0.03249f * r.height)
            cubicTo(
                r.left + 0.96957f * r.width,
                r.top + 0.04191f * r.height,
                r.left + 0.98620f * r.width,
                r.top + 0.06228f * r.height,
                r.left + 0.99388f * r.width,
                r.top + 0.08815f * r.height,
            )
            cubicTo(
                r.left + 1.00000f * r.width,
                r.top + 0.11184f * r.height,
                r.left + 1.00000f * r.width,
                r.top + 0.13385f * r.height,
                r.left + 1.00000f * r.width,
                r.top + 0.17787f * r.height,
            )
            lineTo(r.left + 1.00000f * r.width, r.top + 0.84713f * r.height)
            cubicTo(
                r.left + 1.00000f * r.width,
                r.top + 0.89115f * r.height,
                r.left + 1.00000f * r.width,
                r.top + 0.91316f * r.height,
                r.left + 0.99465f * r.width,
                r.top + 0.93301f * r.height,
            )
            lineTo(r.left + 0.99388f * r.width, r.top + 0.93685f * r.height)
            cubicTo(
                r.left + 0.98620f * r.width,
                r.top + 0.96272f * r.height,
                r.left + 0.96957f * r.width,
                r.top + 0.98309f * r.height,
                r.left + 0.94845f * r.width,
                r.top + 0.99251f * r.height,
            )
            cubicTo(
                r.left + 0.92911f * r.width,
                r.top + 1.00000f * r.height,
                r.left + 0.91114f * r.width,
                r.top + 1.00000f * r.height,
                r.left + 0.87521f * r.width,
                r.top + 1.00000f * r.height,
            )
            lineTo(r.left + 0.14520f * r.width, r.top + 1.00000f * r.height)
            cubicTo(
                r.left + 0.10926f * r.width,
                r.top + 1.00000f * r.height,
                r.left + 0.09130f * r.width,
                r.top + 1.00000f * r.height,
                r.left + 0.07510f * r.width,
                r.top + 0.99345f * r.height,
            )
            lineTo(r.left + 0.07196f * r.width, r.top + 0.99251f * r.height)
            cubicTo(
                r.left + 0.05084f * r.width,
                r.top + 0.98309f * r.height,
                r.left + 0.03421f * r.width,
                r.top + 0.96272f * r.height,
                r.left + 0.02652f * r.width,
                r.top + 0.93685f * r.height,
            )
            cubicTo(
                r.left + 0.02041f * r.width,
                r.top + 0.91316f * r.height,
                r.left + 0.02041f * r.width,
                r.top + 0.89115f * r.height,
                r.left + 0.02041f * r.width,
                r.top + 0.84713f * r.height,
            )
            lineTo(r.left + 0.02041f * r.width, r.top + 0.17787f * r.height)
            cubicTo(
                r.left + 0.02041f * r.width,
                r.top + 0.13385f * r.height,
                r.left + 0.02041f * r.width,
                r.top + 0.11184f * r.height,
                r.left + 0.02575f * r.width,
                r.top + 0.09199f * r.height,
            )
            lineTo(r.left + 0.02652f * r.width, r.top + 0.08815f * r.height)
            cubicTo(
                r.left + 0.03421f * r.width,
                r.top + 0.06228f * r.height,
                r.left + 0.05084f * r.width,
                r.top + 0.04191f * r.height,
                r.left + 0.07196f * r.width,
                r.top + 0.03249f * r.height,
            )
            cubicTo(
                r.left + 0.09130f * r.width,
                r.top + 0.02500f * r.height,
                r.left + 0.10926f * r.width,
                r.top + 0.02500f * r.height,
                r.left + 0.14520f * r.width,
                r.top + 0.02500f * r.height,
            )
            close()
        }

    fun rectangleEllipse(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.99318f * r.width, r.top + 0.49731f * r.height)
            cubicTo(
                r.left + 0.99318f * r.width,
                r.top + 0.93331f * r.height,
                r.left + 0.77221f * r.width,
                r.top + 0.98329f * r.height,
                r.left + 0.49964f * r.width,
                r.top + 0.98329f * r.height,
            )
            cubicTo(
                r.left + 0.22707f * r.width,
                r.top + 0.98329f * r.height,
                r.left + 0.00611f * r.width,
                r.top + 0.93331f * r.height,
                r.left + 0.00611f * r.width,
                r.top + 0.49731f * r.height,
            )
            cubicTo(
                r.left + 0.00611f * r.width,
                r.top + 0.06131f * r.height,
                r.left + 0.22707f * r.width,
                r.top + 0.01134f * r.height,
                r.left + 0.49964f * r.width,
                r.top + 0.01134f * r.height,
            )
            cubicTo(
                r.left + 0.77221f * r.width,
                r.top + 0.01134f * r.height,
                r.left + 0.99318f * r.width,
                r.top + 0.06131f * r.height,
                r.left + 0.99318f * r.width,
                r.top + 0.49731f * r.height,
            )
            close()
        }

    fun rectangleEllipseTextPath(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.50195f * r.width, r.top)
            cubicTo(
                r.left + 0.05983f * r.width,
                r.top,
                r.left + 1.5f,
                r.top + 0.23129f * r.height,
                r.left + 1.5f,
                r.top + 0.49720f * r.height,
            )
            cubicTo(
                r.left + 1.5f,
                r.top + 0.76313f * r.height,
                r.left + 0.05983f * r.width,
                r.bottom - 2.26f,
                r.left + 0.50195f * r.width,
                r.bottom - 2.26f,
            )
            cubicTo(
                r.left + 0.94409f * r.width,
                r.bottom - 2.26f,
                r.right - 0.86f,
                r.top + 0.76313f * r.height,
                r.right - 0.86f,
                r.top + 0.49720f * r.height,
            )
            cubicTo(
                r.right - 0.86f,
                r.top + 0.23129f * r.height,
                r.left + 0.94409f * r.width,
                r.top,
                r.left + 0.50195f * r.width,
                r.top,
            )
            close()
        }

    fun rock1(r: Rect): Path =
        Path().apply {
            moveTo(r.right - 19.63f, r.top + 19.49f)
            cubicTo(r.right - 19.63f, r.top + 19.49f, r.right - 20.02f, r.top + 34.41f, r.right - 5.47f, r.top + 25.74f)
            cubicTo(
                r.right - 5.47f,
                r.top + 25.74f,
                r.right - 28.78f,
                r.bottom - 53.54f,
                r.right - 3.11f,
                r.bottom - 25.02f,
            )
            cubicTo(
                r.right - 3.11f,
                r.bottom - 25.02f,
                r.right - 22.78f,
                r.bottom - 32.52f,
                r.right - 17.27f,
                r.bottom - 16.27f,
            )
            cubicTo(
                r.right - 17.27f,
                r.bottom - 16.27f,
                r.left + 0.77596f * r.width,
                r.bottom - 21.92f,
                r.left + 0.78599f * r.width,
                r.bottom - 9.33f,
            )
            cubicTo(
                r.left + 0.66903f * r.width,
                r.bottom - 17.28f,
                r.left + 0.64091f * r.width,
                r.bottom - 4.8f,
                r.left + 0.64091f * r.width,
                r.bottom - 4.8f,
            )
            cubicTo(
                r.left + 0.57742f * r.width,
                r.bottom - 20.15f,
                r.left + 18.89f,
                r.bottom - 17.28f,
                r.left + 18.89f,
                r.bottom - 17.28f,
            )
            cubicTo(
                r.left + 20f,
                r.bottom - 20.43f,
                r.left + 19.36f,
                r.bottom - 27.84f,
                r.left + 12.91f,
                r.bottom - 27.61f,
            )
            cubicTo(
                r.left + 23.52f,
                r.bottom - 39.85f,
                r.left + 6.37f,
                r.bottom - 40.85f,
                r.left + 6.37f,
                r.bottom - 40.85f,
            )
            cubicTo(r.left + 27.23f, r.bottom - 62.72f, r.left + 4.37f, r.top + 23.24f, r.left + 4.37f, r.top + 23.24f)
            cubicTo(r.left + 4.37f, r.top + 23.24f, r.left + 22.03f, r.top + 34.9f, r.left + 18.89f, r.top + 19.9f)
            cubicTo(
                r.left + 18.89f,
                r.top + 19.9f,
                r.left + 0.23127f * r.width,
                r.top + 25.05f,
                r.left + 0.22459f * r.width,
                r.top + 4.37f,
            )
            cubicTo(
                r.left + 0.22459f * r.width,
                r.top + 4.37f,
                r.right - 85.74f,
                r.top + 25.11f,
                r.right - 19.63f,
                r.top + 19.49f,
            )
            close()
        }

    fun polygon1(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.11681f * r.width, r.top + 0.95064f * r.height)
            cubicTo(
                r.left + 0.11681f * r.width,
                r.top + 0.91575f * r.height,
                r.left + 0.01724f * r.width,
                r.top + 0.19293f * r.height,
                r.left + 0.01724f * r.width,
                r.top + 0.19293f * r.height,
            )
            lineTo(r.left + 0.98037f * r.width, r.top + 0.02717f * r.height)
            lineTo(r.left + 0.86966f * r.width, r.top + 0.96064f * r.height)
            lineTo(r.left + 0.11681f * r.width, r.top + 0.95064f * r.height)
            close()
        }

    fun polygon1TextPath(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.12295f * r.width, r.top + 0.90761f * r.height)
            lineTo(r.left + 0.89344f * r.width, r.top + 0.90761f * r.height)
            lineTo(r.left + 0.95902f * r.width, r.top + 0.13587f * r.height)
            lineTo(r.left + 0.02459f * r.width, r.top + 0.13587f * r.height)
            lineTo(r.left + 0.12295f * r.width, r.top + 0.90761f * r.height)
            close()
        }

    fun screamo1(r: Rect): Path =
        Path().apply {
            moveTo(r.right - 16.18f, r.top + 17.56f)
            lineTo(r.right - 3.96f, r.top + 13.25f)
            lineTo(r.right - 14.17f, r.top + 36.69f)
            lineTo(r.right - 14.17f, r.bottom - 42.41f)
            lineTo(r.right - 2.18f, r.bottom - 21.93f)
            lineTo(r.right - 14.17f, r.bottom - 21.93f)
            lineTo(r.right - 9.13f, r.bottom - 12.15f)
            lineTo(r.left + 0.77760f * r.width, r.bottom - 21.93f)
            lineTo(r.left + 0.54890f * r.width, r.bottom - 1.85f)
            lineTo(r.left + 0.58565f * r.width, r.bottom - 22.46f)
            lineTo(r.left + 18.49f, r.bottom - 10.12f)
            lineTo(r.left + 22.83f, r.bottom - 22.79f)
            lineTo(r.left + 12.15f, r.bottom - 21.93f)
            lineTo(r.left + 22.83f, r.top + 65.33f)
            lineTo(r.left + 1.37f, r.top + 69.73f)
            lineTo(r.left + 22.93f, r.top + 34.49f)
            lineTo(r.left + 10.45f, r.top + 13.25f)
            lineTo(r.left + 22.83f, r.top + 17.91f)
            lineTo(r.left + 22.83f, r.top + 2.37f)
            lineTo(r.left + 0.39369f * r.width, r.top + 19.05f)
            lineTo(r.right - 9.13f, r.top + 2.37f)
            lineTo(r.right - 16.18f, r.top + 17.56f)
            close()
        }

    fun rock2(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.92222f * r.width, r.bottom - 14.01f)
            cubicTo(
                r.left + 0.92222f * r.width,
                r.bottom - 14.01f,
                r.left + 0.63381f * r.width,
                r.bottom - 20.53f,
                r.left + 0.47283f * r.width,
                r.bottom - 0.25f,
            )
            cubicTo(
                r.left + 0.34563f * r.width,
                r.bottom - 21.96f,
                r.left + 10.51f,
                r.bottom - 12.2f,
                r.left + 10.51f,
                r.bottom - 12.2f,
            )
            cubicTo(
                r.left + 14.03f,
                r.bottom - 42.15f,
                r.left + 0.37f,
                r.top + 0.44498f * r.height,
                r.left + 0.37f,
                r.top + 0.44498f * r.height,
            )
            cubicTo(
                r.left + 0.37f,
                r.top + 0.44498f * r.height,
                r.left + 15.7f,
                r.top + 37.61f,
                r.left + 11.79f,
                r.top + 17.48f,
            )
            cubicTo(
                r.left + 11.79f,
                r.top + 17.48f,
                r.left + 0.24593f * r.width,
                r.top + 23.76f,
                r.left + 0.23827f * r.width,
                r.top + 3.52f,
            )
            cubicTo(
                r.left + 0.23827f * r.width,
                r.top + 3.52f,
                r.left + 0.30272f * r.width,
                r.top + 29.29f,
                r.left + 0.50371f * r.width,
                r.top + 0.42f,
            )
            cubicTo(
                r.left + 0.50371f * r.width,
                r.top + 0.42f,
                r.left + 0.51179f * r.width,
                r.top + 19.06f,
                r.left + 0.92222f * r.width,
                r.top + 14.71f,
            )
            cubicTo(
                r.left + 0.92222f * r.width,
                r.top + 14.71f,
                r.right - 17.89f,
                r.top + 0.29793f * r.height,
                r.right - 2.24f,
                r.top + 0.32727f * r.height,
            )
            cubicTo(
                r.right - 14.77f,
                r.top + 0.45623f * r.height,
                r.right - 10.73f,
                r.bottom - 36.27f,
                r.right - 0.39f,
                r.bottom - 32.48f,
            )
            cubicTo(
                r.right - 12.66f,
                r.bottom - 25.75f,
                r.left + 0.92222f * r.width,
                r.bottom - 14.01f,
                r.left + 0.92222f * r.width,
                r.bottom - 14.01f,
            )
            close()
        }

    fun polygon2(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.11897f * r.width, r.top + 0.91033f * r.height)
            cubicTo(
                r.left + 0.11897f * r.width,
                r.top + 0.87543f * r.height,
                r.left + 0.01552f * r.width,
                r.top + 0.03533f * r.height,
                r.left + 0.01552f * r.width,
                r.top + 0.03533f * r.height,
            )
            lineTo(r.left + 0.97414f * r.width, r.top + 0.10054f * r.height)
            lineTo(r.left + 0.86966f * r.width, r.top + 0.97151f * r.height)
            lineTo(r.left + 0.11897f * r.width, r.top + 0.91033f * r.height)
            close()
        }

    fun polygon2TextPath(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.12295f * r.width, r.top + 0.90761f * r.height)
            lineTo(r.left + 0.89344f * r.width, r.top + 0.90761f * r.height)
            lineTo(r.left + 0.96721f * r.width, r.top + 0.09783f * r.height)
            lineTo(r.left + 0.01639f * r.width, r.top + 0.09783f * r.height)
            lineTo(r.left + 0.12295f * r.width, r.top + 0.90761f * r.height)
            close()
        }

    fun screamo2(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 20.32f, r.bottom - 21.65f)
            lineTo(r.left + 5.02f, r.bottom - 16.2f)
            lineTo(r.left + 20.32f, r.bottom - 36.78f)
            lineTo(r.left + 20.32f, r.top + 53.17f)
            lineTo(r.left + 4.45f, r.top + 36.89f)
            lineTo(r.left + 19.94f, r.top + 38.06f)
            lineTo(r.left + 0.25f, r.top + 8.55f)
            lineTo(r.left + 20.32f, r.top + 20.04f)
            lineTo(r.left + 20.32f, r.top + 20.37f)
            lineTo(r.left + 17.3f, r.top + 1.84f)
            lineTo(r.left + 38.77f, r.top + 20.04f)
            lineTo(r.right - 55.25f, r.top + 20.33f)
            lineTo(r.right - 24.25f, r.top + 1.75f)
            lineTo(r.right - 39.6f, r.top + 21.6f)
            lineTo(r.right - 12.09f, r.top + 6.07f)
            lineTo(r.right - 22.12f, r.top + 20.37f)
            lineTo(r.right - 22.12f, r.top + 20.04f)
            lineTo(r.right - 6.5f, r.top + 16.95f)
            lineTo(r.right - 22.12f, r.top + 35.17f)
            lineTo(r.right - 22.12f, r.bottom - 50.45f)
            lineTo(r.right - 2.06f, r.bottom - 26.84f)
            lineTo(r.right - 21.74f, r.bottom - 34.79f)
            lineTo(r.right - 2.06f, r.bottom - 9.62f)
            lineTo(r.right - 22.12f, r.bottom - 21.65f)
            lineTo(r.right - 22.12f, r.bottom - 21.32f)
            lineTo(r.right - 16.79f, r.bottom - 7.06f)
            lineTo(r.right - 42.28f, r.bottom - 21.32f)
            lineTo(r.left + 39.92f, r.bottom - 20.8f)
            lineTo(r.left + 14.48f, r.bottom - 2.25f)
            lineTo(r.left + 20.32f, r.bottom - 21.32f)
        }

    fun rock3(r: Rect): Path =
        Path().apply {
            moveTo(r.right - 13.15f, r.bottom - 17.01f)
            cubicTo(
                r.right - 13.15f,
                r.bottom - 17.01f,
                r.left + 0.72918f * r.width,
                r.bottom - 25.3f,
                r.left + 0.60242f * r.width,
                r.bottom - 7.99f,
            )
            cubicTo(
                r.left + 0.57106f * r.width,
                r.bottom - 30.88f,
                r.left + 0.41092f * r.width,
                r.bottom - 1.54f,
                r.left + 0.41092f * r.width,
                r.bottom - 1.54f,
            )
            cubicTo(
                r.left + 0.29973f * r.width,
                r.bottom - 20.99f,
                r.left + 15.54f,
                r.bottom - 17.01f,
                r.left + 15.54f,
                r.bottom - 17.01f,
            )
            cubicTo(
                r.left + 22.15f,
                r.bottom - 37.83f,
                r.left + 0.37f,
                r.top + 0.54290f * r.height,
                r.left + 0.37f,
                r.top + 0.54290f * r.height,
            )
            cubicTo(
                r.left + 0.37f,
                r.top + 0.54290f * r.height,
                r.left + 22.93f,
                r.top + 62.99f,
                r.left + 15.54f,
                r.top + 15.17f,
            )
            cubicTo(
                r.left + 15.54f,
                r.top + 15.17f,
                r.left + 0.30228f * r.width,
                r.top + 19.84f,
                r.left + 0.27059f * r.width,
                r.top + 1.37f,
            )
            cubicTo(
                r.left + 0.27059f * r.width,
                r.top + 1.37f,
                r.left + 0.47402f * r.width,
                r.top + 29.78f,
                r.left + 0.73486f * r.width,
                r.top + 4.06f,
            )
            cubicTo(
                r.left + 0.73486f * r.width,
                r.top + 4.06f,
                r.right - 47.44f,
                r.top + 23.58f,
                r.right - 13.15f,
                r.top + 15.17f,
            )
            cubicTo(
                r.right - 13.15f,
                r.top + 15.17f,
                r.right - 17f,
                r.top + 0.53485f * r.height,
                r.right - 0.16f,
                r.top + 0.62810f * r.height,
            )
            cubicTo(
                r.right - 0.16f,
                r.top + 0.62810f * r.height,
                r.right - 13.67f,
                r.bottom - 34.42f,
                r.right - 13.15f,
                r.bottom - 17.01f,
            )
            close()
        }

    fun polygon3(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.04508f * r.width, r.top + 0.97011f * r.height)
            cubicTo(
                r.left + 0.04508f * r.width,
                r.top + 0.93522f * r.height,
                r.left + 0.11066f * r.width,
                r.top + 0.07880f * r.height,
                r.left + 0.11066f * r.width,
                r.top + 0.07880f * r.height,
            )
            lineTo(r.left + 0.87295f * r.width, r.top + 0.02989f * r.height)
            lineTo(r.left + 0.96311f * r.width, r.top + 0.92120f * r.height)
            lineTo(r.left + 0.04508f * r.width, r.top + 0.97011f * r.height)
            close()
        }

    fun polygon3TextPath(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.00000f * r.width, r.top + 1.00000f * r.height)
            lineTo(r.left + 1.00000f * r.width, r.top + 1.00000f * r.height)
            lineTo(r.left + 0.94079f * r.width, r.top + 0.00532f * r.height)
            lineTo(r.left + 0.08553f * r.width, r.top + 0.00532f * r.height)
            lineTo(r.left + 0.00000f * r.width, r.top + 1.00000f * r.height)
            close()
        }

    fun screamo3(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 21.32f, r.top + 19.43f)
            lineTo(r.left + 1.25f, r.top + 13.47f)
            lineTo(r.left + 22.04f, r.top + 0.46536f * r.height)
            lineTo(r.left + 2.09f, r.bottom - 18.72f)
            lineTo(r.left + 21.32f, r.bottom - 26.27f)
            lineTo(r.left + 16.54f, r.bottom - 12.01f)
            lineTo(r.left + 46.54f, r.bottom - 25.74f)
            lineTo(r.right - 55.16f, r.bottom - 26.59f)
            lineTo(r.right - 17.57f, r.bottom - 1.25f)
            lineTo(r.right - 33.97f, r.bottom - 26.27f)
            lineTo(r.right - 4.05f, r.bottom - 9.21f)
            lineTo(r.right - 16.12f, r.bottom - 26.27f)
            lineTo(r.right - 16.12f, r.bottom - 26.59f)
            lineTo(r.right - 1.51f, r.top + 34.44f)
            lineTo(r.right - 16.12f, r.top + 39.36f)
            lineTo(r.right - 4.64f, r.top + 13.47f)
            lineTo(r.right - 16.12f, r.top + 19.43f)
            lineTo(r.left + 0.65319f * r.width, r.top + 1.43f)
            lineTo(r.left + 0.67618f * r.width, r.top + 19.1f)
            lineTo(r.left + 0.49680f * r.width, r.top + 5.12f)
            lineTo(r.left + 0.51855f * r.width, r.top + 19.1f)
            lineTo(r.left + 17.97f, r.top + 0.9f)
            lineTo(r.left + 21.32f, r.top + 19.1f)
        }

    fun rock4(r: Rect): Path =
        Path().apply {
            moveTo(r.right - 15.38f, r.bottom - 13.34f)
            cubicTo(
                r.right - 15.38f,
                r.bottom - 13.34f,
                r.left + 0.67569f * r.width,
                r.bottom - 25.29f,
                r.left + 0.39291f * r.width,
                r.bottom - 1.25f,
            )
            cubicTo(
                r.left + 0.38591f * r.width,
                r.bottom - 23.44f,
                r.left + 28.1f,
                r.bottom - 3.17f,
                r.left + 28.1f,
                r.bottom - 3.17f,
            )
            cubicTo(
                r.left + 34.93f,
                r.bottom - 21.36f,
                r.left + 14.31f,
                r.bottom - 13.34f,
                r.left + 14.31f,
                r.bottom - 13.34f,
            )
            cubicTo(
                r.left + 24.28f,
                r.bottom - 63.59f,
                r.left + 1.75f,
                r.top + 0.31560f * r.height,
                r.left + 1.75f,
                r.top + 0.31560f * r.height,
            )
            cubicTo(
                r.left + 1.75f,
                r.top + 0.31560f * r.height,
                r.left + 21.48f,
                r.top + 36.33f,
                r.left + 14.31f,
                r.top + 14.84f,
            )
            cubicTo(
                r.left + 14.31f,
                r.top + 14.84f,
                r.left + 0.34044f * r.width,
                r.top + 19.4f,
                r.left + 0.54680f * r.width,
                r.top + 1.04f,
            )
            cubicTo(
                r.left + 0.54680f * r.width,
                r.top + 1.04f,
                r.right - 51.33f,
                r.top + 27.28f,
                r.right - 26.01f,
                r.top + 3.13f,
            )
            cubicTo(r.right - 26.01f, r.top + 3.13f, r.right - 34.4f, r.top + 19.92f, r.right - 15.38f, r.top + 14.84f)
            cubicTo(
                r.right - 15.38f,
                r.top + 14.84f,
                r.right - 22.86f,
                r.top + 0.37131f * r.height,
                r.right - 0.18f,
                r.top + 0.35274f * r.height,
            )
            cubicTo(
                r.right - 0.18f,
                r.top + 0.35274f * r.height,
                r.right - 19.74f,
                r.bottom - 59.65f,
                r.right - 15.38f,
                r.bottom - 13.73f,
            )
        }

    fun polygon4(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.00839f * r.width, r.top + 0.98798f * r.height)
            lineTo(r.left + 0.00839f * r.width, r.top + 0.00758f * r.height)
            lineTo(r.left + 0.99414f * r.width, r.top + 0.27208f * r.height)
            lineTo(r.left + 0.92411f * r.width, r.top + 0.95419f * r.height)
            lineTo(r.left + 0.00839f * r.width, r.top + 0.98798f * r.height)
            close()
        }

    fun polygon4TextPath(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.00000f * r.width, r.top + 1.00000f * r.height)
            lineTo(r.left + 0.90541f * r.width, r.top + 0.99457f * r.height)
            lineTo(r.left + 0.99550f * r.width, r.top + 0.17935f * r.height)
            lineTo(r.left + 0.00450f * r.width, r.top + 0.17935f * r.height)
            lineTo(r.left + 0.00000f * r.width, r.top + 1.00000f * r.height)
            close()
        }

    fun screamo4(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 21.82f, r.top + 23.6f)
            lineTo(r.left + 18.47f, r.top + 9.62f)
            lineTo(r.left + 0.48731f * r.width, r.top + 23.6f)
            lineTo(r.right - 17.87f, r.top + 1.37f)
            lineTo(r.right - 25.62f, r.top + 23.93f)
            lineTo(r.right - 7.43f, r.top + 20.5f)
            lineTo(r.right - 25.62f, r.top + 0.49849f * r.height)
            lineTo(r.right - 1.17f, r.top + 0.42202f * r.height)
            lineTo(r.right - 25.62f, r.bottom - 26.77f)
            lineTo(r.right - 10.11f, r.bottom - 7.69f)
            lineTo(r.left + 0.66103f * r.width, r.bottom - 26.24f)
            lineTo(r.left + 60.86f, r.bottom - 27.09f)
            lineTo(r.left + 23.26f, r.bottom - 1.75f)
            lineTo(r.left + 39.66f, r.bottom - 26.77f)
            lineTo(r.left + 9.75f, r.bottom - 9.71f)
            lineTo(r.left + 21.82f, r.bottom - 26.77f)
            lineTo(r.left + 1.75f, r.top + 0.52964f * r.height)
            lineTo(r.left + 22.54f, r.top + 0.56381f * r.height)
            lineTo(r.left + 1.75f, r.top + 17.97f)
            lineTo(r.left + 21.82f, r.top + 23.93f)
        }

    fun rightCorner(r: Rect): Path =
        Path().apply {
            moveTo(r.right - 21.64f, r.top)
            lineTo(r.left + 22.5f, r.top)
            lineTo(r.left + 22.5f, r.top + 22.5f)
            lineTo(r.left, r.top + 22.5f)
            lineTo(r.left, r.bottom - 21.38f)
            lineTo(r.left + 22.5f, r.bottom - 21.38f)
            lineTo(r.left + 22.5f, r.bottom)
            lineTo(r.right - 21.64f, r.bottom)
            lineTo(r.right - 21.64f, r.bottom - 22.43f)
            lineTo(r.right, r.bottom - 22.43f)
            lineTo(r.right, r.top + 22.5f)
            lineTo(r.right - 21.64f, r.top + 22.5f)
            lineTo(r.right - 21.64f, r.top)
            close()
        }

    fun diagonalCorner(r: Rect): Path =
        Path().apply {
            moveTo(r.right - 27.53f, r.bottom - 1.43f)
            lineTo(r.left + 26.53f, r.bottom - 1.43f)
            cubicTo(
                r.left + 16.36f,
                r.bottom - 11.58f,
                r.left + 10.66f,
                r.bottom - 17.27f,
                r.left + 0.5f,
                r.bottom - 27.43f,
            )
            lineTo(r.left + 0.5f, r.top + 27.5f)
            cubicTo(r.left + 10.66f, r.top + 17.35f, r.left + 16.36f, r.top + 11.65f, r.left + 26.53f, r.top + 1.5f)
            lineTo(r.right - 27.53f, r.top + 1.5f)
            cubicTo(r.right - 17.36f, r.top + 11.65f, r.right - 11.66f, r.top + 17.35f, r.right - 1.5f, r.top + 27.5f)
            lineTo(r.right - 1.5f, r.bottom - 27.43f)
            cubicTo(
                r.right - 11.66f,
                r.bottom - 17.27f,
                r.right - 17.36f,
                r.bottom - 11.58f,
                r.right - 27.53f,
                r.bottom - 1.43f,
            )
            close()
        }

    fun ellipseCorner(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.29924f * r.width, r.top + 0.01154f * r.height)
            lineTo(r.left + 0.70332f * r.width, r.top + 0.01154f * r.height)
            cubicTo(
                r.left + 0.78686f * r.width,
                r.top + 0.01154f * r.height,
                r.left + 0.82862f * r.width,
                r.top + 0.01154f * r.height,
                r.left + 0.86629f * r.width,
                r.top + 0.02791f * r.height,
            )
            lineTo(r.left + 0.87359f * r.width, r.top + 0.03027f * r.height)
            cubicTo(
                r.left + 0.92268f * r.width,
                r.top + 0.05380f * r.height,
                r.left + 0.96135f * r.width,
                r.top + 0.10474f * r.height,
                r.left + 0.97921f * r.width,
                r.top + 0.16941f * r.height,
            )
            cubicTo(
                r.left + 0.99343f * r.width,
                r.top + 0.22864f * r.height,
                r.left + 0.99343f * r.width,
                r.top + 0.28366f * r.height,
                r.left + 0.99343f * r.width,
                r.top + 0.39370f * r.height,
            )
            lineTo(r.left + 0.99343f * r.width, r.top + 0.61062f * r.height)
            cubicTo(
                r.left + 0.99343f * r.width,
                r.top + 0.72067f * r.height,
                r.left + 0.99343f * r.width,
                r.top + 0.77569f * r.height,
                r.left + 0.98100f * r.width,
                r.top + 0.82530f * r.height,
            )
            lineTo(r.left + 0.97921f * r.width, r.top + 0.83491f * r.height)
            cubicTo(
                r.left + 0.96135f * r.width,
                r.top + 0.89958f * r.height,
                r.left + 0.92268f * r.width,
                r.top + 0.95052f * r.height,
                r.left + 0.87359f * r.width,
                r.top + 0.97406f * r.height,
            )
            cubicTo(
                r.left + 0.82862f * r.width,
                r.top + 0.99279f * r.height,
                r.left + 0.78686f * r.width,
                r.top + 0.99279f * r.height,
                r.left + 0.70332f * r.width,
                r.top + 0.99279f * r.height,
            )
            lineTo(r.left + 0.29924f * r.width, r.top + 0.99279f * r.height)
            cubicTo(
                r.left + 0.21570f * r.width,
                r.top + 0.99279f * r.height,
                r.left + 0.17393f * r.width,
                r.top + 0.99279f * r.height,
                r.left + 0.13626f * r.width,
                r.top + 0.97641f * r.height,
            )
            lineTo(r.left + 0.12897f * r.width, r.top + 0.97406f * r.height)
            cubicTo(
                r.left + 0.07988f * r.width,
                r.top + 0.95052f * r.height,
                r.left + 0.04121f * r.width,
                r.top + 0.89958f * r.height,
                r.left + 0.02334f * r.width,
                r.top + 0.83491f * r.height,
            )
            cubicTo(
                r.left + 0.00912f * r.width,
                r.top + 0.77569f * r.height,
                r.left + 0.00912f * r.width,
                r.top + 0.72067f * r.height,
                r.left + 0.00912f * r.width,
                r.top + 0.61062f * r.height,
            )
            lineTo(r.left + 0.00912f * r.width, r.top + 0.39370f * r.height)
            cubicTo(
                r.left + 0.00912f * r.width,
                r.top + 0.28366f * r.height,
                r.left + 0.00912f * r.width,
                r.top + 0.22864f * r.height,
                r.left + 0.02155f * r.width,
                r.top + 0.17902f * r.height,
            )
            lineTo(r.left + 0.02334f * r.width, r.top + 0.16941f * r.height)
            cubicTo(
                r.left + 0.04121f * r.width,
                r.top + 0.10474f * r.height,
                r.left + 0.07988f * r.width,
                r.top + 0.05380f * r.height,
                r.left + 0.12897f * r.width,
                r.top + 0.03027f * r.height,
            )
            cubicTo(
                r.left + 0.17393f * r.width,
                r.top + 0.01154f * r.height,
                r.left + 0.21570f * r.width,
                r.top + 0.01154f * r.height,
                r.left + 0.29924f * r.width,
                r.top + 0.01154f * r.height,
            )
            close()
        }

    fun bezierCorner(r: Rect): Path =
        Path().apply {
            moveTo(r.right - 22.64f, r.bottom - 0.43f)
            lineTo(r.left + 23.5f, r.bottom - 0.43f)
            cubicTo(
                r.left + 23.5f,
                r.bottom - 13.13f,
                r.left + 13.2f,
                r.bottom - 23.43f,
                r.left + 0.5f,
                r.bottom - 23.43f,
            )
            lineTo(r.left + 0.5f, r.top + 24.5f)
            cubicTo(r.left + 13.2f, r.top + 24.5f, r.left + 23.5f, r.top + 14.2f, r.left + 23.5f, r.top + 1.5f)
            lineTo(r.right - 22.64f, r.top + 1.5f)
            cubicTo(r.right - 22.64f, r.top + 14.2f, r.right - 13.35f, r.top + 24.5f, r.right - 0.64f, r.top + 24.5f)
            lineTo(r.right - 0.64f, r.bottom - 23.43f)
            cubicTo(
                r.right - 13.35f,
                r.bottom - 23.43f,
                r.right - 22.64f,
                r.bottom - 13.13f,
                r.right - 22.64f,
                r.bottom - 0.43f,
            )
            close()
        }

    fun largeCloudTail(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.83553f * r.width, r.top + 0.41311f * r.height)
            cubicTo(
                r.left + 0.93012f * r.width,
                r.top + 0.46118f * r.height,
                r.left + 0.96781f * r.width,
                r.top + 0.57698f * r.height,
                r.left + 0.91967f * r.width,
                r.top + 0.67146f * r.height,
            )
            cubicTo(
                r.left + 0.87160f * r.width,
                r.top + 0.76618f * r.height,
                r.left + 0.75591f * r.width,
                r.top + 0.80384f * r.height,
                r.left + 0.66129f * r.width,
                r.top + 0.75573f * r.height,
            )
            cubicTo(
                r.left + 0.64843f * r.width,
                r.top + 0.86104f * r.height,
                r.left + 0.55260f * r.width,
                r.top + 0.93601f * r.height,
                r.left + 0.44726f * r.width,
                r.top + 0.92318f * r.height,
            )
            cubicTo(
                r.left + 0.34191f * r.width,
                r.top + 0.91036f * r.height,
                r.left + 0.26688f * r.width,
                r.top + 0.81449f * r.height,
                r.left + 0.27970f * r.width,
                r.top + 0.70918f * r.height,
            )
            cubicTo(
                r.left + 0.17539f * r.width,
                r.top + 0.72894f * r.height,
                r.left + 0.07477f * r.width,
                r.top + 0.66036f * r.height,
                r.left + 0.05512f * r.width,
                r.top + 0.55611f * r.height,
            )
            cubicTo(
                r.left + 0.03536f * r.width,
                r.top + 0.45191f * r.height,
                r.left + 0.10394f * r.width,
                r.top + 0.35125f * r.height,
                r.left + 0.20819f * r.width,
                r.top + 0.33153f * r.height,
            )
            cubicTo(
                r.left + 0.15991f * r.width,
                r.top + 0.23694f * r.height,
                r.left + 0.19726f * r.width,
                r.top + 0.12125f * r.height,
                r.left + 0.29177f * r.width,
                r.top + 0.07287f * r.height,
            )
            cubicTo(
                r.left + 0.38629f * r.width,
                r.top + 0.02449f * r.height,
                r.left + 0.50212f * r.width,
                r.top + 0.06194f * r.height,
                r.left + 0.55050f * r.width,
                r.top + 0.15639f * r.height,
            )
            cubicTo(
                r.left + 0.62229f * r.width,
                r.top + 0.07815f * r.height,
                r.left + 0.74381f * r.width,
                r.top + 0.07291f * r.height,
                r.left + 0.82208f * r.width,
                r.top + 0.14449f * r.height,
            )
            cubicTo(
                r.left + 0.90032f * r.width,
                r.top + 0.21625f * r.height,
                r.left + 0.90563f * r.width,
                r.top + 0.33780f * r.height,
                r.left + 0.83401f * r.width,
                r.top + 0.41608f * r.height,
            )
        }

    fun smallCloudTail(r: Rect): Path =
        Path().apply {
            moveTo(r.left + 0.57722f * r.width, r.top + 0.85116f * r.height)
            cubicTo(
                r.left + 0.46894f * r.width,
                r.top + 0.97658f * r.height,
                r.left + 0.27377f * r.width,
                r.top + 0.99511f * r.height,
                r.left + 0.14138f * r.width,
                r.top + 0.89258f * r.height,
            )
            cubicTo(
                r.left + 0.00911f * r.width,
                r.top + 0.79011f * r.height,
                r.left + -0.01056f * r.width,
                r.top + 0.60522f * r.height,
                r.left + 0.09772f * r.width,
                r.top + 0.47985f * r.height,
            )
            cubicTo(
                r.left + 0.03511f * r.width,
                r.top + 0.32906f * r.height,
                r.left + 0.11322f * r.width,
                r.top + 0.15890f * r.height,
                r.left + 0.27233f * r.width,
                r.top + 0.09943f * r.height,
            )
            cubicTo(
                r.left + 0.43127f * r.width,
                r.top + 0.04006f * r.height,
                r.left + 0.61122f * r.width,
                r.top + 0.11406f * r.height,
                r.left + 0.67394f * r.width,
                r.top + 0.26469f * r.height,
            )
            cubicTo(
                r.left + 0.84288f * r.width,
                r.top + 0.28843f * r.height,
                r.left + 0.95983f * r.width,
                r.top + 0.43748f * r.height,
                r.left + 0.93499f * r.width,
                r.top + 0.59779f * r.height,
            )
            cubicTo(
                r.left + 0.90999f * r.width,
                r.top + 0.75790f * r.height,
                r.left + 0.75261f * r.width,
                r.top + 0.86874f * r.height,
                r.left + 0.58344f * r.width,
                r.top + 0.84511f * r.height,
            )
        }
}
