public class App {
    public static void main(String args[]) throws Exception{
        JUDDIconnector publishWebService =
                new JUDDIconnector("uddi.xml");
        String URL = "http://localhost:8000/FighterService?wsdl";
        String serviceKey = publishWebService.publish("Fighters", "Fighters characteristic", URL);
        publishWebService.getServiceListByName("");
        //publishWebService.printServiceDetail(publishWebService.getServiceDetail(serviceKey));
        publishWebService.logOut();
    }
}

