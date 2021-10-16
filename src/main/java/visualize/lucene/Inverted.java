package visualize.lucene;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Logger;

public class Inverted {
    private static final Logger logger = Logger.getLogger(String.valueOf(Inverted.class));

    public static void main(String[] args) throws IOException {
        Properties props = loadProps();

    }

    private static Properties loadProps() {
        try (InputStream input =
                     new FileInputStream(
                             "src/main/resources/application.properties")) {
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);
            return prop;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (Properties) Collections.emptyMap();
    }
}
