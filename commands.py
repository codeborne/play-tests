import getopt
import sys
import os
import os.path
import subprocess
from exceptions import ValueError

MODULE = "play-tests"

COMMANDS = ["tests", "clean-tests", "unit-tests", "pitest", "itests", "ui-tests", "ui-tests-with-coverage", "compile-check", "code-analysis"]

HELP = {
    "tests": "Compile and run all tests",
    "clean-tests": "Clean compiled tests and test results",
    "unit-tests": "Run plain unit-tests",
    "pitest": "Mutation (unit-)tests",
    "itests": "Run integration tests (unit-tests that required play start - they cannot be run with usual unit-tests)",
    "ui-tests": "Run UI tests (in parallel)",
    "ui-tests-with-coverage": "Run UI tests with code coverage (slower, single thread, non-precompiled)",
    "compile-check": "Check that java sources are compilable",
    "code-analysis": "Check that java sources are good enough (currently only FindBugs check is implemented)"
}


def execute_gradle(app, args, threads_count, gradle_opts, *tasks):
    module_dir = os.path.dirname(os.path.realpath(__file__))
    gradle_cmd = ["bash",
                  "%s/gradle" % module_dir,
                  "-b", "%s/build.gradle" % module_dir,
                  "-p", app.path,
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

    uitest_class_pattern = 'ui/**'
    test_coverage_enabled = 'true'
    gradle_opts = []
    test_debug = False
    uitest_debug = False
    daemon = False
    threads_count = None
    optlist, args = getopt.getopt(args, '', ['uitest=', 'coverage=', 'threads=', 'daemon=', 'remote_debug=',
                                             'uitest.debug=', 'test.debug=', 'gradle_opts=', 'application.path='])
    for o, a in optlist:
        if o == '--uitest':
            uitest_class_pattern = a
            print "~ UI TEST: %s" % uitest_class_pattern
            print "~ "
        if o == '--coverage':
            test_coverage_enabled = 'true' if a.lower() in ['true', '1', 't', 'y', 'yes'] else 'false'
            print "~ COVERAGE: %s" % test_coverage_enabled
            print "~ "
        if o == '--threads':
            threads_count = a
            print "~ THREADS: %s" % threads_count
            print "~ "
        if o == '--gradle_opts':
            gradle_opts = a.split()
            print "~ GRADLE OPTS: %s" % gradle_opts
            print "~ "
        if o == '--application.path':
            gradle_opts += ['-Dapplication.path=%s' % a]
            print "~ GRADLE OPTS: %s" % gradle_opts
            print "~ "
        if o == '--remote_debug' or o == '--uitest.debug':
            uitest_debug = a.lower() in ['true', '1', 't', 'y', 'yes']
            print "~ UI Test DEBUG"
            print "~ "
        if o == '--test.debug':
            test_debug = a.lower() in ['true', '1', 't', 'y', 'yes']
            print "~ Test DEBUG"
            print "~ "
        if o == '--daemon':
            daemon = a.lower() in ['true', '1', 't', 'y', 'yes']
            print "~ DAEMON"
            print "~ "

    if test_debug:
        gradle_opts.append("-Dtest.debug=true")
    if uitest_debug:
        gradle_opts.append("-Duitest.debug=true")
    if daemon:
        gradle_opts.append('--daemon')

    if command == 'tests':
        execute_gradle(app, args, threads_count, gradle_opts, 'clean', 'check', 'test', 'itest', 'uitest', 'combineUiTestResults',
                      '-PUITEST_CLASS=%s' % uitest_class_pattern, '-PTEST_COVERAGE_ENABLED=%s' % test_coverage_enabled)
    elif command == 'clean-tests':
        execute_gradle(app, args, threads_count, gradle_opts, 'clean')
    elif command == 'unit-tests':
        execute_gradle(app, args, threads_count, gradle_opts, 'test', 
                       '-PTEST_COVERAGE_ENABLED=%s' % test_coverage_enabled)
    elif command == 'pitest':
        execute_gradle(app, args, threads_count, gradle_opts, 'pitest')
    elif command == 'clean-and-unit-tests':
        execute_gradle(app, args, threads_count, gradle_opts, 'clean', 'test', 
                       '-PTEST_COVERAGE_ENABLED=%s' % test_coverage_enabled)
    elif command == 'ui-tests':
        execute_gradle(app, args, threads_count, gradle_opts, 'uitest', 'combineUiTestResults', '-PUITEST_CLASS=%s' % uitest_class_pattern)
    elif command == 'ui-tests-with-coverage':
        execute_gradle(app, args, threads_count, gradle_opts, 'uitestWithCoverage',
                       '-PUITEST_CLASS=%s' % uitest_class_pattern)
    elif command == 'itests':
        execute_gradle(app, args, threads_count, gradle_opts, 'itest')
    elif command == 'compile-check':
        execute_gradle(app, args, threads_count, gradle_opts, 'testClasses')
    elif command == 'code-analysis':
        execute_gradle(app, args, threads_count, gradle_opts, 'check')
    else:
        raise ValueError("Unknown command: %s" % command)
