package th.in.meen.natmeout;

import java.io.File;
import java.lang.management.ManagementFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import th.in.meen.natmeout.model.CommandLineArgument;
import th.in.meen.natmeout.model.config.ApplicationConfig;
import th.in.meen.natmeout.model.config.NatSideTcpConfigItem;
import th.in.meen.natmeout.model.config.PublicSideTcpConfigItem;
import th.in.meen.natmeout.server.PublicSideTcpServer;

import th.in.meen.natmeout.server.NatSideTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public class NatMeOutApp {

    private static final Logger log = LoggerFactory.getLogger(NatMeOutApp.class);

    public static void main(String[] args) throws Exception {
        //Set PID
        System.setProperty("PID", ManagementFactory.getRuntimeMXBean().getName().toString().split("@")[0]);

        if(args == null || args.length == 0) {
            printUsage();
            System.exit(-1);
        }

        CommandLineArgument commandLineArgument = parseArgument(args);

        if(CommandLineArgument.MODE.PUBLIC.equals(commandLineArgument.getMode()))
        {
            log.info("Starting in public side mode");
            //Parse config
            ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
            ApplicationConfig applicationConfig = ymlMapper.readValue(new File(commandLineArgument.getPathToConfig()), ApplicationConfig.class);

            for(PublicSideTcpConfigItem publicSideTcpConfigItem : applicationConfig.getPublicSide().getTcp()) {
                PublicSideTcpServer publicSideTcpServer = new PublicSideTcpServer(publicSideTcpConfigItem);
            }
        }
        else if(CommandLineArgument.MODE.NAT.equals(commandLineArgument.getMode()))
        {
            log.info("Starting in nat side mode");
            //Parse config
            ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
            ApplicationConfig applicationConfig = ymlMapper.readValue(new File(commandLineArgument.getPathToConfig()), ApplicationConfig.class);

            for(NatSideTcpConfigItem natSideTcpConfigItem : applicationConfig.getNatSide().getTcp()) {
                NatSideTcpServer natSideTcpServer = new NatSideTcpServer(natSideTcpConfigItem);
            }
        }

    }


    public static CommandLineArgument parseArgument(String[] args)
    {
        CommandLineArgument commandLineArgument = new CommandLineArgument();
        for(int i = 0; i < args.length; i++)
        {
            //Single argument mode
            if(args.length == 1)
            {
                switch(args[0].toLowerCase())
                {
                    case "public":
                        commandLineArgument.setMode(CommandLineArgument.MODE.PUBLIC);
                        break;
                    case "nat":
                        commandLineArgument.setMode(CommandLineArgument.MODE.NAT);
                        break;
                    default:
                        throw new IllegalArgumentException("");
                }
                break;
            }

            //Multi argument mode
            if(args[i].startsWith("-mode"))
            {
                switch(args[i+1].toLowerCase())
                {
                    case "public":
                        commandLineArgument.setMode(CommandLineArgument.MODE.PUBLIC);
                        break;
                    case "nat":
                        commandLineArgument.setMode(CommandLineArgument.MODE.NAT);
                        break;
                    default:
                        throw new IllegalArgumentException("");
                }
                i++;
            }
            else if(args[i].startsWith("-config"))
            {
                commandLineArgument.setPathToConfig(args[i+1]);
                i++;
            }
        }

        return commandLineArgument;
    }

    private static void printBanner()
    {
        System.out.println("========================================");
        System.out.println("| NAT me out                           |");
        System.out.println("========================================");
    }
    public static void printUsage()
    {

        String currentJarFile = new java.io.File(NatMeOutApp.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();

        printBanner();
        System.out.println("Usage: java -jar "+currentJarFile+" nat|public");
        System.out.println("or java -jar "+currentJarFile+" -mode nat [-config application.yml]");
        System.out.println("or java -jar "+currentJarFile+" -mode public [-config application.yml]");
        System.out.println("Argument list:");
        System.out.println("  -mode\t\t\tOperation mode choose between nat, or public");
        System.out.println("\t\t\t\tnat is to run this application for nat side");
        System.out.println("\t\t\t\tpublic is to run this application for public side");
    }

}


