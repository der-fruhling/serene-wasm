package net.derfruhling.serene.wasm.module

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.Encode
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print

data class FieldType(val type: StorageType, val isMutable: Boolean) : Encode, Printable {
    override fun encode(out: WasmWriter) {
        type.encode(out)
        out.writeByte(when(isMutable) {
            false -> Constants.CONST
            true -> Constants.MUT
        })
    }

    constructor(reader: WasmReader) : this(
        type = StorageType.decode(reader),
        isMutable = when(val i = reader.readByte()) {
            Constants.CONST -> false
            Constants.MUT -> true
            else -> throw InvalidModuleDataException("Invalid mutability marker $i")
        }
    )

    override fun Printer.print() {
        if(isMutable) wrapInline {
            word("mut")
            type.print(this)
        } else {
            type.print(this)
        }
    }
}
