package net.derfruhling.serene.wasm

@RequiresOptIn("This API is public and may be useful, but it in inherently **not**" +
    " stable! The behavior may change at any time. It is not recommended to use this API in " +
    "libraries, only use within this library and in final applications where the version of this " +
    "library is known.", level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class UnstablePublicApi
