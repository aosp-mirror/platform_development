.class public LExample;

.super Ljava/lang/object;

.method public static <clinit>()V
  .locals 1

  const-string v0, "foo"
  invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V

  const-string v0, "bar"
  invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
.end method
