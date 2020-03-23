package th.in.meen.natmeout.model.config;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class NatSideTcpConfigItem {
    private String natSideDestinationHost;
    private Integer natSideDestinationPort;
    private String tunnelProtocolClass;
    private Map<String, Object> tunnelProtocolConfig;

    public String getNatSideDestinationHost() {
        return natSideDestinationHost;
    }

    public void setNatSideDestinationHost(String natSideDestinationHost) {
        this.natSideDestinationHost = natSideDestinationHost;
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
