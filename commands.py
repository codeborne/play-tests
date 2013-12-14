# Secure
import getopt
import sys
import os
import os.path
import shutil
import subprocess


MODULE = "selenide"

COMMANDS = ["tests", "compile", "clean-tests", "unit-tests", "ui-tests"]

HELP = {
    "tests": "Compile and run all tests",
    "compile": "Compile all the tests with Java code",
    "clean-tests": "Clean compiled tests and test results",
    "unit-tests": "Run plain unit-tests",
    "ui-tests": "Run UI tests"
}


def _create_dir(f):
    if not os.path.isdir(f):
        os.mkdir(f)
    return f


def _remove_dir(f):
    print("Cleanup %s" % f)
    if os.path.isdir(f):
        shutil.rmtree(f)


def _classes_dir(app):
    return os.path.join(app.path, 'test-classes')


def test_result_dir(app):
    return os.path.join(app.path, 'test-result')


def clean_tests(app):
    _remove_dir(_classes_dir(app))
    _remove_dir(test_result_dir(app))


def compile_sources(app, args):
    classes_dir = _create_dir(_classes_dir(app))

    javac_cmd = app.java_cmd(args, cp_args=app.cp_args(), className='play.test.Compiler')
    print "Compiling Java sources to %s ..." % classes_dir
    return_code = subprocess.call(javac_cmd, env=os.environ)
    if 0 != return_code:
        print "Compilation FAILED"
        sys.exit(return_code)

    print "Compilation successful"


def get_classpath(app):
    classpath = [_classes_dir(app), app.agent_path()]

    # The application - recursively add jars to the classpath inside the lib folder to allow for subdirectories
    if os.path.exists(os.path.join(app.path, 'lib')):
        app.find_and_add_all_jars(classpath, os.path.join(app.path, 'lib'))

    # The modules
    for module in app.modules():
        if os.path.exists(os.path.join(module, 'lib')):
            libs = os.path.join(module, 'lib')
            if os.path.exists(libs):
                for jar in os.listdir(libs):
                    if jar.endswith('.jar'):
                        classpath.append(os.path.normpath(os.path.join(libs, '%s' % jar)))

    # The framework
    for jar in os.listdir(os.path.join(app.play_env["basedir"], 'framework/lib')):
        if jar.endswith('.jar'):
            classpath.append(os.path.normpath(os.path.join(app.play_env["basedir"], 'framework/lib/%s' % jar)))

    return classpath


def run_tests(app, args, test_type, test_class_name=None):
    app.check()
    print "~ Running %s tests" % test_type

    _create_dir(test_result_dir(app))

    classpath = ':'.join(get_classpath(app))
    # print "CLASSPATH: %s" % string.join(classpath.split(':'), ",\n")

    java_args=[test_type]
    if test_class_name:
        java_args = [test_type, test_class_name]

    java_cmd = app.java_cmd(args, cp_args=classpath, className='play.test.JUnitRunnerWithXMLOutput', args=java_args)
    # print 'RUNNING: %s' % java_cmd
    # print ""
    # print ""
    # print ""
    # print ""
    return_code = subprocess.call(java_cmd, env=os.environ)

    if 0 != return_code:
        print "Tests FAILED"
        sys.exit(return_code)

    print "Executed %s tests successfully" % test_type


def run_unit_tests(app, args, test_class_name, include, exclude):
    print "UNIT TESTS"
    run_tests(app, args, 'UNIT', test_class_name)


def run_ui_tests(app, args, test_class_name, include, exclude):
    print "UI TESTS"
    ui_args = ['-Dprecompiled=true', '-Dbrowser=chrome', '-Dselenide.reports=test-result']
    run_tests(app, args + ui_args, 'UI', test_class_name)


def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")

    include = None
    exclude = None
    test_class_name = None
    optlist, args = getopt.getopt(args, '', ['include=', 'exclude=', 'test='])
    for o, a in optlist:
        if o == '--include':
            include = a
            print "INCLUDE: %s" % include
            print "~ "
        if o == '--exclude':
            exclude = a
            print "EXCLUDE: %s" % exclude
            print "~ "
        if o == '--test':
            test_class_name = a
            print "TEST CLASS: %s" % exclude
            print "~ "

    if command == 'tests':
        clean_tests(app)
        compile_sources(app, args)
        run_unit_tests(app, args, test_class_name, include, exclude)
        run_ui_tests(app, args, test_class_name, include, exclude)
    elif command == 'compile':
        compile_sources(app, args)
    elif command == 'clean-tests':
        clean_tests(app)
    elif command == 'unit-tests':
        run_unit_tests(app, args, test_class_name, include, exclude)
    elif command == 'ui-tests':
        run_ui_tests(app, args, test_class_name, include, exclude)
    else:
        raise ValueError("Unknown command: %s" % command)
