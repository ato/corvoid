#!/bin/sh
mkdir -p target/classes
javac -d target/classes -sourcepath src src/corvoid/Corvoid.java
java -cp target/classes corvoid.Corvoid uberjar
