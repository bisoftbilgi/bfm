if [ ! -z $1 ] && [ $1 == "-h" ]; then
    echo "-h help"
    echo "-u username"
    echo "-p password"
    echo "-status show cluster status"
    echo "-pause cluster check pause"
    echo "-resume cluster check resume"
    echo "-watchMode [A|M] set watch strategy  A:availability, M:manual"
    echo "-switchOver 192.168.1.22:5432 switch to selected slave"
    echo "-reinit 192.168.1.22:5432 re-initialize selected slave with pg_rewind or pg_basebackup"
    echo "-encrypt clearString  encrpyt the clear string for use encrypted bfm password function"
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

if [ ! -z $1 ] && [ $1 == "-mailPause" ]; then
    export clsCommand="mail-pause"
fi

if [ ! -z $1 ] && [ $1 == "-mailResume" ]; then
    export clsCommand="mail-resume"
fi

if [ ! -z $1 ]  && [ ! -z $2 ] && [ $1 == "-switchOver" ]; then
    export clsCommand="switchOver"
    export targetSlave=$2
fi

if [ ! -z $1 ]  && [ ! -z $2 ] && [ $1 == "-watchMode" ]; then
    export clsCommand="strategy"
    export clsStrategy=$2
fi

if [ ! -z $1 ]  && [ ! -z $2 ] && [ $1 == "-encrypt" ]; then
    export clsCommand="encrypt"
    export clearStr=$2
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

if [ ! -z $2 ] && [ $2 == "-mailPause" ]; then
    export clsCommand="mail-pause"
fi

if [ ! -z $2 ] && [ $2 == "-mailResume" ]; then
    export clsCommand="mail-resume"
fi

if [ ! -z $2 ]  && [ ! -z $3 ] && [ $2 == "-switchOver" ]; then
    export clsCommand="switchOver"
    export targetSlave=$3
fi

if [ ! -z $2 ]  && [ ! -z $3 ] && [ $2 == "-watchMode" ]; then
    export clsCommand="strategy"
    export clsStrategy=$3
fi

if [ ! -z $2 ]  && [ ! -z $3 ] && [ $2 == "-encrypt" ]; then
    export clsCommand="encrypt"
    export clearStr=$3
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

if [ ! -z $3 ] && [ $3 == "-mailPause" ]; then
    export clsCommand="mail-pause"
fi

if [ ! -z $3 ] && [ $3 == "-mailResume" ]; then
    export clsCommand="mail-resume"
fi

if [ ! -z $3 ]  && [ ! -z $4 ] && [ $3 == "-switchOver" ]; then
    export clsCommand="switchOver"
    export targetSlave=$4
fi

if [ ! -z $3 ]  && [ ! -z $4 ] && [ $3 == "-reinit" ]; then
    export clsCommand="reinit"
    export targetSlave=$4
fi

if [ ! -z $3 ]  && [ ! -z $4 ] && [ $3 == "-watchMode" ]; then
    export clsCommand="strategy"
    export clsStrategy=$4
fi

if [ ! -z $3 ]  && [ ! -z $4 ] && [ $3 == "-encrypt" ]; then
    export clsCommand="encrypt"
    export clearStr=$4
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

if [ ! -z $4 ] && [ $4 == "-mailPause" ]; then
    export clsCommand="mail-pause"
fi

if [ ! -z $4 ] && [ $4 == "-mailResume" ]; then
    export clsCommand="mail-resume"
fi

if [ ! -z $4 ]  && [ ! -z $5 ] && [ $4 == "-switchOver" ]; then
    export clsCommand="switchOver"
    export targetSlave=$5
fi

if [ ! -z $4 ]  && [ ! -z $5 ] && [ $4 == "-reinit" ]; then
    export clsCommand="reinit"
    export targetSlave=$5
fi

if [ ! -z $4 ]  && [ ! -z $5 ] && [ $4 == "-watchMode" ]; then
    export clsCommand="strategy"
    export clsStrategy=$5
fi

if [ ! -z $4 ]  && [ ! -z $5 ] && [ $4 == "-encrypt" ]; then
    export clsCommand="encrypt"
    export clearStr=$5
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

if [ ! -z $5 ] && [ $5 == "-mailPause" ]; then
    export clsCommand="mail-pause"
fi

if [ ! -z $5 ] && [ $5 == "-mailResume" ]; then
    export clsCommand="mail-resume"
fi

if [ ! -z $5 ]  && [ ! -z $6 ] && [ $5 == "-switchOver" ]; then
    export clsCommand="switchOver"
    export targetSlave=$6
fi

if [ ! -z $5 ]  && [ ! -z $6 ] && [ $5 == "-reinit" ]; then
    export clsCommand="reinit"
    export targetSlave=$6
fi

if [ ! -z $5 ]  && [ ! -z $6 ] && [ $5 == "-watchMode" ]; then
    export clsCommand="strategy"
    export clsStrategy=$6
fi

if [ ! -z $5 ]  && [ ! -z $6 ] && [ $5 == "-encrypt" ]; then
    export clsCommand="encrypt"
    export clearStr=$6
fi

if [ -z $clsUser ] || [ -z $clsPwd ]; then
    echo "user or password NOT set. "
else
    if [ -f /etc/bfm/bfmwatcher/application.properties ]; then
        export bfmPair=$(cat /etc/bfm/bfmwatcher/application.properties |grep watcher.cluster-pair |cut -d "=" -f 2 |tr -d " ")
        export bfmSSL=$(cat /etc/bfm/bfmwatcher/application.properties |grep bfm.use-tls |cut -d "=" -f 2 |tr -d " ")
    elif [ -f ./application.properties ]; then
        export bfmPair=$(cat /etc/bfm/bfmwatcher/application.properties |grep watcher.cluster-pair |cut -d "=" -f 2 |tr -d " ")
        export bfmSSL=$(cat /etc/bfm/bfmwatcher/application.properties |grep bfm.use-tls |cut -d "=" -f 2 |tr -d " ")
    else
        export bfmPair='127.0.0.1:9994'
        export bfmSSL='false'
    fi

    if [ ! -z $bfmSSL ] && [ $bfmSSL == "true" ]; then
        export bfmProtocol='https'
    else
        export bfmProtocol='http'
    fi

    active_bfm=$(curl -s $bfmProtocol://$bfmPair/bfm/get-active-bfm -u $clsUser:$clsPwd)
    # echo -e "\nActive BFM :"$active_bfm
    if [ ! -z $clsCommand ] && [ $clsCommand == "status" ]; then
        curl -X GET $bfmProtocol://$active_bfm/bfm/cluster-status -u $clsUser:$clsPwd        
    elif [ ! -z $clsCommand ] && [ $clsCommand == "pause" ]; then
        curl -X GET $bfmProtocol://$active_bfm/bfm/check-pause -u $clsUser:$clsPwd
    elif [ ! -z $clsCommand ] && [ $clsCommand == "resume" ]; then
        curl -X GET $bfmProtocol://$active_bfm/bfm/check-resume -u $clsUser:$clsPwd
    elif [ ! -z $clsCommand ] && [ $clsCommand == "mail-pause" ]; then
        curl -X GET $bfmProtocol://$active_bfm/bfm/mail-pause -u $clsUser:$clsPwd
    elif [ ! -z $clsCommand ] && [ $clsCommand == "mail-resume" ]; then
        curl -X GET $bfmProtocol://$active_bfm/bfm/mail-resume -u $clsUser:$clsPwd        
    elif [ ! -z $clsCommand ] && [ $clsCommand == "strategy" ]; then
        curl -X POST  $bfmProtocol://$active_bfm/bfm/watch-strategy/$clsStrategy -u $clsUser:$clsPwd
    elif [ ! -z $clsCommand ] && [ $clsCommand == "switchOver" ]; then
        curl -X POST  $bfmProtocol://$active_bfm/bfm/switchover/$targetSlave -u $clsUser:$clsPwd
    elif [ ! -z $clsCommand ] && [ $clsCommand == "reinit" ]; then
        curl -X POST  $bfmProtocol://$active_bfm/bfm/reinit/$targetSlave -u $clsUser:$clsPwd     
    elif [ ! -z $clsCommand ] && [ $clsCommand == "encrypt" ]; then
        export retCode=$(curl -s -o /dev/null -w "%{http_code}\n" -k $bfmProtocol://$bfmPair/bfm/cluster-status -u $clsUser:$clsPwd)
        if [[ "$retCode"=="000" ]]; then
            export bfmPairIP=$(echo "$bfmPair" | awk -F':' '{print $1}')
            export serverList=$(cat /etc/bfm/bfmwatcher/application.properties |grep server.pglist |cut -d "=" -f 2 |tr -d " " | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+' | tr ',' '\n' | grep -v "$bfmPairIP")
        fi

        curl -k -X POST  $bfmProtocol://$bfmPair/bfm/encrypt/$clearStr -u $clsUser:$clsPwd
        # if [[ "$restval" == *"No route to host"* ]]; then
        #     echo "It's there."
        # fi
    else
        echo "command not found..."$clsCommand
    fi
fi
