#!/bin/bash
# Functions for compiling and running of tests in your app

function is_not_ibank() {
  [ $IBANK != . ]
  return $?
}

function prepare_test_env() {
  CLASSPATH="lib/*:$TARGET/play/framework/*:$TARGET/play/framework/lib/*"
  CLASSES=`find app -name '*.java'`
  for m in `ls modules`; do
    if [ -d modules/$m ]; then
      CLASSPATH=$CLASSPATH:`pwd`/modules/$m/lib/*
      CLASSES="$CLASSES `find modules/$m/app -name '*.java' 2>/dev/null`"
    fi
    if [ -f modules/$m ]; then
      dir=`cat modules/$m`
      CLASSPATH=$CLASSPATH:$dir/lib/*
      CLASSES="$CLASSES `find $dir/app -name '*.java' 2>/dev/null`"
    fi
  done
  CLASSES="$CLASSES `find test -name '*.java'`"

  if is_not_ibank; then
    CLASSES="$CLASSES `find $IBANK/test -name '*.java'`"
  fi

  export CLASSPATH
  export CLASSES
}

function compile_tests() {
  echo "Compiling tests..."
  prepare_test_env

  rm -fr test-classes && mkdir test-classes
  javac -g -source 1.7 -target 1.7 -cp $CLASSPATH $CLASSES -d test-classes || exit 1
}

function run_unit_tests() {
  echo "Running unit tests... "
  prepare_test_env
  mkdir -p test-result
  TESTS_FILE=`pwd`/test-result/unit-tests.txt
  find test-classes -name '*Test.class' | LC_ALL=C sort | sed 's@test-classes/@@; s@\.class$@@; s@/@.@g' | egrep -v '^(ui\.|play.test\.|realworld\.|controllers\.ControllerTest)' \
    | sed "s@\(.*\)@\1,$(pwd)/test-result,\1@" > $TESTS_FILE
  TEST_CLASSPATH=$APPDIR/test-classes:$APPDIR/test:test:$CLASSPATH

  cd $IBANK
  java -Xmx512m -XX:-UseSplitVerifier -cp $TEST_CLASSPATH helpers.JenkinsTestRunner $TESTS_FILE || exit 666
  cd -

  echo "Finished unit tests."
}

function run_ui_tests() {
  echo "Running UI tests... "
  prepare_test_env
  TEST_CLASSPATH=$APPDIR/test-classes:$APPDIR/test:test:$CLASSPATH
  java -Xmx512m -XX:MaxPermSize=128m -XX:-UseSplitVerifier -Dprecompiled=true -Dbrowser=chrome -Dselenide.reports=test-result -cp $TEST_CLASSPATH helpers.JenkinsTestRunner test-result/ui-tests.txt || exit $?
  echo "Finished UI tests."
}
