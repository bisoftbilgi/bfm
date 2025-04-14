Troubleshooting and FAQ
=========================

Purpose
-------
This guide aims to help users quickly identify and resolve common issues encountered when using BFM (Bidirectional Failover Manager). It covers connection problems, certificate and TLS errors, authorization issues, as well as log analysis methods and explanations of common error messages.

Common Issues and Resolutions
-----------------------------

1. **Connection Problems**

   *Issue:* Inability to connect to PostgreSQL or the BFM service.

   *Possible Causes:*
   
   - Firewall rules blocking required ports.
   - Incorrect IP addresses or port numbers in the configuration.
   - Network connectivity problems.

   *Resolution:*
   
   1. Verify that your firewall settings allow traffic on the necessary ports.
   2. Check the values in your *application.properties* file to ensure IP addresses and ports are correct.


2. **Certificate and TLS Errors**

   *Issue:* TLS handshake failures or certificate verification errors during secure communications.

   *Possible Causes:*
   
   - Incorrect TLS settings in the configuration (e.g., `bfm.use-tls` or `minipg.use-tls`).
   - Missing or mislocated keystore files (e.g., `bfm.p12`).
   - Mismatched or improperly configured TLS secret values.

   *Resolution:*
   
   1. Ensure that both `bfm.use-tls` and `minipg.use-tls` are set to `true` if TLS is required.
   2. Verify that the keystore file (`bfm.p12`) is placed in the correct directory.
   3. Review your TLS secret (`bfm.tls-secret`) and certificate configuration for consistency.
   4. Examine the log files for detailed error messages related to TLS failures.

3. **Authorization Issues**

   *Issue:* Authentication failures when connecting to PostgreSQL.

   *Possible Causes:*
   
   - Mismatch between the credentials provided in *application.properties* and those configured on the PostgreSQL server.
   - Incorrect settings for `server.pguser` or `server.pgpassword`.

   *Resolution:*
   
   1. Confirm that the credentials in your *application.properties* file are accurate.
   2. Verify that the PostgreSQL `pg_hba.conf` file is configured to allow connections from the BFM user.
   3. Check the PostgreSQL logs for specific authentication errors.

4. **Troubleshooting Guide**

Step-by-Step Troubleshooting Guide
------------------------------------

1. **Identify the Issue:**  
   Review error messages and log files to determine the root cause.

2. **Consult this Troubleshooting Guide:**  
   Refer to the relevant section corresponding to the identified error.

3. **Apply the Recommended Fix:**  
   Follow the step-by-step instructions provided in this guide.

4. **Test the Solution:**  
   After applying changes, reattempt the operation to ensure the issue is resolved.

5. **Seek Further Help if Needed:**  
   If the problem persists, collect detailed log output and consult the support community or project maintainers.

Conclusion
----------
This FAQ and Troubleshooting Guide is designed to assist you in resolving common issues encountered with BFM. Regularly reviewing log files and keeping your configuration consistent with the guidelines provided will help maintain a stable and secure system.
