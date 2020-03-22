package th.in.meen.natmeout.model.config;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NatSideTcpConfigItem {
    private String natSideDestinationIp;
    private Integer natSideDestinationPort;
    private String tunnelProtocolClass;
    private Map<String, Object> tunnelProtocolConfig;

    public String getNatSideDestinationIp() {
        return natSideDestinationIp;
    }

    public void setNatSideDestinationIp(String natSideDestinationIp) {
        this.natSideDestinationIp = natSideDestinationIp;
    }

    public Integer getNatSideDestinationPort() {
        return natSideDestinationPort;
    }

    public void setNatSideDestinationPort(Integer natSideDestinationPort) {
        this.natSideDestinationPort = natSideDestinationPort;
    }

    public String getTunnelProtocolClass() {
        return tunnelProtocolClass;
    }

    public void setTunnelProtocolClass(String tunnelProtocolClass) {
        this.tunnelProtocolClass = tunnelProtocolClass;
    }

    public Map<String, Object> getTunnelProtocolConfig() {
        return tunnelProtocolConfig;
    }

    public void setTunnelProtocolConfig(Map<String, Object> tunnelProtocolConfig) {
        this.tunnelProtocolConfig = tunnelProtocolConfig;
    }
}
