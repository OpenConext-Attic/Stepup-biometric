package idp.saml;

import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.saml2.core.*;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.schema.XSString;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

public class SAMLBuilder {

  private static final XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

  @SuppressWarnings({"unused", "unchecked"})
  public static <T> T buildSAMLObject(final Class<T> objectClass, QName qName) {
    return (T) builderFactory.getBuilder(qName).buildObject(qName);
  }

  public static Issuer buildIssuer(String issuingEntityName) {
    Issuer issuer = buildSAMLObject(Issuer.class, Issuer.DEFAULT_ELEMENT_NAME);
    issuer.setValue(issuingEntityName);
    issuer.setFormat(NameIDType.ENTITY);
    return issuer;
  }

  public static Subject buildSubject(String subjectNameId, String recipient, String inResponseTo, String clientIp) {
    NameID nameID = buildSAMLObject(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
    nameID.setValue(subjectNameId);
    nameID.setFormat(NameIDType.PERSISTENT);

    Subject subject = buildSAMLObject(Subject.class, Subject.DEFAULT_ELEMENT_NAME);
    subject.setNameID(nameID);

    SubjectConfirmation subjectConfirmation = buildSAMLObject(SubjectConfirmation.class, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
    subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);

    SubjectConfirmationData subjectConfirmationData = buildSAMLObject(SubjectConfirmationData.class, SubjectConfirmationData.DEFAULT_ELEMENT_NAME);

    subjectConfirmationData.setRecipient(recipient);
    subjectConfirmationData.setInResponseTo(inResponseTo);
    subjectConfirmationData.setNotOnOrAfter(new DateTime().plusSeconds(90));
    subjectConfirmationData.setAddress(clientIp);

    subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);

    subject.getSubjectConfirmations().add(subjectConfirmation);

    return subject;
  }

  public static Status buildStatus(String value) {
    Status status = buildSAMLObject(Status.class, Status.DEFAULT_ELEMENT_NAME);
    StatusCode statusCode = buildSAMLObject(StatusCode.class, StatusCode.DEFAULT_ELEMENT_NAME);
    statusCode.setValue(value);
    status.setStatusCode(statusCode);
    return status;
  }

  public static Assertion buildAssertion(SAMLAuthenticationToken token, String entityId, String spEntityId, String spMetaDataUrl) {
    Assertion assertion = buildSAMLObject(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);
    Subject subject = buildSubject(token.getNameId(), token.getAssertionConsumerServiceURL(), token.getId(), token.getClientIpAddress());
    Issuer issuer = buildIssuer(entityId);

    Audience audience = buildSAMLObject(Audience.class, Audience.DEFAULT_ELEMENT_NAME);
    audience.setAudienceURI(spMetaDataUrl);
    AudienceRestriction audienceRestriction = buildSAMLObject(AudienceRestriction.class, AudienceRestriction.DEFAULT_ELEMENT_NAME);
    audienceRestriction.getAudiences().add(audience);

    Conditions conditions = buildSAMLObject(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);
    conditions.getAudienceRestrictions().add(audienceRestriction);
    assertion.setConditions(conditions);

    AuthnStatement authnStatement = buildAuthnStatement(token.getCreationTime(), entityId);

    assertion.setIssuer(issuer);
    assertion.getAuthnStatements().add(authnStatement);
    assertion.setSubject(subject);

    Map<String, List<String>> attributes = singletonMap("urn:mace:dir:attribute-def:uid", singletonList(token.getNameId()));
    assertion.getAttributeStatements().add(buildAttributeStatement(attributes));

    assertion.setID(UUID.randomUUID().toString());
    assertion.setIssueInstant(new DateTime());

    return assertion;
  }

  public static AuthnStatement buildAuthnStatement(DateTime authnInstant, String entityID) {
    AuthnContextClassRef authnContextClassRef = buildSAMLObject(AuthnContextClassRef.class,AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
    authnContextClassRef.setAuthnContextClassRef(AuthnContext.PASSWORD_AUTHN_CTX);

    AuthenticatingAuthority authenticatingAuthority = buildSAMLObject(AuthenticatingAuthority.class,AuthenticatingAuthority.DEFAULT_ELEMENT_NAME);
    authenticatingAuthority.setURI(entityID);

    AuthnContext authnContext = buildSAMLObject(AuthnContext.class,AuthnContext.DEFAULT_ELEMENT_NAME);
    authnContext.setAuthnContextClassRef(authnContextClassRef);
    authnContext.getAuthenticatingAuthorities().add(authenticatingAuthority);

    AuthnStatement authnStatement = buildSAMLObject(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
    authnStatement.setAuthnContext(authnContext);

    authnStatement.setAuthnInstant(authnInstant);

    return authnStatement;

  }

  public static AttributeStatement buildAttributeStatement(final Map<String, List<String>> attributes) {
    AttributeStatement attributeStatement = buildSAMLObject(AttributeStatement.class, AttributeStatement.DEFAULT_ELEMENT_NAME);

    attributes.entrySet().forEach(entry ->
        attributeStatement.getAttributes().add(
            buildAttribute(
                entry.getKey(),
                entry.getValue().stream().map(SAMLBuilder::buildXSString).collect(toList()))));

    return attributeStatement;
  }

  private static Attribute buildAttribute(String name, List<XSString> values) {
    Attribute attribute = buildSAMLObject(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
    attribute.setName(name);
    attribute.getAttributeValues().addAll(values);
    return attribute;
  }

  private static XSString buildXSString(String value) {
    XSString stringValue = buildSAMLObject(XSString.class, XSString.TYPE_NAME);
    stringValue.setValue(value);
    return stringValue;
  }
}
