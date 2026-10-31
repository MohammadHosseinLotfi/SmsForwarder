package com.example.smsforwarder.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

fun kashiVector(
    name: String,
    paths: List<String> = emptyList(),
    circles: List<Triple<Float, Float, Float>> = emptyList()
): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    )

    for (d in paths) {
        val nodes = PathParser().parsePathString(d).toNodes()
        builder.addPath(
            pathData = nodes,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }

    for ((cx, cy, r) in circles) {
        val circlePath = "M ${cx - r} $cy a $r $r 0 1 0 ${r * 2} 0 a $r $r 0 1 0 -${r * 2} 0"
        val nodes = PathParser().parsePathString(circlePath).toNodes()
        builder.addPath(
            pathData = nodes,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }

    return builder.build()
}

object KashiIcons {
    val Dashboard: ImageVector by lazy {
        kashiVector(
            name = "Dashboard",
            paths = listOf(
                "M3 3.75C3 3.34 3.34 3 3.75 3h4.5c.41 0 .75.34.75.75v5.5c0 .41-.34.75-.75.75h-4.5A.75.75 0 0 1 3 9.25v-5.5Z",
                "M11 3.75c0-.41.34-.75.75-.75h4.5c.41 0 .75.34.75.75v2.5c0 .41-.34.75-.75.75h-4.5a.75.75 0 0 1-.75-.75v-2.5Z",
                "M11 9.75c0-.41.34-.75.75-.75h4.5c.41 0 .75.34.75.75v6.5c0 .41-.34.75-.75.75h-4.5a.75.75 0 0 1-.75-.75v-6.5Z",
                "M3 12.75c0-.41.34-.75.75-.75h4.5c.41 0 .75.34.75.75v3.5c0 .41-.34.75-.75.75h-4.5a.75.75 0 0 1-.75-.75v-3.5Z"
            )
        )
    }

    val Layers: ImageVector by lazy {
        kashiVector(
            name = "Layers",
            paths = listOf(
                "M10 2.75 2.75 6.6 10 10.45l7.25-3.85Z",
                "M2.75 10.1 10 13.95l7.25-3.85",
                "M2.75 13.6 10 17.45l7.25-3.85"
            )
        )
    }

    val ArrowLeft: ImageVector by lazy {
        kashiVector(
            name = "ArrowLeft",
            paths = listOf("M16.25 10H3.75", "M8.8 4.95 3.75 10l5.05 5.05")
        )
    }

    val Success: ImageVector by lazy {
        kashiVector(
            name = "Success",
            circles = listOf(Triple(10f, 10f, 7.5f)),
            paths = listOf("m7.1 10.1 1.95 1.95 3.85-3.9")
        )
    }

    val Clock: ImageVector by lazy {
        kashiVector(
            name = "Clock",
            circles = listOf(Triple(10f, 10f, 7.5f)),
            paths = listOf("M10 6.5v3.9l2.6 1.6")
        )
    }

    val Error: ImageVector by lazy {
        kashiVector(
            name = "Error",
            circles = listOf(Triple(10f, 10f, 7.5f)),
            paths = listOf("M10 6.4v4.1", "M10 13.4v.01")
        )
    }

    val Calendar: ImageVector by lazy {
        kashiVector(
            name = "Calendar",
            paths = listOf(
                "M7 3.75H13C16.2 3.75 17 4.55 17 7.75V13.25C17 16.45 16.2 17.25 13 17.25H7C3.8 17.25 3 16.45 3 13.25V7.75C3 4.55 3.8 3.75 7 3.75Z",
                "M3.2 8.2h13.6",
                "M7 2.5v2.6",
                "M13 2.5v2.6",
                "M6.9 11.3v.01",
                "M10 11.3v.01",
                "M13.1 11.3v.01",
                "M6.9 14.1v.01",
                "M10 14.1v.01"
            )
        )
    }

    val Workflow: ImageVector by lazy {
        kashiVector(
            name = "Workflow",
            paths = listOf(
                "M4.5 2.75H6.5C7.9 2.75 8.25 3.1 8.25 4.5V6.5C8.25 7.9 7.9 8.25 6.5 8.25H4.5C3.1 8.25 2.75 7.9 2.75 6.5V4.5C2.75 3.1 3.1 2.75 4.5 2.75Z",
                "M13.5 11.75H15.5C16.9 11.75 17.25 12.1 17.25 13.5V15.5C17.25 16.9 16.9 17.25 15.5 17.25H13.5C12.1 17.25 11.75 16.9 11.75 15.5V13.5C11.75 12.1 12.1 11.75 13.5 11.75Z",
                "M8.25 5.5h3.5c1.4 0 2.5 1.1 2.5 2.5v3.75",
                "M12.75 10.25l1.5 1.5 1.5-1.5"
            )
        )
    }

    val Receipt: ImageVector by lazy {
        kashiVector(
            name = "Receipt",
            paths = listOf(
                "M5 4.9c0-1.2.95-2.15 2.15-2.15h5.7c1.2 0 2.15.95 2.15 2.15v11.6c0 .5-.55.8-.95.5l-1.3-.95a.8.8 0 0 0-.95 0l-1.3.95a.8.8 0 0 1-.95 0l-1.3-.95a.8.8 0 0 0-.95 0l-1.3.95c-.4.3-1 0-1-.5Z",
                "M7.75 7h4.5",
                "M7.75 10h4.5"
            )
        )
    }

    val Webhook: ImageVector by lazy {
        kashiVector(
            name = "Webhook",
            circles = listOf(
                Triple(10f, 5f, 1.75f),
                Triple(4.75f, 14.5f, 1.75f),
                Triple(15.25f, 14.5f, 1.75f)
            ),
            paths = listOf(
                "M8.4 6.6 6 12.95",
                "M6.5 14.5h7",
                "M11.6 6.6l2.4 6.35"
            )
        )
    }

    val ChatSquare: ImageVector by lazy {
        kashiVector(
            name = "ChatSquare",
            paths = listOf(
                "M2.75 6.4c0-2.2 1.45-3.65 3.65-3.65h7.2c2.2 0 3.65 1.45 3.65 3.65v5.2c0 2.2-1.45 3.65-3.65 3.65h-3.35l-3.3 2.4c-.5.35-1.2 0-1.2-.6v-1.8c-1.9-.2-3-1.6-3-3.65Z",
                "M6.75 7.5h6.5",
                "M6.75 10.5h4"
            )
        )
    }

    val SimCard: ImageVector by lazy {
        kashiVector(
            name = "SimCard",
            paths = listOf(
                "M6.5 2.75h5.75l3.25 3.25v9.75c0 .85-.65 1.5-1.5 1.5H6.5c-.85 0-1.5-.65-1.5-1.5V4.25c0-.85.65-1.5 1.5-1.5Z",
                "M7.75 9.25h4.5v4.5h-4.5v-4.5",
                "M10 9.25v4.5",
                "M7.75 11.5h4.5"
            )
        )
    }

    val Trash: ImageVector by lazy {
        kashiVector(
            name = "Trash",
            paths = listOf(
                "M3.5 5.6h13",
                "M7.6 5.5l.35-1.35c.15-.6.7-1 1.3-1h1.5c.6 0 1.15.4 1.3 1l.35 1.35",
                "M15 5.6l-.6 9.1c-.1 1.5-1.35 2.55-2.85 2.55h-3.1c-1.5 0-2.75-1.05-2.85-2.55L5 5.6",
                "M8.6 9.1v4.5",
                "M11.4 9.1v4.5"
            )
        )
    }

    val Edit: ImageVector by lazy {
        kashiVector(
            name = "Edit",
            paths = listOf(
                "M11.15 4.75 4.35 11.45c-.3.3-.5.7-.55 1.1l-.3 2.6c-.05.55.4 1 .95.95l2.6-.3c.4-.05.8-.25 1.1-.55l6.75-6.75a2.65 2.65 0 0 0-3.75-3.75Z",
                "M10.25 5.65l3.75 3.75",
                "M10.9 16.75h5.6"
            )
        )
    }

    val Copy: ImageVector by lazy {
        kashiVector(
            name = "Copy",
            paths = listOf(
                "M9.95 6.75H13.9C16.46 6.75 17.1 7.39 17.1 9.95V13.9C17.1 16.46 16.46 17.1 13.9 17.1H9.95C7.39 17.1 6.75 16.46 6.75 13.9V9.95C6.75 7.39 7.39 6.75 9.95 6.75Z",
                "M13.25 6.75V6.1c0-1.9-1.3-3.2-3.2-3.2H6.1c-1.9 0-3.2 1.3-3.2 3.2v3.95c0 1.9 1.3 3.2 3.2 3.2h.65"
            )
        )
    }

    val Flask: ImageVector by lazy {
        kashiVector(
            name = "Flask",
            paths = listOf(
                "M8.5 2.75h3v4.6l4.2 7.25c.75 1.3-.2 2.65-1.65 2.65h-8.1c-1.45 0-2.4-1.35-1.65-2.65l4.2-7.25Z",
                "M7.5 2.75h5",
                "M6 12.25h8"
            )
        )
    }

    val Filter: ImageVector by lazy {
        kashiVector(
            name = "Filter",
            paths = listOf(
                "M4.6 3.5h10.8c.95 0 1.45 1.1.85 1.8l-4.05 4.8c-.3.35-.45.8-.45 1.25v4.1c0 .4-.25.8-.6.95l-1.7.85c-.55.25-1.15-.15-1.15-.75v-5.15c0-.45-.15-.9-.45-1.25L3.75 5.3c-.6-.7-.1-1.8.85-1.8Z"
            )
        )
    }

    val Plus: ImageVector by lazy {
        kashiVector(
            name = "Plus",
            paths = listOf("M10 5v10", "M5 10h10")
        )
    }

    val Info: ImageVector by lazy {
        kashiVector(
            name = "Info",
            circles = listOf(Triple(10f, 10f, 7.5f)),
            paths = listOf("M10 13.4V9.6", "M10 6.6v.01")
        )
    }

    val Phone: ImageVector by lazy {
        kashiVector(
            name = "Phone",
            paths = listOf(
                "M8.25 2.5H11.75C14.35 2.5 15 3.15 15 5.75V14.25C15 16.85 14.35 17.5 11.75 17.5H8.25C5.65 17.5 5 16.85 5 14.25V5.75C5 3.15 5.65 2.5 8.25 2.5Z",
                "M8.75 14.5h2.5"
            )
        )
    }

    val XCircle: ImageVector by lazy {
        kashiVector(
            name = "XCircle",
            circles = listOf(Triple(10f, 10f, 7.5f)),
            paths = listOf("m7.75 7.75 4.5 4.5", "M12.25 7.75l-4.5 4.5")
        )
    }

    val ChevronLeft: ImageVector by lazy {
        kashiVector(
            name = "ChevronLeft",
            paths = listOf("M12.25 5 7.75 10l4.5 5")
        )
    }

    val ChevronRight: ImageVector by lazy {
        kashiVector(
            name = "ChevronRight",
            paths = listOf("m7.75 5 4.5 5-4.5 5")
        )
    }

    val Banknote: ImageVector by lazy {
        kashiVector(
            name = "Banknote",
            paths = listOf(
                "M4.75 5H15.25C17.45 5 18 5.55 18 7.75V12.25C18 14.45 17.45 15 15.25 15H4.75C2.55 15 2 14.45 2 12.25V7.75C2 5.55 2.55 5 4.75 5Z",
                "M7.5 10a2.5 2.5 0 1 0 5 0 2.5 2.5 0 1 0-5 0",
                "M5 8v.01",
                "M15 12v.01"
            )
        )
    }
}
