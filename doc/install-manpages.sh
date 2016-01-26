#!/bin/sh

set -e

cd "$(dirname "$0")"

mkdir -p ../target/man

for doc in *.adoc; do
  a2x --format manpage -D ../target/man $doc
done

sudo cp -v ../target/man/*.5 /usr/local/share/man/man5
sudo cp -v ../target/man/*.1 /usr/local/share/man/man1
sudo mandb
