cp base-truststore.ks client-truststore.ks
echo "Use password: changeit"
keytool -importcert -file microgram.cert -alias microgram -keystore client-truststore.ks
