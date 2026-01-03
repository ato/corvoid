corvoid
=======

Corvoid is a small build tool for Java projects. It implements a limited but
useful subset of the Apache Maven POM format.  It can resolve and download
dependencies then compile and package a project into jar or uberjar file.

Corvoid has no dependencies beyond JDK 8+ and can be packaged as a
single 100 KB jar.

Usage
-----

See the [manpage](doc/corvoid.adoc).

Motivation
----------

I wrote Corvoid one day when frustrated with Maven's slow startup time
and verbose command line interface. I figured that surely the
process of resolving dependencies wasn't that complex.

Over time I've added little bits of useful functionality like watching
the source tree for changes, showing the sizes of dependencies and
reporting on duplicate class files. All of these can be achieved
by cobbling together enough Maven plugins, but Corvoid does them
out of the box, often faster and with a more discoverable CLI.

Corvoid is a personal tool. It has a number of rough edges but I find it
handy. In practice I mix and match Corvoid and Maven using one or the other
for particular tasks within the same project.

Installation
------------

For easy development, Corvoid can be run directly out of the source checkout.
It requires no dependencies other than a JDK. You can symlink the launch
script somewhere in your `$PATH`:

    ln -s $PWD/bin/corvoid /usr/local/bin

Alternatively you can package it as a jar and create a launch script:

    ./bin/corvoid jar
    cp target/corvoid-*.jar /usr/local/lib

    cat > /usr/local/bin/corvoid <<EOF
    #!/bin/sh
    exec java -cp /usr/local/lib/corvoid-*.jar corvoid.Corvoid "$@"
    EOF

    chmod +x /usr/local/bin/corvoid

To build and install the manpages (requires `a2x` from [asciidoc]):

    ./doc/install-manpages.sh

[asciidoc]: http://www.methods.co.nz/asciidoc/