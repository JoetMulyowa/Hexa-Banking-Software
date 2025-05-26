package org.apache.fineract.infrastructure.momo.api;

import java.io.StringReader;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.momo.data.YoPaymentResponse;
import org.apache.fineract.infrastructure.momo.util.YoPaymentXmlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Mock controller for YO Payment API for testing purposes
 */
@RestController
@RequestMapping("/mock-yo-payment")
@Slf4j
public class MockYoPaymentApiController {

    @Value("${yo.payment.api.username:90004770915}")
    private String validUsername;

    @Value("${yo.payment.api.password:3411774025}")
    private String validPassword;

    @PostMapping(consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
    public String processYoPaymentRequest(@RequestBody String xmlRequest) {
        log.info("Mock YO Payment API received request: {}", xmlRequest);

        // Extract credentials from XML
        try {
            log.info("Starting to parse XML request");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlRequest)));

            String username = getElementValue(document, "APIUsername");
            String password = getElementValue(document, "APIPassword");

            log.info("Extracted credentials - Username: {}, Valid Username: {}", username, validUsername);

            // Validate credentials
            if (!isValidCredentials(username, password)) {
                log.error("Invalid API credentials: username={}, expected={}", username, validUsername);
                return createErrorResponse("ERROR", -9999, "Invalid API Username or API Password");
            }

            log.info("Credentials validated successfully");

            // Generate a random reference number
            String transactionReference = UUID.randomUUID().toString();

            // Create a successful response
            YoPaymentResponse response = new YoPaymentResponse();
            response.setStatus("OK");
            response.setStatusCode(100);
            response.setTransactionStatus("PENDING");
            response.setTransactionReference(transactionReference);

            // Convert to XML
            String xmlResponse = YoPaymentXmlUtil.convertResponseToXml(response);
            log.info("Sending mock YO Payment response: {}", xmlResponse);

            return xmlResponse;
        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage(), e);
            return createErrorResponse("ERROR", -9999, "Invalid XML request format: " + e.getMessage());
        }
    }

    private boolean isValidCredentials(String username, String password) {
        return validUsername.equals(username) && validPassword.equals(password);
    }

    private String createErrorResponse(String status, Integer statusCode, String errorMessage) {
        YoPaymentResponse errorResponse = new YoPaymentResponse();
        errorResponse.setStatus(status);
        errorResponse.setStatusCode(statusCode);
        errorResponse.setStatusMessage(errorMessage);
        return YoPaymentXmlUtil.convertResponseToXml(errorResponse);
    }

    private String getElementValue(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return null;
        }
        Element element = (Element) nodeList.item(0);
        return element.getTextContent();
    }
}
