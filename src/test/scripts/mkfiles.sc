import java.nio.file.{Files, Paths}
import scala.util.{Random, Using}

for (i <- 0 until 10000) {
  Using(Files.newBufferedWriter(Paths.get(String.format("%04d.txt", i)))) { writer =>
    val random = new Random(i)
    for (_ <- 1 to 25000) {
      for (_ <- 1 to 40) {
        writer.write(random.nextPrintableChar())
      }
      writer.write('\n')
    }
  }
  println(i)
}