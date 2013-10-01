# Secure

import re
import getopt
import sys, os, os.path
import shutil
from shutil import copytree, ignore_patterns
import subprocess

MODULE = "selenide"

COMMANDS = ["unit-test", "ui-test"]

HELP = {
    "unit-test": "Run plain unit-tests",
    "ui-test": "Run UI tests"
}


def javac_path():
    if os.environ.has_key('JAVA_HOME'):
        return os.path.normpath("%s/bin/javac" % os.environ['JAVA_HOME'])
    else:
        return "javac"


def prepare_output_dir(app):
    outdir = os.path.join(app.path, 'test-classes')
    if os.path.isdir(outdir):
        shutil.rmtree(outdir)
    # os.mkdir(outdir)
    return outdir


def prepare_test_result_dir(app):
    outdir = os.path.join(app.path, 'test-result')
    if os.path.isdir(outdir):
        shutil.rmtree(outdir)
    os.mkdir(outdir)
    return outdir


def java_files(relative_path, source_folder, suffix='.java'):
    path = os.path.normpath(os.path.join(relative_path, source_folder))

    java_sources = []
    for root, subFolders, files in os.walk(path):
        for file in files:
            if file.endswith(suffix):
                java_sources.append(os.path.join(root, file))
    return java_sources


def compile(app, args, command):
    java_sources = java_files(app.path, "app") + \
                   java_files(app.path, "src") + \
                   java_files(app.path, "test")
    for module in app.modules():
        java_sources += java_files(module, "app")
        java_sources += java_files(module, "src")
        if command == 'ui-test':
            java_sources += java_files(module, "test", "Spec.java")
    outdir = prepare_output_dir(app)
    copytree(os.path.join(app.path, "test"), outdir, ignore=ignore_patterns('*.java'))

    javac_args = ['-g', '-classpath', app.cp_args(), '-d', outdir] + args + java_sources
    with open(os.path.join(outdir, 'javac.params'), 'w') as f:
        f.write("\n".join(javac_args))
    javac_cmd = [javac_path(), '@%s' % os.path.join(outdir, 'javac.params')]
    print "Compiling %d Java sources to %s ..." % (len(java_sources), outdir)
    return_code = subprocess.call(javac_cmd, env=os.environ)
    if 0 != return_code:
        print "Compilation FAILED"
        sys.exit(return_code)

    print "Compilation successful"


def run_tests(command, app, args, test_classes):
    app.check()
    print "~ Running %d tests" % len(test_classes)

    test_result = prepare_test_result_dir(app)
    tests_file=os.path.join(test_result, '%s.txt' % command)
    with open(tests_file, 'w') as f:
        for test_class in test_classes:
            f.write("%s,test-result,%s\n" % (test_class, test_class))

    classpath = "%s:%s" % (app.cp_args(), os.path.join(app.path, 'test-classes'))
    print "CLASSPATH: %s" % classpath

    java_cmd = app.java_cmd(args, cp_args=classpath, className='play.test.JUnitRunnerWithXMLOutput', args=[tests_file])
    print 'RUNNING: %s' % java_cmd
    return_code = subprocess.call(java_cmd, env=os.environ)

    if 0 != return_code:
        print "Tests FAILED"
        sys.exit(return_code)

    print "Executed %d tests successfully" % len(test_classes)


def test_classes(relative_path, source_folder, include, exclude):
    path = os.path.normpath(os.path.join(relative_path, source_folder))

    tests = []
    for root, subFolders, files in os.walk(path):
        for file in files:
            file_path = "%s/%s" % (os.path.relpath(root, path), file)
            if re.search(include, file_path) and not re.search(exclude, file_path):
                (class_name, ext) = os.path.splitext(file_path)
                tests.append(class_name.replace('/', '.'))
    return tests


def run_unit_tests(command, app, args, include, exclude):
    include = include or ".*Test\.java"
    exclude = exclude or "Abstract.*\.java|ui\/.*"

    tests = test_classes(app.path, "test", include, exclude)
    print "UNIT TESTS"
    run_tests(command, app, args, tests)


def run_ui_tests(command, app, args, include, exclude):
    include = include or ".*Spec\.java"
    exclude = exclude or "Abstract.*\.java"
    tests = test_classes(app.path, "test", include, exclude)
    for module in app.modules():
        tests += test_classes(module, "test", include, exclude)

    print "UI TESTS"
    run_tests(command, app, args, tests)


def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")

    include = None
    exclude = None
    optlist, args = getopt.getopt(args, '', ['include=', 'exclude='])
    for o, a in optlist:
        if o == '--include':
            include = a
            print "INCLUDE %s" % include
            print "~ "
        if o == '--exclude':
            exclude = a
            print "EXCLUDE %s" % exclude
            print "~ "

    compile(app, args, command)
    if command == 'ui-test':
        run_ui_tests(command, app, args, include, exclude)
    else:
        run_unit_tests(command, app, args, include, exclude)
