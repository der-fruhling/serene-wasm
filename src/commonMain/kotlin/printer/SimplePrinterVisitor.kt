package net.derfruhling.serene.wasm.printer

import kotlinx.io.Buffer
import kotlinx.io.readByteString
import net.derfruhling.serene.wasm.AbstractModuleVisitor
import net.derfruhling.serene.wasm.ModuleVisitor
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.sections.*

class SimplePrinterVisitor(base: ModuleVisitor?, private val out: Printer) : AbstractModuleVisitor(base) {
    private var currentFuncAfterImports = 0u

    override fun visitMagic(magic: UInt, version: UInt) {
        super.visitMagic(magic, version)
        out.appendLine(";; wasm version $version")
        out.appendLine(";; simple module printer (for debug purposes only)")
        out.appendLine("(module")
        out.indent++
    }

    override fun visitCustomSection(section: UnidentifiedCustomSection) {
        super.visitCustomSection(section)
        out.wrap {
            word("@custom")

            wrap {
                word("class")
                string("unidentified")
            }

            string(section.bytes)
        }
    }

    override fun visitCustomSection(section: CustomSection) {
        super.visitCustomSection(section)
        if(section is Printable) {
            out.nest { section.print(this) }
        } else {
            out.wrap {
                word("@custom")

                wrap {
                    word("class")
                    string(section::class.simpleName.toString())
                }

                val buffer = Buffer()
                section.encodeCustomInternal(WasmWriter(buffer))
                string(buffer.readByteString())
            }
        }
    }

    override fun visitTypeSection(section: TypeSection) {
        super.visitTypeSection(section)
        section.print(out)
    }

    override fun visitImportSection(section: ImportSection) {
        super.visitImportSection(section)
        section.print(out)
        currentFuncAfterImports = out.names.currentFunction
    }

    override fun visitFunctionSection(section: FunctionSection) {
        super.visitFunctionSection(section)
        section.print(out)
    }

    override fun visitTableSection(section: TableSection) {
        super.visitTableSection(section)
        section.print(out)
    }

    override fun visitMemorySection(section: MemorySection) {
        super.visitMemorySection(section)
        section.print(out)
    }

    override fun visitGlobalSection(section: GlobalSection) {
        super.visitGlobalSection(section)
        section.print(out)
    }

    override fun visitExportSection(section: ExportSection) {
        super.visitExportSection(section)
        section.print(out)
    }

    override fun visitStartSection(section: StartSection) {
        super.visitStartSection(section)
        section.print(out)
    }

    override fun visitElementSection(section: ElementSection) {
        super.visitElementSection(section)
        section.print(out)
    }

    override fun visitCodeSection(section: CodeSection) {
        super.visitCodeSection(section)
        out.names.currentFunction = currentFuncAfterImports
        section.print(out)
    }

    override fun visitDataSection(section: DataSection) {
        super.visitDataSection(section)
        section.print(out)
    }

    override fun visitTagSection(section: TagSection) {
        super.visitTagSection(section)
        section.print(out)
    }

    override fun visitUnknownSection(section: UnknownSection) {
        super.visitUnknownSection(section)
        out.wrap {
            word("@unknown")

            wrap {
                word("id")
                word("0x${Hex.format(section.id)}")
            }

            string(section.bytes)
        }
    }

    override fun visitEnd() {
        super.visitEnd()
        out.appendLine(')')
    }
}