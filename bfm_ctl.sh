if [ ! -z $1 ] && [ $1 == "-h" ]; then
    echo "-h help"
    echo "-u username"
    echo "-p password"
    echo "-status show cluster status"
    echo "-pause cluster check pause"
    echo "-resume cluster check resume"
    echo "-watchMode [A|M] set watch strategy  A:availability, M:manual"
    echo "-switchOver 192.168.1.22:5432 switch to selected slave"
fi

if [ ! -z $1 ] && [ ! -z $2 ] && [ $1 == "-u" ]; then
    export clsUser=$2
fi

if [ ! -z $1 ] && [ ! -z $2 ] && [ $1 == "-p" ]; then
    export clsPwd=$2
fi

if [ ! -z $1 ] && [ $1 == "-status" ]; then
    export clsCommand="status"
fi

if [ ! -z $1 ] && [ $1 == "-pause" ]; then
    export clsCommand="pause"
fi

if [ ! -z $1 ] && [ $1 == "-resume" ]; then
    export clsCommand="resume"
fi

if [ ! -z $1 ]  && [ ! -z $2 ] && [ $1 == "-switchOver" ]; then
    export clsCommand="switchOver"
    export targetSlave=$2
fi

if [ ! -z $1 ]  && [ ! -z $2 ] && [ $1 == "-watchMode" ]; then
    export clsCommand="strategy"
    export clsStrategy=$2
fi

if [ ! -z $2 ] && [ ! -z $3 ] && [ $2 == "-u" ]; then
    export clsUser=$3
fi

if [ ! -z $2 ] && [ ! -z $3 ] && [ $2 == "-p" ]; then
    export clsPwd=$3
fi

if [ ! -z $2 ] && [ $2 == "-status" ]; then
    export clsCommand="status"
fi

if [ ! -z $2 ] && [ $2 == "-pause" ]; then
    export clsCommand="pause"
fi

if [ ! -z $2 ] && [ $2 == "-resume" ]; then
    export clsCommand="resume"
fi

if [ ! -z $2 ]  && [ ! -z $3 ] && [ $2 == "-switchOver" ]; then
    export clsCommand="switchOver"
    export targetSlave=$3
fi

if [ ! -z $2 ]  && [ ! -z $3 ] && [ $2 == "-watchMode" ]; then
    export clsCommand="strategy"
    export clsStrategy=$3
fi

if [ ! -z $3 ] && [ ! -z $4 ] &&[ $3 == "-u" ]; then
    export clsUser=$4
fi

if [ ! -z $3 ] && [ ! -z $4 ] && [ $3 == "-p" ]; then
    export clsPwd=$4
fi

if [ ! -z $3 ] && [ $3 == "-status" ]; then
    export clsCommand="status"
fi

if [ ! -z $3 ] && [ $3 == "-pause" ]; then
    export clsCommand="pause"
fi

if [ ! -z $3 ] && [ $3 == "-resume" ]; then
    export clsCommand="resume"
fi

if [ ! -z $3 ]  && [ ! -z $4 ] && [ $3 == "-switchOver" ]; then
    export clsCommand="switchOver"
    export targetSlave=$4
fi

if [ ! -z $3 ]  && [ ! -z $4 ] && [ $3 == "-watchMode" ]; then
    export clsCommand="strategy"
    export clsStrategy=$4
fi

if [ ! -z $4 ] && [ $4 == "-status" ]; then
    export clsCommand="status"
fi

if [ ! -z $4 ] && [ $4 == "-pause" ]; then
    export clsCommand="pause"
fi

if [ ! -z $4 ] && [ $4 == "-resume" ]; then
    export clsCommand="resume"
fi

if [ ! -z $4 ]  && [ ! -z $5 ] && [ $4 == "-switchOver" ]; then
    export clsCommand="switchOver"
    export targetSlave=$5
fi

if [ ! -z $4 ]  && [ ! -z $5 ] && [ $4 == "-watchMode" ]; then
    export clsCommand="strategy"
    export clsStrategy=$5
fi

if [ ! -z $5 ] && [ $5 == "-status" ]; then
    export clsCommand="status"
fi

if [ ! -z $5 ] && [ $5 == "-pause" ]; then
    export clsCommand="pause"
fi

if [ ! -z $5 ] && [ $5 == "-resume" ]; then
    export clsCommand="resume"
fi

if [ ! -z $5 ]  && [ ! -z $6 ] && [ $5 == "-switchOver" ]; then
    export clsCommand="switchOver"
    export targetSlave=$6
fi

if [ ! -z $5 ]  && [ ! -z $6 ] && [ $5 == "-watchMode" ]; then
    export clsCommand="strategy"
    export clsStrategy=$6
fi

if [ -z $clsUser ] || [ -z $clsPwd ]; then
    echo "user or password NOT set. "
else
    if [ ! -z $clsCommand ] && [ $clsCommand == "status" ]; then
        curl -X GET http://localhost:9994/bfm/cluster-status -u $clsUser:$clsPwd
    elif [ ! -z $clsCommand ] && [ $clsCommand == "pause" ]; then
        curl -X GET http://localhost:9994/bfm/check-pause -u $clsUser:$clsPwd
    elif [ ! -z $clsCommand ] && [ $clsCommand == "resume" ]; then
        curl -X GET http://localhost:9994/bfm/check-resume -u $clsUser:$clsPwd
    elif [ ! -z $clsCommand ] && [ $clsCommand == "strategy" ]; then
        curl -X POST  http://localhost:9994/bfm/watch-strategy/$clsStrategy -u $clsUser:$clsPwd
    elif [ ! -z $clsCommand ] && [ $clsCommand == "switchOver" ]; then
        curl -X POST  http://localhost:9994/bfm/switchover/$targetSlave -u $clsUser:$clsPwd
    else
        echo "command not found..."$clsCommand
    fi
fi
