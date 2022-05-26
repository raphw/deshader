Agent that rewrites shaded packages to again use an unshaded package. This avoids duplicate class loading for multiple versions of the same shaded bundle:

Usage:

```cmd
java \
  -javaagent:/usr/bin/deshader.jar=some.lib.netty/io.netty,other.lib.netty/io.netty
  -cp netty.jar:somelib.jar:otherlib.jar:myapp.jar
  MyApp
```

It is the user's responsibility to verify that the unshaded versions are compatible to the specified version of the replaced library.
