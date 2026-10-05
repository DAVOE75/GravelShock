package com.gravelshock.karoo;

import org.junit.Test;
import java.lang.reflect.Method;
import io.hammerhead.karooext.KarooSystemService;

public class DumpApiTest {
    @Test
    public void dump() {
        System.out.println("--- KarooSystemService Methods ---");
        for (Method m : KarooSystemService.class.getMethods()) {
            System.out.println(m.toString());
        }
    }
}
