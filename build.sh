#!/bin/sh

set -eu

CWD=$(pwd)

(cd native/ && git submodule update --init --recursive)
(cd $CWD/native/ && ./compile-native.sh)
(cd $CWD && mvn clean install)
(mkdir $CWD/test_run/ && cd $CWD/test_run/ && cp ../XenonCord.jar ./)
