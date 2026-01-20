package pe.com.movimientos.facturacionsunat.config;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.gob.sunat.service.factura.BillService;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SunatSoapConfig {

    @Value("${sunat.endpoint.url}")
    private String endpointUrl;

    @Value("${sunat.sol.usuario}")
    private String solUsuario;

    @Value("${sunat.sol.clave}")
    private String solClave;

    @Value("${sunat.ruc}")
    private String ruc;

    @Bean
    public BillService billService() {
        // Crear proxy usando JaxWsProxyFactoryBean (no requiere WSDL en runtime)
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(BillService.class);
        factory.setAddress(endpointUrl);

        BillService port = (BillService) factory.create();

        // Configurar autenticación
        BindingProvider bindingProvider = (BindingProvider) port;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();

        // Usuario SOL: RUC + Usuario
        String username = ruc + solUsuario;
        requestContext.put(BindingProvider.USERNAME_PROPERTY, username);
        requestContext.put(BindingProvider.PASSWORD_PROPERTY, solClave);

        return port;
    }
}
