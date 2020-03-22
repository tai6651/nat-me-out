package th.in.meen.natmeout.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NatSideConfig {
    List<NatSideTcpConfigItem> tcp;

    public List<NatSideTcpConfigItem> getTcp() {
        return tcp;
    }

    public void setTcp(List<NatSideTcpConfigItem> tcp) {
        this.tcp = tcp;
    }
}
