package th.in.meen.natmeout.model.config;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationConfig {
    private PublicSideConfig publicSide;
    private NatSideConfig natSide;

    public PublicSideConfig getPublicSide() {
        return publicSide;
    }

    public void setPublicSide(PublicSideConfig publicSide) {
        this.publicSide = publicSide;
    }

    public NatSideConfig getNatSide() {
        return natSide;
    }

    public void setNatSide(NatSideConfig natSide) {
        this.natSide = natSide;
    }
}
