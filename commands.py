# Secure
import string
import re
import getopt
import sys
import os
import os.path
import shutil
from shutil import copytree, ignore_patterns
import subprocess


MODULE = "selenide"

COMMANDS = ["compile", "unit-tests", "ui-tests"]

HELP = {
    "compile": "Compile all the tests with Java code",
    "unit-tests": "Run plain unit-tests",
    "ui-tests": "Run UI tests"
}


def prepare_output_dir(app):
    outdir = os.path.join(app.path, 'test-classes')
    if os.path.isdir(outdir):
        shutil.rmtree(outdir)
    os.mkdir(outdir)
    return outdir


def prepare_test_result_dir(app):
    outdir = os.path.join(app.path, 'test-result')
    if os.path.isdir(outdir):
        shutil.rmtree(outdir)
    os.mkdir(outdir)
    return outdir


def compile_sources(app, args):
    outdir = prepare_output_dir(app)

    javac_cmd = app.java_cmd(args, cp_args=app.cp_args(), className='play.test.Compiler')
    print "Compiling Java sources to %s ..." % outdir
    return_code = subprocess.call(javac_cmd, env=os.environ)
    if 0 != return_code:
        print "Compilation FAILED"
        sys.exit(return_code)

    print "Compilation successful"


def get_classpath(app):
    classpath = [os.path.join(app.path, 'test-classes')]

    # The default
    # classpath.append(os.path.normpath(os.path.join(app.path, 'conf')))
    classpath.append(app.agent_path())

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

def run_tests(app, args, test_type):
    app.check()
    print "~ Running %s tests" % test_type
    print "~ app = %s" % app
    print "~ args = %s" % args

    prepare_test_result_dir(app)

    classpath = ':'.join(get_classpath(app))
    # print "CLASSPATH: %s" % string.join(classpath.split(':'), ",\n")

    java_cmd = app.java_cmd(args, cp_args=classpath, className='play.test.JUnitRunnerWithXMLOutput', args=[test_type])
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


def run_unit_tests(app, args, include, exclude):
    print "UNIT TESTS"
    run_tests(app, args, 'UNIT')


def run_ui_tests(app, args, include, exclude):
    print "UI TESTS"
    ui_args = ['-Dprecompiled=true', '-Dbrowser=chrome', '-Dselenide.reports=test-result']
    run_tests(app, args + ui_args, 'UI')


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

    # compile_sources(app, args)
    if command == 'compile':
        compile_sources(app, args)
    elif command == 'unit-tests':
        run_unit_tests(app, args, include, exclude)
    elif command == 'ui-tests':
        run_ui_tests(app, args, include, exclude)
    else:
        raise ValueError("Unknown command: %s" % command)
