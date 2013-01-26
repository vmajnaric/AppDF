package org.onepf.appdf.parser;

import static org.onepf.appdf.parser.util.XmlUtil.extractChildElements;
import static org.onepf.appdf.parser.util.XmlUtil.getOptionalAttributeValue;
import static org.onepf.appdf.parser.util.XmlUtil.tagNameToFieldName;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.onepf.appdf.model.ContentDescription;
import org.onepf.appdf.model.ContentDescriptor;
import org.onepf.appdf.model.ContentDescriptor.DescriptorValue;
import org.onepf.appdf.model.ContentRating;
import org.onepf.appdf.model.IncludedActivites;
import org.onepf.appdf.model.RatingCertificate;
import org.onepf.appdf.model.RatingCertificate.CertificateType;
import org.onepf.appdf.parser.util.XmlUtil;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public enum ContentDescriptionTag implements NodeParser<ContentDescription> {
    CONTENT_RATING {
        @Override
        public void parse(Node node, ContentDescription element)
                throws ParsingException {
            String rating = node.getTextContent();
            try {
                int digitalRating = Integer.parseInt(rating);
                ContentRating contentRating = ContentRating
                        .getByDigital(digitalRating);
                if (contentRating == null) {
                    throw new ParsingException("Unsupported content rating:"
                            + digitalRating);
                }
                element.setContentRating(contentRating);
            } catch (NumberFormatException e) {
                throw new ParsingException("Non numeric rating:" + rating);
            }
        }

    },
    RATING_CERTIFICATES{
        
        private final static String RATING_CERTIFICATE_TAG = "rating-certificate";
        private final static String TYPE_ATTR = "type";
        private final static String CERTIFICATE_ATTR = "certificate";
        private final static String MARK_ATTR = "mark";

        @Override
        public void parse(Node node, ContentDescription element)
                throws ParsingException {
            List<Node> children = XmlUtil.extractChildElements(node);
            for ( Node child : children ){
              String tagName = child.getNodeName();
              if ( ! RATING_CERTIFICATE_TAG.equalsIgnoreCase(tagName)){
                  throw new ParsingException("Unsupported tag inside rating-certificates:" + tagName);
              }
              NamedNodeMap attributes = child.getAttributes();
              Node typeAttribute = attributes.getNamedItem(TYPE_ATTR);
              if (typeAttribute == null ){
                  throw new ParsingException("Required attribute " +TYPE_ATTR + " missing" );
              }
              String typeName = typeAttribute.getTextContent().toUpperCase();
              RatingCertificate cert = new RatingCertificate();
              try{                  
                CertificateType type = RatingCertificate.CertificateType.valueOf(typeName);
                cert.setType(type);
              }catch(IllegalArgumentException iae){
                  throw new ParsingException("Unsupported certificate type:" + typeName);
              }
              cert.setValue(child.getTextContent());
              cert.setCertificate(getOptionalAttributeValue(attributes, CERTIFICATE_ATTR));
              cert.setMark(getOptionalAttributeValue(attributes, MARK_ATTR));
              element.addRatingCertificate(cert);
            }
        }
        
    },
    CONTENT_DESCRIPTORS{

        @Override
        public void parse(Node node, ContentDescription element)
                throws ParsingException {
            ContentDescriptor descriptor = new ContentDescriptor();
            List<Node> childNodes = extractChildElements(node);
            for(Node child : childNodes){
                String tagName = child.getNodeName();
                String originalTagName = tagName;
                tagName = tagNameToFieldName(tagName);
                String childValue = child.getTextContent();
                try {
                    PropertyDescriptor pd  = new PropertyDescriptor(tagName, ContentDescriptor.class);
                    DescriptorValue value = DescriptorValue.valueOf(childValue.toUpperCase());
                    Method writeMethod = pd.getWriteMethod();
                    writeMethod.invoke(descriptor, value);
                } catch (IntrospectionException e) {                 
                   throw new ParsingException("Unexpected descriptor:" + originalTagName); 
                } catch (IllegalArgumentException iae){
                    throw new ParsingException("Illegal descriptor value:" + childValue);
                } catch (IllegalAccessException e) {
                    throw new ParsingException(e);
                } catch (InvocationTargetException e) {
                    throw new ParsingException(e);
                }
            }
            element.setContentDescriptor(descriptor);
        }
    },
    
    INCLUDED_ACTIVITIES{

        @Override
        public void parse(Node node, ContentDescription element)
                throws ParsingException {
            IncludedActivites activites = new IncludedActivites();
            List<Node> childNodes = extractChildElements(node);
            for(Node child : childNodes){
                String tagName = child.getNodeName();
                String originalTagName = tagName;
                tagName = tagNameToFieldName(tagName);
                String childValue = child.getTextContent();
                try {
                    PropertyDescriptor pd  = new PropertyDescriptor(tagName, IncludedActivites.class);
                    Method writeMethod = pd.getWriteMethod();
                    boolean boolValue = "yes".equalsIgnoreCase(childValue);
                    writeMethod.invoke(activites, boolValue);
                } catch (IntrospectionException e) {
                    e.printStackTrace();
                   throw new ParsingException("Unexpected descriptor:" + originalTagName); 
                } catch (IllegalArgumentException iae){
                    throw new ParsingException("Illegal descriptor value:" + childValue);
                } catch (IllegalAccessException e) {
                    throw new ParsingException(e);
                } catch (InvocationTargetException e) {
                    throw new ParsingException(e);
                }
            }
        }
        
    }
    
}
