#!/bin/bash --login

rm -vrf bin/*
find src/ -name '*.java' | xargs javac -classpath 'lib/commons-cli-1.2.jar:lib/dom4j-1.6.jar:lib/mysql-connector-java-5.1.25-bin.jar' -d bin
pushd bin
jar -cvfm ../sspbench.jar ../MANIFEST.MF $(find . -type f)
popd

