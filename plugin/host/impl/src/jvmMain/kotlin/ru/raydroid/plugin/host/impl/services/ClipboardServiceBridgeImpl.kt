package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException

internal class ClipboardServiceBridgeImpl(
    private val clipboardGateway: ClipboardGateway = AwtClipboardGateway(),
) : ClipboardServiceBridge {
    override suspend fun copy(
        content: ClipboardServiceBridge.ClipboardContent,
        secret: Boolean,
    ) {
        clipboardGateway.copy(content)
    }

    override suspend fun clear() {
        clipboardGateway.clear()
    }

    override suspend fun read(historyOffset: Int): ClipboardServiceBridge.ClipboardContent {
        if (historyOffset != 0) {
            return ClipboardServiceBridge.ClipboardContent()
        }

        return clipboardGateway.read()
    }
}

internal interface ClipboardGateway {
    fun copy(content: ClipboardServiceBridge.ClipboardContent)

    fun clear()

    fun read(): ClipboardServiceBridge.ClipboardContent
}

private class AwtClipboardGateway(
    private val clipboardProvider: () -> Clipboard = {
        Toolkit.getDefaultToolkit().systemClipboard
    },
) : ClipboardGateway {
    override fun copy(content: ClipboardServiceBridge.ClipboardContent) {
        runCatching {
            clipboardProvider().setContents(ClipboardTransferable(content), null)
        }
    }

    override fun clear() {
        runCatching {
            clipboardProvider().setContents(StringSelection(""), null)
        }
    }

    override fun read(): ClipboardServiceBridge.ClipboardContent =
        runCatching {
            val contents = clipboardProvider().getContents(null) ?: return ClipboardServiceBridge.ClipboardContent()
            when {
                contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor) -> {
                    val files = contents.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                    val filePath = (files?.firstOrNull() as? File)?.absolutePath
                    ClipboardServiceBridge.ClipboardContent(filePath = filePath)
                }

                contents.isDataFlavorSupported(DataFlavor.stringFlavor) -> {
                    ClipboardServiceBridge.ClipboardContent(
                        text = contents.getTransferData(DataFlavor.stringFlavor) as? String,
                    )
                }

                else -> ClipboardServiceBridge.ClipboardContent()
            }
        }.getOrElse {
            ClipboardServiceBridge.ClipboardContent()
        }
}

private class ClipboardTransferable(
    private val content: ClipboardServiceBridge.ClipboardContent,
) : Transferable {
    private val flavors =
        buildList {
            if (content.filePath != null) {
                add(DataFlavor.javaFileListFlavor)
            }
            if (content.text != null || content.filePath != null) {
                add(DataFlavor.stringFlavor)
            }
        }.toTypedArray()

    override fun getTransferDataFlavors(): Array<DataFlavor> = flavors.copyOf()

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavors.any { candidate -> candidate == flavor }

    @Throws(UnsupportedFlavorException::class, IOException::class)
    override fun getTransferData(flavor: DataFlavor): Any =
        when {
            flavor == DataFlavor.javaFileListFlavor && content.filePath != null -> listOf(File(content.filePath))
            flavor == DataFlavor.stringFlavor -> content.text ?: content.filePath.orEmpty()
            else -> throw UnsupportedFlavorException(flavor)
        }
}
