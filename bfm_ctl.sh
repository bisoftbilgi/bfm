if [ ! -z $1 ] && [ $1 == "-h" ]; then
    echo "-h help"
    echo "-u username"
    echo "-p password"
    echo "-status show cluster status"
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

if [ ! -z $2 ] && [ ! -z $3 ] && [ $2 == "-u" ]; then
    export clsUser=$3
fi

if [ ! -z $2 ] && [ ! -z $3 ] && [ $2 == "-p" ]; then
    export clsPwd=$3
fi

if [ ! -z $2 ] && [ $2 == "-status" ]; then
    export clsCommand="status"
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

if [ -z $clsUser ] || [ -z $clsPwd ]; then
    echo "user or password NOT set. "
else
    curl -X GET http://localhost:9994/bfm/cluster-status -u $clsUser:$clsPwd
fi
