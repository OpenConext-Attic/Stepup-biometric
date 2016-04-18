package biometric.web;

import biometric.api.BioMetric;
import biometric.api.BioMetricHttpHeaders;
import biometric.api.DefaultBioMetric;
import biometric.api.mock.MockBioMetric;
import biometric.saml.KeyStoreLocator;
import biometric.saml.mock.MockSAMLAuthnFilter;
import biometric.saml.SAMLAuthnFilter;
import biometric.saml.SAMLMessageHandler;
import biometric.api.BioMetricAuthenticationProvider;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.common.binding.encoding.SAMLMessageEncoder;
import org.opensaml.common.binding.security.IssueInstantRule;
import org.opensaml.common.binding.security.MessageReplayRule;
import org.opensaml.saml2.binding.decoding.HTTPRedirectDeflateDecoder;
import org.opensaml.saml2.binding.encoding.HTTPPostSimpleSignEncoder;
import org.opensaml.util.storage.MapBasedStorageService;
import org.opensaml.util.storage.ReplayCache;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.ws.security.provider.BasicSecurityPolicy;
import org.opensaml.ws.security.provider.StaticSecurityPolicyResolver;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.security.credential.CredentialResolver;
import org.opensaml.xml.security.credential.KeyStoreCredentialResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class WebSecurityConfigurer {

  @Value("${sa.metadata.url}")
  private String saMetadataUrl;

  @Value("${sa.public.certificate}")
  private String saPublicCertificate;

  @Value("${sa.entity.id}")
  private String saEntityId;

  @Value("${biometric.entity.id}")
  private String biometricEntityId;

  @Value("${biometric.private.key}")
  private String biometricPrivateKey;

  @Value("${biometric.public.certificate}")
  private String biometricPublicCertificate;

  @Value("${biometric.passphrase}")
  private String biometricPassphrase;


  public WebSecurityConfigurer() throws ConfigurationException {
    DefaultBootstrap.bootstrap();
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth, BioMetric bioMetric) throws Exception {
    //because Autowired this will end up in the global ProviderManager
    auth.authenticationProvider(new BioMetricAuthenticationProvider(bioMetric));
  }

  @Bean
  public CredentialResolver credentialResolver() {
    KeyStore keyStore = new KeyStoreLocator().createKeyStore(
        biometricEntityId,
        biometricPublicCertificate,
        biometricPrivateKey,
        saEntityId,
        saPublicCertificate,
        biometricPassphrase
    );
    return new KeyStoreCredentialResolver(keyStore, Collections.singletonMap(biometricEntityId, biometricPassphrase));
  }

  @Bean
  @Autowired
  public SAMLMessageHandler samlMessageHandler(CredentialResolver credentialResolver) {
    return new SAMLMessageHandler(
        credentialResolver,
        samlMessageDecoder(),
        samlMessageEncoder(),
        securityPolicyResolver(),
        biometricEntityId,
        saEntityId,
        saMetadataUrl);
  }

  private SecurityPolicyResolver securityPolicyResolver() {
    IssueInstantRule instantRule = new IssueInstantRule(90, 300);
    MessageReplayRule replayRule = new MessageReplayRule(new ReplayCache(new MapBasedStorageService(), 14400000));

    BasicSecurityPolicy securityPolicy = new BasicSecurityPolicy();
    securityPolicy.getPolicyRules().addAll(Arrays.asList(instantRule, replayRule));

    return new StaticSecurityPolicyResolver(securityPolicy);
  }

  private SAMLMessageEncoder samlMessageEncoder() {
    return new HTTPPostSimpleSignEncoder(velocityEngine(), "/templates/saml2-post-simplesign-binding.vm", true);
  }

  private SAMLMessageDecoder samlMessageDecoder() {
    return new HTTPRedirectDeflateDecoder(new BasicParserPool());
  }

  private VelocityEngine velocityEngine() {
    VelocityEngine velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());

    velocityEngine.init();
    return velocityEngine;
  }

  @Configuration
  @Order(1)
  public static class BioMetricConfiguration {

    @Value("${biometric.api.key}")
    private String biometricApiKey;

    @Value("${biometric.api.base.url}")
    private String biometricApiBaseUrl;

    @Bean
    @Profile("!dev")
    public BioMetric defaultBioMetric() {
      return new DefaultBioMetric(new BioMetricHttpHeaders(biometricApiKey), biometricApiBaseUrl);
    }

    @Bean
    @Profile("dev")
    public BioMetric mockBioMetric() {
      return new MockBioMetric();
    }
  }

  @Configuration
  @Order
  public static class SecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

    @Autowired
    private SAMLMessageHandler samlMessageHandler;

    @Autowired
    private Environment environment;

    @Value("${sa.entity.id}")
    private String saEntityId;

    @Override
    public void configure(WebSecurity web) throws Exception {
      web.ignoring().antMatchers("/health","/info", "/metadata");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
      http
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
          .and()
          .csrf()
          .disable()
          .addFilterBefore(
              new SAMLAuthnFilter(authenticationManager(), samlMessageHandler),
              BasicAuthenticationFilter.class)
          .authorizeRequests()
          .antMatchers("/**").hasRole("USER");

      if (environment.acceptsProfiles("dev")) {
        //we can't use @Profile, because we need to add it exactly before the real filter
        http.addFilterBefore(new MockSAMLAuthnFilter(saEntityId, authenticationManager()), SAMLAuthnFilter.class);
      }
    }

  }


}
