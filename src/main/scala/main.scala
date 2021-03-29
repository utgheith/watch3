import java.nio.file.{ClosedWatchServiceException, Path, StandardWatchEventKinds, WatchEvent, WatchKey}
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable
import scala.jdk.CollectionConverters._

case class Locked[A <: AnyRef](it : A) {
  def apply[B](f: A => B): B = it.synchronized {
    f(it)
  }
}

object main {

  private val names = (0 to 3).map(n => f"$n%04d")
  private val n_events = names.size * 2 + 1

  def thread(f: => Any) : Thread = {
    val t = new Thread {
      override def run(): Unit = {
        f
      }
    }
    t.setDaemon(false)
    t.start()
    t
  }

  def main(args: Array[String]): Unit = {
    val data = os.pwd / "data"


    os.remove.all(data)
    os.makeDir(data)




    (0 to 1000000).foreach { i =>
      println(i)

      val watch_service = data.wrapped.getFileSystem.newWatchService()

      data.wrapped.register(watch_service,StandardWatchEventKinds.ENTRY_DELETE)

      val root_dir = data / "r"
      os.makeDir(root_dir)

      root_dir.wrapped.register(watch_service, StandardWatchEventKinds.ENTRY_DELETE)

      names.foreach { s =>
        val dir_name = s"d_$s"
        val dir_path = root_dir /dir_name
        val file_path = dir_path / s"file_$s"

        os.makeDir.all(dir_path)
        os.write(file_path, s)

        dir_path.wrapped.register(watch_service, StandardWatchEventKinds.ENTRY_DELETE)
      }



      val changed = Locked(mutable.Set[os.Path]())
      val events = Locked(mutable.Buffer[WatchEvent[_]]())

      val done = new AtomicBoolean(false)

      val child_thread = thread {
        try {
          while (!done.get()) {
            val e = watch_service.poll(1, TimeUnit.SECONDS)

            if (e != null) {
              //println(e.watchable())
              //println(e.pollEvents())
              e.pollEvents.asScala.foreach { we =>
                events(_.append(we))
                val kind = we.kind()

                if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                  val dir = os.Path(e.watchable.asInstanceOf[Path])
                  val ctx = os.SubPath(we.context().asInstanceOf[Path])
                  val path = dir / ctx

                  if (ctx != null) {
                    println(s"delete $path")
                    changed(_.add(path))
                  } else {
                    println(s"???????????? context is null in $we")
                  }
                } else {
                  println(s"????????????? unexpected kind in $we")
                }
              }
            }
          }
        } catch {
          case _ : ClosedWatchServiceException =>
        }
      }

      os.remove.all(root_dir)

      var iter = 0

      while ((changed(_.size) != n_events) && (iter < 10000)) {
        Thread.sleep(1)
        iter += 1
      }

      done.set(true)

      val changed_list = changed(_.toList).sorted
      val event_list = events(_.toList)
      if (changed_list.size != n_events) {
        println(s"size = ${changed_list.size}")
        changed_list.foreach(p => println(s" $p"))
        println("raw events")
        event_list.foreach { e =>
          println(s"  $e")
        }
        println("delete events")

        return
      }

      watch_service.close()
      child_thread.join()
      
    }
  }

}
