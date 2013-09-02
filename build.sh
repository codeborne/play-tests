#!/bin/bash
MODULE="selenide"
VERSION=`grep self conf/dependencies.yml | sed "s/.*$MODULE //"`

rm -fr dist
play dependencies --sync
play build-module
#scp dist/*.zip xp@codeborne.com:/var/www/repo/play-selenide/$MODULE-$VERSION.zip
cp dist/*.zip /var/www/repo/play-selenide/$MODULE-$VERSION.zip
