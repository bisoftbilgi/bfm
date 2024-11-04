####### CREATE CERTIFICATE ########################################

keytool -genkeypair -alias bfm -keyalg RSA -keysize 4096 -storetype JKS -keystore bfm.jks -validity 3650 -storepass changeit -keypass changeit -dname "CN=bfm, OU=bfm, O=bfm, L=bfm, S=bfm, C=bfm" -noprompt

keytool -genkeypair -alias bfm -keyalg RSA -keysize 4096 -storetype PKCS12 -keystore bfm.p12 -validity 3650 -storepass changeit -keypass changeit -dname "CN=bfm, OU=bfm, O=bfm, L=bfm, S=bfm, C=bfm" -noprompt

keytool -list -v -keystore bfm.jks -keypass changeit

keytool -list -v -keystore bfm.p12

keytool -importkeystore -srckeystore bfm.jks -destkeystore bfm.p12 -deststoretype pkcs12

############### IMPORT CERTIFICATE #####################################

keytool -import -alias springboot -file myCertificate.crt -keystore springboot.p12 -storepass password