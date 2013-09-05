#!/bin/bash
# Functions for compiling and running of tests in your app

function modify_test_env() {
  # this can be overridden
  echo
}

function prepare_test_env() {
  TEST_CLASSPATH="lib/*:"
  TEST_CLASSES=`find app -name '*.java'`
  for m in `ls modules`; do
    if [ -d modules/$m ]; then
      TEST_CLASSPATH=$TEST_CLASSPATH:`pwd`/modules/$m/lib/*
      TEST_CLASSES="$TEST_CLASSES `find modules/$m/app -name '*.java' 2>/dev/null`"
    fi
    if [ -f modules/$m ]; then
      dir=`cat modules/$m`
      TEST_CLASSPATH=$TEST_CLASSPATH:$dir/lib/*
      TEST_CLASSES="$TEST_CLASSES `find $dir/app -name '*.java' 2>/dev/null`"
    fi
  done
  TEST_CLASSES="$TEST_CLASSES `find test -name '*.java'`"

  export TEST_CLASSPATH=$TEST_CLASSPATH:test:"$PLAY_HOME/framework/*:$PLAY_HOME/framework/lib/*"
  export TEST_CLASSES=$TEST_CLASSES

  modify_test_env
}

function compile_tests() {
  echo "Compiling tests..."
  prepare_test_env

  rm -fr test-classes && mkdir test-classes
  javac -g -source 1.7 -target 1.7 -cp $TEST_CLASSPATH $TEST_CLASSES -d test-classes || exit 1
}

function run_unit_tests() {
  echo "Running unit tests... "
  prepare_test_env
  mkdir -p test-result
  TESTS_FILE=`pwd`/test-result/unit-tests.txt
  if [ ! -e $TESTS_FILE ]; then
    find test-classes -name '*Test.class' | LC_ALL=C sort | sed 's@test-classes/@@; s@\.class$@@; s@/@.@g' | egrep -v '^(ui\.|play.test\.|realworld\.|controllers\.ControllerTest)' \
      | sed "s@\(.*\)@\1,$(pwd)/test-result,\1@" > $TESTS_FILE
  fi

  java -Xmx512m -XX:-UseSplitVerifier -cp test-classes:$TEST_CLASSPATH helpers.JenkinsTestRunner $TESTS_FILE || exit $?
  echo "Finished unit tests."
}

function run_ui_tests() {
  echo "Running UI tests... "
  prepare_test_env
  TESTS_FILE=`pwd`/test-result/unit-tests.txt
  if [ ! -e $TESTS_FILE ]; then
    find test-classes -name '*Spec.class' | LC_ALL=C sort | sed 's@test-classes/@@; s@\.class$@@; s@/@.@g' \
      | sed 's/\(.*\)/\1,test-result,\1/' > $TESTS_FILE
  fi
  java -Xmx512m -XX:MaxPermSize=128m -XX:-UseSplitVerifier -Dprecompiled=true -Dbrowser=chrome -Dselenide.reports=test-result -cp test-classes:$TEST_CLASSPATH helpers.JenkinsTestRunner $TESTS_FILE || exit $?
  echo "Finished UI tests."
}
