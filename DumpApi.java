import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class DumpApi {
    public static void main(String[] args) throws Exception {
        System.out.println("--- KarooSystemService ---");
        Class<?> kss = Class.forName("io.hammerhead.karooext.KarooSystemService");
        for (Method m : kss.getDeclaredMethods()) {
            System.out.println(m);
        }
        
        System.out.println("--- DataType ---");
        Class<?> dt = Class.forName("io.hammerhead.karooext.models.DataType");
        for (Constructor<?> c : dt.getConstructors()) {
            System.out.println(c);
        }
        
        System.out.println("--- DataPoint ---");
        Class<?> dp = Class.forName("io.hammerhead.karooext.models.DataPoint");
        for (Constructor<?> c : dp.getConstructors()) {
            System.out.println(c);
        }
    }
}
