/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.upei.roblib;

import dk.defxws.fedoragsearch.server.GenericOperationsImpl;
import dk.defxws.fedoragsearch.server.TransformerToText;
import java.io.UnsupportedEncodingException;

import java.rmi.RemoteException;

import java.util.HashMap;
import java.util.Map;


import dk.defxws.fedoragsearch.server.errors.GenericSearchException;
import java.util.logging.Level;
import javax.xml.xpath.XPathExpressionException;

import org.apache.axis.AxisFault;

import org.apache.log4j.Logger;

import fedora.client.FedoraClient;


import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;
import fedora.server.types.gen.MIMETypedStream;
import java.io.ByteArrayInputStream;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author default
 * includes a method to be called during xslt transform
 * returns a nodelist or text with some UTF-8 characters removed (whitespace)
 * useful when indexing non inline xml behind xacml policies Based on Gerts gsearch work.
 */
public class DataStreamForXSLT extends GenericOperationsImpl {

  private static final Logger logger =
          Logger.getLogger(DataStreamForXSLT.class);
  private static final Map fedoraClients = new HashMap();

  public String getDatastreamTextRaw(
          String pid,
          String repositoryName,
          String dsId)
          throws GenericSearchException {
    return getDatastreamTextRaw(pid, repositoryName, dsId,
            config.getFedoraSoap(repositoryName),
            config.getFedoraUser(repositoryName),
            config.getFedoraPass(repositoryName),
            config.getTrustStorePath(repositoryName),
            config.getTrustStorePass(repositoryName));
  }

  public String getDatastreamTextRaw(
          String pid,
          String repositoryName,
          String dsId,
          String fedoraSoap,
          String fedoraUser,
          String fedoraPass,
          String trustStorePath,
          String trustStorePass)
          throws GenericSearchException {
    if (logger.isInfoEnabled()) {
      logger.info("getDatastreamTextRaw"
              + " pid=" + pid
              + " repositoryName=" + repositoryName
              + " dsId=" + dsId
              + " fedoraSoap=" + fedoraSoap
              + " fedoraUser=" + fedoraUser
              + " fedoraPass=" + fedoraPass
              + " trustStorePath=" + trustStorePath
              + " trustStorePass=" + trustStorePass);
    }
    StringBuffer dsBuffer = new StringBuffer();

    String mimetype = "";
    ds = null;
    if (dsId != null) {
      try {
        FedoraAPIA apia = getAPIA(
                repositoryName,
                fedoraSoap,
                fedoraUser,
                fedoraPass,
                trustStorePath,
                trustStorePass);
        MIMETypedStream mts = apia.getDatastreamDissemination(pid,
                dsId, null);
        if (mts == null) {
          return "";
        }
        ds = mts.getStream();
        mimetype = mts.getMIMEType();
      } catch (AxisFault e) {
        if (e.getFaultString().indexOf("DatastreamNotFoundException") > -1
                || e.getFaultString().indexOf("DefaulAccess") > -1) {
          return new String();
        } else {
          throw new GenericSearchException(e.getFaultString() + ": " + e.toString());
        }
      } catch (RemoteException e) {
        throw new GenericSearchException(e.getClass().getName() + ": " + e.toString());
      }
    }
    String dsString = null;

    if (ds != null) {
         dsBuffer = (new TransformerToText().getText(ds, mimetype));
        dsString = DataStreamForXSLT.rmNonValidChars(dsBuffer.toString());

    }
    if (logger.isDebugEnabled()) {
      logger.debug("getDatastreamTextRaw"
              + " pid=" + pid
              + " dsId=" + dsId
              + " mimetype=" + mimetype
              + " dsBuffer=" + dsString);
    }
    return dsString;
  }


//removes UTF-8 characters that are not valid xml maybe a better way to do this but this is what we have for now
  public static String rmNonValidChars(String str) {

    if (str == null) {
      return null;
    }

    StringBuffer s = new StringBuffer();

    for (char c : str.toCharArray()) {

      if ((c == 0x9) || (c == 0xA) || (c == 0xD)
              || ((c >= 0x20) && (c <= 0xD7FF))
              || ((c >= 0xE000) && (c <= 0xFFFD))
              || ((c >= 0x10000) && (c <= 0x10FFFF))) {

        s.append(c);

      }

    }

    return s.toString();

  }

  public NodeList getXMLDatastreamASNodeList(
          String pid,
          String repositoryName,
          String dsId,
          String fedoraSoap,
          String fedoraUser,
          String fedoraPass,
          String trustStorePath,
          String trustStorePass)
          throws GenericSearchException {
    if (logger.isInfoEnabled()) {
      logger.info("getDatastreamTextAsNodeList"
              + " pid=" + pid
              + " repositoryName=" + repositoryName
              + " dsId=" + dsId
              + " fedoraSoap=" + fedoraSoap
              + " fedoraUser=" + fedoraUser
              + " fedoraPass=" + fedoraPass
              + " trustStorePath=" + trustStorePath
              + " trustStorePass=" + trustStorePass);
    }

    String mimetype = "";
    ds = null;
    if (dsId != null) {
      try {
        FedoraAPIA apia = getAPIA(
                repositoryName,
                fedoraSoap,
                fedoraUser,
                fedoraPass,
                trustStorePath,
                trustStorePass);
        MIMETypedStream mts = apia.getDatastreamDissemination(pid,
                dsId, null);
        if (mts == null) {
          return null;
        }
        ds = mts.getStream();
        mimetype = mts.getMIMEType();
      } catch (AxisFault e) {
        if (e.getFaultString().indexOf("DatastreamNotFoundException") > -1
                || e.getFaultString().indexOf("DefaulAccess") > -1) {
          return null;
        } else {
          throw new GenericSearchException(e.getFaultString() + ": " + e.toString());
        }
      } catch (RemoteException e) {
        throw new GenericSearchException(e.getClass().getName() + ": " + e.toString());
      }
    }


    XPath xpath = XPathFactory.newInstance().newXPath();
    String xpathExpression = "/";
    if (ds == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("getDatastreamasNodeList"
                + " pid=" + pid
                + " dsId=" + dsId
                + " mimetype=" + mimetype
                + " dsBuffer=null");
      }
      return null;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("getDatastreamTextAsNodeList"
              + " pid=" + pid
              + " dsId=" + dsId
              + " mimetype=" + mimetype
              + " dsBuffer=" + new String(ds));
    }

    InputSource inputSource = new InputSource(new ByteArrayInputStream(ds));
    NodeList nodes = null;
    try {
      nodes = (NodeList) xpath.evaluate(xpathExpression, inputSource, XPathConstants.NODESET);
    } catch (XPathExpressionException ex) {
      java.util.logging.Logger.getLogger(DataStreamForXSLT.class.getName()).log(Level.SEVERE, null, ex);
    }
    return nodes;
  }

  private static FedoraAPIA getAPIA(
          String repositoryName,
          String fedoraSoap,
          String fedoraUser,
          String fedoraPass,
          String trustStorePath,
          String trustStorePass)
          throws GenericSearchException {
    if (trustStorePath != null) {
      System.setProperty("javax.net.ssl.trustStore", trustStorePath);
    }
    if (trustStorePass != null) {
      System.setProperty("javax.net.ssl.trustStorePassword", trustStorePass);
    }
    FedoraClient client = getFedoraClient(repositoryName, fedoraSoap, fedoraUser, fedoraPass);
    try {
      return client.getAPIA();
    } catch (Exception e) {
      throw new GenericSearchException("Error getting API-A stub"
              + " for repository: " + repositoryName, e);
    }
  }

  private static FedoraAPIM getAPIM(
          String repositoryName,
          String fedoraSoap,
          String fedoraUser,
          String fedoraPass,
          String trustStorePath,
          String trustStorePass)
          throws GenericSearchException {
    if (trustStorePath != null) {
      System.setProperty("javax.net.ssl.trustStore", trustStorePath);
    }
    if (trustStorePass != null) {
      System.setProperty("javax.net.ssl.trustStorePassword", trustStorePass);
    }
    FedoraClient client = getFedoraClient(repositoryName, fedoraSoap, fedoraUser, fedoraPass);
    try {
      return client.getAPIM();
    } catch (Exception e) {
      throw new GenericSearchException("Error getting API-M stub"
              + " for repository: " + repositoryName, e);
    }
  }

  private static FedoraClient getFedoraClient(
          String repositoryName,
          String fedoraSoap,
          String fedoraUser,
          String fedoraPass)
          throws GenericSearchException {
    try {
      String baseURL = getBaseURL(fedoraSoap);
      String user = fedoraUser;
      String clientId = user + "@" + baseURL;
      synchronized (fedoraClients) {
        if (fedoraClients.containsKey(clientId)) {
          return (FedoraClient) fedoraClients.get(clientId);
        } else {
          FedoraClient client = new FedoraClient(baseURL,
                  user, fedoraPass);
          fedoraClients.put(clientId, client);
          return client;
        }
      }
    } catch (Exception e) {
      throw new GenericSearchException("Error getting FedoraClient"
              + " for repository: " + repositoryName, e);
    }
  }

  private static String getBaseURL(String fedoraSoap)
          throws Exception {
    final String end = "/services";
    String baseURL = fedoraSoap;
    if (fedoraSoap.endsWith(end)) {
      return fedoraSoap.substring(0, fedoraSoap.length() - end.length());
    } else {
      throw new Exception("Unable to determine baseURL from fedoraSoap"
              + " value (expected it to end with '" + end + "'): "
              + fedoraSoap);
    }
  }
}
