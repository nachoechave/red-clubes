package com.redclubes.backend.netpagos;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class NetPagosConfigTests {
    NetPagosConfig config(NetPagosConfig.Mode mode, String key, boolean settlement, boolean confirmed, long actor, String... profiles) {
        var env = new MockEnvironment(); env.setActiveProfiles(profiles);
        return new NetPagosConfig(mode, key, 1, false, settlement, confirmed, actor, env);
    }
    @Test void defaultsDoNotEnableProviderOrSettlement() {
        var c = config(NetPagosConfig.Mode.OFF, "", false, false, 0, "prod");
        assertEquals(NetPagosConfig.Mode.OFF, c.mode); assertFalse(c.settlementEnabled);
    }
    @Test void cannotEnableSimulationInProductionOrWithoutExplicitSandboxProfile() {
        assertThrows(IllegalArgumentException.class, () -> config(NetPagosConfig.Mode.SIMULATED, "", false, false, 0, "prod", "netpagos-sandbox"));
        assertThrows(IllegalArgumentException.class, () -> config(NetPagosConfig.Mode.SIMULATED, "", false, false, 0, "test"));
    }
    @Test void stagingNeedsCredentialsAndSettlementNeedsExplicitContractAndActor() {
        assertThrows(IllegalArgumentException.class, () -> config(NetPagosConfig.Mode.STAGING, "", false, false, 0, "netpagos-sandbox"));
        assertThrows(IllegalArgumentException.class, () -> config(NetPagosConfig.Mode.STAGING, "fictional", true, false, 999, "netpagos-sandbox"));
        assertThrows(IllegalArgumentException.class, () -> config(NetPagosConfig.Mode.STAGING, "fictional", true, true, 0, "netpagos-sandbox"));
        assertDoesNotThrow(() -> config(NetPagosConfig.Mode.STAGING, "fictional", true, true, 999, "netpagos-sandbox"));
    }
}
