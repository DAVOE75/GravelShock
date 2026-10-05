import java.io.File
import java.net.URLClassLoader

fun main() {
    val file = File("app/libs/karoo-ext-unpacked/classes.zip")
    val url = file.toURI().toURL()
    val cl = URLClassLoader(arrayOf(url))
    val clazz = cl.loadClass("io.hammerhead.karooext.models.KarooEffect")
    println("KarooEffect is an interface/sealed class. Known sub-classes:")
    for (c in clazz.declaredClasses) {
        println(c.name)
    }
}
