package th.in.meen.natmeout.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */

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
