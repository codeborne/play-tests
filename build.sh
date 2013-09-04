#!/bin/bash
MODULE="selenide"
VERSION=`grep self conf/dependencies.yml | sed "s/.*$MODULE //"`

rm -fr dist
play dependencies --sync
play build-module
#scp dist/*.zip codeborne.com:/var/www/repo/play-$MODULE/$MODULE-$VERSION.zip

TARGET=/var/www/repo/play-$MODULE/$MODULE-$VERSION.zip
if [ -e $TARGET ]; then
    echo "Not publishing, $VERSION already exists"
else
    cp dist/*.zip $TARGET
fi
