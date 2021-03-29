#!/bin/bash

URL=https://github.com/sbt/sbt/releases/download/v1.4.5/sbt-1.4.5.tgz
SBT=.sbt/sbt/bin/sbt

function getit() {
    echo "downloading ..."
    #curl -L $URL --output sbt.tgz
    #mkdir 
}

if [ \! -x .sbt/sbt/bin/sbt ]; then
    echo "downloading ..."
    rm -rf .sbt sbt.tgz
    curl -L $URL --output sbt.tgz
    mkdir -p .sbt
    (cd .sbt ; tar xvfz ../sbt.tgz)
    rm sbt.tgz
fi

exec $SBT "$@"
