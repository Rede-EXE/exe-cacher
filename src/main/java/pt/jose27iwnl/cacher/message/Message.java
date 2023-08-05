package pt.jose27iwnl.cacher.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String id;
    private Map<String, Object> data;
}
