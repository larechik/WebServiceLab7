import org.apache.juddi.api_v3.AccessPointType;
import org.apache.juddi.v3.client.UDDIConstants;
import org.apache.juddi.v3.client.config.UDDIClerk;
import org.apache.juddi.v3.client.config.UDDIClient;
import org.uddi.api_v3.*;
import org.uddi.v3_service.UDDIInquiryPortType;
import org.uddi.v3_service.UDDISecurityPortType;
import org.w3c.dom.Document;

import javax.xml.bind.JAXB;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static java.lang.System.exit;

public class JUDDIconnector {
    private UDDIInquiryPortType inquiry;
    private UDDISecurityPortType security;
    private UDDIClerk clerk;

    public JUDDIconnector(String pathToUddiSettings) {
        try {
            UDDIClient uddiClient = new UDDIClient(pathToUddiSettings);
            this.clerk = uddiClient.getClerk("default");

            security = uddiClient.getTransport("default").getUDDISecurityService();
            inquiry = uddiClient.getTransport("default").getUDDIInquiryService();

            if (clerk == null)
                throw new Exception("the clerk wasn't found, check the config file!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BusinessEntity registerBusiness(String businessName){
        //create business
        BusinessEntity businessEntity = new BusinessEntity();
        businessEntity.getName().add(getName(businessName, "ru-ru"));
        businessEntity.getDescription().add(getDescription(businessName, "ru-ru"));
        //register business
        BusinessEntity register = clerk.register(businessEntity);
        if (register == null) {
            System.out.println("Error register of business");
            return null;
        }
        String businessKey = register.getBusinessKey();
        System.out.println("business key:  " + businessKey);
        return register;
    }

    private BusinessService registerService(String businessKey, String servName, String URL){
        //create and add service
        BindingTemplate bindingTemplate = new BindingTemplate();
        //добавялем путь к WSDL описанию веб-сервиса
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setUseType(AccessPointType.WSDL_DEPLOYMENT.toString());
        accessPoint.setValue(URL);
        bindingTemplate.setAccessPoint(accessPoint);

        BindingTemplates bindingTemplates = new BindingTemplates();
        bindingTemplates.getBindingTemplate().add(bindingTemplate);

        BusinessService businessService = new BusinessService();
        businessService.getName().add(getName(servName, "ru-ru"));
        businessService.getDescription().add(getDescription(servName, "ru-ru"));
        businessService.setBusinessKey(businessKey);
        businessService.setBindingTemplates(bindingTemplates);
        //register service
        BusinessService svc = clerk.register(businessService);

        return svc;
    }


    public String publish(String businessName,
                          String serviceName,
                          String URL){
        try {
            BusinessEntity businessEntity = this.registerBusiness(businessName);
            if(businessEntity == null){
                System.out.println("Error register of business");
                exit(1);
            }

            BusinessService svc = registerService(businessEntity.getBusinessKey(),
                    serviceName,
                    URL);
            if (svc==null){
                System.out.println("Unrecognaize error in business registration");
                exit(1);
            }

            String serviceKey = svc.getServiceKey();
            System.out.println(String.format("ServiceKey %s for service %s", serviceKey, serviceName));

            System.out.println("Service registration is successful.");
            return serviceKey;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //found business
    public BusinessList getBusinessListByName(String nameBusiness) throws Exception{
        FindBusiness findBusiness = new FindBusiness();
        findBusiness.setAuthInfo(clerk.getAuthToken());

        FindQualifiers findQualifiers = new FindQualifiers();
        findQualifiers.getFindQualifier().add(UDDIConstants.APPROXIMATE_MATCH);

        findBusiness.setFindQualifiers(findQualifiers);

        Name searchName = getName("%" + nameBusiness + "%", "ru-ru");
        findBusiness.getName().add(searchName);

        BusinessList businessList = inquiry.findBusiness(findBusiness);

        return businessList;
    }

    public ServiceList getServiceListByName(String nameService) throws Exception{
        FindService findService = new FindService();
        findService.setAuthInfo(clerk.getAuthToken());

        FindQualifiers findQualifiers = new FindQualifiers();
        findQualifiers.getFindQualifier().add(UDDIConstants.APPROXIMATE_MATCH);
        findService.setFindQualifiers(findQualifiers);

        Name searchName = new Name();
        searchName.setValue("%" + nameService + "%");

        findService.getName().add(searchName);

        ServiceList serviceList = inquiry.findService(findService);

        return serviceList;
    }

    public BusinessEntity getBusinessDetail(String businessKey) throws Exception{
        BusinessEntity businessEntity = clerk.getBusinessDetail(businessKey);
        return businessEntity;
    }

    public BusinessService getServiceDetail(String serviceKey) throws Exception{
        BusinessService businessService = clerk.getServiceDetail(serviceKey);
        return businessService;
    }

    public void printServiceWSDLINFOToConsole(String WSDL_url) throws Exception{
        URL url = new URL(WSDL_url);
        URLConnection conn = url.openConnection();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(conn.getInputStream());

        TransformerFactory factory1 = TransformerFactory.newInstance();
        Transformer xform = factory1.newTransformer();

        File file = new File("write.xml");
        if(!file.exists()){
            file.createNewFile();
        }

        StreamResult streamResult = new StreamResult();
        streamResult.setSystemId(file);

        xform.transform(new DOMSource(document), streamResult);
        xform.transform(new DOMSource(document), new StreamResult(System.out));
    }

    public void printServiceDetail(BusinessService businessService) throws Exception{
        if(businessService == null){
            return;
        }
        System.out.println("Название: " + ListToString(businessService.getName()));
        System.out.println("Описание: " + ListToDescString(businessService.getDescription()));
        System.out.println("Ключ: " + (businessService.getServiceKey()));

        JAXB.marshal(businessService, System.out);

        printServiceWSDLINFOToConsole(businessService.getBindingTemplates().getBindingTemplate().get(0).getAccessPoint().getValue());

    }
    private Name getName(String name,
                         String lang){
        Name busName = new Name();
        busName.setValue(name);
        busName.setLang(lang);
        return busName;
    }
    private String ListToString(List<Name> name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.size(); i++) {
            sb.append(name.get(i).getValue()).append(" ");
        }
        return sb.toString();
    }

    private Description getDescription(String descr, String lang){
        Description description = new Description();
        description.setValue(descr);
        description.setLang(lang);
        return description;
    }

    private String ListToDescString(List<Description> name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.size(); i++) {
            sb.append(name.get(i).getValue()).append(" ");
        }
        return sb.toString();
    }

    public void removeBusiness(String businessKey){
        clerk.unRegisterBusiness(businessKey);
    }


    public void logOut(){
        clerk.discardAuthToken();
    }
}

