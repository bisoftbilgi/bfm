if [ ! -z $1 ] && [ $1 == "-h" ]; then
    echo "-h help"
    echo "-u username"
    echo "-p password"
    echo "-status show cluster status"
    echo "-pause set watch strategy to manual"
    echo "-resume set watch strategy to availability"
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
    export clsCommand="strategy"
    export clsStrategy="M"
fi

if [ ! -z $1 ] && [ $1 == "-resume" ]; then
    export clsCommand="strategy"
    export clsStrategy="A"
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
    export clsCommand="strategy"
    export clsStrategy="M"
fi

if [ ! -z $2 ] && [ $2 == "-resume" ]; then
    export clsCommand="strategy"
    export clsStrategy="A"
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
    export clsCommand="strategy"
    export clsStrategy="M"
fi

if [ ! -z $3 ] && [ $3 == "-resume" ]; then
    export clsCommand="strategy"
    export clsStrategy="A"
fi

if [ ! -z $4 ] && [ $4 == "-status" ]; then
    export clsCommand="status"
fi

if [ ! -z $4 ] && [ $4 == "-pause" ]; then
    export clsCommand="strategy"
    export clsStrategy="M"
fi

if [ ! -z $4 ] && [ $4 == "-resume" ]; then
    export clsCommand="strategy"
    export clsStrategy="A"
fi


if [ ! -z $5 ] && [ $5 == "-status" ]; then
    export clsCommand="status"
fi

if [ ! -z $5 ] && [ $5 == "-pause" ]; then
    export clsCommand="strategy"
    export clsStrategy="M"
fi

if [ ! -z $5 ] && [ $5 == "-resume" ]; then
    export clsCommand="strategy"
    export clsStrategy="A"
fi

if [ -z $clsUser ] || [ -z $clsPwd ]; then
    echo "user or password NOT set. "
else
    if [ ! -z $clsCommand ] && [ $clsCommand == "status" ]; then
        curl -X GET http://localhost:9994/bfm/cluster-status -u $clsUser:$clsPwd
    elif [ ! -z $clsCommand ] && [ $clsCommand == "strategy" ]; then
        curl -X POST  http://localhost:9994/bfm/watch-strategy/$clsStrategy -u $clsUser:$clsPwd
    else
        echo "command not found..."$clsCommand
    fi
fi
