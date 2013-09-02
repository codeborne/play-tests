#!/bin/bash
rm -fr dist
play dependencies --sync
play build-module
#scp dist/play-selenide-*.zip xp@codeborne.com:/var/www/repo/play-selenide/
cp dist/play-selenide-*.zip /var/www/repo/play-selenide/
