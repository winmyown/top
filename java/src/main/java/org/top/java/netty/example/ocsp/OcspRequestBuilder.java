
package org.top.java.netty.example.ocsp;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.operator.DigestCalculator;

/**
 * This is a simplified version of BC's own {@link OCSPReqBuilder}.
 *
 * @see OCSPReqBuilder
 */

/**
 * 这是BC自己的{@link OCSPReqBuilder}的简化版本。
 *
 * @see OCSPReqBuilder
 */
public class OcspRequestBuilder {

    private static final SecureRandom GENERATOR = new SecureRandom();

    private SecureRandom generator = GENERATOR;

    private DigestCalculator calculator = Digester.sha1();

    private X509Certificate certificate;

    private X509Certificate issuer;

    public OcspRequestBuilder generator(SecureRandom generator) {
        this.generator = generator;
        return this;
    }

    public OcspRequestBuilder calculator(DigestCalculator calculator) {
        this.calculator = calculator;
        return this;
    }

    public OcspRequestBuilder certificate(X509Certificate certificate) {
        this.certificate = certificate;
        return this;
    }

    public OcspRequestBuilder issuer(X509Certificate issuer) {
        this.issuer = issuer;
        return this;
    }

    /**
     * ATTENTION: The returned {@link OCSPReq} is not re-usable/cacheable! It contains a one-time nonce
     * and CA's will (should) reject subsequent requests that have the same nonce value.
     */

    /**
     * 注意：返回的 {@link OCSPReq} 不可重复使用/缓存！它包含一次性随机数，
     * 并且 CA 将（应该）拒绝具有相同随机数值的后续请求。
     */
    public OCSPReq build() throws OCSPException, IOException, CertificateEncodingException {
        SecureRandom generator = checkNotNull(this.generator, "generator");
        DigestCalculator calculator = checkNotNull(this.calculator, "calculator");
        X509Certificate certificate = checkNotNull(this.certificate, "certificate");
        X509Certificate issuer = checkNotNull(this.issuer, "issuer");

        BigInteger serial = certificate.getSerialNumber();

        CertificateID certId = new CertificateID(calculator,
                new X509CertificateHolder(issuer.getEncoded()), serial);

        OCSPReqBuilder builder = new OCSPReqBuilder();
        builder.addRequest(certId);

        byte[] nonce = new byte[8];
        generator.nextBytes(nonce);

        Extension[] extensions = new Extension[] {
                new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false,
                        new DEROctetString(nonce)) };

        builder.setRequestExtensions(new Extensions(extensions));

        return builder.build();
    }
}
