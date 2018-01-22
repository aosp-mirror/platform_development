.class public LHello;

.super Ljava/lang/object;

.method public static <clinit>()V
  .locals 1

  const-string v0, "hello"
  invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V

  const-string v0, "world"
  invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
.end method
