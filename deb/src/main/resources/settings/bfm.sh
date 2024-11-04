#!/bin/bash
export SERVICE_NAME=bfm
export PATH_TO_JAR=/etc/bfm/bfmwatcher/bfm-app.jar
export PATH_TO_APP_PROP=/etc/bfm/bfmwatcher/application.properties

case $1 in
start)
        echo "Starting $SERVICE_NAME ..."
        export PS_10=$(ps -ef|grep bfmwatcher|awk 'NR==1{print $10}')
        if [ "$PS_10" = "$PATH_TO_JAR" ]
        then
                export PID=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $2}')
                echo "$SERVICE_NAME is already running on $PID pid number"
        else
                if [ -f "/etc/bfm/bfmwatcher/bfm.log" ]
                then
                        echo "old logfile archiving..."
                        mv /etc/bfm/bfmwatcher/bfm.log /etc/bfm/bfmwatcher/bfm_$(date +"%Y-%m-%d_%H-%M-%S").log
                fi
                nohup java -jar $PATH_TO_JAR -Dspring.config.location=$PATH_TO_APP_PROP >> /etc/bfm/bfmwatcher/bfm.log 2>&1 &
        
                export PS_10=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $10}')
                echo $PS_10
                echo $PATH_TO_JAR
                if [ "$PS_10" = "$PATH_TO_JAR" ]
                then
                        export PID2=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $2}')
                        echo "$SERVICE_NAME is started on $PID2 pid number"
                else
                        echo "$SERVICE_NAME could not start ..."
                fi
        fi

;;
stop)
        export PS_10=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $10}')
        if [ "$PS_10" = "$PATH_TO_JAR" ]
        then
                export PID=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $2}')
                echo "$SERVICE_NAME is running on $PID pid number"
                kill $PID;
                echo "$SERVICE_NAME stopped..."
        else
                echo "$SERVICE_NAME is not running ..."
        fi
;;
restart)
                export PS_10=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $10}')
                if [ "$PS_10" = "$PATH_TO_JAR" ]
                then
                        export PID=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $2}')
                        echo "$SERVICE_NAME is running on $PID pid number"
                        kill $PID;
                        echo "$SERVICE_NAME stopped..."
                        if [ -f "/etc/bfm/bfmwatcher/bfm.log" ]
                        then
                                echo "old logfile archiving..."
                                mv /etc/bfm/bfmwatcher/bfm.log /etc/bfm/bfmwatcher/bfm_$(date +"%Y-%m-%d_%H-%M-%S").log
                        fi

                        nohup java -jar $PATH_TO_JAR -Dspring.config.location=$PATH_TO_APP_PROP >> /etc/bfm/bfmwatcher/bfm.log 2>&1 &

                        export PS_10=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $10}')
                        if [ "$PS_10" = "$PATH_TO_JAR" ]
                        then
                                export PID2=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $2}')
                                echo "$SERVICE_NAME is restarted on $PID2 pid number"
                        else
                                echo "$SERVICE_NAME could not start ..."
                        fi
                else
                        if [ -f "/etc/bfm/bfmwatcher/bfm.log" ]
                        then
                                echo "old logfile archiving..."
                                mv /etc/bfm/bfmwatcher/bfm.log /etc/bfm/bfmwatcher/bfm_$(date +"%Y-%m-%d_%H-%M-%S").log
                        fi

                        nohup java -jar $PATH_TO_JAR -Dspring.config.location=$PATH_TO_APP_PROP > /etc/bfm/bfmwatcher/bfm.log 2>&1 &

                        export PS_10=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $10}')
                        if [ "$PS_10" = "$PATH_TO_JAR" ]
                        then
                                export PID=$(ps -ef|grep bfmwatcher |awk 'NR==1{print $2}')
                                echo "$SERVICE_NAME is started on $PID pid number"
                        else
                                echo "$SERVICE_NAME could not start ..."
                        fi

                fi

;;
 esac
