import getopt
import sys
import os
import os.path
import shutil
import subprocess
from exceptions import ValueError

MODULE = "play-tests"

COMMANDS = ["tests", "clean-tests", "unit-tests", "itests", "ui-tests",         # The new parallel tests
            "tests1", "compile", "clean-tests1", "unit-tests1", "ui-tests1",    # The old home-made runner (deprecated)
            "tests2", "clean-tests2", "unit-tests2", "ui-tests2"                # Synonyms for new runner (deprecated)
            ]

HELP = {
    "tests": "Compile and run all tests",
    "clean-tests": "Clean compiled tests and test results",
    "unit-tests": "Run plain unit-tests",
    "itests": "Run integration tests (unit-tests that required play start - they cannot be run with usual unit-tests)",
    "ui-tests": "Run UI tests (in parallel)",
    "compile": "deprecated (not needed anymore)",
    "tests2": "deprecated (use play tests)",
    "clean-tests2": "deprecated (use play clean-tests)",
    "unit-tests2": "deprecated (use play unit-tests)",
    "ui-tests2": "deprecated (use play ui-tests)"
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


def run_tests(app, args, test_type, is_random_order=False, test_class_name=None):
    app.check()
    print "~ Running %s tests" % test_type

    _create_dir(test_result_dir(app))

    classpath = ':'.join(get_classpath(app))
    # print "CLASSPATH: %s" % string.join(classpath.split(':'), ",\n")

    java_args = [test_type, is_random_order]
    if test_class_name:
        java_args = [test_type, is_random_order, test_class_name]

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


def run_unit_tests(app, args, test_class_name, is_random_order):
    print "UNIT TESTS"
    run_tests(app, args, 'UNIT', is_random_order, test_class_name)


def run_ui_tests(app, args, test_class_name, is_random_order):
    print "UI TESTS"
    ui_args = ['-Dprecompiled=true', '-Dbrowser=chrome', '-Dselenide.reports=test-result',
               '-Djava.net.preferIPv4Stack=true',
               '-DBUILD_URL=%s' % os.environ.get('BUILD_URL', '')]
    run_tests(app, args + ui_args, 'UI', is_random_order, test_class_name)


def run_tests2(app, args, threads_count, gradle_opts, *tasks):
    module_dir = os.path.dirname(os.path.realpath(__file__))
    gradle_cmd = ["bash",
                  "%s/gradle" % module_dir,
                  "-b", "%s/build.gradle" % module_dir,
                  "-PPLAY_APP=%s" % app.path,
                  "-PPLAY_HOME=%s" % app.play_env["basedir"],
                  "-Dfile.encoding=UTF-8",
                  ] + list(tasks) + list(args) + gradle_opts
    if threads_count:
        gradle_cmd += ['-PTHREADS=%s' % threads_count]

    print "~ %s" % ' '.join(gradle_cmd)
    return_code = subprocess.call(gradle_cmd, env=os.environ)
    if 0 != return_code:
        print "~ %ss FAILED" % list(tasks)
        sys.exit(return_code)

    print "~ Executed %s successfully" % list(tasks)


def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")

    is_random_order = ''
    test_class_name = None
    uitest_class_pattern = 'ui/**'
    gradle_opts = []
    remote_debug = False
    daemon = False
    threads_count = None
    optlist, args = getopt.getopt(args, '', ['test=', 'uitest=', 'threads=', 'daemon=', 'remote_debug=', 'gradle_opts=', 'random='])
    for o, a in optlist:
        if o == '--test':  # deprecated
            test_class_name = a
            print "~ TEST CLASS: %s" % test_class_name
            print "~ "
        if o == '--uitest':
            uitest_class_pattern = a
            print "~ UI TEST: %s" % uitest_class_pattern
            print "~ "
        if o == '--threads':
            threads_count = a
            print "~ THREADS: %s" % threads_count
            print "~ "
        if o == '--gradle_opts':
            gradle_opts = a.split()
            print "~ GRADLE OPTS: %s" % gradle_opts
            print "~ "
        if o == '--remote_debug':
            remote_debug = a.lower() in ['true', '1', 't', 'y', 'yes']
            print "~ REMOTE DEBUG"
            print "~ "
        if o == '--daemon':
            daemon = a.lower() in ['true', '1', 't', 'y', 'yes']
            print "~ DAEMON"
            print "~ "
        if o == '--random':  # deprecated
            is_random_order = a
            if is_random_order.lower() == 'true':
                print "~ RANDOM ORDER"
                print "~ "

    if remote_debug:
        gradle_opts.append("-Duitest.debug=true")
    if daemon:
        gradle_opts.append('--daemon')

    if command == 'tests' or command == 'tests2':
        run_tests2(app, args, threads_count, gradle_opts, 'clean', 'test', 'itest', 'uitest', '-PUITEST_CLASS=%s' % uitest_class_pattern)
    elif command == 'clean-tests' or command == 'clean-tests2':
        run_tests2(app, args, threads_count, gradle_opts, 'cleanTest')
    elif command == 'unit-tests' or command == 'unit-tests2':
        run_tests2(app, args, threads_count, gradle_opts, 'test')
    elif command == 'ui-tests' or command == 'ui-tests2':
        run_tests2(app, args, threads_count, gradle_opts, 'uitest', '-PUITEST_CLASS=%s' % uitest_class_pattern)
    elif command == 'itests':
        run_tests2(app, args, threads_count, gradle_opts, 'itest')

    # Deprecated commands:
    elif command == 'tests1':
        clean_tests(app)
        compile_sources(app, args)
        run_unit_tests(app, args, test_class_name, is_random_order)
        run_ui_tests(app, args, test_class_name, is_random_order)
    elif command == 'compile':
        compile_sources(app, args)
    elif command == 'clean-tests1':
        clean_tests(app)
    elif command == 'unit-tests1':
        run_unit_tests(app, args, test_class_name, is_random_order)
    elif command == 'ui-tests1':
        run_ui_tests(app, args, test_class_name, is_random_order)
    else:
        raise ValueError("Unknown command: %s" % command)
