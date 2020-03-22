package th.in.meen.natmeout.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicSideConfig {
    List<PublicSideTcpConfigItem> tcp;

    public List<PublicSideTcpConfigItem> getTcp() {
        return tcp;
    }

    public void setTcp(List<PublicSideTcpConfigItem> tcp) {
        this.tcp = tcp;
    }
}
