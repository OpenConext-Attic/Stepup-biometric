package idp.web;

import idp.saml.SAMLAuthnFilter;
import idp.saml.SAMLMessageHandler;
import idp.security.BioMetricAuthenticationProvider;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.common.binding.security.IssueInstantRule;
import org.opensaml.common.binding.security.MessageReplayRule;
import org.opensaml.saml2.binding.decoding.HTTPPostSimpleSignDecoder;
import org.opensaml.util.storage.MapBasedStorageService;
import org.opensaml.util.storage.ReplayCache;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.ws.security.provider.BasicSecurityPolicy;
import org.opensaml.ws.security.provider.StaticSecurityPolicyResolver;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.parse.BasicParserPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.Arrays;

/**
 *
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfigurer {

  public WebSecurityConfigurer() throws ConfigurationException {
    DefaultBootstrap.bootstrap();
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    //because Autowired this will end up in the global ProviderManager
    auth.authenticationProvider(new BioMetricAuthenticationProvider());
  }

  @Configuration
  @Order
  public static class SecurityConfigurationAdapter extends WebSecurityConfigurerAdapter  {

    @Override
    public void configure(HttpSecurity http) throws Exception {
      http
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
          .and()
          .csrf()
          .disable()
          .addFilterBefore(
              new SAMLAuthnFilter(authenticationManager(), samlMessageHandler()),
              BasicAuthenticationFilter.class)
          .authorizeRequests()
          .antMatchers("/**").hasRole("USER")
          .and()
          .formLogin().loginPage("/singleSignOn").permitAll();
    }

    private SAMLMessageHandler samlMessageHandler() {
      return new SAMLMessageHandler(velocityEngine(), decoder(), securityPolicyResolver());
    }

    private SecurityPolicyResolver securityPolicyResolver() {
      IssueInstantRule instantRule = new IssueInstantRule(90, 300);
      MessageReplayRule replayRule = new MessageReplayRule(new ReplayCache(new MapBasedStorageService(),14400000));

      BasicSecurityPolicy securityPolicy = new BasicSecurityPolicy();
      securityPolicy.getPolicyRules().addAll(Arrays.asList(instantRule, replayRule));

      return new StaticSecurityPolicyResolver(securityPolicy);
    }

    private SAMLMessageDecoder decoder() {
      return new HTTPPostSimpleSignDecoder(new BasicParserPool());
    }

    private VelocityEngine velocityEngine() {
      VelocityEngine velocityEngine = new VelocityEngine();
      velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
      velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());

      velocityEngine.init();
      return velocityEngine;
    }
  }



}
