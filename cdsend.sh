#!/bin/bash
BASEDIR=$(dirname $0)
java -cp $BASEDIR/target/Cloudino-engine-1.0-SNAPSHOT.jar io.cloudino.utils.HexSender $1 $2 $3
