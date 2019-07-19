#!/bin/bash
# Abort on Error
set -e

export PING_SLEEP=30s
#export WORKDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#export BUILD_OUTPUT=$WORKDIR/build.out

#touch $BUILD_OUTPUT

#dump_output() {
#   echo Tailing the last 500 lines of output:
#   #tail -500 $BUILD_OUTPUT
#   cat $BUILD_OUTPUT
#}

error_handler() {
  [ -e target/rat.txt ] && cat target/rat.txt
  echo
  echo "-------------------------------------------------------"
  echo ERROR: An error was encountered with the build.
  #dump_output

  echo ERROR: Test results:
  find . -name 'Surefire test.xml' -exec cat {} \;
  #find . -name 'Command line test.xml' -exec cat {} \;


  # nicely terminate the ping output loop
  kill $PING_LOOP_PID
  exit 1
}
# If an error occurs, run our error handler to output a tail of the build
trap 'error_handler' ERR

# Set up a repeating loop to send some output to Travis.
bash -c "while true; do echo \$(date) - building ...; sleep $PING_SLEEP; done" &
export PING_LOOP_PID=$!

# My build is using maven, but you could build anything with this, E.g.
# your_build_command_1
# your_build_command_2
export _JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true
export LUMEER_HOME=$(pwd)/war
export LUMEER_DEFAULTS=defaults-ci.properties
export SKIP_SECURITY=true
mvn -Ptests install -B -Dlumeer.db.embed.skip=true
#mvn -P-default install -Dlumeer.db.host=ds119508.mlab.com -Dlumeer.db.port=19508 -Dlumeer.db.name=lumeer-ci
#mvn -l $BUILD_OUTPUT -P-default install -Dlumeer.db.host=ds119508.mlab.com -Dlumeer.db.port=19508 -Dlumeer.db.name=lumeer-ci

# The build finished without returning an error so dump a tail of the output
#dump_output

# nicely terminate the ping output loop
kill $PING_LOOP_PID