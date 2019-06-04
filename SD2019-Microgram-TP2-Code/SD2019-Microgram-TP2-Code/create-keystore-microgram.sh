keytool -genkey -alias server -keyalg RSA -validity 365 -keystore microgram.ks -storetype pkcs12
keytool -exportcert -alias server -keystore microgram.ks -file microgram.cert
