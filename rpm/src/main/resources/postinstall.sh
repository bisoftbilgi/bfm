# Ensure our service is enabled

chmod +x /etc/bfm/bfmwatcher/bfmctl
if [ $1 -eq 1 ] ; then 
  # Initial installation 
  systemctl enable bfm.service >/dev/null 2>&1 || : 
fi

