# jmake

A simple build system for Java.

Note: Run from project root!

This build system requires a lib/ and src/ directory at the top level. The 
lib/ folder must contain all build time and run time dependencies. The src/
folder must contain all the sources. Because most projects (like Maven projects)
have a src/ folder, using *jmake* often means simply creating a lib/ folder 
and copying all jar dependencies into it.

The goals are to:
* Make Java builds easier
* Share repo with all dependencies included and pinned to required versions
* Make it easier to update dependencies if vulnerabilities are found
* Build without Maven and all its dependencies
* Work with no network access
* Require nothing but OpenJDK

Build a Java library:

```
cp path/to/Jmake.java project-dir
cd project-dir
java Jmake.java
```

Build a Java program:

```
cp path/to/Jmake.java project-dir
cd project-dir
java Jmake.java package.MainClass
```

An executable script is automatically produced if a
main class is provided to *jmake*.

*jmake* produces a jar file in the bin/ directory. The executable script expects
the jar file and dependencies to be in the bin/ and lib/ folder, respectively.
To make running the program easier, consider creating an alias for the
executable script in your terminal like this (in ~/.bashrc):

`alias program-name="path/to/progam-name"`

and then call it with just `program-name`.

By placing *Jmake.java* in the top-level directory the build instructions 
can live with the code. It also means that builds can be modified per project.

*jmake* runs tests a little bit differently than Java programmers may
be used to. *jmake* expects all tests to be in the _main_ method (in `public static
void main(String[] args)`) and to use Java assertions 
(https://docs.oracle.com/javase/8/docs/technotes/guides/language/assert.html). 
This setup is nice because:

* all tests live conveniently with the class itself so that the usage
can be understood and copied easily into new projects,
* and because no additional dependencies are required. 

While not as capable as *junit*, all tests are now run from about 34 lines of code.
