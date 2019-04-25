package um.project.titanlander.Debugger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class DataLogger {

    public Map<Double, Map<String, Double>> data = new LinkedHashMap<>();

    public DataLogger() {}

    public Map<Double, Map<String, Double>> getData() {
        return data;
    }

    public void add(double height, String system, double value) {
        if(data.containsKey(height)) {
            this.data.get(height).put(system, value);
        } else {
            this.data.put(height, new HashMap<String, Double>() {{
                this.put(system, value);
            }});
        }
    }

}
