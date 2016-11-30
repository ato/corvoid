package corvoid;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CorvoidTest {
    @Test
    public void capify() throws Exception {
        assertEquals("HelloThereWorld", Corvoid.capify("hello_there-world"));
    }
}