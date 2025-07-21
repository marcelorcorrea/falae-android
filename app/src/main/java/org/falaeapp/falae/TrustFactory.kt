package org.falaeapp.falae

import android.content.Context
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory


class TrustFactory {
    companion object {
        fun getSslContextFromTrustManager(context: Context): SSLContext {
            val cf = CertificateFactory.getInstance("X.509")

            val isrgRoot1Input = context.resources.openRawResource(R.raw.isrgrootx1)
            val isrgRoot1Certificate: Certificate = isrgRoot1Input.use {
                cf.generateCertificate(it)
            }

            val isrgRoot2Input = context.resources.openRawResource(R.raw.isrgrootx2)
            val isrgRoot2Certificate: Certificate = isrgRoot2Input.use {
                cf.generateCertificate(it)
            }

            val keyStoreType = KeyStore.getDefaultType()
            val keyStore = KeyStore.getInstance(keyStoreType).apply {
                load(null, null)
                setCertificateEntry("isrgrootx1", isrgRoot1Certificate)
                setCertificateEntry("isrgrootx2", isrgRoot2Certificate)
            }

            val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
            val tmf = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
                init(keyStore)
            }
            // Create an SSLContext that uses our TrustManager
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, tmf.trustManagers, null)
            return sslContext
        }
    }
}