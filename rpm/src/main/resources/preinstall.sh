# Add the "bfm" group and user
/usr/sbin/useradd -c "bfm" -U \
        -s /sbin/nologin -r -d /var/bfm bfm 2> /dev/null || :
