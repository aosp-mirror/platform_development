# Metalava

(Also known as "doclava2", but deliberately not named doclava2 since crucially it
does not generate docs; it's intended only for **meta**data extraction and generation.)

Metalava is a metadata generator intended for the Android source tree, used for
a number of purposes:

* Allow extracting the API (into signature text files, into stub API files (which
  in turn get compiled into android.jar, the Android SDK library)
  and more importantly to hide code intended to be implementation only, driven
  by javadoc comments like @hide, @$doconly, @removed, etc, as well as various
  annotations.

* Extracting source level annotations into external annotations file (such as
  the typedef annotations, which cannot be stored in the SDK as .class level
  annotations).

* Diffing versions of the API and determining whether a newer version is compatible
  with the older version.

## Building and running

To build:

    $ ./gradlew

This builds a binary distribution in `build/install/metalava`.

To run metalava:

    $ ./build/install/metalava/bin/metalava
                    _        _
     _ __ ___   ___| |_ __ _| | __ ___   ____ _
    | '_ ` _ \ / _ \ __/ _` | |/ _` \ \ / / _` |
    | | | | | |  __/ || (_| | | (_| |\ V / (_| |
    |_| |_| |_|\___|\__\__,_|_|\__,_| \_/ \__,_|

    metalava extracts metadata from source code to generate artifacts such as the
    signature files, the SDK stub files, external annotations etc.

    Usage: metalava <flags>

    Flags:

    --help                                This message.
    --quiet                               Only include vital output
    --verbose                             Include extra diagnostic output

    ...
(*output truncated*)

Metalava has a new command line syntax, but it also understands the doclava1
flags and translates them on the fly. Flags that are ignored are listed on
the command line. If metalava is dropped into an Android framework build for
example, you'll see something like this (unless running with --quiet) :

    metalava: Ignoring unimplemented doclava1 flag -encoding (UTF-8 assumed)
    metalava: Ignoring unimplemented doclava1 flag -source  (1.8 assumed)
    metalava: Ignoring javadoc-related doclava1 flag -J-Xmx1600m
    metalava: Ignoring javadoc-related doclava1 flag -J-XX:-OmitStackTraceInFastThrow
    metalava: Ignoring javadoc-related doclava1 flag -XDignore.symbol.file
    metalava: Ignoring javadoc-related doclava1 flag -doclet
    metalava: Ignoring javadoc-related doclava1 flag -docletpath
    metalava: Ignoring javadoc-related doclava1 flag -templatedir
    metalava: Ignoring javadoc-related doclava1 flag -htmldir
    ...

## Features

* Compatibility with doclava1: in compat mode, metalava spits out the same
  signature files for the framework as doclava1.

* Ability to read in an existing android.jar file instead of from source, which means
  we can regenerate signature files etc for older versions according to new formats
  (e.g. to fix past errors in doclava, such as annotation instance methods which were
  accidentally not included.)

* Ability to merge in data (annotations etc) from external sources, such as
  IntelliJ external annotations data as well as signature files containing
  annotations. This isn't just merged at export time, it's merged at codebase
  load time such that it can be part of the API analysis.

* Support for an updated signature file format:

  * Address errors in the doclava1 format which for example was missing annotation
    class instance methods

  * Improve the signature format such that it for example labels enums "enum"
    instead of "abstract class extends java.lang.Enum", annotations as "@interface"
    instead of "abstract class extends java.lang.Annotation", sorts modifiers in
    the canonical modifier order, using "extends" instead of "implements" for
    the superclass of an interface, and many other similar tweaks outlined
    in the `Compatibility` class. (Metalava also allows (and ignores) block
    comments in the signature files.)

  * Add support for writing (and reading) annotations into the signature
    files. This is vital now that some of these annotations become part of
    the API contract (in particular nullness contracts, as well as parameter
    names and default values.)

  * Support for a "compact" nullness format -- one based on Kotlin's syntax. Since
    the goal is to have **all** API elements explicitly state their nullness
    contract, the signature files would very quickly become bloated with
    @NonNull and @Nullable annotations everywhere. So instead, the signature
    format now uses a suffix of `?` for nullable, `!` for not yet annotated, and
    nothing for non-null.

    Instead of

        method public java.lang.Double convert0(java.lang.Float);
        method @Nullable public java.lang.Double convert1(@NonNull java.lang.Float);

    we have

        method public java.lang.Double! convert0(java.lang.Float!);
        method public java.lang.Double? convert1(java.lang.Float);


  * Other compactness improvements: Skip packages in some cases both for
    export and reinsert during import. Specifically, drop "java.lang."
    from package names such that you have

        method public void onUpdate(int, String);

    instead of

        method public void onUpdate(int, java.lang.String);

    Similarly, annotations (the ones considered part of the API; unknown
    annotations are not included in signature files) use just the simple
    name instead of the full package name, e.g. `@UiThread` instead of
    `@android.annotation.UiThread`.

  * Misc documentation handling; for example, it attempts to fix sentences
    that javadoc will mistreat, such as sentences that "end" with "e.g. ".
    It also looks for various common typos and fixes those; here's a sample
    error message running metalava on master:
    Enhancing docs:

        frameworks/base/core/java/android/content/res/AssetManager.java:166: error: Replaced Kitkat with KitKat in documentation for Method android.content.res.AssetManager.getLocales() [Typo]
        frameworks/base/core/java/android/print/PrinterCapabilitiesInfo.java:122: error: Replaced Kitkat with KitKat in documentation for Method android.print.PrinterCapabilitiesInfo.Builder.setColorModes(int, int) [Typo]

* Built-in support for injecting new annotations for use by the Kotlin compiler,
  not just nullness annotations found in the source code and annotations merged
  in from external sources, but also inferring whether nullness annotations
  have recently changed and if so marking them as @Migrate (which lets the
  Kotlin compiler treat errors in the user code as warnings instead of errors.)

* Support for generating documentation into the stubs files (so we can run javadoc or
  [Dokka](https://github.com/Kotlin/dokka) on the stubs files instead of the source
  code). This means that the documentation tool itself does not need to be able to
  figure out which parts of the source code is included in the API and which one is
  implementation; it is simply handed the filtered API stub sources that include
  documentation.

* Support for parsing Kotlin files. API files can now be implemented in Kotlin
  as well and metalava will parse and extract API information from them just
  as is done for Java files.

* Like doclava1, metalava can diff two APIs and warn about API compatibility
  problems such as removing API elements. Metalava adds new warnings around
  nullness, such as attempting to change a nullness contract incompatibly
  (e.g. you can change a parameter from non null to nullable for final classes,
  but not versa).  It also lets you diff directly on a source tree; it doesn't
  require you to create two signature files to diff.

* Consistent stubs: In doclava1, the code which iterated over the API and generated
  the signature files and generated the stubs had diverged, so there was some
  inconsistency. In metalava the stub files contain **exactly** the same signatures
  as in the signature files.

* Metalava can generate reports about nullness annotation coverage (which helps
  target efforts since we plan to annotate the entire API). First, it can
  generate a raw count:

        Nullness Annotation Coverage Statistics:
        1279 out of 46900 methods were annotated (2%)
        2 out of 21683 fields were annotated (0%)
        2770 out of 47492 parameters were annotated (5%)

  More importantly, you can also point it to some existing compiled applications
  (.class or .jar files) and it will then measure the annotation coverage of
  the APIs used by those applications. This lets us target the most important
  APIs that are currently used by a corpus of apps and target our annotation
  efforts in a targeted way. For example, running the analysis on the current
  version of framework, and pointing it to the
  [Plaid](https://github.com/nickbutcher/plaid) app's compiled output with

      ... --annotation-coverage-of ~/plaid/app/build/intermediates/classes/debug

  This produces the following output:

    324 methods and fields were missing nullness annotations out of 650 total API references.
    API nullness coverage is 50%

    |--------------------------------------------------------------|------------------|
    | Qualified Class Name                                         |      Usage Count |
    |--------------------------------------------------------------|-----------------:|
    | android.os.Parcel                                            |              146 |
    | android.view.View                                            |              119 |
    | android.view.ViewPropertyAnimator                            |              114 |
    | android.content.Intent                                       |              104 |
    | android.graphics.Rect                                        |               79 |
    | android.content.Context                                      |               61 |
    | android.widget.TextView                                      |               53 |
    | android.transition.TransitionValues                          |               49 |
    | android.animation.Animator                                   |               34 |
    | android.app.ActivityOptions                                  |               34 |
    | android.view.LayoutInflater                                  |               31 |
    | android.app.Activity                                         |               28 |
    | android.content.SharedPreferences                            |               26 |
    | android.content.SharedPreferences.Editor                     |               26 |
    | android.text.SpannableStringBuilder                          |               23 |
    | android.view.ViewGroup.MarginLayoutParams                    |               21 |
    | ... (99 more items                                           |                  |
    |--------------------------------------------------------------|------------------|

    Top referenced un-annotated members:

    |--------------------------------------------------------------|------------------|
    | Member                                                       |      Usage Count |
    |--------------------------------------------------------------|-----------------:|
    | Parcel.readString()                                          |               62 |
    | Parcel.writeString(String)                                   |               62 |
    | TextView.setText(CharSequence)                               |               34 |
    | TransitionValues.values                                      |               28 |
    | View.getContext()                                            |               28 |
    | ViewPropertyAnimator.setDuration(long)                       |               26 |
    | ViewPropertyAnimator.setInterpolator(android.animation.Ti... |               26 |
    | LayoutInflater.inflate(int, android.view.ViewGroup, boole... |               23 |
    | Rect.left                                                    |               22 |
    | Rect.top                                                     |               22 |
    | Intent.Intent(android.content.Context, Class<?>)             |               21 |
    | Rect.bottom                                                  |               21 |
    | TransitionValues.view                                        |               21 |
    | VERSION.SDK_INT                                              |               18 |
    | Context.getResources()                                       |               18 |
    | EditText.getText()                                           |               18 |
    | ... (309 more items                                          |                  |
    |--------------------------------------------------------------|------------------|


  From this it's clear that it would be useful to start annotating android.os.Parcel
  and android.view.View for example where there are unannotated APIs that are
  frequently used, at least by this app.

* Built on top of a full, type-resolved AST. Doclava1 was integrated with javadoc,
  which meant that most of the source tree was opaque. Therefore, as just one example,
  the code which generated documentation for typedef constants had to require the
  constants to all share a single prefix it could look for. However, in metalava,
  annotation references are available at the AST level, so it can resolve references
  and map them back to the original field references and include those directly.
  
* Support for extracting annotations. Metalava can also generate the external annotation
  files needed by Studio and lint in Gradle, which captures the typedefs (@IntDef and
  @StringDef classes) in the source code. Prior to this this was generated manually
  via the development/tools/extract code. This also merges in manually curated data;
  some of this is in the manual/ folder in this project.
  
* Support for extracting API levels (api-versions.xml). This was generated by separate
  code (tools/base/misc/api-generator), invoked during the build. This functionality
  is now rolled into metalava, which has one very important attribute: metalava
  will use this information when recording API levels for API usage. (Prior to this,
  this was based on signature file parsing in doclava, which sometimes generated
  incorrect results. Metalava uses the android.jar files themselves to ensure that
  it computes the exact available SDK data for each API level.)

## Architecture & Implementation

Metalava is implemented on top of IntelliJ parsing APIs (PSI and UAST). However,
these are hidden behind a "model": an abstraction layer which only exposes high
level concepts like packages, classes and inner classes, methods, fields, and
modifier lists (including annotations).

This is done for multiple reasons:

(1) It allows us to have multiple "back-ends": for example, metalava can read
    in a model not just from parsing source code, but from reading older SDK
    android.jar files (e.g. backed by bytecode) or reading previous signature
    files.  Reading in multiple versions of an API lets doclava perform "diffing",
    such as warning if an API is changing in an incompatible way. It can also
    generate signature files in the new format (including data that was missing
    in older signature files, such as annotation methods) without having to
    parse older source code which may no longer be easy to parse.

(2) There's a lot of logic for deciding whether code found in the source tree
    should be included in the API. With the model approach we can build up an
    API and for example mark a subset of its methods as included. By having
    a separate hierarchy we can easily perform this work once and pass around
    our filtered model instead of passing around PsiClass and PsiMethod instances
    and having to keep the filtered data separately and remembering to always
    consult the filter, not the PSI elements directly.

The basic API element class is "Item". (In doclava1 this was called a "DocInfo".)
There are several sub interfaces of Item: PackageItem, ClassItem, MemberItem,
MethodItem, FieldItem, ParameterItem, etc. And then there are several
implementation hierarchies: One is PSI based, where you point metalava to a
source tree or a .jar file, and it constructs Items built on top of PSI:
PsiPackageItem, PsiClassItem, PsiMethodItem, etc. Another is textual, based
on signature files: TextPackageItem, TextClassItem, and so on.

The "Codebase" class captures a complete API snapshot (including classes
that are hidden, which is why it's called a "Codebase" rather than an "API").

There are methods to load codebases - from source folders, from a .jar file,
from a signature file. That's how API diffing is performed: you load two
codebases (from whatever source you want, typically a previous API signature
file and the current set of source folders), and then you "diff" the two.

There are several key helpers that help with the implementation, detailed next.

### Visiting Items

First, metalava provides an ItemVisitor. This lets you visit the API easily.
For example, here's how you can visit every class:

    coebase.accept(object : ItemVisitor() {
        override fun visitClass(cls: ClassItem) {
            // code operating on the class here
        }
    })

Similarly you can visit all items (regardless of type) by overriding
`visitItem`, or to specifically visit methods, fields and so on
overriding `visitPackage`, `visitClass`, `visitMethod`, etc.

There is also an `ApiVisitor`. This is a subclass of the `ItemVisitor`,
but which limits itself to visiting code elements that are part of the
API.

This is how for example the SignatureWriter and the StubWriter are both
implemented: they simply extend `ApiVisitor`, which means they'll
only export the API items in the codebase, and then in each relevant
method they emit the signature or stub data:

    class SignatureWriter(
            private val writer: PrintWriter,
            private val generateDefaultConstructors: Boolean,
            private val filter: (Item) -> Boolean) : ApiVisitor(
            visitConstructorsAsMethods = false) {

    ....

    override fun visitConstructor(constructor: ConstructorItem) {
        writer.print("    ctor ")
        writeModifiers(constructor)
        writer.print(constructor.containingClass().fullName())
        writeParameterList(constructor)
        writeThrowsList(constructor)
        writer.print(";\n")
    }

    ....

### Visiting Types

There is a `TypeVisitor` similar to `ItemVisitor` which you can use
to visit all types in the codebase.

When computing the API, all types that are included in the API should be
included (e.g. if `List<Foo>` is part of the API then `Foo` must be too).
This is easy to do with the `TypeVisitor`.

### Diffing Codebases

Another visitor which helps with implementation is the ComparisonVisitor:

    open class ComparisonVisitor {
        open fun compare(old: Item, new: Item) {}
        open fun added(item: Item) {}
        open fun removed(item: Item) {}

        open fun compare(old: PackageItem, new: PackageItem) { }
        open fun compare(old: ClassItem, new: ClassItem) { }
        open fun compare(old: MethodItem, new: MethodItem) { }
        open fun compare(old: FieldItem, new: FieldItem) { }
        open fun compare(old: ParameterItem, new: ParameterItem) { }

        open fun added(item: PackageItem) { }
        open fun added(item: ClassItem) { }
        open fun added(item: MethodItem) { }
        open fun added(item: FieldItem) { }
        open fun added(item: ParameterItem) { }

        open fun removed(item: PackageItem) { }
        open fun removed(item: ClassItem) { }
        open fun removed(item: MethodItem) { }
        open fun removed(item: FieldItem) { }
        open fun removed(item: ParameterItem) { }
    }

This makes it easy to perform API comparison operations.

For example, metalava has a feature to mark "newly annotated" nullness annotations
as migrated. To do this, it just extends `ComparisonVisitor`, overrides the
`compare(old: Item, new: Item)` method, and checks whether the old item
has no nullness annotations and the new one does, and if so, also marks
the new annotations as @Migrate.

Similarly, the API Check can simply override

    open fun removed(item: Item) {
        reporter.report(error, item, "Removing ${Item.describe(item)} is not allowed")
    }

to flag all API elements that have been removed as invalid (since you cannot
remove API.)

### Documentation Generation

As mentioned above, metalava generates documentation directly into the stubs
files, which can then be processed by Dokka and Javadoc to generate the
same docs as before.

Doclava1 was integrated with javadoc directly, so the way it generated
metadata docs (such as documenting permissions, ranges and typedefs from
annotations) was to insert auxiliary tags (`@range`, `@permission`, etc) and
then this would get converted into English docs later via `macros_override.cs`.

This it not how metalava does it; it generates the English documentation
directly. This was not just convenient for the implementation (since metalava
does not use javadoc data structures to pass maps like the arguments for
the typedef macro), but should also help Dokka -- and arguably the Kotlin
code which generates the documentation is easier to reason about and to
update when it's handling loop conditionals. (As a result I for example
improved some of the grammar, e.g. when it's listing a number of possible
constants the conjunction is usually "or", but if it's a flag, the sentence
begins with "a combination of " and then the conjunction at the end should
be "and").

## Current Status

Some things are still missing before this tool can be integrated:

- doclava1 had various error checking, and many of these have not been included yet

- the code needs cleanup, and some performance optimizations (it's about 3x
  slower than doclava1)
