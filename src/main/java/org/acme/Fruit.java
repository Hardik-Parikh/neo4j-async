package org.acme;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.neo4j.driver.types.Node;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fruit {

    public Long id;

    public String name;

    public static Fruit from(Node node) {
        return new Fruit(node.id(), node.get("name").asString());
    }
}
