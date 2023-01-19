package com.datastax.oss.pulsaroperator.controllers.utils;

import com.datastax.oss.pulsaroperator.MockKubernetesClient;
import com.datastax.oss.pulsaroperator.crds.GlobalSpec;
import com.datastax.oss.pulsaroperator.crds.configs.tls.TlsConfig;
import io.fabric8.certmanager.api.model.v1.Certificate;
import io.fabric8.certmanager.api.model.v1.Issuer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CertManagerCertificatesProvisionerTest {

    @Test
    public void testDefaults() throws Exception {
        final MockKubernetesClient mockKubernetesClient = new MockKubernetesClient("ns");
        final GlobalSpec spec = GlobalSpec.builder()
                .name("pul")
                .tls(TlsConfig.builder()
                        .enabled(true)
                        .certProvisioner(TlsConfig.CertProvisionerConfig.builder()
                                .selfSigned(TlsConfig.SelfSignedCertProvisionerConfig.builder()
                                        .enabled(true)
                                        .build())
                                .build())
                        .build())
                .build();
        spec.applyDefaults(null);
        new CertManagerCertificatesProvisioner(mockKubernetesClient.getClient(), "ns", spec)
                .generateCertificates();

        Assert.assertEquals(
                mockKubernetesClient.getCreatedResource(Certificate.class, "pul-ca-certificate").getResourceYaml(),
                """
                        ---
                        apiVersion: cert-manager.io/v1
                        kind: Certificate
                        metadata:
                          name: pul-ca-certificate
                          namespace: ns
                        spec:
                          commonName: ns.svc.cluster.local
                          isCA: true
                          issuerRef:
                            name: pul-self-signed-issuer
                          secretName: pul-ss-ca
                          usages:
                          - server auth
                          - client auth
                        """);

        Assert.assertEquals(
                mockKubernetesClient.getCreatedResource(Certificate.class, "pul-server-tls").getResourceYaml(),
                """
                        ---
                        apiVersion: cert-manager.io/v1
                        kind: Certificate
                        metadata:
                          name: pul-server-tls
                          namespace: ns
                        spec:
                          dnsNames:
                          - '*.pul-broker.ns.svc.cluster.local'
                          - '*.pul-broker.ns'
                          - '*.pul-broker'
                          - pul-broker.ns.svc.cluster.local
                          - pul-broker.ns
                          - pul-broker
                          - pul-proxy.ns.svc.cluster.local
                          - pul-proxy.ns
                          - pul-proxy
                          - pul-function-ca.ns.svc.cluster.local
                          - pul-function-ca.ns
                          - pul-function-ca
                          issuerRef:
                            name: pul-ca-issuer
                          secretName: pulsar-tls
                          """);

        Assert.assertEquals(
                mockKubernetesClient.getCreatedResource(Issuer.class, "pul-self-signed-issuer").getResourceYaml(),
                """
                        ---
                        apiVersion: cert-manager.io/v1
                        kind: Issuer
                        metadata:
                          name: pul-self-signed-issuer
                          namespace: ns
                        spec:
                          selfSigned: {}
                        """);

        Assert.assertEquals(
                mockKubernetesClient.getCreatedResource(Issuer.class, "pul-ca-issuer").getResourceYaml(),
                """
                        ---
                        apiVersion: cert-manager.io/v1
                        kind: Issuer
                        metadata:
                          name: pul-ca-issuer
                          namespace: ns
                        spec:
                          ca:
                            secretName: pul-ss-ca
                        """);

    }

}