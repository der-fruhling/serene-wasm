@file:Suppress("unused")

package net.derfruhling.serene.wasm.instruction

import net.derfruhling.serene.wasm.module.BlockType
import kotlin.reflect.KClass

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@MustBeDocumented
annotation class OpContract(val expr: String) {
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
    @MustBeDocumented
    annotation class Args(vararg val types: KClass<*>, val lastRepeating: Boolean = false)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("block")
    @Args(BlockType::class)
    annotation class Block(val isLoop: Boolean)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class BlockControl(val type: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class RequiresSpecialHandling
}
