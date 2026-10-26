package com.redclubes.backend.netpagos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;

@Configuration
public class NetPagosBeans {
    @Bean NetPagosGateway netPagosGateway(NetPagosConfig config, ObjectMapper mapper) {
        return config.mode == NetPagosConfig.Mode.SIMULATED ? new NetPagosSimulator() : new NetPagosHttpGateway(config.apiKey, mapper);
    }
}
