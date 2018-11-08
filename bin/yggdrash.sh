#!/usr/bin/env bash

echo "================================================================================"

if [ -n $YGGDRASH_HOME ]
then
    export YGGDRASH_HOME="$PWD/.."
fi

HOME=$YGGDRASH_HOME
BIN=$HOME/bin
PID=$HOME/bin/yggdrash.pid
LOG=$HOME/logs/yggdrash.log
ERROR=$HOME/logs/error.log

CMD="yggdrash-node.jar"
COMMAND="$BIN/$CMD"
EXEFILE="$YGGDRASH_HOME/yggdrash-node/build/libs/yggdrash-node.jar"


status() {
    echo "YGGDARSH Status"

    if [ -f $PID ]
    then
        echo "Pid file: $( cat $PID ) [$PID]"
        ps -ef | grep -v grep | grep $( cat $PID )
    else
        echo "YGGDRASH node is not started."
    fi
}

start() {

    if [ ! -f $EXEFILE ]
    then
        echo "JAR:$EXEFILE"
        cd $YGGDRASH_HOME
        echo $PWD
        ./gradlew -PspringProfiles=prod clean build
        cd $BIN
    fi

    if [ -f $PID ]
    then
        echo "Already started. PID: [$( cat $PID )]"
    else
        echo "YGGDRASH Start"
        touch $PID
        if nohup $COMMAND >>$LOG 2>&1 &
        then echo $! >$PID
            echo "Started the YGGDRASH node.($PID)"
            echo "$(date '+%Y-%m-%d %X'): START" >>$LOG
            echo "Log file: $LOG"
        else echo "Error... "
            /bin/rm $PID
        fi
    fi
}

kill_cmd() {
    SIGNAL=""; MSG="Killing "
    while true
    do
        LIST=`ps -ef | grep -v grep | grep $CMD | grep -w $USER | awk '{print $2}'`
        if [ "$LIST" ]
        then
            echo "$MSG $LIST"
            echo $LIST | xargs kill $SIGNAL
            sleep 2
            SIGNAL="-9" ; MSG="Killing $SIGNAL"
            if [ -f $PID ]
            then
                /bin/rm $PID
            fi
        else
           echo "All killed...";
           break
        fi
    done
}

stop() {
    echo "YGGDRASH Stop"

    if [ -f $PID ]
    then
        if kill $( cat $PID )
        then echo "Stopping the YGGDRASH node."
             echo "$(date '+%Y-%m-%d %X'): STOP" >>$LOG
        fi
        /bin/rm $PID
        kill_cmd
    else
        echo "No pid file. Already stopped?"
    fi
}


case "$1" in
    'start')
            start
            ;;
    'stop')
            stop
            ;;
    'restart')
            stop ; echo "Sleeping..."; sleep 1 ;
            start
            ;;
    'status')
            status
            ;;
    *)
            echo
            echo "Usage: $0 { start | stop | restart | status }"
            echo
            exit 1
            ;;
esac

echo "================================================================================"

exit 0
