This directory contains all the code extending the common Flicker library
to make it fully compatible with Winscope. The common Flicker library is
written is Kotlin and compiled to JavaScript and then extended by the code in
this directory.

To use flickerlib in the rest of the Winscope source code use
`import { ... } from '@/flickerlib'` rather than importing the compiled
common Flicker library directly.

The flickerlib classes are extended through mixins (functions, getter, and
setters) that are injected into the original compiled common Flicker library
classes.