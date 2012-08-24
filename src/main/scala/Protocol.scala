package com.klout.akkamemcache

import akka.actor.IO
import akka.util.ByteString


object Iteratees {
    import Constants._

    def ascii(bytes: ByteString): String = bytes.decodeString("US-ASCII").trim

    val readLine: IO.Iteratee[Option[String]] = IO take 5 flatMap {
        case Value => processValue map (Some(_))
        case other => IO takeUntil CRLF map (_ => Some(other.toString))
    }

    val processValue: IO.Iteratee[String] =
        for {
            key <- IO takeUntil Space
            _ <- IO takeUntil Space
            length <- IO takeUntil Space map (ascii(_).toInt)
            _ <- IO takeUntil CRLF
            value <- IO take length
            _ <- IO takeUntil CRLF
            _ <- IO takeUntil CRLF
        } yield "key: [%s], length: [%d], value: [%s]" format (key, length, value)


    val processLine: IO.Iteratee[Unit] = readLine map {
        case Some(thing) => println("something: "  + thing)
        case _ => println("error")
    }


}

object Constants {

    val Space = ByteString(" ")

    val CRLF = ByteString("\r\n")

    val Value = ByteString("VALUE")

    val End = ByteString("END")



}

object Protocol {
    import Constants._

    trait Command {
        def toByteString: ByteString
    }
    case class SetCommand(key: String, payload: ByteString, ttl: Long) extends Command {
        override def toByteString = ByteString("set " + key + " " + ttl + " 0 " + payload.size) ++ CRLF ++ payload ++ CRLF
    }

    case class DeleteCommand(key: String) extends Command {
        override def toByteString = ByteString("delete "+key ) ++ CRLF
    }

    case class GetCommand(key: String) extends Command {
        override def toByteString = ByteString("get " + key) ++ CRLF
    }

}
