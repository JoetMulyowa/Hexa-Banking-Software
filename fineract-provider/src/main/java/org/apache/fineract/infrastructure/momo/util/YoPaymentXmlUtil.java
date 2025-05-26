/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.momo.util;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.fineract.infrastructure.momo.data.YoPaymentRequest;
import org.apache.fineract.infrastructure.momo.data.YoPaymentResponse;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.math.BigDecimal;

/**
 * Utility class for handling XML for Yo Payment API
 */
@Component
public class YoPaymentXmlUtil {

    /**
     * Convert YoPaymentRequest object to XML string format required by Yo Payment API
     *
     * @param request
     *            The request object
     * @return XML string representation of the request
     */
    public static String convertRequestToXml(YoPaymentRequest request) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("AutoCreate");
            doc.appendChild(rootElement);

            // Request element
            Element requestElement = doc.createElement("Request");
            rootElement.appendChild(requestElement);

            // Create and append required elements
            addElement(doc, requestElement, "APIUsername", request.getApiUsername());
            addElement(doc, requestElement, "APIPassword", request.getApiPassword());
            addElement(doc, requestElement, "Method", request.getMethod());

            // Add transaction-specific elements for withdrawal or deposit
            if ("acwithdrawfunds".equals(request.getMethod()) || "acdepositfunds".equals(request.getMethod())) {
                addElement(doc, requestElement, "NonBlocking", request.getNonBlocking());
                addElement(doc, requestElement, "Amount", request.getAmount().toString());
                addElement(doc, requestElement, "Account", request.getAccount());

                if (request.getAccountProviderCode() != null) {
                    addElement(doc, requestElement, "AccountProviderCode", request.getAccountProviderCode());
                }

                addElement(doc, requestElement, "Narrative", request.getNarrative());

                if (request.getExternalReference() != null) {
                    addElement(doc, requestElement, "ExternalReference", request.getExternalReference());
                }
            } else if ("actransactioncheckstatus".equals(request.getMethod())) {
                // For transaction status check
                addElement(doc, requestElement, "ExternalReference", request.getExternalReference());
            }

            // Convert to XML string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error creating XML request", e);
        }
    }

    /**
     * Parse XML response from Yo Payment API into YoPaymentResponse object
     *
     * @param xmlResponse
     *            The XML response from the API
     * @return The parsed response object
     */
    public static YoPaymentResponse parseXmlResponse(String xmlResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlResponse)));
            document.getDocumentElement().normalize();

            // Get the Response element
            NodeList responseList = document.getElementsByTagName("Response");
            if (responseList.getLength() == 0) {
                throw new RuntimeException("Invalid XML response format: missing Response element");
            }

            Node responseNode = responseList.item(0);
            if (responseNode.getNodeType() != Node.ELEMENT_NODE) {
                throw new RuntimeException("Invalid XML response format: Response is not an element");
            }

            Element responseElement = (Element) responseNode;
            YoPaymentResponse response = new YoPaymentResponse();

            // Parse the common fields
            response.setStatus(getElementValue(responseElement, "Status"));
            response.setStatusCode(parseIntOrNull(getElementValue(responseElement, "StatusCode")));
            response.setStatusMessage(getElementValue(responseElement, "StatusMessage"));
            response.setErrorMessageCode(parseIntOrNull(getElementValue(responseElement, "ErrorMessageCode")));
            response.setErrorMessage(getElementValue(responseElement, "ErrorMessage"));
            response.setTransactionStatus(getElementValue(responseElement, "TransactionStatus"));
            response.setTransactionReference(getElementValue(responseElement, "TransactionReference"));
            response.setMNOTransactionReferenceId(getElementValue(responseElement, "MNOTransactionReferenceId"));

            // Parse additional fields if present
            response.setAccountNumber(getElementValue(responseElement, "AccountNumber"));
            response.setAmountWithdrawn(getElementValue(responseElement, "AmountWithdrawn"));
            response.setBalance(getElementValue(responseElement, "Balance"));
            response.setCurrency(getElementValue(responseElement, "Currency"));
            response.setNarrativeText(getElementValue(responseElement, "NarrativeText"));

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing XML response", e);
        }
    }

    /**
     * Parse XML request into YoPaymentRequest object
     *
     * @param xmlRequest
     *            The XML request to parse
     * @return The parsed request object
     */
    public static YoPaymentRequest parseXmlRequest(String xmlRequest) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlRequest)));
            document.getDocumentElement().normalize();

            // Get the Request element
            NodeList requestList = document.getElementsByTagName("Request");
            if (requestList.getLength() == 0) {
                throw new RuntimeException("Invalid XML request format: missing Request element");
            }

            Node requestNode = requestList.item(0);
            if (requestNode.getNodeType() != Node.ELEMENT_NODE) {
                throw new RuntimeException("Invalid XML request format: Request is not an element");
            }

            Element requestElement = (Element) requestNode;
            YoPaymentRequest request = new YoPaymentRequest();

            // Parse the common fields
            request.setApiUsername(getElementValue(requestElement, "APIUsername"));
            request.setApiPassword(getElementValue(requestElement, "APIPassword"));
            request.setMethod(getElementValue(requestElement, "Method"));
            
            // Parse transaction-specific fields for withdrawal or deposit
            if ("acwithdrawfunds".equals(request.getMethod()) || "acdepositfunds".equals(request.getMethod())) {
                request.setNonBlocking(getElementValue(requestElement, "NonBlocking"));
                String amountStr = getElementValue(requestElement, "Amount");
                if (amountStr != null) {
                    request.setAmount(new BigDecimal(amountStr));
                }
                request.setAccount(getElementValue(requestElement, "Account"));
                request.setAccountProviderCode(getElementValue(requestElement, "AccountProviderCode"));
                request.setNarrative(getElementValue(requestElement, "Narrative"));
                request.setExternalReference(getElementValue(requestElement, "ExternalReference"));
            } else if ("actransactioncheckstatus".equals(request.getMethod())) {
                request.setExternalReference(getElementValue(requestElement, "ExternalReference"));
            }

            return request;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing XML request", e);
        }
    }

    /**
     * Convert YoPaymentResponse object to XML string format
     *
     * @param response
     *            The response object
     * @return XML string representation of the response
     */
    public static String convertResponseToXml(YoPaymentResponse response) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("AutoCreate");
            doc.appendChild(rootElement);

            // Response element
            Element responseElement = doc.createElement("Response");
            rootElement.appendChild(responseElement);

            // Create and append response elements
            addElement(doc, responseElement, "Status", response.getStatus());
            if (response.getStatusCode() != null) {
                addElement(doc, responseElement, "StatusCode", response.getStatusCode().toString());
            }
            addElement(doc, responseElement, "StatusMessage", response.getStatusMessage());
            if (response.getErrorMessageCode() != null) {
                addElement(doc, responseElement, "ErrorMessageCode", response.getErrorMessageCode().toString());
            }
            addElement(doc, responseElement, "ErrorMessage", response.getErrorMessage());
            addElement(doc, responseElement, "TransactionStatus", response.getTransactionStatus());
            addElement(doc, responseElement, "TransactionReference", response.getTransactionReference());
            addElement(doc, responseElement, "MNOTransactionReferenceId", response.getMNOTransactionReferenceId());
            addElement(doc, responseElement, "AccountNumber", response.getAccountNumber());
            addElement(doc, responseElement, "AmountWithdrawn", response.getAmountWithdrawn());
            addElement(doc, responseElement, "Balance", response.getBalance());
            addElement(doc, responseElement, "Currency", response.getCurrency());
            addElement(doc, responseElement, "NarrativeText", response.getNarrativeText());

            // Convert to XML string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error creating XML response", e);
        }
    }

    /**
     * Helper method to add an element to a parent element
     */
    private static void addElement(Document doc, Element parent, String elementName, String textContent) {
        if (textContent != null) {
            Element element = doc.createElement(elementName);
            element.setTextContent(textContent);
            parent.appendChild(element);
        }
    }

    /**
     * Helper method to get an element value from an XML element
     */
    private static String getElementValue(Element parent, String elementName) {
        NodeList nodeList = parent.getElementsByTagName(elementName);
        if (nodeList.getLength() == 0) {
            return null;
        }

        return nodeList.item(0).getTextContent();
    }

    /**
     * Helper method to parse a string to Integer or return null
     */
    private static Integer parseIntOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
