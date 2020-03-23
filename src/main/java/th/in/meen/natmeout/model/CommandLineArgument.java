package th.in.meen.natmeout.model;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public class CommandLineArgument {
    public enum MODE
    {
        NAT,
        PUBLIC
    }
    private String pathToConfig;

    private MODE mode;

    public MODE getMode() {
        return mode;
    }

    public void setMode(MODE mode) {
        this.mode = mode;
    }

    public String getPathToConfig() {
        if(pathToConfig == null)
            return "application.yml";
        return pathToConfig;
    }

    public void setPathToConfig(String pathToConfig) {
        this.pathToConfig = pathToConfig;
    }

}
