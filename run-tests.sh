#!/bin/bash
# Abort on Error
set -e

export _JAVA_OPTIONS="-Djava.net.preferIPv4Stack=true"
export LUMEER_HOME=$(pwd)/war
export LUMEER_DEFAULTS=defaults-ci.properties
export SKIP_SECURITY=true
mvn -Ptests install -B -Dlumeer.db.embed.skip=true
