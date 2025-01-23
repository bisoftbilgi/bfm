####### CREATE CERTIFICATE ########################################

keytool -genkeypair -alias bfm -keyalg RSA -keysize 4096 -storetype JKS -keystore bfm.jks -validity 3650 -storepass changeit -keypass changeit -dname "CN=bfm, OU=bfm, O=bfm, L=bfm, S=bfm, C=bfm" -noprompt

keytool -genkeypair -alias bfm -keyalg RSA -keysize 4096 -storetype PKCS12 -keystore bfm.p12 -validity 3650 -storepass changeit -keypass changeit -dname "CN=bfm, OU=bfm, O=bfm, L=bfm, S=bfm, C=bfm" -noprompt

keytool -list -v -keystore bfm.jks -keypass changeit

keytool -list -v -keystore bfm.p12

keytool -importkeystore -srckeystore bfm.jks -destkeystore bfm.p12 -deststoretype pkcs12

############### IMPORT CERTIFICATE #####################################

keytool -import -alias springboot -file myCertificate.crt -keystore springboot.p12 -storepass password

openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -sha256 -days 3650 -nodes -subj "/C=XX/ST=StateName/L=CityName/O=CompanyName/OU=CompanySectionName/CN=bfm"


CRT ve PEM dosyalarindan asagidaki kibi p12 uretebiliyo
openssl pkcs12 -export -out certificate.p12 -inkey privatekey.pem -in certificate.crt -certfile chain.crt