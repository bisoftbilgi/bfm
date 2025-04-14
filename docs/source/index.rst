=========================================
BFM - Bidirectional Failover Manager
=========================================

**Enterprise-Grade PostgreSQL High Availability Solution**

Overview
========
BFM (Bidirectional Failover Manager) is an advanced PostgreSQL failover solution designed to ensure high availability and business continuity for mission-critical database environments. With its comprehensive feature set and robust architecture, BFM provides reliable and secure failover management for PostgreSQL clusters of any size.

Key Features
============
- **Intelligent Failover**: Priority-based node selection and promotion
- **Enhanced Security**: TLS encryption and credential protection
- **Virtual IP Management**: Seamless application connectivity during failovers
- **Self High-Availability**: Redundant BFM instances for management reliability
- **Comprehensive Monitoring**: Web UI and command-line interfaces

Quick Start
===========
.. code-block:: bash

 
   sudo dnf install -y https://nexus.bisoft.com.tr/repository/bfm-yum/repo/bisoft-repo-1.0-1.noarch.rpm
   sudo dnf install -y bfm-rpm-package

  
   sudo systemctl enable bfm
   sudo systemctl start bfm

.. toctree::
   :maxdepth: 2
   :caption: Contents

   setup-guide
   extended-architecture
   application-properties
   troubleshooting


.. footer::

   BFM Documentation | Copyright © 2025 BiSoft Bilgi Teknolojileri | Version 1.0.0
   All rights reserved.
