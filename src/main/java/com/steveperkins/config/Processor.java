package com.steveperkins.config;

import com.ecwid.consul.v1.ConsulClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

/**
 * <p>The command-line runnable class responsible for reading properties files from Git / the filesystem, and
 * loading them into Consul for retrieval by the various applications.</p>
 *
 * <p>Properties files are stored underneath the "properties" directory.  There is a subdirectory for each
 * environment (e.g. "staging", "prod"), and a file within each subdirectory for each application in
 * that environment (e.g. "sampleapp", "reportbuilder", "streamingthingy").</p>
 *
 * This class is invoked from the Gradle "runProcessor" task.  You might setup a continuous integration server
 * job to do this every time the Git repository is updated.
 */
public class Processor {

    /**
     * @param args Should contain one argument: the host or IP address of your Consul agent (e.g. 127.0.0.1)
     */
    public static void main(String args[]) {
        final ConsulClient client = new ConsulClient(args[0]);

        // Iterate over all subdirectories within the top-level properties directory.  Each one corresponds
        // to a particular environment (dev, prod, etc).
        final File propertiesDirectory = new File("properties");
        Arrays.stream(propertiesDirectory.listFiles())
                .filter(File::isDirectory)
                .forEach(environmentDirectory -> {

                    // For each environment, purge that path within Consul and then iterate over all properties
                    // files.  Each one corresponds to a particular application/service in that environment.
                    final String environment = environmentDirectory.getName();
                    client.deleteKVValues(environment);
                    Arrays.stream(environmentDirectory.listFiles())
                            .filter(file -> file.getName().endsWith(".properties"))
                            .forEach(file -> {

                                // For each application, load its properties from the file and write them to Consul.
                                final String application = file.getName().replaceAll(".properties", "");
                                final Properties properties = new Properties();
                                try {
                                    properties.load(new FileInputStream(file));
                                    final Enumeration propertyNames = properties.propertyNames();
                                    while (propertyNames.hasMoreElements()) {
                                        final String key = propertyNames.nextElement().toString();
                                        final String value = properties.getProperty(key);
                                        final String path = String.format("%s/%s/%s", environment, application, key);
                                        System.out.printf("Writing to consul: %s=%s%n", path, value);
                                        client.setKVValue(path, value);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                });
    }

}

