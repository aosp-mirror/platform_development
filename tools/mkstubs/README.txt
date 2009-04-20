2009/04/20.

-------
1- Goal
-------

MkStub is small tool that takes a given JAR and filters all the private stuff we don't want to
expose, e.g.:
- remove all private members.
- only include a subset of classes.
- exclude specific classes, fields or methods.

Each method body is replaced by the bytecode for 'throw new RuntimeException("stub");'.


--------
2- Usage
--------

To control it, you give it patterns like this:

  +foo  => accepts all items which signature is exactly "foo"
  +foo* => accepts all items which signature starts by "foo"
  -bar  => rejects all items which signature is exactly "bar"
  -bar* => rejects all items which signature starts by "bar"

Signatures are defined by:
- a package name, e.g. com.android.blah
- a dot followed by a class name
- a # followed by a field or method name
- an internal "Java method signature" that define parameters types and return value. 

Examples of signatures:
 com.android.blah
 com.android.blah.MyClass
 com.android.blah.MyClass$MyInnerClass
 com.android.blah.MyClass#mPrivateField
 com.android.blah.MyClass#getInternalStuff
 com.android.blah.MyClass#getInternalStuff(Ljava/lang/String;I)V

An example of configuration file:
 +com.android.blah
 -com.android.blah.MyClass$MyInnerClass
 -com.android.blah.MyClass#mPrivateField
 -com.android.blah.MyClass#getInternalStuff(Ljava/lang/String;I)V

This would include only the indicated package yet would totally exclude the inner class
and the specific field and the method with the exact given signature.



To invoke MkStub, the syntax is:

  $ java -jar mkstubs input.jar output.jar [@configfile -pattern +pattern ...]
    


--------------------
3- Known Limitations
--------------------

Most of the following limitations exist solely because of the short development time and
because the tool was designed to solve one task and not just to be super generic. That means
any limitation here can be easily lifted.

- The generated constructors are not proper. They do not invoke the matching super()
  before the generated throw exception. Any attempt to load such a class should trigger
  an error from the byte code verifier or the class loader.

- We do not currently check whether a class or method uses only included types.
  Suggestion: if type x.y.z is excluded, then any field, annotation, generic type,
  method parameter or return value that uses that type should generate a fatal error.

- We do not filter out private classes. Their .class will still be present in the
  output (and stubbed), unless they are explicitly excluded.
  This is not orthogonal to the fact that private fields and methods are automatically
  excluded.

- Private fields and methods are automatically excluded. There is no command line
  switch to prevent that.

- The stubbed source is always generated. For example if the output jar name is
  given as ~/somedir/myfinal.jar, there will be a directory created at
  ~/somedir/myfinal.jar_sources that will contain the equivalent Java sources.
  There is not command line switch to prevent that.

- There is no attempt to match features or behavior with DroidDoc.

-- 
end
