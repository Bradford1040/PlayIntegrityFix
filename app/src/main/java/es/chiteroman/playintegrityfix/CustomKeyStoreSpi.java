package es.chiteroman.playintegrityfix;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

public final class CustomKeyStoreSpi extends KeyStoreSpi {
    private static final String EAT_OID = "1.3.6.1.4.1.11129.2.1.25";
    private static final String ASN1_OID = "1.3.6.1.4.1.11129.2.1.17";
    private static final String KNOX_OID = "1.3.6.1.4.1.236.11.3.23.7";
    public static volatile KeyStoreSpi keyStoreSpi;

    @Override
    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        return keyStoreSpi.engineGetKey(alias, password);
    }

    @Override
    public Certificate[] engineGetCertificateChain(String alias) {

        boolean isDroidGuard = EntryPoint.isDroidGuard();

        Certificate[] certificates = keyStoreSpi.engineGetCertificateChain(alias);

        if (certificates[0] instanceof X509Certificate leaf) {

            boolean attestationExtensions = leaf.getExtensionValue(EAT_OID) != null || leaf.getExtensionValue(ASN1_OID) != null || leaf.getExtensionValue(KNOX_OID) != null;

            if (isDroidGuard && attestationExtensions) {
                EntryPoint.LOG("DroidGuard and attestation extension detected! Throwing exception...");
                throw new UnsupportedOperationException();
            }
        }

        return certificates;
    }

    @Override
    public Certificate engineGetCertificate(String alias) {

        boolean isDroidGuard = EntryPoint.isDroidGuard();

        Certificate certificate = keyStoreSpi.engineGetCertificate(alias);

        if (certificate instanceof X509Certificate leaf) {

            boolean attestationExtensions = leaf.getExtensionValue(EAT_OID) != null || leaf.getExtensionValue(ASN1_OID) != null || leaf.getExtensionValue(KNOX_OID) != null;

            if (isDroidGuard && attestationExtensions) {
                EntryPoint.LOG("DroidGuard and attestation extension detected! Throwing exception...");
                throw new UnsupportedOperationException();
            }
        }

        return certificate;
    }

    @Override
    public Date engineGetCreationDate(String alias) {
        return keyStoreSpi.engineGetCreationDate(alias);
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
        keyStoreSpi.engineSetKeyEntry(alias, key, password, chain);
    }

    @Override
    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
        keyStoreSpi.engineSetKeyEntry(alias, key, chain);
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
        keyStoreSpi.engineSetCertificateEntry(alias, cert);
    }

    @Override
    public void engineDeleteEntry(String alias) throws KeyStoreException {
        keyStoreSpi.engineDeleteEntry(alias);
    }

    @Override
    public Enumeration<String> engineAliases() {
        return keyStoreSpi.engineAliases();
    }

    @Override
    public boolean engineContainsAlias(String alias) {
        return keyStoreSpi.engineContainsAlias(alias);
    }

    @Override
    public int engineSize() {
        return keyStoreSpi.engineSize();
    }

    @Override
    public boolean engineIsKeyEntry(String alias) {
        return keyStoreSpi.engineIsKeyEntry(alias);
    }

    @Override
    public boolean engineIsCertificateEntry(String alias) {
        return keyStoreSpi.engineIsCertificateEntry(alias);
    }

    @Override
    public String engineGetCertificateAlias(Certificate cert) {
        return keyStoreSpi.engineGetCertificateAlias(cert);
    }

    @Override
    public void engineStore(OutputStream stream, char[] password) throws CertificateException, IOException, NoSuchAlgorithmException {
        keyStoreSpi.engineStore(stream, password);
    }

    @Override
    public void engineLoad(InputStream stream, char[] password) throws CertificateException, IOException, NoSuchAlgorithmException {
        keyStoreSpi.engineLoad(stream, password);
    }
}
