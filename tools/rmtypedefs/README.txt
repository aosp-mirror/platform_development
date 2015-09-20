Android TypeDef Remover 1.0

This utility finds and removes all .class files that have been
annotated with the @IntDef annotation (android.annotations.IntDef) or
the @StringDef annotation (android.annotations.StringDef).

It also makes sure that these annotations have source level retention
(@Retention(RetentionPolicy.SOURCE)), since otherwise uses of the
typedef will appear in .class files as well.

This is intended to be used during the build to strip out any typedef
annotation classes, since these are not needed (or desirable) in the
system image.
