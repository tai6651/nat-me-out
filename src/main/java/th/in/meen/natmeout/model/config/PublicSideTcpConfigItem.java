package th.in.meen.natmeout.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicSideTcpConfigItem {
    private Integer publicSidePort;
    private String tunnelProtocolClass;
    private Map<String, Object> tunnelProtocolConfig;

    public Integer getPublicSidePort() {
        return publicSidePort;
    }

    public void setPublicSidePort(Integer publicSidePort) {
        this.publicSidePort = publicSidePort;
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
