BFM Setup Guide
===============

**Bidirectional Failover Manager (BFM)**

Prerequisites
-------------

- PostgreSQL cluster 
- Linux (for this guide, we focus on Rocky Linux/CentOS/RHEL)
- Root or sudo privileges
- Docker (optional, for containerized deployment)
- Network connectivity between all nodes

Installation Process
--------------------

1. Repository Setup & Installation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Linux (RockyLinux/CentOS/RHEL)**

.. code-block:: bash

   
   sudo dnf install -y https://nexus.bisoft.com.tr/repository/bfm-yum/repo/bisoft-repo-1.0-1.noarch.rpm
  
   sudo dnf install -y bfm-rpm-package

   
   sudo yum install -y https://nexus.bisoft.com.tr/repository/bfm-yum/repo/bisoft-repo-1.0-1.noarch.rpm
   sudo yum install -y bfm-rpm-package

1. Source Code Deployment (Development)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: bash

   
   git clone https://github.com/bisoftbilgi/bfm.git
   cd bfm



   ./mvnw clean package

1. Configuration Setup
~~~~~~~~~~~~~~~~~~~~~~

Edit ``/etc/bfm/config/application.properties``:

.. code-block:: ini

   # Core Configuration
   bfm.use-tls = false                 # Set to 'false' for testing without certificates
   bfm.user-crypted = false            # Set to 'true' if using encrypted passwords
   server.pgpassword = StrongPassword  # PostgreSQL password

   # Network Configuration
   server.port = 8080                  # Web UI and API port
   bfm.vip.enabled = true              # Enable Virtual IP feature
   bfm.vip.interface = eth0            # Network interface for VIP
   bfm.vip.address = 192.168.1.100     # Virtual IP address

   # PostgreSQL Instance Configuration
   bfm.nodes[0].name = pg-node1
   bfm.nodes[0].host = 192.168.1.101
   bfm.nodes[0].port = 5432
   bfm.nodes[0].priority = 100

   bfm.nodes[1].name = pg-node2
   bfm.nodes[1].host = 192.168.1.102
   bfm.nodes[1].port = 5432
   bfm.nodes[1].priority = 90

   # High Availability Configuration
   bfm.ha-mode = true                  # Enable dual BFM instances
   bfm.ha-partner = 192.168.1.150      # Partner BFM address

1. Launch BFM Service
~~~~~~~~~~~~~~~~~~~~~

**System Service (Linux)**

.. code-block:: bash

  
   sudo systemctl enable bfm
   sudo systemctl start bfm
   sudo systemctl status bfm  

**Docker Container**

.. code-block:: bash

   
   docker run -d --name bfm \
     -p 8080:8080 \
     -v /path/to/config:/etc/bfm/config \
     --restart unless-stopped \
     bfm/bfm:latest

**Manual Execution**

.. code-block:: bash


   java -jar /etc/bfm/bfmwatcher/bfm-app.jar

1. Verify Installation
~~~~~~~~~~~~~~~~~~~~~~

1. **Check for successful startup messages**:

   .. code-block:: text

      Tomcat started on port(s): 8080 (http)
      BFM is now managing PostgreSQL cluster

2. **Access the web interface** at ``http://[server-ip]:8080``

3. **Verify logs at**:
   
   - ``/var/log/bfm/bfm.log`` (system installation)
   - Console output (manual execution)

4. **Test command-line functionality**:

   .. code-block:: bash

      bfm_ctl status

Troubleshooting
---------------

**Common Issues**
~~~~~~~~~~~~~~~~~

+------------------------+-----------------------------------------------------------+
| **Issue**              | **Resolution**                                            |
+========================+===========================================================+
| Connection refused     | Check firewall rules and PostgreSQL's pg_hba.conf         |
+------------------------+-----------------------------------------------------------+
| Authentication failure | Verify credentials in application.properties              |
+------------------------+-----------------------------------------------------------+
| Service won't start    | Check logs for missing dependencies or permission errors  |
+------------------------+-----------------------------------------------------------+




