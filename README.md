# jmake

A simple build system for Java.

Note: Run from project root!

This build system requires a lib/ and src/ directory at the top level. The 
lib/ folder must contain all build time and run time dependencies. Because
Maven projects have a src/ folder, simply add the lib/ folder and copy in
the dependencies.

The goals are to:
* Make Java builds easier
* Share repo with all dependencies included and pinned to required versions
* Make it easier to update dependencies if vulnerabilities are found
* Build without Maven and all its dependencies
* Work with no network access
* Require nothing but OpenJDK

Build a Java library:
```
cd project-dir
jmake
```

Build a Java program:
```
cd project-dir
jmake package.MainClass
```

Build a Java library or program for release:
`jmake -r`  or `jmake -r package.MainClass`

Building a release requires that:
1. the project directory is a git repo and 
2. that it's status is clean.

An executable script is automatically produced if a
main class is provided to *jmake*.

*jmake* produces a jar file in the top level directory. Don't separate 
the jar file from its lib/ folder. Consider creating an alias for the
executable script in your terminal like this (in ~/.bashrc):

`alias program-name="path/to/progam-name"`

and then call it with just `program-name`.

*jmake* copies itself into the top-level directory to a file named *build.sh* 
so that the build instructions can live with the code. It also means that 
builds can be modified per project. Just call build.sh instead of jmake.

This script doesn't run tests (yet) or produce a build script for Windows.

This script does not clean out prior builds. You must manually delete the
generated bin/ folder to remove past build artifacts, old class files, etc.
